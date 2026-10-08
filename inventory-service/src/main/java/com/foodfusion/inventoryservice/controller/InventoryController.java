package com.foodfusion.inventoryservice.controller;

import com.foodfusion.inventoryservice.dto.InventoryResponse;
import com.foodfusion.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    // uri : http://localhost:8083/api/inventory/iphone_13,iphone_12
    @GetMapping("{sku-code}")
    @ResponseStatus(HttpStatus.OK)
    public boolean isInStock(@PathVariable("sku-code") String skuCode){
        return inventoryService.isInStock(skuCode);
    }

    // uri : http://localhost:8083/api/inventory?skuCode=iphone_13&skuCode=iphone_12
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @SneakyThrows // Do not use in PROD ENV
    public List<InventoryResponse> isInStock(@RequestParam List<String> skuCode){
        log.info("Thread Started");
        //Thread.sleep(10000);  // commenting sleep to not trigger timeout . uncomment to try out the timeout features
        log.info("Thread Ended");
        return  inventoryService.isInStock(skuCode);

    }
}
