package com.inventory.msp.controller;

import com.inventory.msp.services.ExcelUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.XSlf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
public class ExcelUploadController {

    private final ExcelUploadService excelUploadService;
    private static final Logger log= LoggerFactory.getLogger(ExcelUploadController.class);

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file) {
        log.info("received file:{}",file.getOriginalFilename());
        try {
            excelUploadService.uploadExcel(file);
            return ResponseEntity.ok("Excel uploaded successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }
}
