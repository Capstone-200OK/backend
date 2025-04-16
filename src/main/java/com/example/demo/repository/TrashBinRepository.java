package com.example.demo.repository;

import com.example.demo.entity.TrashBin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrashBinRepository extends JpaRepository<TrashBin, Long> {
    List<TrashBin> findByDeletedAtBefore(LocalDateTime time);
}