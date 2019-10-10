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

package com.googlesource.gerrit.plugins.kafka.api;

import com.gerritforge.gerrit.eventbroker.BrokerApi;
import com.gerritforge.gerrit.eventbroker.EventMessage;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.inject.Inject;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.googlesource.gerrit.plugins.kafka.subscribe.KafkaEventDeserializer;
import java.util.function.Consumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.Deserializer;

@Singleton
public class KafkaApiModule extends LifecycleModule {
  private Multimap<String, Consumer<EventMessage>> activeConsumers = ImmutableMultimap.of();

  @Inject(optional = true)
  public void setPreviousBrokerApi(DynamicItem<BrokerApi> previousBrokerApi) {
    if (previousBrokerApi != null && previousBrokerApi.get() != null) {
      this.activeConsumers = previousBrokerApi.get().consumersMap();
    }
  }

  @Override
  protected void configure() {
    bind(new TypeLiteral<Deserializer<byte[]>>() {}).toInstance(new ByteArrayDeserializer());
    bind(new TypeLiteral<Deserializer<EventMessage>>() {}).to(KafkaEventDeserializer.class);
    bind(new TypeLiteral<Multimap<String, Consumer<EventMessage>>>() {})
        .toInstance(activeConsumers);

    DynamicItem.bind(binder(), BrokerApi.class).to(KafkaBrokerApi.class).in(Scopes.SINGLETON);
    listener().to(KafkaBrokerApi.class);
  }
}
