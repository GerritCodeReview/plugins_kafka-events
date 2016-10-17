// Copyright (C) 2016 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.kafka.session.type;

import com.googlesource.gerrit.plugins.kafka.config.KafkaProperties;
import com.googlesource.gerrit.plugins.kafka.session.Session;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KafkaSession implements Session {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(KafkaSession.class);
  private final KafkaProperties properties;
  private volatile Producer<String, String> producer;

  public KafkaSession(KafkaProperties properties) {
    this.properties = properties;
  }

  @Override
  public boolean isOpen() {
    if (producer != null) {
      return true;
    }
    return false;
  }

  @Override
  public void connect() {
    if (isOpen()) {
      LOGGER.debug("Already connected.");
      return;
    }

    LOGGER.info("Connect to {}...",
        properties.getProperty("bootstrap.servers"));
    setConnectionClassLoader();
    producer = new KafkaProducer<>(properties);
    LOGGER.info("Connection established.");
  }

  private void setConnectionClassLoader() {
    Thread.currentThread().setContextClassLoader(
        KafkaSession.class.getClassLoader());
  }

  @Override
  public void disconnect() {
    LOGGER.info("Disconnecting...");
    if (producer != null) {
      LOGGER.info("Closing Producer {}...", producer);
      producer.close();
    }
    producer = null;
  }

  @Override
  public void publish(String messageBody) {
    producer.send(new ProducerRecord<>(properties.getTopic(), "" + System.nanoTime(),
        messageBody));
  }
}
