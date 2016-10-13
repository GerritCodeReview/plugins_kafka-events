Kafka: Gerrit event producer for Apache Kafka
=======================

* Author: GerritForge
* Repository: https://gerrit.googlesource.com/plugins/kafka
* CI/Release: https://gerrit-ci.gerritforge.com/search/?q=kafka

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


    buck build plugins/kafka

The output is created in

    buck-out/gen/plugins/kafka/kafka.jar

Minimum Configuration
---------------------
Assuming a running Kafka broker on the same Gerrit host, add the following
settings to gerrit.config:

```
  [plugin "kafka"]
    server = localhost:9092
    topic = gerrit-events
```

