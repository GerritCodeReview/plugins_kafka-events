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

import com.google.common.annotations.VisibleForTesting;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class KafkaSubscriberProperties extends KafkaProperties {
  private static final long serialVersionUID = 1L;
  private static final String DEFAULT_POLLING_INTERVAL_MS = "1000";
  private static final String DEFAULT_NUMBER_OF_SUBSCRIBERS = "6";

  private final Integer pollingInterval;
  private final String groupId;
  private final Integer numberOfSubscribers;

  @Inject
  public KafkaSubscriberProperties(
      PluginConfigFactory configFactory, @PluginName String pluginName) {
    super(configFactory, pluginName);

    this.pollingInterval =
        Integer.parseInt(getProperty("polling.interval.ms", DEFAULT_POLLING_INTERVAL_MS));
    this.groupId = getProperty("group.id");
    this.numberOfSubscribers =
        Integer.parseInt(getProperty("number.of.subscribers", DEFAULT_NUMBER_OF_SUBSCRIBERS));
  }

  @VisibleForTesting
  public KafkaSubscriberProperties(int pollingInterval, String groupId, int numberOfSubscribers) {
    super(true);
    this.pollingInterval = pollingInterval;
    this.groupId = groupId;
    this.numberOfSubscribers = numberOfSubscribers;
  }

  public Integer getPollingInterval() {
    return pollingInterval;
  }

  public String getGroupId() {
    return groupId;
  }

  public Integer getNumberOfSubscribers() {
    return numberOfSubscribers;
  }
}
