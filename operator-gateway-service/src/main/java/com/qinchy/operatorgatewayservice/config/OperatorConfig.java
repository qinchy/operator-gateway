package com.qinchy.operatorgatewayservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "config")
@Data
public class OperatorConfig {
    private Map<String, Map<String, String>> operatorConfig;
}
