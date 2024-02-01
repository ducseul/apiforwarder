package com.ducseul.apiforwarder.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
@Builder
public class ResponseCacheEntity {
    private String responseBody;
    private Map<String, Serializable> headers;
}
