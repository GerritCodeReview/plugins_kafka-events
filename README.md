Kafka: Gerrit event producer for Apache Kafka
=======================

* Author: GerritForge
* Repository: https://gerrit.googlesource.com/plugins/kafka-events
* CI/Release: https://gerrit-ci.gerritforge.com/search/?q=kafka-events

[![Build Status](https://gerrit-ci.gerritforge.com/job/plugin-kafka-events-master/1/badge/icon)](https://gerrit-ci.gerritforge.com/job/plugin-kafka-events-master/1/)

Synopsis
----------------------

This plugins allows to define a distributed stream of events
published by Gerrit.

Events can be anything, from the traditional stream events
to the Gerrit metrics.

This plugin requires Gerrit 2.13 or laster.

Environments
---------------------

* `linux`
* `java-1.8`
* `Buck`

Build
---------------------

Clone or link this plugin to the plugins directory of Gerrit's source
tree, and issue the command:


    buck build plugins/kafka-events

The output is created in

    buck-out/gen/plugins/kafka-events/kafka-events.jar

Minimum Configuration
---------------------
Assuming a running Kafka broker on the same Gerrit host, add the following
settings to gerrit.config:

```
  [plugin "kafka-events"]
    bootstrapServers = localhost:9092
```

