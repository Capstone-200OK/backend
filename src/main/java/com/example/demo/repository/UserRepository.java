package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일로 사용자 조회
     *
     * @param userEmail 사용자 이메일
     * @return 사용자 엔티티 (Optional)
     */
    Optional<User> findByEmail(String userEmail);
}
