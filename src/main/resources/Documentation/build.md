Build
=====

This plugin is built with Buck.

Build in Gerrit tree
--------------------

Clone or link this plugin to the plugins directory of Gerrit's source
tree, and issue the command:

```
  buck build plugins/kafka-events
```

The output is created in

```
  buck-out/gen/plugins/kafka-events/kafka-events.jar
```

This project can be imported into the Eclipse IDE:

```
  ./tools/eclipse/project.py
```

How to build the Gerrit Plugin API is described in the [Gerrit
documentation](../../../Documentation/dev-buck.html#_extension_and_plugin_api_jar_files).
