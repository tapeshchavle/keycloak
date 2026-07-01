package com.company.inventory.model;
import lombok.*;
import java.math.BigDecimal;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Product {
    private String productId;
    private String name;
    private String category;
    private String sku;
    private BigDecimal price;
    private Integer stockQuantity;
    private Integer reorderLevel;
    private String warehouse;
    private String status;     // IN_STOCK, LOW_STOCK, OUT_OF_STOCK
    private String supplier;
}
