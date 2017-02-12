load("//tools/bzl:maven_jar.bzl", "maven_jar")

def external_plugin_deps():
  maven_jar(
    name = 'kafka-client',
    artifact = 'org.apache.kafka:kafka-clients:0.10.0.1',
    sha1 = '36ebf4044d0e546bf74c95629d736ca63320a323',
  )
