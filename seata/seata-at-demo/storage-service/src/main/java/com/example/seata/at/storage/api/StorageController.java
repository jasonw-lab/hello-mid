package com.example.seata.at.storage.api;

import com.example.seata.at.storage.api.dto.CommonResponse;
import com.example.seata.at.storage.api.dto.DeductRequest;
import com.example.seata.at.storage.service.StorageService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/storage")
public class StorageController {
    private static final Logger log = LoggerFactory.getLogger(StorageController.class);

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/deduct")
    public CommonResponse<String> deduct(@Valid @RequestBody DeductRequest req) {
        // Log received request body at INFO level
        log.info("Received DeductRequest: productId={}, count={}", req.getProductId(), req.getCount());
        storageService.deduct(req.getProductId(), req.getCount());
        return CommonResponse.ok("deducted");
    }

    @ExceptionHandler(StorageService.InsufficientStockException.class)
    public ResponseEntity<CommonResponse<Void>> handleStock(StorageService.InsufficientStockException ex) {
        return ResponseEntity.badRequest().body(CommonResponse.fail(ex.getMessage()));
    }
}
