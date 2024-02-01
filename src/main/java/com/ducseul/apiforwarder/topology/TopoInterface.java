package com.ducseul.apiforwarder.topology;

import org.apache.catalina.connector.ResponseFacade;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;

public interface TopoInterface {
    ResponseEntity process(HttpServletRequest request);
}
