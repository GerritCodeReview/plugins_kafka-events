// Copyright (C) 2020 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.kafka.publish;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Futures;
import com.googlesource.gerrit.plugins.kafka.config.KafkaProperties;
import com.googlesource.gerrit.plugins.kafka.session.KafkaProducerProvider;
import com.googlesource.gerrit.plugins.kafka.session.KafkaSession;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KafkaSessionTest {
  KafkaSession objectUnderTest;
  @Mock KafkaProducer<String, String> kafkaProducer;
  @Mock KafkaProducerProvider producerProvider;
  @Mock KafkaProperties properties;
  @Mock KafkaEventsPublisherMetrics publisherMetrics;
  @Captor ArgumentCaptor<Callback> callbackCaptor;

  RecordMetadata recordMetadata;
  String message = "sample_message";
  private String topic = "index";

  @Before
  public void setUp() {
    when(producerProvider.get()).thenReturn(kafkaProducer);
    when(properties.getTopic()).thenReturn(topic);

    recordMetadata = new RecordMetadata(new TopicPartition(topic, 0), 0L, 0L, 0L, 0L, 0, 0);

    objectUnderTest = new KafkaSession(producerProvider, properties, publisherMetrics);
    objectUnderTest.connect();
  }

  @Test
  public void shouldIncrementBrokerMetricCounterWhenMessagePublishedInSyncMode() {
    when(properties.isSendAsync()).thenReturn(false);
    when(kafkaProducer.send(any())).thenReturn(Futures.immediateFuture(recordMetadata));
    objectUnderTest.publish(message);
    verify(publisherMetrics, only()).incrementBrokerPublishedMessage();
  }

  @Test
  public void shouldIncrementBrokerFailedMetricCounterWhenMessagePublishingFailedInSyncMode() {
    when(properties.isSendAsync()).thenReturn(false);
    when(kafkaProducer.send(any())).thenReturn(Futures.immediateFailedFuture(new Exception()));
    objectUnderTest.publish(message);
    verify(publisherMetrics, only()).incrementBrokerFailedToPublishMessage();
  }

  @Test
  public void shouldIncrementBrokerFailedMetricCounterWhenUnexpectedExceptionInSyncMode() {
    when(properties.isSendAsync()).thenReturn(false);
    when(kafkaProducer.send(any())).thenThrow(new RuntimeException("Unexpected runtime exception"));
    try {
      objectUnderTest.publish(message);
    } catch (RuntimeException e) {
      // expected
    }
    verify(publisherMetrics, only()).incrementBrokerFailedToPublishMessage();
  }

  @Test
  public void shouldIncrementBrokerMetricCounterWhenMessagePublishedInAsyncMode() {
    when(properties.isSendAsync()).thenReturn(true);
    when(kafkaProducer.send(any(), any())).thenReturn(Futures.immediateFuture(recordMetadata));

    objectUnderTest.publish(message);

    verify(kafkaProducer).send(any(), callbackCaptor.capture());
    callbackCaptor.getValue().onCompletion(recordMetadata, null);
    verify(publisherMetrics, only()).incrementBrokerPublishedMessage();
  }

  @Test
  public void shouldIncrementBrokerFailedMetricCounterWhenMessagePublishingFailedInAsyncMode() {
    when(properties.isSendAsync()).thenReturn(true);
    when(kafkaProducer.send(any(), any()))
        .thenReturn(Futures.immediateFailedFuture(new Exception()));

    objectUnderTest.publish(message);

    verify(kafkaProducer).send(any(), callbackCaptor.capture());
    callbackCaptor.getValue().onCompletion(null, new Exception());
    verify(publisherMetrics, only()).incrementBrokerFailedToPublishMessage();
  }

  @Test
  public void shouldIncrementBrokerFailedMetricCounterWhenUnexpectedExceptionInAsyncMode() {
    when(properties.isSendAsync()).thenReturn(true);
    when(kafkaProducer.send(any(), any()))
        .thenThrow(new RuntimeException("Unexpected runtime exception"));
    try {
      objectUnderTest.publish(message);
    } catch (RuntimeException e) {
      // expected
    }
    verify(publisherMetrics, only()).incrementBrokerFailedToPublishMessage();
  }
}
