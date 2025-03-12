package com.example.demo.dto;

import com.example.demo.entity.User;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
public class UserDTO {
    private Long id;
    private String email;
    private String nickname;
    private String password;

    static public UserDTO fromEntity(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickName())
                .build();
    }
}
