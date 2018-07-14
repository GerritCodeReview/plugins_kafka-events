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

package com.googlesource.gerrit.plugins.kafka.config;

import org.apache.kafka.common.serialization.StringSerializer;
import com.google.common.base.CaseFormat;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class KafkaProperties extends java.util.Properties {
  private static final long serialVersionUID = 0L;

  private final String topic;

  @Inject
  public KafkaProperties(PluginConfigFactory configFactory, @PluginName String pluginName) {
    super();
    setDefaults();
    PluginConfig fromGerritConfig = configFactory.getFromGerritConfig(pluginName);
    topic = fromGerritConfig.getString("topic", "gerrit");
    applyConfig(fromGerritConfig);
  }

  private void setDefaults() {
    put("acks", "all");
    put("retries", 0);
    put("batch.size", 16384);
    put("linger.ms", 1);
    put("buffer.memory", 33554432);
    put("key.serializer", StringSerializer.class.getName());
    put("value.serializer", StringSerializer.class.getName());
  }

  private void applyConfig(PluginConfig config) {
    for (String name : config.getNames()) {
      Object value = config.getString(name);
      String propName =
          CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, name).replaceAll("-", ".");
      put(propName, value);
    }
  }

  public String getTopic() {
    return topic;
  }
}
