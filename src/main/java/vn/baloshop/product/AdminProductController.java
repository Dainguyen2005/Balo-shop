package vn.baloshop.product;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/products")
public class AdminProductController {
    private final ProductRepository products;

    public AdminProductController(ProductRepository products) {
        this.products = products;
    }

    @GetMapping
    public List<ProductResponse> list() {
        return products.findAll().stream().map(ProductResponse::from).toList();
    }

    @PostMapping
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(products.save(new Product(request)));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        Product product = findProduct(id);
        product.update(request);
        return ProductResponse.from(products.save(product));
    }

    @DeleteMapping("/{id}")
    public ProductResponse deactivate(@PathVariable Long id) {
        Product product = findProduct(id);
        product.deactivate();
        return ProductResponse.from(products.save(product));
    }

    @PatchMapping("/{id}/active")
    public ProductResponse setActive(@PathVariable Long id, @Valid @RequestBody ProductActiveRequest request) {
        Product product = findProduct(id);
        product.setActive(request.active());
        return ProductResponse.from(products.save(product));
    }

    private Product findProduct(Long id) {
        return products.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy balo."));
    }
}
