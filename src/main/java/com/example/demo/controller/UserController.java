package com.example.demo.controller;

import com.example.demo.dto.MessageResponse;
import com.example.demo.dto.UserDTO.LoginDTO;
import com.example.demo.dto.UserDTO.UserDTO;
import com.example.demo.dto.UserDTO.UserResponseDTO;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    /**
     * 사용자 로그인
     *
     * @param loginDTO 로그인 정보
     * @return 사용자 응답 DTO
     */
    @PostMapping("/login")
    public ResponseEntity<UserResponseDTO> login(@RequestBody LoginDTO loginDTO) {
        UserResponseDTO response = userService.login(loginDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 회원가입
     *
     * @param userDTO 회원가입 정보
     * @return 성공 여부
     */
    @PostMapping("/signup")
    public ResponseEntity<Boolean> signup(@RequestBody UserDTO userDTO) {
        userService.signup(userDTO);
        return ResponseEntity.ok(true);
    }

    /**
     * 이메일로 사용자 조회
     *
     * @param email 사용자 이메일
     * @return 사용자 응답 DTO
     */
    @GetMapping("/by-email/{email}")
    public ResponseEntity<UserResponseDTO> findByEmail(@PathVariable String email) {
        UserResponseDTO response = userService.findByEmail(email);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 삭제 (회원 탈퇴)
     *
     * @param id 사용자 ID
     * @return 처리 결과 메시지
     */
    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable("userId") Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(new MessageResponse("회원 탈퇴 성공"));
    }
}
