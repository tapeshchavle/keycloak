package com.company.inventory.controller;

import com.company.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * ================================================================
 * INVENTORY CONTROLLER — JWT-Protected Inventory API
 * ================================================================
 *
 * SSO DEMONSTRATION:
 * The SAME JWT token issued by Keycloak at login works for ALL 3 services:
 *
 *   alice.chen (ROLE_USER) can:
 *     ✅ GET /api/payroll/my-salary     (payroll service, port 8082)
 *     ✅ GET /api/hr/employees/me       (hr service, port 8083)
 *     ✅ GET /api/inventory/products    (inventory service, port 8084) ← HERE
 *     ❌ GET /api/payroll/salaries      (needs ROLE_PAYROLL)
 *     ❌ GET /api/hr/employees          (needs ROLE_HR)
 *
 *   bob.johnson (ROLE_USER + ROLE_INVENTORY) can:
 *     ✅ GET /api/inventory/products    (any user)
 *     ✅ GET /api/inventory/stock       (ROLE_INVENTORY)
 *     ✅ POST /api/inventory/products   (ROLE_INVENTORY)
 *     ❌ GET /api/hr/employees          (needs ROLE_HR)
 *
 * All role checks are done by reading the JWT — no Keycloak call per request!
 */
@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory", description = "Inventory Service — protected by Keycloak JWT")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) { this.inventoryService = inventoryService; }

    /**
     * GET /api/inventory/products — Product catalog (any authenticated user).
     * Demonstrates that ROLE_USER can read the product list.
     */
    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('USER', 'INVENTORY', 'ADMIN')")
    @Operation(summary = "Product catalog", description = "All authenticated employees can browse products")
    public ResponseEntity<Map<String, Object>> getProducts(@AuthenticationPrincipal Jwt jwt) {
        log.info("Product catalog viewed by: {}", jwt.getClaimAsString("preferred_username"));
        return ResponseEntity.ok(Map.of(
            "products", inventoryService.getAllProducts(),
            "viewedBy", jwt.getClaimAsString("preferred_username"),
            "note", "Any ROLE_USER can read the catalog. Write access requires ROLE_INVENTORY."
        ));
    }

    /**
     * GET /api/inventory/stock — Stock levels and inventory value.
     * Requires ROLE_INVENTORY or ROLE_ADMIN.
     */
    @GetMapping("/stock")
    @PreAuthorize("hasAnyRole('INVENTORY', 'ADMIN')")
    @Operation(summary = "Stock levels", description = "Inventory team/Admin — stock levels and valuation")
    public ResponseEntity<Map<String, Object>> getStock(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Map.of(
            "stockSummary", inventoryService.getStockSummary(),
            "products", inventoryService.getAllProducts(),
            "requestedBy", jwt.getClaimAsString("preferred_username"),
            "accessGrantedVia", "ROLE_INVENTORY or ROLE_ADMIN in JWT"
        ));
    }

    /**
     * POST /api/inventory/products — Add new product.
     * Requires ROLE_INVENTORY or ROLE_ADMIN.
     */
    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('INVENTORY', 'ADMIN')")
    @Operation(summary = "Add product", description = "Inventory team/Admin — add new product to catalog")
    public ResponseEntity<Map<String, Object>> addProduct(
            @RequestBody Map<String, Object> product,
            @AuthenticationPrincipal Jwt jwt) {
        String addedBy = jwt.getClaimAsString("preferred_username");
        log.info("Product added by: {} (INVENTORY/ADMIN role verified)", addedBy);
        return ResponseEntity.ok(Map.of(
            "message", "Product added successfully",
            "product", product,
            "addedBy", addedBy,
            "timestamp", Instant.now().toString()
        ));
    }

    /**
     * PUT /api/inventory/products/{id}/reorder — Trigger reorder.
     * Requires ROLE_INVENTORY or ROLE_ADMIN.
     */
    @PutMapping("/products/{id}/reorder")
    @PreAuthorize("hasAnyRole('INVENTORY', 'ADMIN')")
    @Operation(summary = "Reorder product", description = "Trigger purchase order for low stock product")
    public ResponseEntity<Map<String, Object>> reorderProduct(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> reorderDetails,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Map.of(
            "message", "Purchase order created for product: " + id,
            "productId", id,
            "quantity", reorderDetails != null ? reorderDetails.getOrDefault("quantity", 50) : 50,
            "orderedBy", jwt.getClaimAsString("preferred_username"),
            "timestamp", Instant.now().toString(),
            "status", "PURCHASE_ORDER_CREATED"
        ));
    }

    /**
     * DELETE /api/inventory/products/{id} — Remove product.
     * ADMIN ONLY — permanent deletion requires admin approval.
     */
    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete product", description = "Admin only — permanently remove product from catalog")
    public ResponseEntity<Map<String, Object>> deleteProduct(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Map.of(
            "message", "Product deleted: " + id,
            "deletedBy", jwt.getClaimAsString("preferred_username"),
            "timestamp", Instant.now().toString(),
            "warning", "ROLE_ADMIN required for permanent deletion"
        ));
    }
}
