
@PLUGIN@ Configuration
=========================

Global configuration of the @PLUGIN@ plugin is done in the gerrit.config file in $GERRIT_SITE/etc.
--------------------

## Sample configuration.

```
[plugin "kafka-events"]
        bootstrapServers = localhost:9092
```

All the Apache Kafka properties configuration needs to
be defined using a lower camel-case notation.

Example: bootstrapServers correspond to the Apache Kafka property
bootstrap.servers.

See [Apache Kafka Producer Config](http://kafka.apache.org/documentation.html#producerconfigs)
for a full list of available settings and the values allowed.

Default Values
-----------------

|name                 | value
|:--------------------|:------------------
| acks                | all
| retries             | 0
| batchSize           | 16384
| lingerMs            | 1
| bufferMemory        | 33554432
| keySerializer       | org.apache.kafka.common.serialization.StringSerializer
| valueSerializer     | org.apache.kafka.common.serialization.StringSerializer

Additional properties
---------------------

`plugin.kafka-events.groupId`
:	Kafka consumer group for receiving messages.
	Default: Gerrit instance-id

`plugin.kafka-events.pollingIntervalMs`
:	Polling interval in msec for receiving messages from Kafka topic subscription.
	Default: 1000

`plugin.kafka-events.numberOfSubscribers`
:	Number of threads available for subscribers.
	Default: 4