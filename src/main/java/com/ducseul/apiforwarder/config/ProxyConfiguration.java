package com.ducseul.apiforwarder.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "application.forwarder.proxy")
public class ProxyConfiguration {
    private boolean useProxy;
    private String host;
    private int port;
}
