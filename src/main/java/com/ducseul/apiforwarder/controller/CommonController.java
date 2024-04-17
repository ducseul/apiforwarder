package com.ducseul.apiforwarder.controller;


import com.ducseul.apiforwarder.config.Configuration;
import com.ducseul.apiforwarder.config.EndpointMapConfig;
import com.ducseul.apiforwarder.config.ProxyConfiguration;
import com.ducseul.apiforwarder.entity.Constants;
import com.ducseul.apiforwarder.entity.MapEntry;
import com.ducseul.apiforwarder.entity.RequestWrapper;
import com.ducseul.apiforwarder.entity.ResponseCacheEntity;
import com.ducseul.apiforwarder.service.RedisService;
import com.ducseul.apiforwarder.topology.*;
import com.ducseul.apiforwarder.utils.HTTPUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/**")
public class CommonController {
    private final Configuration configuration;
    private final EndpointMapConfig endpointMapConfig;
    private final ProxyConfiguration proxyConfiguration;
    private final RedisService redisService;
    private final ForwardExecutorImp forwdExecutor;
    private final MockExecutorImp mockExecutor;
    private final DummyExecutorImp dummyExecutor;
    private final EvalMockExecutorImp evalMockExecutorImp;

    private final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    private static final Logger logger = LoggerFactory.getLogger(CommonController.class);

    @GetMapping
    public <T> ResponseEntity<T> getMapping(HttpServletRequest request) {
        return process(request, "GET");
    }

    @PostMapping
    public <T> ResponseEntity<T> postMapping(HttpServletRequest request) {
        return process(request, "POST");
    }

    @PutMapping
    public <T> ResponseEntity<T> putMapping(HttpServletRequest request) {
        return process(request, "PUT");
    }

    @PatchMapping
    public <T> ResponseEntity<T> patchMapping(HttpServletRequest request) {
        return process(request, "PATCH");
    }

    @DeleteMapping
    public <T> ResponseEntity<T> deleteMapping(HttpServletRequest request) {
        return process(request, "DELETE");
    }

    @RequestMapping(method = RequestMethod.HEAD)
    public <T> ResponseEntity<T> headMapping(HttpServletRequest request) {
        return process(request, "HEAD");
    }

    @RequestMapping(method = RequestMethod.OPTIONS)
    public <T> ResponseEntity<T> optionsMapping(HttpServletRequest request) {
        return process(request, "OPTIONS");
    }

    @SuppressWarnings("unchecked")
    public <T> ResponseEntity<T> process(HttpServletRequest request, String method) {
        Long startTime = System.currentTimeMillis();
        HTTPUtils httpUtils = new HTTPUtils();
        String originUrl = request.getQueryString() != null
                ? request.getRequestURI() + "?" + request.getQueryString()
                : request.getRequestURI();
        MapEntry mapperEndpoint = null;
        List<MapEntry> endpointMap = endpointMapConfig.getEndpointMapConfig();
        for (MapEntry mapEntry : endpointMap) {
            if (originUrl.startsWith(mapEntry.getKey().trim())) {
                mapperEndpoint = mapEntry;
                break;
            }
        }

        if (mapperEndpoint == null) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .headers(new HttpHeaders())
                    .body((T) "Don't have forward rule for endpoint yet");
        }
        if (mapperEndpoint.getMethod() != null) {
            boolean allowMethod = Arrays.stream(mapperEndpoint.getMethod()).anyMatch(method::equals);
            if (!allowMethod) {
                return new ResponseEntity<>((T) "Method is not allow", HttpStatus.NOT_ACCEPTABLE);
            }
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
                .startProcessTime(startTime)
                .build();

        if (configuration.isRedisEnable() && !header.containsKey("Cache-disable")) {
            String key = gson.toJson(requestWrapper);
            ResponseCacheEntity cacheValue = redisService.getValues(key);
            if (cacheValue != null) {
                return new ResponseEntity<>((T) cacheValue.getResponseBody(), HttpStatus.OK);
            }
        }
        try {
            @SuppressWarnings("rawtypes")
            ExecutorInterface topo = null;
            switch (Constants.API_MODE.from(mapperEndpoint.getMode())) {
                case FORWARD: {
                    topo = forwdExecutor;
                    break;
                }
                case MOCK: {
                    topo = mockExecutor;
                    break;
                }
                case EVAL: {
                    topo = evalMockExecutorImp;
                    break;
                }
                default:
                    topo = dummyExecutor;
            }
            return topo.process(requestWrapper, mapperEndpoint, false);
        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
            UUID checkpoint = UUID.randomUUID();
            HashMap<String, String> returnValue = new HashMap<>();
            returnValue.put("message", exception.getMessage());
            returnValue.put("checkpoint", checkpoint.toString());
            logger.info("{}\n{}", new Gson().toJson(requestWrapper), new Gson().toJson(returnValue));
            return new ResponseEntity<>((T) new Gson().toJson(returnValue), HttpStatus.BAD_GATEWAY);
        }

    }
}
