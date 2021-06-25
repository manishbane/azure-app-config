package com.baneman.azure.appconfig;

import com.azure.core.http.okhttp.OkHttpAsyncHttpClientBuilder;
import com.azure.data.appconfiguration.ConfigurationClient;
import com.azure.data.appconfiguration.ConfigurationClientBuilder;
import com.azure.data.appconfiguration.models.SettingSelector;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Allows to connect to an Azure App Config endpoint and retrieve entries, including rerouting to Azure Key Vault
 */
public class AppConfigReader {
    private static final String KEY_VAULT_JSON_CONTENT_TYPE = "application/vnd.microsoft.appconfig.keyvaultref+json;charset=utf-8";

    private final Map<URI, SecretClient> keyVaultClientCache = new ConcurrentHashMap<>();
    private final TokenCredentialFactory tokenCredentialFactory;

    public AppConfigReader(TokenCredentialFactory tokenCredentialFactory) {
        this.tokenCredentialFactory = tokenCredentialFactory;
    }

    /**
     * Reads key values from Azure App config endpoint, removing a namespace prefix in the key
     * Typically:
     *  sharedNameSpace = 'shared',
     *  applicationNamespace = 'cetrack/',
     *  commonLabel = 'common'
     *  envLabel = 'test3'
     * @return a key value Map, the value is typed as Object instead of String to allow building a Spring MapPropertySource
     */
    public Map<String,Object> readConfigurationMap(String endpoint, String sharedNameSpace, String applicationNamespace,
                                                   String commonLabel, String envLabel)
    {
        ConfigurationClient configurationClient = new ConfigurationClientBuilder()
                .endpoint(endpoint)
                .credential(tokenCredentialFactory.get())
                .httpClient(new OkHttpAsyncHttpClientBuilder().build())
                .buildClient();

        StringBuffer searchLabel = new StringBuffer(commonLabel);

        if(envLabel != null && !envLabel.isEmpty()) {
            searchLabel.append(",");
            searchLabel.append(envLabel);
        }

        String searchLabelStr = searchLabel.toString();
        SettingSelector selector = new SettingSelector().setKeyFilter(sharedNameSpace + "*").setLabelFilter(searchLabelStr);
        Map<String,Object> configMap = configurationClient.listConfigurationSettings(selector).stream()
                .map(configurationSetting -> {
                    String key = configurationSetting.getKey().replace(sharedNameSpace, "");
                    String value = configurationSetting.getValue();

                    //redirection to a keyvault entry
                    if(KEY_VAULT_JSON_CONTENT_TYPE.equals(configurationSetting.getContentType())) {
                        value = getKeyVaultEntry(value);
                    }

                    //System.out.printf("Reading %s: %s%n", key, value);

                    return Tuples.of(key,value);
                })
                .collect(Collectors.toMap(Tuple2::getT1, Tuple2::getT2, (t1, t2) -> t2));

        if (applicationNamespace != null) {
            selector = new SettingSelector().setKeyFilter(applicationNamespace + "*").setLabelFilter(searchLabelStr);
            configurationClient.listConfigurationSettings(selector).stream()
                    .forEach(configurationSetting -> {
                        String key = configurationSetting.getKey().replace(applicationNamespace, "");
                        String value = configurationSetting.getValue();

                        //redirection to a keyvault entry
                        if(KEY_VAULT_JSON_CONTENT_TYPE.equals(configurationSetting.getContentType())) {
                            value = getKeyVaultEntry(value);
                        }

                        //System.out.printf("Reading %s: %s%n", key, value);

                        configMap.put(key,value);
                    });
        }
        return configMap;
    }

    /**
     * Get the key vault entry from URL
     * @param jsonKeyVaultUri like {"uri:"https://kvau-die1-gl-int001.vault.azure.net/secrets/spring-poc-secret"}
     * @return
     */
    private String getKeyVaultEntry(String jsonKeyVaultUri) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            URI uri = new URI(mapper.readValue(jsonKeyVaultUri, KeyVaultUrl.class).getUri());

            String[] urlParts = uri.getSchemeSpecificPart().split("/");
            String secretKey = urlParts[urlParts.length - 1];

            return getSecretClient(uri).getSecret(secretKey).getValue();
        } catch (RuntimeException | IOException | URISyntaxException e) {
            throw new IllegalStateException("Error retrieving Key Vault entry for "+ jsonKeyVaultUri, e);
        }
    }

    private SecretClient getSecretClient(URI uri) {
        return keyVaultClientCache.computeIfAbsent(uri, key ->
                new SecretClientBuilder()
        .vaultUrl("https://" + uri.getHost())
        .credential(tokenCredentialFactory.get())
        .httpClient(new OkHttpAsyncHttpClientBuilder().build())
        .buildClient());
    }

}
