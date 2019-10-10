// Copyright (C) 2019 The Android Open Source Project
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
package com.googlesource.gerrit.plugins.kafka.subscribe;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.gerritforge.gerrit.eventbroker.EventMessage;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.server.config.GerritServerId;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.kafka.config.KafkaSubscriberProperties;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.Deserializer;

public class KafkaEventSubscriber {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Consumer<byte[], byte[]> consumer;
  private final OneOffRequestContext oneOffCtx;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  private final Deserializer<EventMessage> valueDeserializer;
  private final KafkaSubscriberProperties configuration;
  private final KafkaEventSubscriberMetrics subscriberMetrics;

  private java.util.function.Consumer<EventMessage> messageProcessor;

  private String topic;

  @Inject
  public KafkaEventSubscriber(
      KafkaSubscriberProperties configuration,
      KafkaConsumerFactory consumerFactory,
      Deserializer<byte[]> keyDeserializer,
      Deserializer<EventMessage> valueDeserializer,
      @GerritServerId String instanceId,
      OneOffRequestContext oneOffCtx,
      KafkaEventSubscriberMetrics subscriberMetrics) {

    this.configuration = configuration;
    this.oneOffCtx = oneOffCtx;
    this.subscriberMetrics = subscriberMetrics;

    final ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(KafkaEventSubscriber.class.getClassLoader());
      this.consumer = consumerFactory.create(keyDeserializer, UUID.fromString(instanceId));
    } finally {
      Thread.currentThread().setContextClassLoader(previousClassLoader);
    }
    this.valueDeserializer = valueDeserializer;
  }

  public void subscribe(String topic, java.util.function.Consumer<EventMessage> messageProcessor) {
    try {
      logger.atInfo().log(
          "Kafka consumer subscribing to topic alias [%s] for event topic [%s]", topic, topic);
      consumer.subscribe(Collections.singleton(topic));
      this.messageProcessor = messageProcessor;
      this.topic = topic;
      while (!closed.get()) {
        ConsumerRecords<byte[], byte[]> consumerRecords =
            consumer.poll(Duration.ofMillis(configuration.getPollingInterval()));
        consumerRecords.forEach(
            consumerRecord -> {
              try (ManualRequestContext ctx = oneOffCtx.open()) {
                EventMessage event =
                    valueDeserializer.deserialize(consumerRecord.topic(), consumerRecord.value());
                messageProcessor.accept(event);
              } catch (Exception e) {
                logger.atSevere().withCause(e).log(
                    "Malformed event '%s': [Exception: %s]",
                    new String(consumerRecord.value(), UTF_8));
                subscriberMetrics.incrementSubscriberFailedToConsumeMessage();
              }
            });
      }
    } catch (WakeupException e) {
      // Ignore exception if closing
      if (!closed.get()) throw e;
    } catch (Exception e) {
      subscriberMetrics.incrementSubscriberFailedToPollMessages();
      throw e;
    } finally {
      consumer.close();
    }
  }

  public void shutdown() {
    closed.set(true);
    consumer.wakeup();
  }

  public java.util.function.Consumer<EventMessage> getMessageProcessor() {
    return messageProcessor;
  }

  public String getTopic() {
    return topic;
  }
}
