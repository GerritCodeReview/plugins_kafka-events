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

import java.util.ArrayList;
import java.util.List;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.kafka.config.KafkaProperties;
import com.googlesource.gerrit.plugins.kafka.message.Publisher;
import com.googlesource.gerrit.plugins.kafka.message.PublisherFactory;

@Singleton
public class Manager implements LifecycleListener {

  private final PublisherFactory publisherFactory;
  private final KafkaProperties properties;
  private final List<Publisher> publisherList = new ArrayList<>();

  @Inject
  public Manager(
      PublisherFactory publisherFactory,
      KafkaProperties properties) {
    this.publisherFactory = publisherFactory;
    this.properties = properties;
  }

  @Override
  public void start() {
    Publisher publisher = publisherFactory.create(properties);
    publisher.start();
    publisherList.add(publisher);
  }

  @Override
  public void stop() {
    for (Publisher publisher : publisherList) {
      publisher.stop();
    }
    publisherList.clear();
  }
}
