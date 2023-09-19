package com.contentgrid.liaison.kubernetes;

public record WebappConfiguration(String applicationId, String authority, String clientId, String apiUrl) {
    static WebappConfiguration from(WebappCfgmap webapp, GatewayCfgmap gateway) {
        return new WebappConfiguration(webapp.applicationId(), webapp.issuer(), webapp.clientId(), gateway.apiUrl());
    }
}
