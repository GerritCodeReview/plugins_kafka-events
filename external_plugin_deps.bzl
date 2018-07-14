load("//tools/bzl:maven_jar.bzl", "maven_jar")

def external_plugin_deps():
    maven_jar(
        name = "kafka_client",
        artifact = "org.apache.kafka:kafka-clients:0.10.0.1",
        sha1 = "36ebf4044d0e546bf74c95629d736ca63320a323",
    )

    maven_jar(
        name = "testcontainers-kafka",
        artifact = "org.testcontainers:kafka:1.8.0",
        sha1 = "1d6f0a529dd87bc66bc68a09b8b58ed17ebf33f6",
    )
