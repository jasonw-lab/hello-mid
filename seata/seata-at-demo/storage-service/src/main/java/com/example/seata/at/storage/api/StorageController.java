package com.example.seata.at.storage.api;

import com.example.seata.at.storage.api.dto.CommonResponse;
import com.example.seata.at.storage.api.dto.DeductRequest;
import com.example.seata.at.storage.service.StorageATService;
import com.example.seata.at.storage.service.StorageATServiceImpl;
import com.example.seata.at.storage.service.StorageTccService;
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

    private final StorageATService storageATService;
    private final StorageTccService storageTccService;

    public StorageController(StorageATService storageATService, StorageTccService storageTccService) {
        this.storageATService = storageATService;
        this.storageTccService = storageTccService;
    }

    @PostMapping("/deduct")
    public CommonResponse<String> deduct(@Valid @RequestBody DeductRequest req) {
        // Log received request body at INFO level
        log.info("Received DeductRequest: productId={}, count={}", req.getProductId(), req.getCount());
        storageATService.deduct(req.getProductId(), req.getCount());
        return CommonResponse.ok("deducted");
    }

    @PostMapping("/deduct/tcc")
    public CommonResponse<String> deductTcc(@Valid @RequestBody DeductRequest req) {
        String orderNo = req.getOrderNo();
        if (orderNo == null || orderNo.isBlank()) {
            orderNo = java.util.UUID.randomUUID().toString();
        }
        String xid = null;
        try { xid = io.seata.core.context.RootContext.getXID(); } catch (Throwable ignore) {}
        log.info("Received DeductRequest TCC: orderNo={}, productId={}, count={}, xid={}", orderNo, req.getProductId(), req.getCount(), xid);
        storageTccService.tryDeduct(req.getProductId(), req.getCount());
        return CommonResponse.ok("tcc-try-deducted");
    }

    @ExceptionHandler(StorageATServiceImpl.InsufficientStockException.class)
    public ResponseEntity<CommonResponse<Void>> handleStock(StorageATServiceImpl.InsufficientStockException ex) {
        return ResponseEntity.badRequest().body(CommonResponse.fail(ex.getMessage()));
    }
}
