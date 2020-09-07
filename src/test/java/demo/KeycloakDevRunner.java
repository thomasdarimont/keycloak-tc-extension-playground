package demo;

import org.testcontainers.containers.output.BaseConsumer;
import org.testcontainers.containers.output.OutputFrame;

public class KeycloakDevRunner {

    public static void main(String[] args) throws Exception {

        var kc = new KeycloakDevContainer("quay.io/keycloak/keycloak:11.0.2");

        kc.withFixedExposedPort(8080, 8080);
        kc.withFixedExposedPort(8787, 8787);
        kc.withClassFolderChangeTrackingEnabled(true);
        kc.withRealmImportFile("demo-realm.json");
        kc.start();

        class StdoutConsumer extends BaseConsumer<StdoutConsumer> {

            @Override
            public void accept(OutputFrame outputFrame) {
                System.out.print(outputFrame.getUtf8String());
            }
        }
        kc.followOutput(new StdoutConsumer().withRemoveAnsiCodes(true));

        System.out.println("Keycloak Running, you can now attach your remote debugger!");
        System.in.read();
    }

}
