package com.baneman.azure.appconfig.spring;

import com.baneman.azure.appconfig.AppConfigReader;
import com.baneman.azure.appconfig.TokenCredentialFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.Map;

/**
 * Injects property values retrieved from Azure App Config
 * To be declared like this in a Spring configuration file:
 *
 * <pre>
    <bean id="azurePropertyPlaceHolderConfigurer" class="com.baneman.azure.spring.AzurePropertySourcesPlaceHolderConfigurer">
        <constructor-arg index="0" name="tokenCredentialFactory" ref="tokenCredentialFactory" />
        <property name="ignoreUnresolvablePlaceholders" value="false" />
        <property name="ignoreResourceNotFound" value="false" />
        <property name="order" value="0" />
    </bean>
 * </pre>
 *
 */
public class AzurePropertySourcesPlaceHolderConfigurer extends PropertySourcesPlaceholderConfigurer {
    private Environment environment;
    private final AppConfigReader reader;
    private final String namespace;
    private final String endpoint;
    private final String label;

    public AzurePropertySourcesPlaceHolderConfigurer(TokenCredentialFactory tokenCredentialFactory) {
        endpoint = AppConfigPropertySource.getVariable(AppConfigPropertySource.ENDPOINT);
        String inputNamespace = AppConfigPropertySource.getVariableOrNull(AppConfigPropertySource.NAMESPACE);
        namespace = inputNamespace != null && inputNamespace.matches("/$") ? inputNamespace + "/" : inputNamespace;
        label = AppConfigPropertySource.getVariableOrNull(AppConfigPropertySource.LABEL);

        reader = new AppConfigReader(tokenCredentialFactory);
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
        super.setEnvironment(environment);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        //TokenCredential tokenCredential = beanFactory.getBean(TokenCredential.class);

        Map<String, Object> configMap = reader.readConfigurationMap(endpoint, AppConfigPropertySource.DEFAULT_NAMESPACE,
                                                                    namespace, AppConfigPropertySource.DEFAULT_LABEL, label);
        MutablePropertySources propertySources = ((ConfigurableEnvironment) environment).getPropertySources();
        propertySources.addFirst(new MapPropertySource("bootstrap", configMap));
        //propertySources.addLast(new PropertiesPropertySource("system", System.getProperties()));

        setPropertySources(propertySources);

        super.postProcessBeanFactory(beanFactory);
    }
}
