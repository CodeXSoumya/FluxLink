package com.fluxlink.lb;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Map;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;

@RestController
public class LoadBalancerController {

    private final BackendService backendService;
    private final RestTemplate restTemplate;

    public LoadBalancerController(BackendService backendService, RestTemplate restTemplate) {
        this.backendService = backendService;
        this.restTemplate = restTemplate;
    }

    @GetMapping("/actuator/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @RequestMapping("/**")
    public ResponseEntity<byte[]> proxyRequest(HttpServletRequest request, HttpEntity<byte[]> requestEntity) throws URISyntaxException {
        String targetInstance = backendService.getNextInstance();
        String requestUrl = request.getRequestURI();
        
        if (request.getQueryString() != null) {
            requestUrl += "?" + request.getQueryString();
        }

        URI uri = new URI(targetInstance + requestUrl);

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!headerName.equalsIgnoreCase("host")) { // avoid host header mismatch
                headers.add(headerName, request.getHeader(headerName));
            }
        }

        HttpEntity<byte[]> forwardEntity = new HttpEntity<>(requestEntity.getBody(), headers);

        return restTemplate.exchange(
                uri,
                HttpMethod.valueOf(request.getMethod()),
                forwardEntity,
                byte[].class
        );
    }
}
