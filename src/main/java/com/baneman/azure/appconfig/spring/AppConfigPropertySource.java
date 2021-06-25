package com.baneman.azure.appconfig.spring;

import com.baneman.azure.appconfig.AppConfigReader;
import com.baneman.azure.appconfig.TokenCredentialFactory;
import org.springframework.core.env.PropertySource;

import java.util.Map;

/**
 <pre>
    ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"classpath:bean.xml"}, false);
    context.getEnvironment().getPropertySources().addFirst(new AppConfigPropertySource("bootstrap"), new TokenCredentialFactory());
    context.refresh();
 </pre>
 */
public class AppConfigPropertySource extends PropertySource<Object> {
    public static final String NAMESPACE = "APP_CONFIG_NAMESPACE";
    public static final String DEFAULT_NAMESPACE = "/shared/";
    public static final String DEFAULT_LABEL = "common";
    public static final String ENDPOINT = "APP_CONFIG_ENDPOINT";
    public static final String LABEL = "APP_CONFIG_LABEL";

    private final AppConfigReader reader;
    private final Map<String, Object> configHolder;

    public AppConfigPropertySource(String name, TokenCredentialFactory factory) {
        super(name);

        String endpoint = getVariable(ENDPOINT);
        String inputNamespace = getVariableOrNull(NAMESPACE);
        String label = getVariableOrNull(LABEL);
        String namespace = inputNamespace != null && inputNamespace.matches("/$") ? inputNamespace + "/" : inputNamespace;

        reader = new AppConfigReader(factory);
        configHolder = reader.readConfigurationMap(endpoint, DEFAULT_NAMESPACE, namespace, DEFAULT_LABEL, label);
    }

    @Override
    public Object getProperty(String name) {
        return configHolder.get(name);
    }

    public static final String getVariableOrNull(String varName) {
        String result = System.getenv(varName);
        if (result != null && !result.isEmpty()) {
            return  result;
        }

        return System.getProperty(varName);
    }

    public static final String getVariable(String varName) {
        String result = getVariableOrNull(varName);

        if (result == null && result.isEmpty()) {
            throw new IllegalStateException(varName + " not defined");
        }

        return result;
    }
}
