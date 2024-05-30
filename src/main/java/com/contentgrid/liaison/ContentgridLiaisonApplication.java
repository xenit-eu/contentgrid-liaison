package com.contentgrid.liaison;

import com.contentgrid.liaison.config.LiaisonProperties;
import com.contentgrid.liaison.kubernetes.KubernetesDiscovery;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.impl.KubernetesClientImpl;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(LiaisonProperties.class)
public class ContentgridLiaisonApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentgridLiaisonApplication.class, args);
    }

    @Bean
    KubernetesDiscovery discovery(LiaisonProperties properties, KubernetesClient client) {
        return new KubernetesDiscovery(properties.getDiscovery(), client);
    }

    @Bean
    KubernetesClient k8sClient() {
        return new KubernetesClientBuilder().build();
    }

    @Bean
    ApplicationRunner runnerWebapps(KubernetesDiscovery discovery) {
        return args -> {
            discovery.discoverWebapp();
        };
    }
    @Bean
    ApplicationRunner runnerGateway(KubernetesDiscovery discovery) {
        return args -> {
            discovery.discoverGateway();
        };
    }
    @Bean
    ApplicationRunner runnerUiConfig(KubernetesDiscovery discovery) {
        return args -> {
            discovery.discoverUiConfig();
        };
    }


}
