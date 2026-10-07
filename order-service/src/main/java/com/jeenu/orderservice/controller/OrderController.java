package com.jeenu.orderservice.controller;

import com.jeenu.orderservice.dto.OrderRequest;
import com.jeenu.orderservice.service.OrderService;
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

import static com.jeenu.orderservice.constants.Constants.RESPONSE_500;


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
    public CompletableFuture< ResponseEntity<Object> >placeOrder(@RequestBody OrderRequest orderRequest){
        return CompletableFuture.supplyAsync(()->orderService.placeOrder(orderRequest));
    }

    public CompletableFuture< ResponseEntity<Object> > fallbackMethod(OrderRequest orderRequest,RuntimeException runtimeException){
        return CompletableFuture.supplyAsync( ()-> new ResponseEntity<>(new HashMap<String, String>()
        {{
            put("Message",RESPONSE_500);
            put("Status",HttpStatus.SERVICE_UNAVAILABLE.toString());
        }}, HttpStatus.SERVICE_UNAVAILABLE) );
    }
}
