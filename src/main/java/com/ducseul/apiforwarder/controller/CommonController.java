package com.ducseul.apiforwarder.controller;


import com.ducseul.apiforwarder.config.Configuration;
import com.ducseul.apiforwarder.config.EndpointMapConfig;
import com.ducseul.apiforwarder.config.ProxyConfiguration;
import com.ducseul.apiforwarder.entity.Constants;
import com.ducseul.apiforwarder.entity.MapEntry;
import com.ducseul.apiforwarder.entity.RequestWrapper;
import com.ducseul.apiforwarder.entity.ResponseCacheEntity;
import com.ducseul.apiforwarder.service.RedisService;
import com.ducseul.apiforwarder.utils.FileUtils;
import com.ducseul.apiforwarder.utils.HTTPUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/**")
public class CommonController {
    @Autowired
    private Configuration configuration;

    @Autowired
    private EndpointMapConfig endpointMapConfig;

    @Autowired
    private ProxyConfiguration proxyConfiguration;

    @Autowired
    private RedisService redisService;

    private final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    private static final Logger logger = LoggerFactory.getLogger(CommonController.class);

    @GetMapping
    public <T> ResponseEntity<T> getMapping(HttpServletRequest request) {
        return process(request);
    }

    @PostMapping
    public <T> ResponseEntity<T> postMapping(HttpServletRequest request) {
        return process(request);
    }

    @PutMapping
    public <T> ResponseEntity<T> putMapping(HttpServletRequest request) {
        return process(request);
    }

    @PatchMapping
    public <T> ResponseEntity<T> patchMapping(HttpServletRequest request) {
        return process(request);
    }

    @DeleteMapping
    public <T> ResponseEntity<T> deleteMapping(HttpServletRequest request) {
        return process(request);
    }

    @RequestMapping(method = RequestMethod.HEAD)
    public <T> ResponseEntity<T> headMapping(HttpServletRequest request) {
        return process(request);
    }

    @RequestMapping(method = RequestMethod.OPTIONS)
    public <T> ResponseEntity<T> optionMapping(HttpServletRequest request) {
        return process(request);
    }

    @SuppressWarnings("unchecked")
    public <T> ResponseEntity<T> process(HttpServletRequest request) {
        HTTPUtils httpUtils = new HTTPUtils();
        String originUrl = request.getRequestURI() + "?" + request.getQueryString();
        MapEntry mapperEndpoint = null;
        List<MapEntry> endpointMap = endpointMapConfig.getEndpointMapConfig();
        for (MapEntry mapEntry : endpointMap) {
            if (originUrl.startsWith(mapEntry.getKey().trim())) {
                mapperEndpoint = mapEntry;
                break;
            }
        }

        if (mapperEndpoint == null){
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .headers(new HttpHeaders())
                    .body((T) "Don't have forward rule for endpoint yet");
        }

        String requestBody = httpUtils.getBody(request);
        Map<String, Serializable> header = httpUtils.getHeaders(request);
        HashMap<String, String> cookies = httpUtils.getCookies(request);


        Proxy proxy = null;
        if (proxyConfiguration.isUseProxy()) {
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyConfiguration.getHost(), proxyConfiguration.getPort()));
        }

        String forwardUrl = originUrl.replaceFirst(mapperEndpoint.getKey(), mapperEndpoint.getValue());
        RequestWrapper requestWrapper = RequestWrapper.builder()
                .method(Constants.METHOD.getMethod(request.getMethod()))
                .requestUrl(forwardUrl)
                .originUrl(originUrl)
                .connectionTimeout(configuration.getConnectionTimeout())
                .readTimeOut(configuration.getReadTimeOut())
                .proxy(proxy)
                .headers(header)
                .cookies(cookies)
                .body(requestBody)
                .build();

        if (configuration.getRedisEnable() && !header.containsKey("Cache-disable"))  {
            String key = gson.toJson(requestWrapper);
            ResponseCacheEntity cacheValue = redisService.getValues(key);
            if(cacheValue != null){
                return new ResponseEntity<>((T) cacheValue.getResponseBody(), HttpStatus.OK);
            }
        }
        try {
            switch (Constants.API_MODE.from(mapperEndpoint.getMode())) {
                case FORWARD: {
                    return doForward(requestWrapper, mapperEndpoint, configuration.getRedisEnable());
                }
                case MOCK: {
                    return doMock(requestWrapper, mapperEndpoint);
                }
            }
        } catch (Exception exception){
            logger.error(exception.getMessage(), exception);
            UUID checkpoint = UUID.randomUUID();
            HashMap<String, String> returnValue = new HashMap<>();
            returnValue.put("message", exception.getMessage());
            returnValue.put("checkpoint", checkpoint.toString());
            return new ResponseEntity<>((T) new Gson().toJson(returnValue), HttpStatus.CHECKPOINT);
        }
        return new ResponseEntity<>((T) "{Not supported yet}", HttpStatus.NOT_ACCEPTABLE);
    }

    @SuppressWarnings("unchecked")
    private <T> ResponseEntity<T> doMock(RequestWrapper requestWrapper, MapEntry mapperEndpoint) {
        String jsonMockPath = mapperEndpoint.getValue();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        StringBuilder log = new StringBuilder("-----------------------------\n");
        log.append(String.format("Mock Request: %s", mapperEndpoint.getKey()));
        return new ResponseEntity<>((T) FileUtils.getFileContent(jsonMockPath), responseHeaders, HttpStatus.OK);
    }

    @SuppressWarnings("unchecked")
    private <T> ResponseEntity<T> doForward(RequestWrapper requestWrapper, MapEntry mapperEndpoint, boolean usingRedis) throws IOException {
        HTTPUtils httpUtils = new HTTPUtils();
        String forwardUrl = requestWrapper.getOriginUrl().replaceFirst(mapperEndpoint.getKey(), mapperEndpoint.getValue());
        requestWrapper.setRequestUrl(forwardUrl);;

        HttpHeaders responseHeaders = new HttpHeaders();

        RequestWrapper responseWrapper = HTTPUtils.doRequestUsingHTTPUrlConnection(requestWrapper);
        responseHeaders.setContentType(responseWrapper.getContentType());
        if (responseWrapper.getHeaders() != null) {
            for (String headerKey : responseWrapper.getHeaders().keySet()) {
                if(headerKey == null){
                    continue;
                }
                responseHeaders.put(headerKey, (List<String>) responseWrapper.getHeaders().get(headerKey));
            }
        }

        StringBuilder log = new StringBuilder("-----------------------------\n");
        log.append("Request: \n");
        log.append(new Gson().toJson(requestWrapper));
        log.append("Response: \n");
        log.append(new Gson().toJson(responseWrapper));

        if (usingRedis){

            ResponseCacheEntity cacheEntity = ResponseCacheEntity.builder()
                    .responseBody(responseWrapper.getBody())
                    .build();
            redisService.putValues(gson.toJson(requestWrapper), cacheEntity);
        }
        if(responseWrapper.getContentType()!= null
                && responseWrapper.getContentType().equals(MediaType.APPLICATION_PDF)){
            InputStream inputStream = Files.newInputStream(new File(responseWrapper.getFilePath()).toPath());
            byte[] pdfBytes = FileUtils.readStreamBytes(inputStream);
            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body((T) pdfBytes);
        }
        return new ResponseEntity<>((T) responseWrapper.getBody(), responseHeaders, HttpStatus.OK);
    }
}
