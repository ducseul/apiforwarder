package com.ducseul.apiforwarder.topology;

import com.ducseul.apiforwarder.entity.MapEntry;
import com.ducseul.apiforwarder.entity.RequestWrapper;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class DummyExecutorImp implements ExecutorInterface<String> {
    private static final Logger logger = LoggerFactory.getLogger(DummyExecutorImp.class);

    @Override
    public ResponseEntity<String> process(RequestWrapper requestWrapper, MapEntry mapperEndpoint, boolean usingRedis) throws Exception {
        StringBuilder log = new StringBuilder(2000);
        log.append(new Gson().toJson(requestWrapper));
        log.append("\n{Not supported yet}");
        logger.info("{}", log);
        return new ResponseEntity<>("{Not supported yet}", HttpStatus.NOT_ACCEPTABLE);
    }
}
