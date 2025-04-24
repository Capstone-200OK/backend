package com.example.demo.repository;

import com.example.demo.entity.ImportantBin;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportantBinRepository extends JpaRepository<ImportantBin, Long> {
    List<ImportantBin> findAllByUser(User user);
}
