package com.example.demo.controller;

import com.example.demo.dto.TrashBinRequestDTO;
import com.example.demo.dto.TrashBinResponseDTO;
import com.example.demo.service.TrashBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/trash")
@RequiredArgsConstructor
public class TrashBinController {

    private final TrashBinService trashBinService;

    @PostMapping("/move")
    public ResponseEntity<Void> moveToTrash(@RequestBody TrashBinRequestDTO dto) {
        trashBinService.moveToTrash(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/restore")
    public ResponseEntity<Void> restore(@RequestBody List<Long> trashIds) {
        trashBinService.restoreAll(trashIds);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deletePermanently(@RequestBody List<Long> trashIds) {
        trashBinService.deleteAllPermanently(trashIds);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/list/{userId}")
    public List<TrashBinResponseDTO> getTrashFiles(@PathVariable Long userId) {
        return trashBinService.getTrashFilesByUser(userId);
    }
}
