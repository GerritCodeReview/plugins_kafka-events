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

package com.googlesource.gerrit.plugins.kafka.publish;

import com.gerritforge.gerrit.eventbroker.SourceAwareEventWrapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.gerrit.server.config.GerritServerId;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.kafka.broker.MessageLogger;
import com.googlesource.gerrit.plugins.kafka.broker.MessageLogger.Direction;
import com.googlesource.gerrit.plugins.kafka.session.KafkaSession;
import java.util.UUID;

@Singleton
public class KafkaPublisher implements EventListener {

  private final KafkaSession session;
  private final Gson gson;
  private final UUID instanceId;
  private final MessageLogger msgLog;

  @Inject
  public KafkaPublisher(
      KafkaSession kafkaSession,
      Gson gson,
      @GerritServerId String instanceId,
      MessageLogger msgLog) {
    this.session = kafkaSession;
    this.gson = gson;
    this.instanceId = UUID.fromString(instanceId);
    this.msgLog = msgLog;
  }

  public void start() {
    if (!session.isOpen()) {
      session.connect();
    }
  }

  public void stop() {
    session.disconnect();
  }

  @Override
  public void onEvent(Event event) {
    if (session.isOpen()) {
      session.publish(gson.toJson(event));
    }
  }

  public boolean publish(String topic, Event event) {
    SourceAwareEventWrapper brokerEvent = toBrokerEvent(event);
    Boolean eventPublished = session.publish(topic, getPayload(brokerEvent));
    if (eventPublished) {
      msgLog.log(Direction.PUBLISH, brokerEvent);
    }
    return eventPublished;
  }

  private String getPayload(SourceAwareEventWrapper event) {
    return gson.toJson(event);
  }

  private SourceAwareEventWrapper toBrokerEvent(Event event) {
    JsonObject body = eventToJson(event);
    return new SourceAwareEventWrapper(
        new SourceAwareEventWrapper.EventHeader(
            UUID.randomUUID(), event.getType(), instanceId, event.eventCreatedOn),
        body);
  }

  @VisibleForTesting
  public JsonObject eventToJson(Event event) {
    return gson.toJsonTree(event).getAsJsonObject();
  }
}
