Regarding Java files from 'scr/com/sqldalmaker/netbeans':

    - to build Eclipse plugin, hide these files into a zip.

    - to build Netbeans plug-in, unzipped them back.

To build IntelliJ plugin

    - exclude a lib providing 'velocity-1.7-dep.jar' form Intellij SDK Classpath,
      use 'velocity-1.7-dep.jar' from 'lib' folder instead

    - never use layouts and components from intellij.uidesigner like GridLayoutManager and HSpacer,
      remove 'intellij' folder from sqldalmaker-intellij.zip distribution
