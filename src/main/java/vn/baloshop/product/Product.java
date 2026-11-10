package vn.baloshop.product;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private int stock;
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    protected Product() {
    }

    public Product(String name, String description, String imageUrl, BigDecimal price, int stock) {
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.price = price;
        this.stock = stock;
    }

    public Product(ProductRequest request) {
        update(request);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public boolean isActive() {
        return active;
    }

    public void update(ProductRequest request) {
        name = request.name().trim();
        description = request.description().trim();
        imageUrl = request.imageUrl() == null ? "" : request.imageUrl().trim();
        price = request.price();
        stock = request.stock();
    }

    public void deactivate() {
        active = false;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
}
