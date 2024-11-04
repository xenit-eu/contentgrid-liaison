package com.contentgrid.liaison.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "liaison", ignoreUnknownFields = false)
@Data
public class LiaisonProperties {

    private LiaisonDiscoveryProperties discovery = new LiaisonDiscoveryProperties();
    private Map<String, String> additionalProperties = new HashMap<>();


    @Data
    public static class LiaisonDiscoveryProperties {
        private ConfigmapDiscoveryProperties webapp;
        private ConfigmapDiscoveryProperties gateway;
    }

    @Data
    public static class ConfigmapDiscoveryProperties {
        private String namespace;
        private Map<String, String> labels = new HashMap<>();
        private int resyncIntervalSeconds;
    }
}
