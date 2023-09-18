package com.contentgrid.liaison.contentgridliaison;

import com.contentgrid.liaison.kubernetes.KubernetesDiscovery;
import com.contentgrid.liaison.kubernetes.WebappConfiguration;
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

    @BeforeEach
    public void setup() {
        this.client = WebTestClient.bindToApplicationContext(applicationContext)
                .configureClient()
                .build();

        Mockito.when(discovery.findByDomain("123.contentgrid.app"))
                .thenReturn(new WebappConfiguration("http://123.contentgrid.cloud", "123-authority", "123-client"));
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
                        Assertions.assertThat(s).contains("baseUrl: \"http://123.contentgrid.cloud\""));
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
