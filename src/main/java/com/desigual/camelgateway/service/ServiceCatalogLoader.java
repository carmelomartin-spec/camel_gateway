package com.desigual.camelgateway.service;

import com.desigual.camelgateway.config.GatewayProperties;
import com.desigual.camelgateway.model.config.ServiceDefinition;
import com.desigual.camelgateway.model.config.ServiceDefinitionFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class ServiceCatalogLoader {

    @Bean
    public ServiceCatalog serviceCatalog(GatewayProperties gatewayProperties) throws Exception {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(
            "classpath*:config/environments/" + gatewayProperties.getEnvironment() + "/services/*.yml"
        );

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        List<ServiceDefinition> services = new ArrayList<>();

        for (Resource resource : resources) {
            ServiceDefinitionFile definitionFile = mapper.readValue(
                resource.getInputStream(),
                ServiceDefinitionFile.class
            );

            if (definitionFile.getService() != null) {
                services.add(definitionFile.getService());
            }
        }

        return new ServiceCatalog(services);
    }
}
