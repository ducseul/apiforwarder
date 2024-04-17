package com.ducseul.apiforwarder.topology;

import com.ducseul.apiforwarder.entity.MapEntry;
import com.ducseul.apiforwarder.entity.RequestWrapper;
import com.ducseul.apiforwarder.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

@Service
public class EvalMockExecutorImp implements ExecutorInterface{
    private static final Logger logger = LoggerFactory.getLogger(EvalMockExecutorImp.class);
    @Override
    public ResponseEntity process(RequestWrapper requestWrapper, MapEntry mapperEndpoint, boolean usingRedis) throws Exception {
        String jsEvaluate = mapperEndpoint.getValue();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        StringBuilder log = new StringBuilder("\n\n-----------------------------\n");
        log.append(String.format("Mock Request: %s", mapperEndpoint.getKey()));

        StringBuilder js = new StringBuilder();
        js.append("var origin = '").append(mapperEndpoint.getKey()).append("';").append("\n");
        js.append(jsEvaluate).append("\n");
        logger.info("{}", js);

        logger.info("Evaluate javascript: \n{}", js);
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        Object output = engine.eval(js.toString());
        return new ResponseEntity<>("Haizz", responseHeaders, HttpStatus.OK);
    }
}
