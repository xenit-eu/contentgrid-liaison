package com.contentgrid.liaison.contentgridliaison;

import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(
        webEnvironment = WebEnvironment.DEFINED_PORT,
        properties = {
                "management.endpoints.web.exposure.include=*",
                "management.prometheus.metrics.export.enabled=true"
                //bootRun profile disables metrics, which is fine for other tests but should be overridden here
        })
class ContentgridLiaisonApplicationManagementTests {

    @LocalManagementPort
    private int managementPort;
    @Autowired
    private TestRestTemplate rest;

    @Container
    private static final K3sContainer K8S = new K3sContainer(DockerImageName.parse("rancher/k3s:latest"));

    @TestConfiguration
    public static class K3sClientConfiguration {

        @Bean
        @Primary
        KubernetesClient testKubernetesClient() {
            return new KubernetesClientBuilder().withConfig(fromKubeconfig(K8S.getKubeConfigYaml())).build();
        }
    }

    @Test
    void getPrometheusMetrics() {
        ResponseEntity<String> resp = rest.getForEntity("http://localhost:" + managementPort + "/actuator/prometheus", String.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    }

}
