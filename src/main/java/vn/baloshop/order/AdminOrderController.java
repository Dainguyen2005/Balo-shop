package vn.baloshop.order;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/orders")
public class AdminOrderController {
    private final OrderService orders;

    public AdminOrderController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping
    public List<OrderResponse> list() {
        return orders.listAll();
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusRequest request) {
        return orders.updateStatus(id, request.status());
    }

    @PostMapping("/{id}/payment-received")
    public OrderResponse confirmTransfer(@PathVariable Long id) {
        return orders.markTransferReceived(id);
    }
}
