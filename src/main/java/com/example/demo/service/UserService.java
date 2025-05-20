package com.example.demo.service;

import com.example.demo.dto.UserDTO.LoginDTO;
import com.example.demo.dto.UserDTO.UserDTO;
import com.example.demo.dto.UserDTO.UserResponseDTO;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FolderAccessService folderAccessService;

    /**
     * 사용자 로그인
     *
     * @param loginDTO 이메일, 비밀번호 정보
     * @return 로그인한 사용자의 ID, 닉네임
     */
    public UserResponseDTO login(LoginDTO loginDTO) {
        // 이메일 기준 사용자 조회
        User user = userRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new RuntimeException("등록되지 않은 이메일입니다."));

        // 지금은 암호화 없이 단순 문자열 비교
        if (!loginDTO.getPassword().equals(user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return new UserResponseDTO(user.getId(), user.getNickName());
    }

    /**
     * 사용자 회원가입
     *
     * @param userDTO 사용자 정보 (이메일, 비밀번호, 닉네임)
     */
    public void signup(UserDTO userDTO) {
        // 이메일 중복 체크
        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // User 엔티티 생성
        User user = User.builder()
                .nickName(userDTO.getNickname())
                .email(userDTO.getEmail())
                .password(userDTO.getPassword()) // ⚠️ 실제 운영 시엔 passwordEncoder.encode() 필요
                .build();

        // DB 저장
        userRepository.save(user);
    }

    /**
     * ID로 사용자 조회
     *
     * @param userId 사용자 ID
     * @return User 엔티티
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * 사용자 탈퇴 (삭제)
     *
     * @param userId 사용자 ID
     */
    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(userId);
    }

    /**
     * 이메일로 사용자 조회
     *
     * @param email 이메일 주소
     * @return UserResponseDTO (ID, 닉네임)
     */
    public UserResponseDTO findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return new UserResponseDTO(user.getId(), user.getNickName());
    }
}
