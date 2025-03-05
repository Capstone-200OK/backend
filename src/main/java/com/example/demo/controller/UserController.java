package com.example.demo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {
    @PostMapping("/login")
    String login() {
        return "success";
    }
    @PostMapping("/signup")
    String signup() {
        return "success";
    }
}
