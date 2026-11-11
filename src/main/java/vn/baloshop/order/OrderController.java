package vn.baloshop.order;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orders;
    private final DemoTransferQrService demoTransferQr;

    public OrderController(OrderService orders, DemoTransferQrService demoTransferQr) {
        this.orders = orders;
        this.demoTransferQr = demoTransferQr;
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

    @GetMapping(value = "/{id}/demo-transfer-qr", produces = "image/svg+xml")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> getDemoTransferQr(@PathVariable Long id, Authentication authentication) {
        OrderResponse order = orders.get(id, authentication.getName());
        if (order.paymentMethod() != PaymentMethod.BANK_TRANSFER_DEMO) {
            throw new IllegalStateException("Mã QR chỉ có cho đơn chuyển khoản demo.");
        }
        String content = String.format(Locale.ROOT,
                "BALO SHOP DEMO ONLY - KHONG CHUYEN TIEN THAT | ORDER %d | AMOUNT %s VND | REFERENCE BALO-%d",
                order.id(), order.total().toPlainString(), order.id());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.valueOf("image/svg+xml"))
                .body(demoTransferQr.createSvg(content));
    }
}
