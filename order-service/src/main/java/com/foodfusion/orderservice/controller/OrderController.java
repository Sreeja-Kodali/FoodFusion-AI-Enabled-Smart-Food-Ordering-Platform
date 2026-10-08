package com.foodfusion.orderservice.controller;

import com.foodfusion.orderservice.dto.OrderRequest;
import com.foodfusion.orderservice.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import com.foodfusion.orderservice.model.Order;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;

import static com.foodfusion.orderservice.constants.Constants.RESPONSE_500;


@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @CircuitBreaker(name = "inventory",fallbackMethod = "fallbackMethod")
    @TimeLimiter(name= "inventory") // adding return type CompletableFuture cause of Timelimiter
    @Retry(name="inventory")
    public CompletableFuture<ResponseEntity<Object>> placeOrder(
            @Valid @RequestBody OrderRequest orderRequest,
            @RequestHeader(name = "X-User-Id", required = false) String userId
    ){
        return CompletableFuture.supplyAsync(()->orderService.placeOrder(orderRequest, userId));
    }

    @GetMapping("/history")
    public List<Order> getOrderHistory(
            @RequestHeader(name = "X-User-Id", required = false) String userId
    ) {
        return orderService.getOrderHistory(userId);
    }

    @GetMapping("/{orderNumber}")
    public Order getOrder(
            @PathVariable String orderNumber,
            @RequestHeader(name = "X-User-Id", required = false) String userId,
            @RequestHeader(name = "X-User-Roles", required = false) String roles
    ) {
        Order order = orderService.getOrder(orderNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));
        boolean isAdmin = roles != null && List.of(roles.split(",")).contains("ADMIN");
        if (!isAdmin && (userId == null || !userId.equals(order.getUserId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You may only access your own orders.");
        }
        return order;
    }

    public CompletableFuture< ResponseEntity<Object> > fallbackMethod(OrderRequest orderRequest,RuntimeException runtimeException){
        return CompletableFuture.supplyAsync( ()-> new ResponseEntity<>(new HashMap<String, String>()
        {{
            put("Message",RESPONSE_500);
            put("Status",HttpStatus.SERVICE_UNAVAILABLE.toString());
        }}, HttpStatus.SERVICE_UNAVAILABLE) );
    }
}
