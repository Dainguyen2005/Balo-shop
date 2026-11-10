package vn.baloshop.product;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductRepository products;

    public ProductController(ProductRepository products) {
        this.products = products;
    }

    @GetMapping
    public List<ProductResponse> list() {
        return products.findAllByActiveTrueOrderByIdAsc().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return products.findByIdAndActiveTrue(id).map(ProductResponse::from)
                .orElseThrow(() -> new java.util.NoSuchElementException("Không tìm thấy balo."));
    }
}
