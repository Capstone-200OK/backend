package com.example.demo.controller;

import com.example.demo.dto.TrashBinRequestDTO;
import com.example.demo.dto.TrashBinResponseDTO;
import com.example.demo.service.TrashBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.example.demo.entity.TrashBin;

import java.util.List;

@RestController
@RequestMapping("/trash")
@RequiredArgsConstructor
public class TrashBinController {

    private final TrashBinService trashBinService;

    @PostMapping("/move")
    public TrashBin moveToTrash(@RequestBody TrashBinRequestDTO dto) {
        return trashBinService.moveToTrash(dto);
    }

    @PostMapping("/restore/{trashId}")
    public void restore(@PathVariable Long trashId) {
        trashBinService.restore(trashId);
    }

    @DeleteMapping("/delete/{trashId}")
    public void deletePermanently(@PathVariable Long trashId) {
        trashBinService.deletePermanently(trashId);
    }

    @GetMapping("/list/{userId}")
    public List<TrashBinResponseDTO> getTrashFiles(@PathVariable Long userId) {
        return trashBinService.getTrashFilesByUser(userId);
    }
}
