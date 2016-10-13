include_defs('//bucklets/gerrit_plugin.bucklet')
include_defs('//bucklets/maven_jar.bucklet')

gerrit_plugin(
  name = 'kafka-events',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/resources/**/*']),
  manifest_entries = [
    'Gerrit-PluginName: kafka-events',
    'Gerrit-Module: com.googlesource.gerrit.plugins.kafka.Module',
    'Implementation-Title: Gerrit Apache Kafka plugin',
    'Implementation-URL: https://gerrit.googlesource.com/plugins/kafka-events',
    'Implementation-Vendor: GerritForge',
  ],
  deps = [
    ':kafka-client',
  ],
  provided_deps = [
    '//lib:gson',
    '//lib/commons:codec',
    '//lib/commons:lang',
  ],
)

java_library(
  name = 'classpath',
  deps = [':kafka__plugin'],
)

maven_jar(
  name = 'kafka-client',
  id = 'org.apache.kafka:kafka-clients:0.10.0.1',
  license = 'Apache2.0',
  exclude_java_sources = True,
  visibility = [],
)

