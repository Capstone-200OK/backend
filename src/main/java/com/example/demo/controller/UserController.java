package com.example.demo.controller;

import com.example.demo.dto.UserDTO.*;
import com.example.demo.dto.MessageResponse;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
    final private UserService userService;
    @PostMapping("/login")
    public ResponseEntity<UserResponseDTO> login(@RequestBody LoginDTO loginDTO) {
        UserResponseDTO response = userService.login(loginDTO);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/signup")
    public ResponseEntity<Boolean> signup(@RequestBody UserDTO userDTO) {
        userService.signup(userDTO); // 예외 나면 아래 코드 실행 안 됨
        return ResponseEntity.ok(true);
    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable("userId") Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(new MessageResponse("회원 탈퇴 성공"));
    }
}
