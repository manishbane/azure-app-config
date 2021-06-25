package com.baneman.azure.appconfig;

/**
 * Utility class to decode a URI retrieved from a JSON string. Used to decode app config values that reference a KeyVault
 */
public class KeyVaultUrl {
    private String uri;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
