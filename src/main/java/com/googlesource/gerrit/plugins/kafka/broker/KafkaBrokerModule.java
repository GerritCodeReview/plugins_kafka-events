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
import com.gerritforge.gerrit.eventbroker.SourceAwareEventWrapper;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.gson.Gson;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.googlesource.gerrit.plugins.kafka.consumer.KafkaEventDeserializer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.Deserializer;

@Singleton
public class KafkaBrokerModule extends LifecycleModule {

  @Override
  protected void configure() {
    bind(new TypeLiteral<Deserializer<byte[]>>() {}).toInstance(new ByteArrayDeserializer());
    bind(new TypeLiteral<Deserializer<SourceAwareEventWrapper>>() {})
        .to(KafkaEventDeserializer.class);

    listener().to(BrokerPublisher.class);
    bind(BrokerSession.class).to(KafkaSession.class);
    DynamicItem.bind(binder(), BrokerApi.class).to(KafkaBrokerApi.class).in(Scopes.SINGLETON);
    listener().to(KafkaBrokerApi.class);

    listener().to(Log4jMessageLogger.class);
    bind(MessageLogger.class).to(Log4jMessageLogger.class);

    bind(Gson.class)
        .annotatedWith(BrokerGson.class)
        .toProvider(GsonProvider.class)
        .in(Singleton.class);
  }
}
