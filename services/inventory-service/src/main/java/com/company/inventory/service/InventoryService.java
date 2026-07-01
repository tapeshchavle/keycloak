package com.company.inventory.service;
import com.company.inventory.model.Product;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
@Service
public class InventoryService {
    private final List<Product> productDb = new ArrayList<>(List.of(
        Product.builder().productId("PROD001").name("Laptop Pro 15").category("Electronics")
            .sku("LP-15-2024").price(new BigDecimal("1299.99")).stockQuantity(45).reorderLevel(10)
            .warehouse("WH-NYC").status("IN_STOCK").supplier("TechSupplies Inc").build(),
        Product.builder().productId("PROD002").name("Office Chair Ergonomic").category("Furniture")
            .sku("OC-ERG-001").price(new BigDecimal("449.99")).stockQuantity(8).reorderLevel(5)
            .warehouse("WH-CHI").status("LOW_STOCK").supplier("FurnitureCo").build(),
        Product.builder().productId("PROD003").name("Wireless Headset").category("Electronics")
            .sku("WH-BT-500").price(new BigDecimal("189.99")).stockQuantity(0).reorderLevel(15)
            .warehouse("WH-NYC").status("OUT_OF_STOCK").supplier("AudioTech").build(),
        Product.builder().productId("PROD004").name("Standing Desk").category("Furniture")
            .sku("SD-ADJ-200").price(new BigDecimal("799.99")).stockQuantity(22).reorderLevel(8)
            .warehouse("WH-SF").status("IN_STOCK").supplier("FurnitureCo").build()
    ));
    public List<Product> getAllProducts() { return productDb; }
    public Optional<Product> getById(String id) { return productDb.stream().filter(p->p.getProductId().equals(id)).findFirst(); }
    public Map<String,Object> getStockSummary() {
        long inStock = productDb.stream().filter(p->"IN_STOCK".equals(p.getStatus())).count();
        long lowStock = productDb.stream().filter(p->"LOW_STOCK".equals(p.getStatus())).count();
        long outOfStock = productDb.stream().filter(p->"OUT_OF_STOCK".equals(p.getStatus())).count();
        BigDecimal totalValue = productDb.stream()
            .map(p->p.getPrice().multiply(new BigDecimal(p.getStockQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of("totalProducts",productDb.size(),"inStock",inStock,"lowStock",lowStock,"outOfStock",outOfStock,"totalInventoryValue",totalValue,"currency","USD");
    }
}
