package com.ducseul.apiforwarder.config;


import com.ducseul.apiforwarder.entity.MapEntry;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "application.forwarder")
public class EndpointMapConfig {
    private List<MapEntry> endpointMapConfig;
}