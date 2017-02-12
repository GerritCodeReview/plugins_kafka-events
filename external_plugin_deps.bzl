load("//tools/bzl:maven_jar.bzl", "maven_jar")

def external_plugin_deps():
  maven_jar(
    name = 'kafka-client',
    artifact = 'org.apache.kafka:kafka-clients:0.10.0.1',
    exclude_java_sources = True,
  )
