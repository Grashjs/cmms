package com.grash.controller;

import com.grash.dto.checkout.CheckoutRequest;
import com.grash.dto.checkout.CheckoutResponse;
import com.grash.service.PaddleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/paddle")
@RequiredArgsConstructor
public class PaddleController {

    private final PaddleService paddleService;

    @PostMapping("/create-checkout-session")
    public CheckoutResponse createCheckoutSession(@Valid @RequestBody CheckoutRequest request) {
        return paddleService.createCheckoutSession(request);
    }
}