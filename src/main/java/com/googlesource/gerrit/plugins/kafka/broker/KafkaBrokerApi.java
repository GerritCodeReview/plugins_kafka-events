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

package com.googlesource.gerrit.plugins.kafka.broker;

import com.gerritforge.gerrit.eventbroker.BrokerApi;
import com.gerritforge.gerrit.eventbroker.EventConsumer;
import com.gerritforge.gerrit.eventbroker.SourceAwareEventWrapper;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.events.Event;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.googlesource.gerrit.plugins.kafka.consumer.KafkaEventSubscriber;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class KafkaBrokerApi implements BrokerApi, LifecycleListener {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final BrokerPublisher publisher;
  private final Provider<KafkaEventSubscriber> subscriberProvider;
  private List<KafkaEventSubscriber> subscribers;
  private ExecutorService executor;
  DynamicSet<EventConsumer> consumers;

  @Inject
  public KafkaBrokerApi(
      BrokerPublisher publisher,
      Provider<KafkaEventSubscriber> subscriberProvider,
      @ConsumerExecutor ExecutorService executor,
      DynamicSet<EventConsumer> consumers) {
    this.publisher = publisher;
    this.subscriberProvider = subscriberProvider;
    this.executor = executor;
    this.consumers = consumers;
    subscribers = new ArrayList<>();
  }

  @Override
  public boolean send(String topic, Event event) {
    return publisher.publish(topic, event);
  }

  @Override
  public void receiveAsync(String topic, Consumer<SourceAwareEventWrapper> consumer) {
    executor.execute(new ReceiverJob(topic, consumer));
  }

  @Override
  public void reconnect(List<EventConsumer> consumers) {
    for (KafkaEventSubscriber subscriber : subscribers) {
      subscriber.shutdown();
    }
    consumers.forEach(consumer -> receiveAsync(consumer.getTopic(), consumer.getConsumer()));
  }

  @Override
  public void start() {
    consumers.forEach(consumer -> receiveAsync(consumer.getTopic(), consumer.getConsumer()));
  }

  @Override
  public void stop() {
    for (KafkaEventSubscriber subscriber : subscribers) {
      subscriber.shutdown();
    }
    logger.atInfo().log("shutting down consumers");
    executor.shutdown();
  }

  private class ReceiverJob implements Runnable {
    private String topic;
    private Consumer<SourceAwareEventWrapper> consumer;

    public ReceiverJob(String topic, Consumer<SourceAwareEventWrapper> consumer) {
      this.topic = topic;
      this.consumer = consumer;
    }

    @Override
    public void run() {
      KafkaEventSubscriber subscriber = subscriberProvider.get();
      synchronized (subscribers) {
        subscribers.add(subscriber);
      }
      subscriber.subscribe(EventTopic.of(topic), consumer);
    }
  }
}
