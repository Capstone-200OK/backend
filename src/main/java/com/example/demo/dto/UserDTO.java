package com.example.demo.dto;

import com.example.demo.entity.User;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
public class UserDTO {
    private String email;
    private String nickname;
    private String password;
}
