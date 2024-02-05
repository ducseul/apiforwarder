package com.ducseul.apiforwarder.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;

@Getter
@Setter
public class MapEntry {
    private String key;
    private String value;
    private String mode;
    private Integer priority;
    private String loadBalancer;
    private String stickMode;
    private String[] method;
}