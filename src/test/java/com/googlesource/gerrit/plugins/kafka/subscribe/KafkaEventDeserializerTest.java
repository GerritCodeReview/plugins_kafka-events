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

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.gerritforge.gerrit.eventbroker.EventGsonProvider;
import com.gerritforge.gerrit.eventbroker.EventMessage;
import com.google.gson.Gson;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

public class KafkaEventDeserializerTest {
  private KafkaEventDeserializer deserializer;

  @Before
  public void setUp() {
    final Gson gson = new EventGsonProvider().get();
    deserializer = new KafkaEventDeserializer(gson);
  }

  @Test
  public void kafkaEventDeserializerShouldParseAKafkaEvent() {
    final UUID eventId = UUID.randomUUID();
    final String eventType = "event-type";
    final UUID sourceInstanceId = UUID.randomUUID();
    final long eventCreatedOn = 10L;
    final String eventJson =
        String.format(
            "{ "
                + "\"header\": { \"eventId\": \"%s\", \"eventType\": \"%s\", \"sourceInstanceId\": \"%s\", \"eventCreatedOn\": %d },"
                + "\"body\": { \"type\": \"project-created\" }"
                + "}",
            eventId, eventType, sourceInstanceId, eventCreatedOn);
    final EventMessage event = deserializer.deserialize("ignored", eventJson.getBytes(UTF_8));

    assertThat(event.getHeader().eventId).isEqualTo(eventId);
    assertThat(event.getHeader().sourceInstanceId).isEqualTo(sourceInstanceId);
  }

  @Test(expected = RuntimeException.class)
  public void kafkaEventDeserializerShouldFailForInvalidJson() {
    deserializer.deserialize("ignored", "this is not a JSON string".getBytes(UTF_8));
  }

  @Test(expected = RuntimeException.class)
  public void kafkaEventDeserializerShouldFailForInvalidObjectButValidJSON() {
    deserializer.deserialize("ignored", "{}".getBytes(UTF_8));
  }
}
