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

package com.googlesource.gerrit.plugins.kafka;

import com.google.gerrit.common.EventListener;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import com.googlesource.gerrit.plugins.kafka.config.KafkaProperties;
import com.googlesource.gerrit.plugins.kafka.config.KafkaPropertiesProvider;
import com.googlesource.gerrit.plugins.kafka.message.GsonProvider;
import com.googlesource.gerrit.plugins.kafka.message.MessagePublisher;
import com.googlesource.gerrit.plugins.kafka.message.Publisher;
import com.googlesource.gerrit.plugins.kafka.message.PublisherFactory;
import com.googlesource.gerrit.plugins.kafka.session.KafkaSessionFactory;
import com.googlesource.gerrit.plugins.kafka.session.SessionFactoryProvider;
import com.googlesource.gerrit.plugins.kafka.worker.DefaultEventWorker;

class Module extends AbstractModule {

  @Override
  protected void configure() {
    bind(KafkaSessionFactory.class).toProvider(SessionFactoryProvider.class);

    install(new FactoryModuleBuilder().implement(Publisher.class, MessagePublisher.class).build(PublisherFactory.class));
    bind(KafkaProperties.class).toProvider(KafkaPropertiesProvider.class).in(Singleton.class);
    bind(Gson.class).toProvider(GsonProvider.class).in(Singleton.class);

    DynamicSet.bind(binder(), LifecycleListener.class).to(Manager.class);
    DynamicSet.bind(binder(), EventListener.class).to(DefaultEventWorker.class);
  }
}
