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

    public String toCurl() {
        StringBuilder curlCommand = new StringBuilder("curl --location ");
        curlCommand.append("--request ").append(method).append(" ");
        curlCommand.append("'").append(requestUrl).append("' ");
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, Serializable> entry : headers.entrySet()) {
                curlCommand.append("--header '")
                        .append(entry.getKey())
                        .append(": ")
                        .append(entry.getValue())
                        .append("' ");
            }
        }
        // Add cookies
        if (cookies != null && !cookies.isEmpty()) {
            curlCommand.append("--header 'cookie: ");
            for (Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                curlCommand.append(cookieEntry.getKey())
                        .append("=")
                        .append(cookieEntry.getValue())
                        .append("; ");
            }
            // Remove the last semicolon and space
            curlCommand.setLength(curlCommand.length() - 2);
            curlCommand.append("' ");
        }
        // Add body data if it's not empty
        if (body != null && !body.isEmpty()) {
            curlCommand.append("--data-raw '").append(body).append("' ");
        }
        // Set timeouts
        if (connectionTimeout > 0) {
            curlCommand.append("--connect-timeout ").append(connectionTimeout).append(" ");
        }
        if (readTimeOut > 0) {
            curlCommand.append("--max-time ").append(readTimeOut).append(" ");
        }
        // Optionally add verbose output
        if (Boolean.TRUE.equals(isVerbose)) {
            curlCommand.append("--verbose ");
        }
        return curlCommand.toString();
    }
}
