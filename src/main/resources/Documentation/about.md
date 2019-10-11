This plugin is a Kafka implementation of the broker api interface. 

When provided this plugin enables the replication of Gerrit _indexes_,
_caches_,  and _stream events_ across sites.

Originally this code was a part of [multi-site plugin](https://gerrit.googlesource.com/plugins/multi-site/) but currently can be use independently.

## Setup

* Install @PLUGIN@ plugin

Install the kafka-events plugin into the `$GERRIT_SITE/plugins` directory of all
the Gerrit servers that are part of the cluster.

* Configure @PLUGIN@ plugin

Create the `$GERRIT_SITE/etc/@PLUGIN@.config` on all Gerrit servers with the
following basic settings. Where `kafka-*` is the host that is running kafka
and `9092` is the default kafka port, please change them accordingly:

```
[kafka]
  bootstrapServers = kafka-1:9092,kafka-2:9092,kafka-3:9092
```

For further information and supported options, refer to [config](config.md)
documentation.
