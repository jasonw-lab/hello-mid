package com.example.seata.at.order.api;

import com.example.seata.at.order.api.dto.CommonResponse;
import com.example.seata.at.order.api.dto.OrderCreateRequest;
import com.example.seata.at.order.domain.entity.Order;
import com.example.seata.at.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public CommonResponse<Order> create(@Valid @RequestBody OrderCreateRequest req) {
        Order order = orderService.createOrder(req);
        return new CommonResponse<Order>() {{
            setSuccess(true);
            setMessage("OK");
            setData(order);
        }};
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        CommonResponse<Void> res = new CommonResponse<>();
        res.setSuccess(false);
        res.setMessage(msg);
        return ResponseEntity.badRequest().body(res);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<CommonResponse<Void>> handleRuntime(RuntimeException ex) {
        CommonResponse<Void> res = new CommonResponse<>();
        res.setSuccess(false);
        res.setMessage(ex.getMessage());
        return ResponseEntity.badRequest().body(res);
    }
}
