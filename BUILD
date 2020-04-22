load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)

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
        "@kafka-client//jar",
        "@events-broker//jar",
    ],
)

junit_tests(
    name = "kafka_events_tests",
    srcs = glob(["src/test/java/**/*.java"]),
    tags = ["kafka-events"],
    deps = [
        ":kafka-events__plugin_test_deps",
        "//lib/testcontainers",
        "@duct-tape//jar",
        "@kafka-client//jar",
        "@events-broker//jar",
        "@testcontainers-kafka//jar",
    ],
)

java_library(
    name = "kafka-events__plugin_test_deps",
    testonly = 1,
    visibility = ["//visibility:public"],
    exports = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":kafka-events__plugin",
        "@testcontainers-kafka//jar",
        "//lib/testcontainers",
    ],
)
