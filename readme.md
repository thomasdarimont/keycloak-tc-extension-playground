Keycloak DevContainer Experiment
---

PoC for developing custom Keycloak extensions with a Keycloak docker container managed by test-containers.
The traditional way of developing Keycloak extensions involves Keycloak specific packaging and deployment 
if said extension in order to actually test / run the code. 

In this PoC we expose the current target/classes folder directly to a docker container running Keycloak which
immediately picks up any changes in our code, once the IDE finished the compilation. This is done
by leveraging the support for exploded deployments in JBoss / Wildfly / Keycloak.

This allows us to develop our extensions in the context of a running Keycloak instance, which reduces
development time significantly since we don't need to repackage our extension first to try it out.
Note that Remote Debugging of the newly deployed code is also possible and allows a neat dynamic development experience.

This approach can be used by other projects as well, e.g. standard Java EE projects that use application servers
that support exploded code deployment.   

# KeycloakDevRunner
The KeycloakDevRunner can be used to expose Keycloak extension from the current project to a Keycloak docker container.

## Using the KeycloakDevRunner  
- Import the protect into an IDE, e.g. IntelliJ.
- Run the "KeycloakDevRunner" class
- Create a remote Debugging configuration for the project (localhost:8787)
- Attach a remote debugger

- Change the String `### INIT1 ` in `DemoEventListenerFactory` to `### INIT2 `
- If not done automatically, trigger a rebuild of your project, (ctrl-shift-f9 in IntelliJ)
- Watch the log for the following lines:
```
15:15:17,170 INFO  [org.keycloak.subsystem.server.extension.KeycloakProviderDeploymentProcessor] (MSC service thread 1-1) Undeploying Keycloak provider: extensions.jar
15:15:17,172 INFO  [org.jboss.as.server.deployment] (MSC service thread 1-1) WFLYSRV0028: Stopped deployment extensions.jar (runtime-name: extensions.jar) in 4ms
15:15:17,175 INFO  [org.jboss.as.server.deployment] (MSC service thread 1-1) WFLYSRV0027: Starting deployment of "extensions.jar" (runtime-name: "extensions.jar")
15:15:17,191 INFO  [org.keycloak.subsystem.server.extension.KeycloakProviderDeploymentProcessor] (MSC service thread 1-4) Deploying Keycloak provider: extensions.jar
15:15:17,242 INFO  [demo.keycloak.eventlistener.DemoEventListenerFactory] (MSC service thread 1-4) ### INIT2 demo-eventlistener
15:15:17,242 WARN  [org.keycloak.services] (MSC service thread 1-4) KC-SERVICES0047: demo-eventlistener (demo.keycloak.eventlistener.DemoEventListenerFactory) is implementing the internal SPI eventsListener. This SPI is internal and may change without notice
15:15:17,258 INFO  [org.jboss.as.server] (DeploymentScanner-threads - 2) WFLYSRV0016: Replaced deployment "extensions.jar" with deployment "extensions.jar"
```
- The `INIT2` indicates that Keycloak saw the new code.
- If you place a breakpoint on the line with `### INIT2` you'll see that the breakpoint hits as soon as the new classes are deployed.

# Keycloak Integration Tests
You can test your Keycloak extensions within a Keycloak container by using the `KeycloakDevContainer` which exposes the current classes path
to the started Keycloak container.
See [DemoOidcProtocolMapperTest](src/test/java/demo/keycloak/oidcmapper/DemoOidcProtocolMapperTest.java) for an example of this approach.

# Misc

## Prepare Keycloak realm

To start with a proper Realm configuration it is advisable to configure the realm in a standalone keycloak instance 
first and export the realm configuration after wards.  

Start a standalone Keycloak instance:
```
bin/standalone.sh
```

Start a standalone Keycloak instance with export:
```
bin/standalone.sh \
  -Dkeycloak.migration.action=export \
  -Dkeycloak.migration.file=demo-realm.json \
  -Dkeycloak.migration.dir=. \
  -Dkeycloak.migration.usersExportStrategy=REALM_FILE \
  -Dkeycloak.migration.realmName=demo
```