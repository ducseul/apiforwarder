package com.ducseul.apiforwarder.entity;

import com.google.gson.annotations.Expose;
import lombok.*;

import java.io.Serializable;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequestWrapper {
    @Expose
    private String requestUrl;
    @Expose
    private String originUrl;
    @Expose
    private String filePath;
    @Expose
    private Constants.METHOD method;
    @Expose
    private String requestIp;
    private Map<String, Serializable> headers;
    private HashMap<String, String> cookies;
    @Expose
    private String body;
    private Integer readTimeOut = 0;
    private Integer connectionTimeout = 0;
    private Proxy proxy;
    private Boolean isVerbose;
}
