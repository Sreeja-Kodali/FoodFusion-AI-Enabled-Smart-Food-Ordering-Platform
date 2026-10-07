package com.jeenu.inventoryservice.service;

import com.jeenu.inventoryservice.dto.InventoryResponse;
import com.jeenu.inventoryservice.model.Inventory;
import com.jeenu.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public boolean isInStock(String skuCode) {
        Optional<Inventory> stock = inventoryRepository.findBySkuCode(skuCode);
        return stock.isPresent();
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> isInStock(List<String> skucodes){
        List<Inventory> inventoryList = inventoryRepository.findBySkuCodeIn(skucodes);
        log.info("Inventory List from db :{}",inventoryList.toString());
        return inventoryList.stream()
                .map(this::maptoInventoryResponse)
                .toList();
    }

    private InventoryResponse maptoInventoryResponse(Inventory inventory){
        return InventoryResponse.builder()
                .skuCode(inventory.getSkuCode())
                .isInStock(inventory.getQuantity()>0)
                .build();
    }
}
