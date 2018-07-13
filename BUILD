load("//tools/bzl:plugin.bzl", "gerrit_plugin")

gerrit_plugin(
    name = "kafka-events",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: kafka-events",
        "Gerrit-Module: com.googlesource.gerrit.plugins.kafka.Module",
        "Implementation-Title: Gerrit Apache Kafka plugin",
        "Implementation-URL: https://gerrit.googlesource.com/plugins/kafka-events",
    ],
    resources = glob(["src/main/resources/**/*"]),
    deps = [
        "@kafka_client//jar",
    ],
)
