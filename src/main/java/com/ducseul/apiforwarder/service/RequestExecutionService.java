package com.ducseul.apiforwarder.service;

import com.ducseul.apiforwarder.entity.MapEntry;
import com.ducseul.apiforwarder.entity.RequestWrapper;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

public interface RequestExecutionService {
    <T> ResponseEntity<T> doMock(RequestWrapper requestWrapper, MapEntry mapperEndpoint) throws Exception;

    <T> ResponseEntity<T> doForward(RequestWrapper requestWrapper, MapEntry mapperEndpoint, boolean usingRedis) throws Exception;
}
