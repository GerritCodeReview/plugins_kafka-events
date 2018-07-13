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

package com.googlesource.gerrit.plugins.kafka.message;

import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import com.googlesource.gerrit.plugins.kafka.config.KafkaProperties;
import com.googlesource.gerrit.plugins.kafka.session.Session;
import com.googlesource.gerrit.plugins.kafka.session.SessionFactoryProvider;

public class Publisher implements EventListener {

  private final Session session;
  private final KafkaProperties properties;
  private final Gson gson;
  private boolean available = true;

  @Inject
  public Publisher(
      SessionFactoryProvider sessionFactoryProvider,
      Gson gson,
      @Assisted KafkaProperties properties) {
    this.session = sessionFactoryProvider.get().create(properties);
    this.properties = properties;
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

  public Session getSession() {
    return session;
  }

  public KafkaProperties getProperties() {
    return properties;
  }

  public String getName() {
    return "Kafka";
  }
}
