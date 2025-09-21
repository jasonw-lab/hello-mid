package com.example.seata.at.order.api;

import com.example.seata.at.order.api.dto.CommonResponse;
import com.example.seata.at.order.api.dto.OrderDTO;
import com.example.seata.at.order.domain.entity.Order;
import com.example.seata.at.order.domain.entity.TccOrder;
import com.example.seata.at.order.service.OrderATService;
import com.example.seata.at.order.service.OrderTccService;
import io.seata.spring.annotation.GlobalTransactional;
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
    private final OrderATService orderATService;
    private final OrderTccService orderTccService;

    public OrderController(OrderATService orderATService, OrderTccService orderTccService) {
        this.orderATService = orderATService;
        this.orderTccService = orderTccService;
    }

    @PostMapping
    public CommonResponse<Order> create(@Valid @RequestBody OrderDTO req) {
        if (req.getOrderNo() == null || req.getOrderNo().trim().isEmpty()) {
            // Generate orderNo on server side if client didn't provide one
            req.setOrderNo(java.util.UUID.randomUUID().toString());
        }
        Order order = orderATService.placeOrder(req);
        return new CommonResponse<Order>() {{
            setSuccess(true);
            setMessage("OK");
            setData(order);
        }};
    }

    @PostMapping("/tcc")
    @GlobalTransactional
    public CommonResponse<TccOrder> createOrderTcc(@Valid @RequestBody OrderDTO orderDTO) {
        if (orderDTO.getOrderNo() == null || orderDTO.getOrderNo().trim().isEmpty()) {
            orderDTO.setOrderNo(java.util.UUID.randomUUID().toString());
        }
        TccOrder order =  orderTccService.tryCreate(orderDTO,null);
        return new CommonResponse<TccOrder>() {{
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
