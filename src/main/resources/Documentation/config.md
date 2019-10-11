
@PLUGIN@ Configuration
=========================

Global configuration of the @PLUGIN@ plugin is done in the @PLUGIN@.config file in the site's etc directory.

File '@PLUGIN@.config'
--------------------

## Sample configuration.

```
[kafka]
  bootstrapServers = kafka-1:9092,kafka-2:9092,kafka-3:9092

  indexEventTopic = gerrit_index
  streamEventTopic = gerrit_stream
  cacheEventTopic = gerrit_cache_eviction
  projectListEventTopic = gerrit_project_list

[kafka "publisher"]
  KafkaProp-compressionType = none
  KafkaProp-deliveryTimeoutMs = 60000

[kafka "subscriber"]
  pollingIntervalMs = 1000

  KafkaProp-enableAutoCommit = true
  KafkaProp-autoCommitIntervalMs = 1000
  KafkaProp-autoCommitIntervalMs = 5000

```

## Configuration parameters

```kafka.bootstrapServers```
:	  List of Kafka broker hosts (host:port) to use for publishing events to the message
    broker

```kafka.indexEventTopic```
:   Name of the Kafka topic to use for publishing indexing events
    Defaults to GERRIT.EVENT.INDEX

```kafka.streamEventTopic```
:   Name of the Kafka topic to use for publishing stream events
    Defaults to GERRIT.EVENT.STREAM

```kafka.cacheEventTopic```
:   Name of the Kafka topic to use for publishing cache eviction events
    Defaults to GERRIT.EVENT.CACHE

```kafka.projectListEventTopic```
:   Name of the Kafka topic to use for publishing cache eviction events
    Defaults to GERRIT.EVENT.PROJECT.LIST

```kafka.subscriber.pollingIntervalMs```
:   Polling interval in milliseconds for checking incoming events

    Defaults: 1000

#### Custom kafka properties:

In addition to the above settings, custom Kafka properties can be explicitly set
for `publisher` and `subscriber`.
In order to be acknowledged, these properties need to be prefixed with the
`KafkaProp-` prefix and be formatted using camel case, as follows: `KafkaProp-yourPropertyValue`

For example, if you want to set the `auto.commit.interval.ms` property for
consumers, you need to configure this property as `KafkaProp-autoCommitIntervalMs`.

The complete list of available settings can be found directly in the kafka website:

* **Publisher**: https://kafka.apache.org/documentation/#producerconfigs
* **Subscriber**: https://kafka.apache.org/documentation/#consumerconfigs
