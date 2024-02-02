package com.ducseul.apiforwarder.utils;

import com.ducseul.apiforwarder.entity.RequestWrapper;
import com.google.gson.Gson;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public class HTTPUtils {
    public RequestWrapper doRequest(RequestWrapper requestWrapper) {
        RequestWrapper responseWrapper = RequestWrapper.builder()
                .requestUrl(requestWrapper.getRequestUrl())
                .method(requestWrapper.getMethod())
                .isVerbose(requestWrapper.getIsVerbose())
                .build();
        RestTemplate restTemplate = createRestTemplate(requestWrapper);
        HttpHeaders requestHeader = new HttpHeaders();
        requestHeader.setAccept(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_XML));
        for (String headerKey : requestWrapper.getHeaders().keySet()) {
            String headerValue = requestWrapper.getHeaders().get(headerKey).toString();
            ArrayList<String> headerArr = new ArrayList<>();
            headerArr.add(headerValue);
            requestHeader.put(headerKey, headerArr);
        }

        HttpEntity<String> requestEntity = new HttpEntity<>(requestWrapper.getBody(), requestHeader);

        try {
            ResponseEntity<String> response = restTemplate.exchange(requestWrapper.getRequestUrl(),
                    Objects.requireNonNull(HttpMethod.resolve(requestWrapper.getMethod().toString())),
                    requestEntity,
                    String.class);

            responseWrapper.setHeaders(getHeaders(response.getHeaders()));
            MediaType contentType = response.getHeaders().getContentType();
            if (contentType != null) {
                if (contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
                    String jsonResponse = response.getBody();
//                    System.out.println("Received JSON response: " + jsonResponse);
                    responseWrapper.setBody(jsonResponse);
                } else if (contentType.isCompatibleWith(MediaType.APPLICATION_OCTET_STREAM)) {
                    InputStream inputStream = new ByteArrayInputStream(response.getBody().getBytes());
                    try {
                        // Write file to temp folder
                        Path tempFilePath = Files.createTempFile("api_response", ".file");
                        Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("File written to temp folder: " + tempFilePath.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (contentType.isCompatibleWith(MediaType.APPLICATION_XML)) {
                    // Handle XML response here if needed
                    throw new RuntimeException("Unsupported content type: " + contentType);
                } else if (contentType.isCompatibleWith(MediaType.TEXT_HTML)) {
                    // REST template is designed to consume RestServices.
                    responseWrapper.setBody("fuck");
                } else {
                    throw new RuntimeException("Unsupported content type: " + contentType);
                }
            } else {
                throw new RuntimeException("No content type provided in the response.");
            }
        } catch (Exception exception){
            UUID checkpoint = UUID.randomUUID();
            HashMap<String, String> returnValue = new HashMap<>();
            returnValue.put("message", exception.getMessage());
            returnValue.put("checkpoint", checkpoint.toString());
            responseWrapper.setBody(new Gson().toJson(returnValue));

        }
        return responseWrapper;
    }

    public static RequestWrapper doRequestUsingHTTPUrlConnection(RequestWrapper requestWrapper) {
        RequestWrapper responseWrapper = RequestWrapper.builder()
                .requestUrl(requestWrapper.getRequestUrl())
                .method(requestWrapper.getMethod())
                .isVerbose(requestWrapper.getIsVerbose())
                .build();

        try {
            URL url = new URL(requestWrapper.getRequestUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(requestWrapper.getMethod().toString());

            // Set request headers
            for (Map.Entry<String, Serializable> entry : requestWrapper.getHeaders().entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue().toString());
            }

            // Enable input/output streams
            connection.setDoOutput(true);

            if(requestWrapper.getBody() != null && !requestWrapper.getBody().isEmpty()) {
                // Set content type based on the request body
                connection.setRequestProperty("Content-Type", "application/json");

                // Write request body
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(requestWrapper.getBody().getBytes());
                    outputStream.flush();
                }
            }

            int responseCode = connection.getResponseCode();
            InputStream inputStream = (responseCode < 400) ? connection.getInputStream() : connection.getErrorStream();

            // Set response headers
            Map<String, List<String>> headerFields = connection.getHeaderFields();
            responseWrapper.setHeaders(headerFields.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> new ArrayList<>(entry.getValue())
                    )));

            // Process response based on content type
            String contentType = connection.getHeaderField("Content-Type");
            if (contentType != null) {
                switch (contentType) {
                    case "application/json":
                        String jsonResponse = readStream(inputStream);
                        responseWrapper.setContentType(MediaType.APPLICATION_JSON);
                        responseWrapper.setBody(jsonResponse);
                        break;
                    case "application/octet-stream":
                        // Handle binary response (if needed)
                        throw new UnsupportedOperationException("Binary response handling not implemented.");
                    case "application/xml":
                        // Handle XML response here if needed
                        throw new RuntimeException("Unsupported content type: " + contentType);
                    case "text/html":
                        // Handle HTML response (if needed)
                        responseWrapper.setBody("HTML response received");
                        break;
                    case "application/pdf":
                        // Handle PDF response here if needed
                        Path tempPath = savePdfToFile(inputStream);
                        if(tempPath.toFile().exists() && tempPath.toFile().canRead()){
                            responseWrapper.setFilePath(tempPath.toString());
                        }
                        responseWrapper.setContentType(MediaType.APPLICATION_PDF);
                        break;
                    default:
                        throw new RuntimeException("Unsupported content type: " + contentType);
                }
            } else {
                throw new RuntimeException("No content type provided in the response.");
            }


            connection.disconnect();
        } catch (IOException exception) {
            // Handle exceptions
            UUID checkpoint = UUID.randomUUID();
            HashMap<String, String> returnValue = new HashMap<>();
            returnValue.put("message", exception.getMessage());
            returnValue.put("checkpoint", checkpoint.toString());
            responseWrapper.setBody(new Gson().toJson(returnValue));
        }

        return responseWrapper;
    }

    private static Path savePdfToFile(InputStream inputStream) throws IOException {
        // Write PDF content to a temp file
        Path tempFilePath = Files.createTempFile("api_response", ".pdf");
        Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("PDF written to temp folder: " + tempFilePath.toString());
        return tempFilePath;
    }

    private static String readStream(InputStream inputStream) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
    private Map<String, Serializable> getHeaders(HttpHeaders headers) {
        Map<String, List<String>> headerMap = headers.entrySet().stream()
                .collect(HashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        HashMap::putAll);

        // Convert Map<String, List<String>> to Map<String, Serializable>
        Map<String, Serializable> serializableHeaderMap = new HashMap<>();
        headerMap.forEach((key, value) -> {
            // Convert List<String> to Serializable
            serializableHeaderMap.put(key, (Serializable) value);
        });

        return serializableHeaderMap;
    }

    public RestTemplate createRestTemplate(RequestWrapper requestWrapper) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        // Set the timeouts in milliseconds
        requestFactory.setConnectTimeout(requestWrapper.getConnectionTimeout());
        requestFactory.setReadTimeout(requestWrapper.getReadTimeOut());
        if(requestWrapper.getProxy() != null){
            requestFactory.setProxy(requestWrapper.getProxy());
        }
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }

    public String getBody(HttpServletRequest request) {
        StringBuilder requestBody = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return requestBody.toString();
    }

    public Map<String, Serializable> getHeaders(HttpServletRequest request) {
        Map<String, Serializable> headers = new HashMap<>();
        try {
            Map<String, Serializable> tmp = headers = Collections
                    .list(request.getHeaderNames())
                    .stream()
                    .collect(Collectors.toMap(h -> h, h -> {
                        ArrayList<String> headerValues = Collections.list(request.getHeaders(h));
                        return headerValues.size() == 1 ? headerValues.get(0) : headerValues;
                    }));
            headers.putAll(tmp);
        } catch (Exception exception) {

        }
        return headers;
    }

    public HashMap<String, String> getCookies(HttpServletRequest request) {
        HashMap<String, String> cookiesValue = new HashMap<>();
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String name = cookie.getName();
                String value = cookie.getValue();
                cookiesValue.put(name, value);
                // Process each cookie's name and value
                System.out.println("Name: " + name + ", Value: " + value);
            }
        } else {
            System.out.println("No cookies found in the request.");
        }
        return cookiesValue;
    }

    public static String getClientIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("HTTP_X_FORWARDED");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("HTTP_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("HTTP_FORWARDED");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("HTTP_VIA");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("REMOTE_ADDR");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
