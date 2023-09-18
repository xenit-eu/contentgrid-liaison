package com.contentgrid.liaison.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "liaison", ignoreUnknownFields = false)
@Data
public class LiaisonProperties {

    private LiaisonDiscoveryProperties discovery = new LiaisonDiscoveryProperties();

    @Data
    public static class LiaisonDiscoveryProperties {
        private String namespace;
        private Map<String, String> labels = new HashMap<>();
        private int resyncIntervalSeconds;
    }
}
