package com.ducseul.apiforwarder.topology;

import com.ducseul.apiforwarder.entity.MapEntry;
import com.ducseul.apiforwarder.entity.RequestWrapper;
import com.ducseul.apiforwarder.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MockExecutorImp implements ExecutorInterface<String> {
    private static final Logger logger = LoggerFactory.getLogger(MockExecutorImp.class);
    @Override
    @SuppressWarnings("unchecked, rawtypes")
    public ResponseEntity process(RequestWrapper requestWrapper, MapEntry mapperEndpoint, boolean usingRedis) {
        String jsonMockPath = mapperEndpoint.getValue();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        StringBuilder log = new StringBuilder("\n\n-----------------------------\n");
        log.append(String.format("Mock Request: %s", mapperEndpoint.getKey()));
        logger.info("{}", log);
        return new ResponseEntity<>(FileUtils.getFileContent(jsonMockPath), responseHeaders, HttpStatus.OK);
    }
}
