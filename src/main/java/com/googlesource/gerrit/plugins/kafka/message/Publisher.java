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

import com.google.gerrit.server.events.EventListener;

import com.googlesource.gerrit.plugins.kafka.config.KafkaProperties;
import com.googlesource.gerrit.plugins.kafka.session.Session;

public interface Publisher extends EventListener {
  void start();
  void stop();
  void enable();
  void disable();
  boolean isEnabled();
  Session getSession();
  KafkaProperties getProperties();
  String getName();
}