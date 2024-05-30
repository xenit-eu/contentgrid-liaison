package com.contentgrid.liaison.contentgridliaison;

import static io.fabric8.kubernetes.client.Config.fromKubeconfig;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.time.Duration;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@Testcontainers
@SpringBootTest
public class KubernetesDiscoveryIntegrationTest {

    @Container
    private static final K3sContainer K8S = new K3sContainer(DockerImageName.parse("rancher/k3s:latest"))
            .withCommand("server", "--disable=traefik");

    @TestConfiguration
    public static class K3sClientConfiguration {
        @Bean
        @Primary
        KubernetesClient testKubernetesClient() {
            return new KubernetesClientBuilder().withConfig(fromKubeconfig(K8S.getKubeConfigYaml())).build();
        }
    }

    @Autowired
    ApplicationContext applicationContext;

    private WebTestClient webTestClient;
    private KubernetesClient kubernetesClient;

    @BeforeEach
    public void setup() {
        this.webTestClient = WebTestClient.bindToApplicationContext(applicationContext)
                .configureClient()
                .build();

        this.kubernetesClient = new KubernetesClientBuilder().withConfig(fromKubeconfig(K8S.getKubeConfigYaml()))
                .build();
    }


    @Test
    public void discoverNewConfigmapTest() {
        this.webTestClient.get()
                .uri("http://123.contentgrid.app/config.js")
                .header("Host", "123.contentgrid.app")
                .exchange()
                .expectStatus().isNotFound();

        this.kubernetesClient.configMaps().resource(
                new ConfigMapBuilder()
                        .editOrNewMetadata()
                            .withNamespace("default")
                            .withName("123-webapp-cfg")
                            .addToLabels("app.contentgrid.com/service-type", "webapp")
                            .addToLabels("app.contentgrid.com/application-id", "123")
                        .endMetadata()
                        .addToData(Map.of(
                                "contentgrid.routing.domains", "123.contentgrid.app",
                                "contentgrid.api.url", "https://123.contentgrid.cloud",
                                "contentgrid.oidc.issuer", "123-authority",
                                "contentgrid.oidc.client", "123-client"
                        ))
                        .build()
        ).create();

        this.kubernetesClient.configMaps().resource(
                new ConfigMapBuilder()
                        .editOrNewMetadata()
                            .withNamespace("default")
                            .withName("123-gw-cfg")
                            .addToLabels("app.contentgrid.com/service-type", "gateway")
                            .addToLabels("app.contentgrid.com/application-id", "123")
                        .endMetadata()
                        .addToData("contentgrid.routing.domains", "123.contentgrid.cloud")
                        .build()
        ).create();

        Awaitility.await().atMost(Duration.ofSeconds(1)).untilAsserted(() ->
                this.webTestClient.get()
                        .uri("http://123.contentgrid.app/config.js")
                        .header("Host", "123.contentgrid.app")
                        .exchange()
                        .expectStatus().isOk()
                        .expectHeader().contentType("text/javascript")
                        .expectBody(new ParameterizedTypeReference<String>() {}).value(s ->
                                Assertions.assertThat(s).contains("123-authority"))
        );
    }

    @Test
    public void unhappyWebappConfigMapTest() {
        // gateway configmap gets to be correct
        this.kubernetesClient.configMaps().resource(
                new ConfigMapBuilder()
                        .editOrNewMetadata()
                            .withNamespace("default")
                            .withName("456-gw-cfg")
                            .addToLabels("app.contentgrid.com/service-type", "gateway")
                            .addToLabels("app.contentgrid.com/application-id", "456")
                        .endMetadata()
                        .addToData("contentgrid.routing.domains", "456.contentgrid.cloud")
                        .build()
        ).create();
        // Wrong service type
        this.kubernetesClient.configMaps().resource(
                new ConfigMapBuilder()
                        .editOrNewMetadata()
                        .withNamespace("default")
                        .withName("webapp-cfg-1")
                        .addToLabels("app.contentgrid.com/service-type", "foo")
                        .addToLabels("app.contentgrid.com/application-id", "456")
                        .endMetadata()
                        .addToData(Map.of(
                                "contentgrid.routing.domains", "456.contentgrid.app",
                                "contentgrid.api.url", "https://456.contentgrid.cloud",
                                "contentgrid.oidc.issuer", "456-authority",
                                "contentgrid.oidc.client", "456-client"
                        ))
                        .build()
        ).create();
        // Wrong namespace
        this.kubernetesClient.namespaces().resource(
                new NamespaceBuilder().withNewMetadata().withName("foo").endMetadata().build()).create();
        this.kubernetesClient.configMaps().resource(
                new ConfigMapBuilder()
                        .editOrNewMetadata()
                        .withNamespace("foo")
                        .withName("webapp-cfg-2")
                        .addToLabels("app.contentgrid.com/service-type", "webapp")
                        .addToLabels("app.contentgrid.com/application-id", "456")
                        .endMetadata()
                        .addToData(Map.of(
                                "contentgrid.routing.domains", "456.contentgrid.app",
                                "contentgrid.api.url", "https://456.contentgrid.cloud",
                                "contentgrid.oidc.issuer", "456-authority",
                                "contentgrid.oidc.client", "456-client"
                        ))
                        .build()
        ).create();
        // No data
        this.kubernetesClient.configMaps().resource(
                new ConfigMapBuilder()
                        .editOrNewMetadata()
                        .withNamespace("default")
                        .withName("webapp-cfg-3")
                        .addToLabels("app.contentgrid.com/service-type", "webapp")
                        .addToLabels("app.contentgrid.com/application-id", "456")
                        .endMetadata()
                        .build()
        ).create();
        // Wrong domain
        this.kubernetesClient.configMaps().resource(
                new ConfigMapBuilder()
                        .editOrNewMetadata()
                        .withNamespace("default")
                        .withName("webapp-cfg-4")
                        .addToLabels("app.contentgrid.com/service-type", "webapp")
                        .endMetadata()
                        .addToData(Map.of(
                                "contentgrid.routing.domains", "456.contentgrid.example",
                                "contentgrid.api.url", "https://456.contentgrid.cloud",
                                "contentgrid.oidc.issuer", "456-authority",
                                "contentgrid.oidc.client", "456-client"
                        ))
                        .build()
        ).create();

        // verify that none of those invalid configmaps got registered for 456.contentgrid.app

        Awaitility.await().pollDelay(Duration.ofSeconds(1)).untilAsserted(() ->
                this.webTestClient.get()
                        .uri("http://456.contentgrid.app/config.js")
                        .header("Host", "456.contentgrid.app")
                        .exchange()
                        .expectStatus().isNotFound()
        );

        // verify that we didn't break the informer and can still register a correct configmap

        this.kubernetesClient.configMaps().resource(
                new ConfigMapBuilder()
                        .editOrNewMetadata()
                            .withNamespace("default")
                            .withName("webapp-cfg-5")
                            .addToLabels("app.contentgrid.com/service-type", "webapp")
                            .addToLabels("app.contentgrid.com/application-id", "456")
                        .endMetadata()
                        .addToData(Map.of(
                                "contentgrid.routing.domains", "456.contentgrid.app",
                                "contentgrid.oidc.issuer", "456-authority",
                                "contentgrid.oidc.client", "456-client"
                        ))
                        .build()
        ).create();

        Awaitility.await().pollDelay(Duration.ofSeconds(1)).untilAsserted(() ->
                this.webTestClient.get()
                        .uri("http://456.contentgrid.app/config.js")
                        .header("Host", "456.contentgrid.app")
                        .exchange()
                        .expectStatus().isOk()
        );
    }

    @Test
    public void unhappyGatewayConfigMapTest() {
        // webapp configmap gets to be correct
        this.kubernetesClient.configMaps().resource(
                new ConfigMapBuilder()
                        .editOrNewMetadata()
                        .withNamespace("default")
                        .withName("789-webapp-cfg")
                        .addToLabels("app.contentgrid.com/service-type", "webapp")
                        .addToLabels("app.contentgrid.com/application-id", "789")
                        .endMetadata()
                        .addToData(Map.of(
                                "contentgrid.routing.domains", "789.contentgrid.app",
                                "contentgrid.api.url", "https://789.contentgrid.cloud",
                                "contentgrid.oidc.issuer", "789-authority",
                                "contentgrid.oidc.client", "789-client"
                        ))
                        .build()
        ).create();
        // Wrong service type
        this.kubernetesClient.configMaps().resource(
                new ConfigMapBuilder()
                        .editOrNewMetadata()
                        .withNamespace("default")
                        .withName("gw-cfg-1")
                        .addToLabels("app.contentgrid.com/service-type", "goatway")
                        .addToLabels("app.contentgrid.com/application-id", "789")
                        .endMetadata()
                        .addToData("contentgrid.routing.domains", "789.contentgrid.cloud")
                        .build()
        ).create();
        // Wrong namespace
        this.kubernetesClient.namespaces().resource(
                new NamespaceBuilder().withNewMetadata().withName("bar").endMetadata().build()).create();
        this.kubernetesClient.configMaps().resource(
                new ConfigMapBuilder()
                        .editOrNewMetadata()
                        .withNamespace("bar")
                        .withName("gw-cfg-2")
                        .addToLabels("app.contentgrid.com/service-type", "gateway")
                        .addToLabels("app.contentgrid.com/application-id", "789")
                        .endMetadata()
                        .addToData("contentgrid.routing.domains", "789.contentgrid.cloud")
                        .build()
        ).create();
        // No data
        this.kubernetesClient.configMaps().resource(
                new ConfigMapBuilder()
                        .editOrNewMetadata()
                        .withNamespace("default")
                        .withName("gw-cfg-3")
                        .addToLabels("app.contentgrid.com/service-type", "gateway")
                        .addToLabels("app.contentgrid.com/application-id", "789")
                        .endMetadata()
                        .build()
        ).create();
        // Empty domain
        this.kubernetesClient.configMaps().resource(
                new ConfigMapBuilder()
                        .editOrNewMetadata()
                        .withNamespace("default")
                        .withName("gw-cfg-4")
                        .addToLabels("app.contentgrid.com/service-type", "gateway")
                        .addToLabels("app.contentgrid.com/application-id", "789")
                        .endMetadata()
                        .addToData("contentgrid.routing.domains", "")
                        .build()
        ).create();

        // verify that none of those invalid configmaps got registered for 789.contentgrid.app

        Awaitility.await().pollDelay(Duration.ofSeconds(1)).untilAsserted(() ->
                this.webTestClient.get()
                        .uri("http://789.contentgrid.app/config.js")
                        .header("Host", "789.contentgrid.app")
                        .exchange()
                        .expectStatus().isNotFound()
        );

        // verify that we didn't break the informer and can still register a correct configmap

        this.kubernetesClient.configMaps().resource(
                new ConfigMapBuilder()
                        .editOrNewMetadata()
                        .withNamespace("default")
                        .withName("gw-cfg-5")
                        .addToLabels("app.contentgrid.com/service-type", "gateway")
                        .addToLabels("app.contentgrid.com/application-id", "789")
                        .endMetadata()
                        .addToData("contentgrid.routing.domains", "789.contentgrid.cloud")
                        .build()
        ).create();

        Awaitility.await().pollDelay(Duration.ofSeconds(1)).untilAsserted(() ->
                this.webTestClient.get()
                        .uri("http://789.contentgrid.app/config.js")
                        .header("Host", "789.contentgrid.app")
                        .exchange()
                        .expectStatus().isOk()
        );
    }

    @Test
    public void discoverNewUiConfigTest() {
        this.webTestClient.get()
                .uri("http://000.contentgrid.app/config.js")
                .header("Host", "000.contentgrid.app")
                .exchange()
                .expectStatus().isNotFound();

        this.kubernetesClient.configMaps().resource(
                new ConfigMapBuilder()
                        .editOrNewMetadata()
                        .withNamespace("default")
                        .withName("000-webapp-cfg")
                        .addToLabels("app.contentgrid.com/service-type", "webapp")
                        .addToLabels("app.contentgrid.com/application-id", "000")
                        .endMetadata()
                        .addToData(Map.of(
                                "contentgrid.routing.domains", "000.contentgrid.app",
                                "contentgrid.api.url", "https://000.contentgrid.cloud",
                                "contentgrid.oidc.issuer", "000-authority",
                                "contentgrid.oidc.client", "000-client"
                        ))
                        .build()
        ).create();

        this.kubernetesClient.configMaps().resource(
                new ConfigMapBuilder()
                        .editOrNewMetadata()
                        .withNamespace("default")
                        .withName("000-gw-cfg")
                        .addToLabels("app.contentgrid.com/service-type", "gateway")
                        .addToLabels("app.contentgrid.com/application-id", "000")
                        .endMetadata()
                        .addToData("contentgrid.routing.domains", "000.contentgrid.cloud")
                        .build()
        ).create();

        this.kubernetesClient.configMaps().resource(
                new ConfigMapBuilder()
                        .editOrNewMetadata()
                        .withNamespace("default")
                        .withName("000-ui-cfg")
                        .addToLabels("app.contentgrid.com/service-type", "webapp")
                        .addToLabels("app.contentgrid.com/application-id", "000")
                        .endMetadata()
                        .addToData("contentgrid.routing.domains", "000.contentgrid.app")
                        .addToData("contentgrid.ui.config", """
                                { "worker": { "views": [ {"type":"vertical", "elements": [
                                    { "type": "control", "options": {"name":"first_name"} },
                                    { "type": "control", "options": {"name":"last_name"} }
                                ]}]}}
                                """)
                        .build()
        ).create();

        Awaitility.await().atMost(Duration.ofSeconds(1)).untilAsserted(() ->
                this.webTestClient.get()
                        .uri("http://000.contentgrid.app/config.js")
                        .header("Host", "000.contentgrid.app")
                        .exchange()
                        .expectStatus().isOk()
                        .expectHeader().contentType("text/javascript")
                        .expectBody(new ParameterizedTypeReference<String>() {}).value(s ->
                                Assertions.assertThat(s).contains("\"name\":\"last_name\""))
        );
    }
}
