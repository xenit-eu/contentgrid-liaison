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
        var config = discovery.findByDomain(host);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(JAVASCRIPT)
                .body(makeConfigJs(config));
    }

    private String makeConfigJs(WebappConfiguration config) {
        return """
                window.config = {
                    baseUrl: "%s",
                    oidc: {
                        authority: "%s",
                        client_id: "%s",
                    }
                };
                """.formatted(config.apiUrl(), config.authority(), config.clientId());
    }
}
