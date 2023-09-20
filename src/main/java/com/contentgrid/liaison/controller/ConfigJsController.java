package com.contentgrid.liaison.controller;

import com.contentgrid.liaison.kubernetes.KubernetesDiscovery;
import com.contentgrid.liaison.kubernetes.WebappConfiguration;
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
                            client_id: "%s",
                        }
                    }
                };
                """.formatted(config.apiUrl(), config.authority(), config.clientId());
    }
}
