load("//tools/bzl:maven_jar.bzl", "MAVEN_LOCAL", "maven_jar")

def external_plugin_deps():
    maven_jar(
        name = "kafka-client",
        artifact = "org.apache.kafka:kafka-clients:2.1.0",
        sha1 = "34d9983705c953b97abb01e1cd04647f47272fe5",
    )

    maven_jar(
        name = "testcontainers-kafka",
        artifact = "org.testcontainers:kafka:1.10.6",
        sha1 = "5984e31306bd6c84a36092cdd19e0ef7e2268d98",
    )

    maven_jar(
        name = "events-broker",
        artifact = "com.gerritforge:events-broker:3.0.3",
        sha1 = "efdc5bf6897563e2f6f85bfc1b8a5d65e3393424",
    )
