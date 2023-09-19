package com.contentgrid.liaison.kubernetes;

import com.contentgrid.liaison.config.LiaisonProperties.LiaisonDiscoveryProperties;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class KubernetesDiscovery {
    private final LiaisonDiscoveryProperties discoveryProperties;
    private final KubernetesClient client;

    private final ConcurrentHashMap<String, WebappConfiguration> configmaps = new ConcurrentHashMap<>();

    private final static String DOMAINS_KEY = "contentgrid.routing.domains";
    private final static String DOMAINS_SEPARATOR = ",";
    private final static String API_URL_KEY = "contentgrid.api.url";
    private final static String ISSUER_KEY = "contentgrid.oidc.issuer";
    private final static String CLIENT_ID_KEY = "contentgrid.oidc.client";

    public void discover() {
        client.configMaps()
                .inNamespace(discoveryProperties.getNamespace())
                .withLabels(discoveryProperties.getLabels())
                .inform(new ResourceEventHandler<ConfigMap>() {
                    @Override
                    public void onAdd(ConfigMap cm) {
                        for (String domain : cm.getData().get(DOMAINS_KEY).split(DOMAINS_SEPARATOR)) {
                            log.debug("Registered domain {}", domain);
                            var webappCfg = new WebappConfiguration(
                                    cm.getData().get(API_URL_KEY),
                                    cm.getData().get(ISSUER_KEY),
                                    cm.getData().get(CLIENT_ID_KEY)
                            );
                            configmaps.put(domain, webappCfg);
                        }

                    }

                    @Override
                    public void onUpdate(ConfigMap oldObj, ConfigMap newObj) {
                        this.onAdd(newObj);
                    }

                    @Override
                    public void onDelete(ConfigMap cm, boolean deletedFinalStateUnknown) {
                        for (String domain : cm.getData().get(DOMAINS_KEY).split(DOMAINS_SEPARATOR)) {
                            log.debug("Deleting domain {}", domain);
                            configmaps.remove(domain);
                        }
                    }
                }, discoveryProperties.getResyncIntervalSeconds() * 1000L);
    }

    public WebappConfiguration findByDomain(String domain) {
        return this.configmaps.get(domain);
    }


}

