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

import static com.google.common.truth.Truth.assertThat;
import static com.googlesource.gerrit.plugins.kafka.broker.KafkaConfiguration.KAFKA_SECTION;
import static com.googlesource.gerrit.plugins.kafka.broker.KafkaConfiguration.KafkaPublisher.KAFKA_PUBLISHER_SUBSECTION;
import static com.googlesource.gerrit.plugins.kafka.broker.KafkaConfiguration.KafkaSubscriber.KAFKA_SUBSCRIBER_SUBSECTION;
import static org.mockito.Mockito.when;

import com.google.gerrit.server.config.PluginConfigFactory;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KafkaConfigurationTest {

  private Config kafkaConfig;
  @Mock PluginConfigFactory configFactory;

  @Before
  public void setup() {
    kafkaConfig = new Config();
    when(configFactory.getGlobalPluginConfig("multi-site")).thenReturn(kafkaConfig);
  }

  private KafkaConfiguration getConfiguration() {
    return new KafkaConfiguration(configFactory, "multi-site");
  }

  @Test
  public void kafkaSubscriberPropertiesAreIgnoredWhenPrefixIsNotSet() {
    final String kafkaPropertyName = "fooBarBaz";
    final String kafkaPropertyValue = "aValue";
    kafkaConfig.setString(
        KAFKA_SECTION, KAFKA_SUBSCRIBER_SUBSECTION, kafkaPropertyName, kafkaPropertyValue);

    final String property = getConfiguration().kafkaSubscriber().getProperty("foo.bar.baz");

    assertThat(property).isNull();
  }

  @Test
  public void kafkaPublisherPropertiesAreIgnoredWhenPrefixIsNotSet() {
    final String kafkaPropertyName = "fooBarBaz";
    final String kafkaPropertyValue = "aValue";
    kafkaConfig.setString(
        KAFKA_SECTION, KAFKA_PUBLISHER_SUBSECTION, kafkaPropertyName, kafkaPropertyValue);

    final String property = getConfiguration().kafkaPublisher().getProperty("foo.bar.baz");

    assertThat(property).isNull();
  }

  @Test
  public void shouldReturnKafkaTopicAliasForIndexTopic() {
    setKafkaTopicAlias("indexEventTopic", "gerrit_index");
    final String property = getConfiguration().getKafka().getTopicAlias(EventTopic.INDEX_TOPIC);

    assertThat(property).isEqualTo("gerrit_index");
  }

  @Test
  public void shouldReturnKafkaTopicAliasForStreamEventTopic() {
    setKafkaTopicAlias("streamEventTopic", "gerrit_stream_events");
    final String property =
        getConfiguration().getKafka().getTopicAlias(EventTopic.STREAM_EVENT_TOPIC);

    assertThat(property).isEqualTo("gerrit_stream_events");
  }

  @Test
  public void shouldReturnKafkaTopicAliasForProjectListEventTopic() {
    setKafkaTopicAlias("projectListEventTopic", "gerrit_project_list");
    final String property =
        getConfiguration().getKafka().getTopicAlias(EventTopic.PROJECT_LIST_TOPIC);

    assertThat(property).isEqualTo("gerrit_project_list");
  }

  @Test
  public void shouldReturnKafkaTopicAliasForCacheEventTopic() {
    setKafkaTopicAlias("cacheEventTopic", "gerrit_cache");
    final String property = getConfiguration().getKafka().getTopicAlias(EventTopic.CACHE_TOPIC);

    assertThat(property).isEqualTo("gerrit_cache");
  }

  private void setKafkaTopicAlias(String topicKey, String topic) {
    kafkaConfig.setString(KAFKA_SECTION, null, topicKey, topic);
  }
}