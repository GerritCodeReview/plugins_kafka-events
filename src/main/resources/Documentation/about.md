This plugin publishes gerrit stream events to an Apache Kafka topic.

It also provides a Kafka-based implementation of a generic
[Events Broker Api](https://github.com/GerritForge/events-broker) which can be used by
Gerrit and other plugins.

# Use-cases

## CI/CD Validation

Gerrit stream events can be published to the internal network where other subscribers
can trigger automated jobs (e.g. CI/CD validation) for fetching the changes and validating
them through build and testing.

__NOTE__: This use-case would require a CI/CD system (e.g. Jenkins, Zuul or other) and
the development of a Kafka-based subscriber to receive the event and trigger the build.

Events replication
------------------

Multiple Gerrit masters in a multi-site setup can be informed on the stream events
happening on every node thanks to the notification to a Kafka pub/sub topic.

__NOTE__: This use-case would require the [multi-site plugin](https://gerrit.googlesource.com/plugins/multi-site)
on each of the Gerrit masters that are part of the same multi-site cluster.

# Setup

* Install @PLUGIN@ plugin

Install the kafka-events plugin into the `$GERRIT_SITE/plugins` directory.

* Configure @PLUGIN@ plugin

Update the `$GERRIT_SITE/etc/gerrit.config` with the
following basic settings. Where `kafka-*` is the host that is running kafka
and `9092` is the default kafka port, please change them accordingly:

```
[plugin "kafka-events"]
  bootstrapServers = kafka-1:9092,kafka-2:9092,kafka-3:9092
```

For further information and supported options, refer to [config](config.md)
documentation.

