package vn.baloshop.order;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public OrderResponse get(@PathVariable Long id, Authentication authentication) {
        return orders.get(id, authentication.getName());
    }

    @PostMapping("/{id}/demo-payment")
    @PreAuthorize("isAuthenticated()")
    public OrderResponse markDemoPaymentReceived(@PathVariable Long id, Authentication authentication) {
        return orders.markDemoPaymentReceived(id, authentication.getName());
    }
}
