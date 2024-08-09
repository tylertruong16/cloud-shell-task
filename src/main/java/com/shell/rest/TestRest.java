package com.shell.rest;

import com.shell.service.ChromeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestRest {

    final ChromeService chromeService;

    public TestRest(ChromeService chromeService) {
        this.chromeService = chromeService;
    }

    @GetMapping("/test")
    public Object testCallBrowser(@RequestParam String email) {
        chromeService.connectGoogle(email);
        return ResponseEntity.ok(Map.of("message", "ok"));
    }

}
