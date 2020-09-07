package demo.keycloak.oidcmapper;

import demo.KeycloakDevContainer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

@Testcontainers
public class DemoOidcProtocolMapperTest {

    public static final String DEMO_REALM = "demo";
    public static final String DEMO_CLIENT = "demo-client";
    public static KeycloakDevContainer keycloak;

    public static Keycloak keycloakClient;

    @BeforeClass
    public static void beforeClass() {
        keycloak = new KeycloakDevContainer("quay.io/keycloak/keycloak:11.0.2");
        keycloak.withReuse(true);
        keycloak.withExposedPorts(8080, 8787);
        keycloak.withFixedExposedPort(8787, 8787);
        keycloak.withRealmImportFile("demo-realm.json");
        keycloak.start();

        keycloakClient = Keycloak.getInstance(keycloak.getAuthServerUrl(), "master", keycloak.getAdminUsername(), keycloak.getAdminPassword(), "admin-cli");

        ProtocolMapperRepresentation demoMapper = new ProtocolMapperRepresentation();
        demoMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        demoMapper.setProtocolMapper(DemoOidcProtocolMapper.ID);
        demoMapper.setName("demo-mapper");
        Map<String, String> config = new HashMap<>();
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        demoMapper.setConfig(config);

        RealmResource demoRealm = keycloakClient.realm(DEMO_REALM);
        ClientRepresentation demoClient = demoRealm.clients().findByClientId(DEMO_CLIENT).get(0);
        demoRealm.clients().get(demoClient.getId()).getProtocolMappers().createMapper(demoMapper).close();
    }

    @Test
    public void testDemoMapper() throws Exception {
//        Keycloak keycloakClient = Keycloak.getInstance(keycloak.getAuthServerUrl(), "master", keycloak.getAdminUsername(), keycloak.getAdminPassword(), "admin-cli");

        Keycloak demoClient = Keycloak.getInstance(keycloak.getAuthServerUrl(), DEMO_REALM, "demo-user", "test1234", DEMO_CLIENT);
        AccessTokenResponse tokenResponse = demoClient.tokenManager().getAccessToken();
        System.out.println(tokenResponse);

        TokenVerifier<AccessToken> verifier = TokenVerifier.create(tokenResponse.getToken(), AccessToken.class);
        verifier.parse();
        AccessToken accessToken = verifier.getToken();
        Object customClaimValue = accessToken.getOtherClaims().get(DemoOidcProtocolMapper.CUSTOM_CLAIM_NAME);
        System.out.printf("Custom Claim name %s=%s", DemoOidcProtocolMapper.CUSTOM_CLAIM_NAME, customClaimValue);
        Assertions.assertNotNull(customClaimValue);
    }
}