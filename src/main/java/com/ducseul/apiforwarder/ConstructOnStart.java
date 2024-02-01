package com.ducseul.apiforwarder;

import com.ducseul.apiforwarder.config.Configuration;
import com.ducseul.apiforwarder.config.EndpointMapConfig;
import com.ducseul.apiforwarder.config.ProxyConfiguration;
import com.ducseul.apiforwarder.entity.MapEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Comparator;

@Component
public class ConstructOnStart {
    private static final Logger logger = LoggerFactory.getLogger(ConstructOnStart.class);
    @Autowired
    private Configuration config;
    @Autowired
    private EndpointMapConfig endpointMapConfig;
    @Autowired
    private ProxyConfiguration proxyConfiguration;

    @PostConstruct
    public void init() {
        // Make sure every folder exist before hence
        try {
            logger.info("PostConstruct start checking process.");
            logger.info(config.toString());

            endpointMapConfig.getEndpointMapConfig().stream().forEach(p -> {
                if (p.getMode() == null){
                    logger.warn("Config %s has no mode define. I will be guess base on values. ");
                }
                if (p.getPriority() == null){
                    p.setPriority(Integer.MAX_VALUE);
                }
            });
            endpointMapConfig.getEndpointMapConfig().sort(new Comparator<MapEntry>() {
                @Override
                public int compare(MapEntry o1, MapEntry o2) {
                    if (o1.getPriority() == null || o2.getPriority() == null){
                        return -1;
                    }
                    return o1.getPriority().compareTo(o2.getPriority());
                }
            });
            for (MapEntry enpointMap : endpointMapConfig.getEndpointMapConfig()) {
                switch (enpointMap.getMode()){
                    case "forward":
                        logger.info(String.format("Map context path %s to %s", enpointMap.getKey(), enpointMap.getValue()));
                        break;
                    case "mock":
                        logger.info(String.format("Map context path %s as file content %s", enpointMap.getKey(), enpointMap.getValue()));
                        break;
                }

            }
            logger.info("PostConstruct init and checking done.");
        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }
    }
}
