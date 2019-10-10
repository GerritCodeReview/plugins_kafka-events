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

package com.googlesource.gerrit.plugins.kafka.config;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import java.util.Properties;
import java.util.UUID;

public class KafkaSubscriberProperties extends KafkaProperties {
  private static final long serialVersionUID = 1L;
  private static final String DEFAULT_POLLING_INTERVAL_MS = "1000";

  private final Integer pollingInterval;

  @Inject
  public KafkaSubscriberProperties(
      PluginConfigFactory configFactory, @PluginName String pluginName) {
    super(configFactory, pluginName);

    this.pollingInterval =
        Integer.parseInt(getProperty("pollingIntervalMs", DEFAULT_POLLING_INTERVAL_MS));
  }

  public Properties initPropsWith(UUID instanceId) {
    String groupId = getProperty("groupId", instanceId.toString());
    this.put("group.id", groupId);

    return this;
  }

  public Integer getPollingInterval() {
    return pollingInterval;
  }
}
