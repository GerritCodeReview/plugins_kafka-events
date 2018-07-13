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

import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.kafka.config.KafkaProperties;
import com.googlesource.gerrit.plugins.kafka.message.Publisher;
import com.googlesource.gerrit.plugins.kafka.message.PublisherFactory;
import com.googlesource.gerrit.plugins.kafka.worker.DefaultEventWorker;
import com.googlesource.gerrit.plugins.kafka.worker.EventWorker;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class Manager implements LifecycleListener {

  private final EventWorker defaultEventWorker;
  private final PublisherFactory publisherFactory;
  private final KafkaProperties properties;
  private final List<Publisher> publisherList = new ArrayList<>();

  @Inject
  public Manager(
      DefaultEventWorker defaultEventWorker,
      PublisherFactory publisherFactory,
      KafkaProperties properties) {
    this.defaultEventWorker = defaultEventWorker;
    this.publisherFactory = publisherFactory;
    this.properties = properties;
  }

  @Override
  public void start() {
    Publisher publisher = publisherFactory.create(properties);
    publisher.start();
    defaultEventWorker.addPublisher(publisher);
    publisherList.add(publisher);
  }

  @Override
  public void stop() {
    for (Publisher publisher : publisherList) {
      publisher.stop();
      defaultEventWorker.removePublisher(publisher);
    }
    publisherList.clear();
  }
}
