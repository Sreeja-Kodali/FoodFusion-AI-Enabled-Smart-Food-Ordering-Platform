package com.foodfusion.workflow.controller;

import com.foodfusion.workflow.model.OrderWorkflow;
import com.foodfusion.workflow.repository.OrderWorkflowRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping({"/api/workflows/orders", "/api/workflow/orders"})
public class OrderWorkflowController {
    private final OrderWorkflowRepository workflows;

    public OrderWorkflowController(OrderWorkflowRepository workflows) {
        this.workflows = workflows;
    }

    @GetMapping("/{orderNumber}")
    public OrderWorkflow getWorkflow(
            @PathVariable String orderNumber,
            @RequestHeader(name = "X-User-Id", required = false) String userId,
            @RequestHeader(name = "X-User-Roles", required = false) String roles
    ) {
        OrderWorkflow workflow = workflows.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found."));
        boolean isAdmin = roles != null && List.of(roles.split(",")).contains("ADMIN");
        if (!isAdmin && (userId == null || !userId.equals(workflow.getUserId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You may only access your own workflow.");
        }
        return workflow;
    }
}
