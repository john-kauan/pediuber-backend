package com.pediuber.pediuber.controller;

import com.pediuber.pediuber.policy.OverflowPolicyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OverflowController {

    private final OverflowPolicyService overflowPolicyService;

    public OverflowController(OverflowPolicyService overflowPolicyService) {
        this.overflowPolicyService = overflowPolicyService;
    }

    @GetMapping("/overflow")
    public boolean isOverflowing() {
        return overflowPolicyService.isOverloaded();
    }

}
