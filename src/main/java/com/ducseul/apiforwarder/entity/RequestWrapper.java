package com.ducseul.apiforwarder.entity;

import com.google.gson.annotations.Expose;
import lombok.*;
import org.springframework.http.MediaType;

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
    @Expose
    private Map<String, Serializable> headers;
    @Expose
    private HashMap<String, String> cookies;
    @Expose
    private String body;
    private Integer readTimeOut = 0;
    private Integer connectionTimeout = 0;
    private Proxy proxy;
    private Boolean isVerbose;
    @Expose
    private MediaType contentType = MediaType.TEXT_HTML;
    private Long startProcessTime;
    @Expose
    private Long processTime;

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl.replaceAll("(?<!:)/+", "/")
        ;
    }

    public void setOriginUrl(String originUrl) {
        this.originUrl = originUrl.replaceAll("(?<!:)/+", "/")
        ;
    }
}
