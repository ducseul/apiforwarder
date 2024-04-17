package com.ducseul.apiforwarder.topology;

import com.ducseul.apiforwarder.config.Configuration;
import com.ducseul.apiforwarder.entity.MapEntry;
import com.ducseul.apiforwarder.entity.RequestWrapper;
import com.ducseul.apiforwarder.entity.ResponseCacheEntity;
import com.ducseul.apiforwarder.service.RedisService;
import com.ducseul.apiforwarder.utils.FileUtils;
import com.ducseul.apiforwarder.utils.HTTPUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("rawtypes")
public class ForwardExecutorImp implements ExecutorInterface {
    private static final Logger logger = LoggerFactory.getLogger(ForwardExecutorImp.class);
    private final RedisService redisService;
    private final Configuration configuration;

    private final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();
    @Override
    public ResponseEntity process(RequestWrapper requestWrapper, MapEntry mapperEndpoint, boolean usingRedis) throws IOException {
        RequestWrapper responseWrapper = null;
        HttpHeaders responseHeaders = new HttpHeaders();
        try {
            String forwardUrl = requestWrapper.getOriginUrl().replaceFirst(mapperEndpoint.getKey(), mapperEndpoint.getValue());
            requestWrapper.setRequestUrl(forwardUrl);

            responseWrapper = HTTPUtils.doRequestUsingHTTPUrlConnection(requestWrapper);
            responseHeaders.setContentType(responseWrapper.getContentType());
            if (responseWrapper.getHeaders() != null) {
                for (String headerKey : responseWrapper.getHeaders().keySet()) {
                    if (headerKey == null) {
                        continue;
                    }
                    responseHeaders.put(headerKey, (List<String>) responseWrapper.getHeaders().get(headerKey));
                }
            }

            if (usingRedis) {
                ResponseCacheEntity cacheEntity = ResponseCacheEntity.builder()
                        .responseBody(responseWrapper.getBody())
                        .build();
                redisService.putValues(gson.toJson(requestWrapper), cacheEntity);
            }

            responseWrapper.setProcessTime(System.currentTimeMillis() - requestWrapper.getStartProcessTime());
        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        } finally {
            if (configuration.isVerbose()) {
                StringBuilder log = new StringBuilder("\n\n-----------------------------\n");
                log.append("Request: \n");
                log.append(new Gson().toJson(requestWrapper));
                log.append("\nResponse: \n");
                log.append(new Gson().toJson(responseWrapper));
                logger.info("{}", log);
            }
        }

        if (responseWrapper.getContentType() != null
                && responseWrapper.getContentType().equals(MediaType.APPLICATION_PDF)) {
            InputStream inputStream = Files.newInputStream(new File(responseWrapper.getFilePath()).toPath());
            byte[] pdfBytes = FileUtils.readStreamBytes(inputStream);
            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        }
        return new ResponseEntity<>(responseWrapper.getBody(), responseHeaders, HttpStatus.OK);
    }
}
