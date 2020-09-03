package demo.keycloak.eventlistener;

import lombok.extern.jbosslog.JBossLog;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

@JBossLog
public class DemoEventListener implements EventListenerProvider {

    @Override
    public void onEvent(Event event) {
        log.infof("Demo Event: %s", event.getType());
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
    }
}
