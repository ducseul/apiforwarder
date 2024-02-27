package com.ducseul.apiforwarder.service;

import com.ducseul.apiforwarder.entity.MapEntry;
import com.ducseul.apiforwarder.entity.RequestWrapper;
import org.springframework.http.ResponseEntity;

public class RequestExecutionServiceImp implements RequestExecutionService{

    @Override
    public <T> ResponseEntity<T> doMock(RequestWrapper requestWrapper, MapEntry mapperEndpoint) {
        return null;
    }

    @Override
    public <T> ResponseEntity<T> doForward(RequestWrapper requestWrapper, MapEntry mapperEndpoint, boolean usingRedis) throws Exception {
        return null;
    }


}
