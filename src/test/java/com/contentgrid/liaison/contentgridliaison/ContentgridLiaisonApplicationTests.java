package com.contentgrid.liaison.contentgridliaison;

import static com.contentgrid.liaison.contentgridliaison.KubernetesDiscoveryIntegrationTest.executeJS;

import com.contentgrid.liaison.config.LiaisonProperties;
import com.contentgrid.liaison.kubernetes.KubernetesDiscovery;
import com.contentgrid.liaison.kubernetes.WebappConfiguration;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
class ContentgridLiaisonApplicationTests {

    @Autowired
    ApplicationContext applicationContext;

    WebTestClient client;

    @MockBean
    KubernetesDiscovery discovery;

    @MockBean
    LiaisonProperties properties;

    @BeforeEach
    public void setup() {
        this.client = WebTestClient.bindToApplicationContext(applicationContext)
                .configureClient()
                .build();

        Mockito.when(discovery.findByDomain("123.contentgrid.app")).thenReturn(Optional.of(
                new WebappConfiguration("123", "123-authority", "123-client", "http://123.contentgrid.cloud", null)));
        Mockito.when(discovery.findByDomain("456.contentgrid.app")).thenReturn(Optional.of(
                new WebappConfiguration("456", "456-authority", "456-client", "http://456.contentgrid.cloud", """
                        { "worker": { "views": [ {"type":"vertical", "elements": []} ] } }
                        """)));
        Mockito.when(properties.getAdditionalProperties()).thenReturn(Map.of());
    }

    @Test
    void getConfigJsTest() {
        client.get()
                .uri("http://123.contentgrid.app/config.js")
                .header("Host", "123.contentgrid.app")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("text/javascript")
                .expectBody(new ParameterizedTypeReference<String>() {}).value(s ->
                        Assertions.assertThat(s).contains("apiBaseUrl: \"http://123.contentgrid.cloud\""));
    }

    @Test
    void getAdditionalPropertiesTest() {
        Mockito.when(properties.getAdditionalProperties()).thenReturn(Map.of(
                "foo", "bar",
                "renditionUriTemplate", "https://renditions.contentgrid.cloud/get/pdf{?url}"));
        client.get()
                .uri("http://123.contentgrid.app/config.js")
                .header("Host", "123.contentgrid.app")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("text/javascript")
                .expectBody(new ParameterizedTypeReference<String>() {}).value(s ->
                        executeJS(s, window -> {
                            Assertions.assertThat(window.getMember("contentGridConfig")
                                    .getMember("v1")
                                    .getMember("foo")
                                    .asString()).isEqualTo("bar");
                            Assertions.assertThat(window.getMember("contentGridConfig")
                                    .getMember("v1")
                                    .getMember("renditionUriTemplate")
                                    .asString()).isEqualTo("https://renditions.contentgrid.cloud/get/pdf{?url}");
                        }));
    }

    @Test
    void getUiConfigTest() {
        client.get()
                .uri("http://456.contentgrid.app/config.js")
                .header("Host", "456.contentgrid.app")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("text/javascript")
                .expectBody(new ParameterizedTypeReference<String>() {}).value(s ->
                        Assertions.assertThat(s).contains("\"elements\": []"));
    }

    @Test
    void getConfigJs_missing() {
        client.get()
                .uri("http://404.contentgrid.app/config.js")
                .header("Host", "404.contentgrid.app")
                .exchange()
                .expectStatus().isNotFound();
    }
}
