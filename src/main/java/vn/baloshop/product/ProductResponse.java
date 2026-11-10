package vn.baloshop.product;

import java.math.BigDecimal;

public record ProductResponse(Long id, String name, String description, String imageUrl,
                              BigDecimal price, int stock, boolean active) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(),
                product.getImageUrl(), product.getPrice(), product.getStock(), product.isActive());
    }
}
