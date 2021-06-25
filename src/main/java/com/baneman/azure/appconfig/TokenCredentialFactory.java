package com.baneman.azure.appconfig;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.AzureCliCredentialBuilder;
import com.azure.identity.ManagedIdentityCredentialBuilder;

/**
 * Allows to retrieve token credentials depending whether you are on Azure cloud or on-prem,
 * based on the MANAGED_IDENTITY_CLIENT_ID environment variable or system property being defined or not
 * to be declared like this in a Spring configuration file:
 *
    <pre>{@code
    <bean id="tokenCredentialFactory" class="com.baneman.azure.TokenCredentialfactory" />
   }</pre>
 */
public class TokenCredentialFactory {
    private static final String CLIENT_ID = "MANAGED_IDENTITY_CLIENT_ID";
    private static String getClientId() {
        String uuid = System.getenv(CLIENT_ID);
        if (uuid != null && !uuid.isEmpty()) {
            return uuid;
        }

        return System.getProperty(CLIENT_ID);
    }

    public TokenCredential get() {
        String uuid = getClientId();
        if (uuid == null || uuid.isEmpty()) {
            return localCredential();
        }

        return remoteCredential();
    }

    private TokenCredential remoteCredential() {
        return new ManagedIdentityCredentialBuilder().build();
    }

    private TokenCredential localCredential() {
        return new AzureCliCredentialBuilder().build();
    }
}
