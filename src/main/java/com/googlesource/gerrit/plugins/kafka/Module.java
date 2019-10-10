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

import com.gerritforge.gerrit.eventbroker.EventGsonProvider;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.events.EventListener;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.kafka.api.KafkaApiModule;
import com.googlesource.gerrit.plugins.kafka.config.KafkaProperties;
import com.googlesource.gerrit.plugins.kafka.publish.KafkaPublisher;
import com.googlesource.gerrit.plugins.kafka.subscribe.KafkaGroupId;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

class Module extends AbstractModule {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final KafkaApiModule kafkaBrokerModule;

  @Inject
  public Module(KafkaApiModule kafkaBrokerModule) {
    this.kafkaBrokerModule = kafkaBrokerModule;
  }

  @Override
  protected void configure() {
    bind(Gson.class).toProvider(EventGsonProvider.class).in(Singleton.class);
    DynamicSet.bind(binder(), LifecycleListener.class).to(Manager.class);
    DynamicSet.bind(binder(), EventListener.class).to(KafkaPublisher.class);

    install(kafkaBrokerModule);
  }

  @Provides
  @Singleton
  @KafkaGroupId
  public UUID getKafkaGroupId(SitePaths sitePaths, @PluginName String pluginName)
      throws IOException {
    UUID kafkaGroupId = null;
    Path dataDir = sitePaths.data_dir.resolve(pluginName);
    if (!dataDir.toFile().exists()) {
      dataDir.toFile().mkdirs();
    }
    String serverIdFile = dataDir.toAbsolutePath().toString() + "/" + KafkaProperties.GROUP_ID_FILE;

    kafkaGroupId = tryToLoadSavedGroupId(serverIdFile);

    if (kafkaGroupId == null) {
      kafkaGroupId = UUID.randomUUID();
      Files.createFile(Paths.get(serverIdFile));
      try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(serverIdFile))) {
        writer.write(kafkaGroupId.toString());
      } catch (IOException e) {
        logger.atWarning().log(
            String.format(
                "Cannot write kafka group ID, a new one will be generated at instance restart. (%s)",
                e.getMessage()));
      }
    }
    return kafkaGroupId;
  }

  private UUID tryToLoadSavedGroupId(String serverIdFile) {
    if (Files.exists(Paths.get(serverIdFile))) {
      try (BufferedReader br = Files.newBufferedReader(Paths.get(serverIdFile))) {
        return UUID.fromString(br.readLine());
      } catch (IOException e) {
        logger.atWarning().log(
            String.format(
                "Cannot read kafka group ID from path '%s', deleting the old file and generating a new ID: (%s)",
                serverIdFile, e.getMessage()));
        try {
          Files.delete(Paths.get(serverIdFile));
        } catch (IOException e1) {
          logger.atWarning().log(
              String.format(
                  "Cannot delete old kafka group ID file at path '%s' with instance ID while generating a new one: (%s)",
                  serverIdFile, e1.getMessage()));
        }
      }
    }
    return null;
  }
}
