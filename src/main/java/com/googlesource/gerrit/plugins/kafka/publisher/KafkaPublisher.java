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

package com.googlesource.gerrit.plugins.kafka.publisher;

import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.googlesource.gerrit.plugins.kafka.session.KafkaSession;

@Singleton
public class KafkaPublisher implements EventListener {

  private final KafkaSession session;
  private final Gson gson;
  private boolean available = true;

  @Inject
  public KafkaPublisher(
      KafkaSession kafkaSession,
      Gson gson) {
    this.session = kafkaSession;
    this.gson = gson;
  }

  public void start() {
    if (!session.isOpen()) {
      session.connect();
      available = true;
    }
  }

  public void stop() {
    session.disconnect();
    available = false;
  }

  @Override
  public void onEvent(Event event) {
    if (available && session.isOpen()) {
      session.publish(gson.toJson(event));
    }
  }

  public void enable() {
    available = true;
  }

  public void disable() {
    available = false;
  }

  public boolean isEnabled() {
    return available;
  }

  public String getName() {
    return "Kafka";
  }
}
