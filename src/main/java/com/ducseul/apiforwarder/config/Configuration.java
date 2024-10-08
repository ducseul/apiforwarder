package com.ducseul.apiforwarder.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Getter
@PropertySource("classpath:application.yml")
public class Configuration {
    @Autowired
    private Environment env;

    @Value("${application.forwarder.verbose:false}")
    private boolean isVerbose;

    @Value("${application.forwarder.connection-timeout}")
    private Integer connectionTimeout;

    @Value("${application.forwarder.read-timeout}")
    private Integer readTimeOut;

    @Value("${application.redis.enabled}")
    private boolean redisEnable;

    @Value("${application.redis.time-to-live}")
    private Integer dataTTL;

    @Override
    public String toString() {
        return "Configuration{" +
                ", isVerbose=" + isVerbose +
                ", connectionTimeout=" + connectionTimeout +
                ", readTimeOut=" + readTimeOut +
                ", usingRedis=" + redisEnable +
                ", dataTimeToLive=" + dataTTL +
                '}';
    }
}
