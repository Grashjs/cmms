package com.grash.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/config")
class ApiController {

    @Value("${stripe.publishable-key}")
    private String stripePublishableKey;

    @Value("${keygen.account-id}")
    private String keygenAccountId;

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> getConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("stripePublishableKey", stripePublishableKey);
        config.put("keygenAccountId", keygenAccountId);
        return ResponseEntity.ok(config);
    }
}