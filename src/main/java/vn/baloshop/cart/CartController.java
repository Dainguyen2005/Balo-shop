package vn.baloshop.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.baloshop.order.CheckoutRequest;
import vn.baloshop.order.OrderResponse;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/carts")
public class CartController {
    private final CartService carts;

    public CartController(CartService carts) {
        this.carts = carts;
    }

    @PostMapping
    public CartResponse create() {
        return carts.create();
    }

    @GetMapping("/{cartId}")
    public CartResponse get(@PathVariable UUID cartId) {
        return carts.get(cartId);
    }

    @PostMapping("/{cartId}/items")
    public CartResponse add(@PathVariable UUID cartId, @Valid @RequestBody CartItemRequest request) {
        return carts.add(cartId, request);
    }

    @PutMapping("/{cartId}/items/{productId}")
    public CartResponse update(@PathVariable UUID cartId, @PathVariable Long productId,
                               @RequestParam @Positive int quantity) {
        return carts.update(cartId, productId, quantity);
    }

    @DeleteMapping("/{cartId}/items/{productId}")
    public CartResponse remove(@PathVariable UUID cartId, @PathVariable Long productId) {
        return carts.remove(cartId, productId);
    }

    @PostMapping("/{cartId}/checkout")
    @PreAuthorize("isAuthenticated()")
    public OrderResponse checkout(@PathVariable UUID cartId, @Valid @RequestBody CheckoutRequest request,
                                  Authentication authentication) {
        return carts.checkout(cartId, request, authentication.getName());
    }
}
