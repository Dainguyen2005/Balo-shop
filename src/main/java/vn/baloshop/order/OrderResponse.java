package vn.baloshop.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, String customerName, String email, String phone, String shippingAddress,
                            BigDecimal total, String status, PaymentMethod paymentMethod, String paymentStatus,
                            String payerBankName, String payerAccountName, String payerAccountLastFour,
                            String transferReference,
                            Instant createdAt, List<OrderItemResponse> items) {
    public static OrderResponse from(ShopOrder order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(item.getProductId(), item.getProductName(), item.getQuantity(),
                        item.getUnitPrice(), item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))))
                .toList();
        return new OrderResponse(order.getId(), order.getCustomerName(), order.getEmail(), order.getPhone(),
                order.getShippingAddress(), order.getTotal(), order.getStatus(), order.getPaymentMethod(),
                order.getPaymentStatus(), order.getPayerBankName(), order.getPayerAccountName(),
                order.getPayerAccountLastFour(),
                order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER
                        || order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER_DEMO
                        ? "BALO-" + order.getId() : null,
                order.getCreatedAt(), items);
    }
}
