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
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<String> getMapping(HttpServletRequest request) {
        return process(request);
    }

    @PostMapping
    public ResponseEntity<String> postMapping(HttpServletRequest request) {
        return process(request);
    }

    @PutMapping
    public ResponseEntity<String> putMapping(HttpServletRequest request) {
        return process(request);
    }

    @PatchMapping
    public ResponseEntity<String> patchMapping(HttpServletRequest request) {
        return process(request);
    }

    @DeleteMapping
    public ResponseEntity<String> deleteMapping(HttpServletRequest request) {
        return process(request);
    }

    @RequestMapping(method = RequestMethod.HEAD)
    public ResponseEntity<String> headMapping(HttpServletRequest request) {
        return process(request);
    }

    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<String> optionMapping(HttpServletRequest request) {
        return process(request);
    }


    public ResponseEntity<String> process(HttpServletRequest request) {
        HTTPUtils httpUtils = new HTTPUtils();
        String originUrl = request.getRequestURI();
        MapEntry mapperEndpoint = null;
        List<MapEntry> endpointMap = endpointMapConfig.getEndpointMapConfig();
        for (MapEntry mapEntry : endpointMap) {
            if (originUrl.startsWith(mapEntry.getKey().trim())) {
                mapperEndpoint = mapEntry;
                break;
            }
        }

        if (mapperEndpoint == null){
            return new ResponseEntity<>("Don't have forward rule for endpoint yet", HttpStatus.FORBIDDEN);
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
                return new ResponseEntity<>(cacheValue.getResponseBody(), HttpStatus.OK);
            }
        }
        switch (Constants.API_MODE.from(mapperEndpoint.getMode())){
            case FORWARD:{
                return doForward(requestWrapper, mapperEndpoint, configuration.getRedisEnable());
            }
            case MOCK:{
                return doMock(requestWrapper, mapperEndpoint);
            }
        }
        return new ResponseEntity<>("{Not supported yet}", HttpStatus.NOT_ACCEPTABLE);
    }

    private ResponseEntity<String> doMock(RequestWrapper requestWrapper, MapEntry mapperEndpoint) {
        String jsonMockPath = mapperEndpoint.getValue();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        StringBuilder log = new StringBuilder("-----------------------------\n");
        log.append(String.format("Mock Request: %s", mapperEndpoint.getKey()));
        return new ResponseEntity<>(FileUtils.getFileContent(jsonMockPath), responseHeaders, HttpStatus.OK);
    }

    private ResponseEntity<String> doForward(RequestWrapper requestWrapper, MapEntry mapperEndpoint, boolean usingRedis) {
        HTTPUtils httpUtils = new HTTPUtils();
        String forwardUrl = requestWrapper.getOriginUrl().replaceFirst(mapperEndpoint.getKey(), mapperEndpoint.getValue());
        requestWrapper.setRequestUrl(forwardUrl);;

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.TEXT_HTML);

        RequestWrapper responseWrapper = httpUtils.doRequest(requestWrapper);
        if (responseWrapper.getHeaders() != null) {
            for (String headerKey : responseWrapper.getHeaders().keySet()) {
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
        return new ResponseEntity<>(responseWrapper.getBody(), responseHeaders, HttpStatus.OK);
    }
}
