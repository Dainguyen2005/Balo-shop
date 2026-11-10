package vn.baloshop.order;

import java.math.BigDecimal;

public record OrderItemResponse(Long productId, String productName, int quantity, BigDecimal unitPrice,
                                BigDecimal subtotal) {
}
