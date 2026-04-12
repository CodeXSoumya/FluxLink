package com.fluxlink.lb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class BackendService {

    private static final Logger log = LoggerFactory.getLogger(BackendService.class);

    @Value("${backend.instances:}")
    private List<String> configuredInstances;

    private final List<String> activeInstances = new CopyOnWriteArrayList<>();
    private final AtomicInteger currentIndex = new AtomicInteger(0);
    private final RestTemplate restTemplate;

    public BackendService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void init() {
        if (configuredInstances != null) {
            activeInstances.addAll(configuredInstances);
        }
        log.info("Initialized Load Balancer with backends: {}", activeInstances);
    }

    public String getNextInstance() {
        if (activeInstances.isEmpty()) {
            throw new IllegalStateException("No healthy backend instances available");
        }
        int index = Math.abs(currentIndex.getAndIncrement() % activeInstances.size());
        return activeInstances.get(index);
    }

    @Scheduled(fixedRate = 5000)
    public void healthCheck() {
        if (configuredInstances == null || configuredInstances.isEmpty()) {
            return;
        }

        List<String> healthyNow = new ArrayList<>();
        for (String instance : configuredInstances) {
            try {
                restTemplate.getForObject(instance + "/actuator/health", String.class);
                healthyNow.add(instance);
            } catch (Exception e) {
                log.warn("Health check failed for instance: {}", instance);
            }
        }

        // Update active instances atomically
        for (String inst : configuredInstances) {
            if (healthyNow.contains(inst) && !activeInstances.contains(inst)) {
                activeInstances.add(inst);
                log.info("Instance {} is healthy and added to rotation", inst);
            } else if (!healthyNow.contains(inst) && activeInstances.contains(inst)) {
                activeInstances.remove(inst);
                log.warn("Instance {} is UNHEALTHY and removed from rotation", inst);
            }
        }
    }
}
