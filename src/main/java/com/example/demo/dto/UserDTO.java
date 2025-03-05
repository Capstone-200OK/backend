package com.example.demo.dto;

import com.example.demo.entity.User;
import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String email;
    private String nickname;
    private String password;

    static public UserDTO of(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setEmail(user.getEmail());
        userDTO.setNickname(user.getNickName());
        userDTO.setPassword(user.getPassword());
        return userDTO;
    }
}
