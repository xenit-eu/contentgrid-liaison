package com.contentgrid.liaison.kubernetes;

public record WebappConfiguration(
        String applicationId,
        String authority,
        String clientId,
        String apiUrl,
        String uiConfig
) {
    static WebappConfiguration from(WebappCfgmap webapp, GatewayCfgmap gateway) {
        return new WebappConfiguration(webapp.applicationId(), webapp.issuer(), webapp.clientId(), gateway.apiUrl(), null);
    }
    static WebappConfiguration from(WebappCfgmap webapp, GatewayCfgmap gateway, UiConfigCfgmap uiConfig) {
        return new WebappConfiguration(webapp.applicationId(), webapp.issuer(), webapp.clientId(), gateway.apiUrl(),
                uiConfig.payload());
    }
}
