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
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.SocatContainer;
import org.testcontainers.containers.wait.internal.ExternalPortListeningCheck;
import org.testcontainers.containers.wait.internal.InternalCommandPortListeningCheck;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

@NoHttpd
@TestPlugin(name = "kafka-events", sysModule = "com.googlesource.gerrit.plugins.kafka.Module")
public class EventConsumerIT extends LightweightPluginDaemonTest {
  static final long KAFKA_POLL_TIMEOUT = 10000L;
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private KafkaContainer kafka;

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
          new InternalCommandPortListeningCheck(waitStrategyTarget, internalPorts);

      Callable<Boolean> externalCheck =
          new ExternalPortListeningCheck(waitStrategyTarget, externalLivenessCheckPorts);

      try {
        Unreliables.retryUntilTrue(
            (int) startupTimeout.getSeconds(),
            TimeUnit.SECONDS,
            () ->
                getRateLimiter().getWhenReady(() -> internalCheck.call() && externalCheck.call()));

      } catch (TimeoutException e) {
        throw new ContainerLaunchException(
            "Timed out waiting for container port to open ("
                + waitStrategyTarget.getContainerIpAddress()
                + " ports: "
                + externalLivenessCheckPorts
                + " should be listening)");
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
