// Copyright (C) 2018 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.kafka;

import static com.google.common.truth.Truth.assertThat;
import static java.lang.String.format;
import static org.junit.Assert.fail;

import com.gerritforge.gerrit.eventbroker.EventGsonProvider;
import com.google.common.collect.Iterables;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.acceptance.GerritConfig;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.NoHttpd;
import com.google.gerrit.acceptance.PushOneCommit;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.UseLocalDisk;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.common.ChangeMessageInfo;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.Event;
import com.google.gson.Gson;
import com.googlesource.gerrit.plugins.kafka.config.KafkaProperties;
import java.io.IOException;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.ExecInContainerPattern;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.SocatContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

@NoHttpd
@TestPlugin(name = "kafka-events", sysModule = "com.googlesource.gerrit.plugins.kafka.Module")
public class EventConsumerIT extends LightweightPluginDaemonTest {
  static final long KAFKA_POLL_TIMEOUT = 10000L;
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private KafkaContainer kafka;

  public static class MyInternalCommandPortListeningCheck
      implements java.util.concurrent.Callable<Boolean> {
    private final WaitStrategyTarget waitStrategyTarget;
    private final Set<Integer> internalPorts;

    public MyInternalCommandPortListeningCheck(
        WaitStrategyTarget waitStrategyTarget, Set<Integer> internalPorts) {
      super();
      this.waitStrategyTarget = waitStrategyTarget;
      this.internalPorts = internalPorts;
    }

    @Override
    public Boolean call() {
      String command = "true";

      for (int internalPort : internalPorts) {
        command += " && ";
        command += " (";
        command += format("cat /proc/net/tcp* | awk '{print $2}' | grep -i ':0*%x'", internalPort);
        command += " || ";
        command += format("nc -vz -w 1 localhost %d", internalPort);
        command += " || ";
        command += format("/bin/bash -c '</dev/tcp/localhost/%d'", internalPort);
        command += ")";
      }

      Instant before = Instant.now();
      try {
        logger.atInfo().log(
            "Checking for internal ports %s by running '%s'", internalPorts, command);
        ExecResult result =
            ExecInContainerPattern.execInContainer(
                waitStrategyTarget.getContainerInfo(), "/bin/sh", "-c", command);
        logger.atInfo().log(
            "Check for %s took %s", internalPorts, Duration.between(before, Instant.now()));
        return result.getExitCode() == 0;
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }
  }

  public static class MyExternalPortListeningCheck implements Callable<Boolean> {
    private final ContainerState containerState;
    private final Set<Integer> externalLivenessCheckPorts;

    public MyExternalPortListeningCheck(
        ContainerState containerState, Set<Integer> externalLivenessCheckPorts) {
      super();
      this.containerState = containerState;
      this.externalLivenessCheckPorts = externalLivenessCheckPorts;
    }

    @Override
    public Boolean call() {
      String address = containerState.getContainerIpAddress();

      externalLivenessCheckPorts
          .parallelStream()
          .forEach(
              externalPort -> {
                try {
                  logger.atInfo().log(
                      "Trying to open a socket connection to %s:%d", address, externalPort);
                  new Socket(address, externalPort).close();
                  logger.atInfo().log(
                      "Socket connection to %s:%d *SUCCEEDED*", address, externalPort);
                } catch (IOException e) {
                  logger.atInfo().log("Socket connection to %s:%d *FAILED*", address, externalPort);
                  throw new IllegalStateException("Socket not listening yet: " + externalPort);
                }
              });
      return true;
    }
  }

  public static class MyHostPortWaitStrategy extends AbstractWaitStrategy {
    public static MyHostPortWaitStrategy INSTANCE = new MyHostPortWaitStrategy();

    @Override
    protected void waitUntilReady() {
      final Set<Integer> externalLivenessCheckPorts = getLivenessCheckPorts();
      if (externalLivenessCheckPorts.isEmpty()) {
        logger.atInfo().log(
            "Liveness check ports of {} is empty. Not waiting.",
            waitStrategyTarget.getContainerInfo().getName());
        return;
      }

      @SuppressWarnings("unchecked")
      List<Integer> exposedPorts = waitStrategyTarget.getExposedPorts();

      final Set<Integer> internalPorts = getInternalPorts(externalLivenessCheckPorts, exposedPorts);

      Callable<Boolean> internalCheck =
          new MyInternalCommandPortListeningCheck(waitStrategyTarget, internalPorts);

      Callable<Boolean> externalCheck =
          new MyExternalPortListeningCheck(waitStrategyTarget, externalLivenessCheckPorts);

      try {
        retryUntilTrue(
            (int) startupTimeout.getSeconds(),
            TimeUnit.SECONDS,
            () ->
                getRateLimiter().getWhenReady(() -> internalCheck.call() && externalCheck.call()));

      } catch (Exception e) {
        throw new ContainerLaunchException(
            "Timed out waiting for container port to open ("
                + waitStrategyTarget.getContainerIpAddress()
                + " ports: "
                + externalLivenessCheckPorts
                + " should be listening)");
      }
    }

    private void retryUntilTrue(int timeout, TimeUnit timeoutUnit, Callable<Boolean> predicate)
        throws Exception {
      long startTime = System.currentTimeMillis();
      long timeoutMsec = timeoutUnit.toMillis(timeout);

      while (!predicate.call()) {
        Thread.sleep(100L);
        if ((startTime + timeoutMsec) > System.currentTimeMillis()) {
          throw new Exception("Timeout waiting for condition");
        }
      }
    }

    private Set<Integer> getInternalPorts(
        Set<Integer> externalLivenessCheckPorts, List<Integer> exposedPorts) {
      return exposedPorts.stream()
          .filter(it -> externalLivenessCheckPorts.contains(waitStrategyTarget.getMappedPort(it)))
          .collect(Collectors.toSet());
    }
  }

  @Override
  public void setUpTestPlugin() throws Exception {
    try {
      try (SocatContainer proxy =
          new SocatContainer() {
            protected org.testcontainers.containers.wait.strategy.WaitStrategy getWaitStrategy() {
              return MyHostPortWaitStrategy.INSTANCE;
            }
          }) {
        proxy
            .withTarget(KafkaContainer.KAFKA_PORT, "localhost")
            .withTarget(KafkaContainer.ZOOKEEPER_PORT, "localhost")
            .start();

        kafka = new KafkaContainer();
        kafka.start();

        System.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
      }

    } catch (IllegalStateException e) {
      fail("Cannot start container. Is docker daemon running?");
    }

    super.setUpTestPlugin();
  }

  @Override
  public void tearDownTestPlugin() {
    super.tearDownTestPlugin();
    if (kafka != null) {
      kafka.stop();
    }
  }

  @Test
  @UseLocalDisk
  @GerritConfig(name = "plugin.kafka-events.groupId", value = "test-consumer-group")
  @GerritConfig(
      name = "plugin.kafka-events.keyDeserializer",
      value = "org.apache.kafka.common.serialization.StringDeserializer")
  @GerritConfig(
      name = "plugin.kafka-events.valueDeserializer",
      value = "org.apache.kafka.common.serialization.StringDeserializer")
  public void consumeEvents() throws Exception {
    PushOneCommit.Result r = createChange();

    ReviewInput in = ReviewInput.recommend();
    in.message = "LGTM";
    gApi.changes().id(r.getChangeId()).revision("current").review(in);
    List<ChangeMessageInfo> messages =
        new ArrayList<>(gApi.changes().id(r.getChangeId()).get().messages);
    assertThat(messages).hasSize(2);
    String expectedMessage = "Patch Set 1: Code-Review+1\n\nLGTM";
    assertThat(messages.get(1).message).isEqualTo(expectedMessage);

    List<String> events = new ArrayList<>();
    KafkaProperties kafkaProperties = kafkaProperties();
    try (Consumer<String, String> consumer = new KafkaConsumer<>(kafkaProperties)) {
      consumer.subscribe(Collections.singleton(kafkaProperties.getTopic()));
      ConsumerRecords<String, String> records = consumer.poll(KAFKA_POLL_TIMEOUT);
      for (ConsumerRecord<String, String> record : records) {
        events.add(record.value());
      }
    }

    // There are 6 events are received in the following order:
    // 1. refUpdate:        ref: refs/sequences/changes
    // 2. refUpdate:        ref: refs/changes/01/1/1
    // 3. refUpdate:        ref: refs/changes/01/1/meta
    // 4. patchset-created: ref: refs/changes/01/1/1
    // 5. refUpdate:        ref: refs/changes/01/1/meta
    // 6. comment-added:    ref: refs/heads/master

    assertThat(events).hasSize(6);
    String commentAddedEventJson = Iterables.getLast(events);

    Gson gson = new EventGsonProvider().get();
    Event event = gson.fromJson(commentAddedEventJson, Event.class);
    assertThat(event).isInstanceOf(CommentAddedEvent.class);

    CommentAddedEvent commentAddedEvent = (CommentAddedEvent) event;
    assertThat(commentAddedEvent.comment).isEqualTo(expectedMessage);
  }

  private KafkaProperties kafkaProperties() {
    return plugin.getSysInjector().getInstance(KafkaProperties.class);
  }
}
