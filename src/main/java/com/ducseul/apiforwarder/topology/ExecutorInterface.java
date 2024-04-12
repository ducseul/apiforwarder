package com.ducseul.apiforwarder.topology;

import com.ducseul.apiforwarder.entity.MapEntry;
import com.ducseul.apiforwarder.entity.RequestWrapper;
import org.springframework.http.ResponseEntity;

public interface ExecutorInterface<T> {
    ResponseEntity<T>  process(RequestWrapper requestWrapper, MapEntry mapperEndpoint, boolean usingRedis) throws Exception;
}
