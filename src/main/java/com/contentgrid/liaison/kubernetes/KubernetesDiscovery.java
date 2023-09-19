package com.contentgrid.liaison.kubernetes;

import com.contentgrid.liaison.config.LiaisonProperties.LiaisonDiscoveryProperties;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class KubernetesDiscovery {
    private final LiaisonDiscoveryProperties discoveryProperties;
    private final KubernetesClient client;

    private final ConcurrentHashMap<String, WebappCfgmap> webappCfgmaps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, GatewayCfgmap> gatewayCfgmaps = new ConcurrentHashMap<>();

    private final static String DOMAINS_KEY = "contentgrid.routing.domains";
    private final static String DOMAINS_SEPARATOR = ",";
    private final static String APPLICATION_ID_LABEL = "app.contentgrid.com/application-id";
    private final static String ISSUER_KEY = "contentgrid.oidc.issuer";
    private final static String CLIENT_ID_KEY = "contentgrid.oidc.client";
    private final static String API_URL_KEY = "contentgrid.api.url";

    public void discoverWebapp() {
        //
        // service-type: webapp
        //
        client.configMaps()
                .inNamespace(discoveryProperties.getWebapp().getNamespace())
                .withLabels(discoveryProperties.getWebapp().getLabels())
                .inform(new ResourceEventHandler<ConfigMap>() {
                    @Override
                    public void onAdd(ConfigMap cm) {
                        for (String domain : cm.getData().get(DOMAINS_KEY).split(DOMAINS_SEPARATOR)) {
                            log.debug("Registered frontend domain {}", domain);
                            var webappCfg = new WebappCfgmap(
                                    cm.getMetadata().getLabels().get(APPLICATION_ID_LABEL),
                                    cm.getData().get(ISSUER_KEY),
                                    cm.getData().get(CLIENT_ID_KEY)
                            );
                            webappCfgmaps.put(domain, webappCfg);
                        }

                    }

                    @Override
                    public void onUpdate(ConfigMap oldObj, ConfigMap newObj) {
                        this.onAdd(newObj);
                    }

                    @Override
                    public void onDelete(ConfigMap cm, boolean deletedFinalStateUnknown) {
                        for (String domain : cm.getData().get(DOMAINS_KEY).split(DOMAINS_SEPARATOR)) {
                            log.debug("Deleting frontend domain {}", domain);
                            webappCfgmaps.remove(domain);
                        }
                    }
                }, discoveryProperties.getWebapp().getResyncIntervalSeconds() * 1000L);
    }
    public void discoverGateway() {
        //
        // service-type: gateway
        //
        client.configMaps()
                .inNamespace(discoveryProperties.getGateway().getNamespace())
                .withLabels(discoveryProperties.getGateway().getLabels())
                .inform(new ResourceEventHandler<ConfigMap>() {
                    @Override
                    public void onAdd(ConfigMap cm) {
                        var firstDomain = cm.getData().get(DOMAINS_KEY).split(DOMAINS_SEPARATOR)[0];
                        var appId = cm.getMetadata().getLabels().get(APPLICATION_ID_LABEL);
                        gatewayCfgmaps.put(appId, new GatewayCfgmap(appId, "https://" + firstDomain));
                        log.debug("Registered backend domain {}", firstDomain);
                    }

                    @Override
                    public void onUpdate(ConfigMap oldObj, ConfigMap newObj) {
                        this.onAdd(newObj);
                    }

                    @Override
                    public void onDelete(ConfigMap cm, boolean deletedFinalStateUnknown) {
                        log.debug("Deleting backend domain {}", cm.getData().get(DOMAINS_KEY).split(DOMAINS_SEPARATOR)[0]);
                        gatewayCfgmaps.remove(cm.getMetadata().getLabels().get(APPLICATION_ID_LABEL));
                    }
                }, discoveryProperties.getGateway().getResyncIntervalSeconds() * 1000L);


    }

    public Optional<WebappConfiguration> findByDomain(@NonNull String domain) {
        return Optional.ofNullable(this.webappCfgmaps.get(domain))
                .flatMap(wa -> Optional.ofNullable(wa.applicationId())
                        .map(this.gatewayCfgmaps::get)
                        .map(gw -> WebappConfiguration.from(wa, gw)));
    }


}

