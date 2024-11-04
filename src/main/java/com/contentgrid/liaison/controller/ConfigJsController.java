package com.contentgrid.liaison.controller;

import com.contentgrid.liaison.config.LiaisonProperties;
import com.contentgrid.liaison.kubernetes.KubernetesDiscovery;
import com.contentgrid.liaison.kubernetes.WebappConfiguration;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ConfigJsController {
    private final MediaType JAVASCRIPT = MediaType.parseMediaType("text/javascript");

    private final KubernetesDiscovery discovery;
    private final LiaisonProperties properties;

    @GetMapping("/config.js")
    public ResponseEntity<String> getConfigJs(ServerHttpRequest request) {

        var host = request.getURI().getHost();
        var maybeConfig = discovery.findByDomain(host);
        return maybeConfig
                .map(config -> ResponseEntity.ok()
                        .contentType(JAVASCRIPT)
                        .body(makeConfigJs(config)))
                .orElseGet(() -> ResponseEntity.notFound().build());

    }

    private String makeConfigJs(WebappConfiguration config) {
        return """
                window.contentGridConfig = {
                    v1: {
                        apiBaseUrl: "%s",
                        oidc: {
                            authority: "%s",
                            client_id: "%s"
                        },
                        %s
                        uiConfig: %s
                    }
                };""".formatted(config.apiUrl(), config.authority(), config.clientId(), formatAdditionalProperties(),
                // Simply dumping json into javascript via string templating is icky, but will probably be fine for now
                config.uiConfig());
    }

    private String formatAdditionalProperties() {
        return properties.getAdditionalProperties().entrySet().stream()
                        .map(e -> String.format("%s: \"%s\",", e.getKey(), e.getValue()))
                        .collect(Collectors.joining("\n        "));
    }
}
