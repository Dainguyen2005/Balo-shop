package vn.baloshop.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.baloshop.account.CustomerAccount;
import vn.baloshop.account.CustomerAccountService;
import vn.baloshop.product.Product;
import vn.baloshop.product.ProductRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class OrderService {
    private final ProductRepository products;
    private final ShopOrderRepository orders;
    private final CustomerAccountService customers;
    private final PaymentOptionsService paymentOptions;

    public OrderService(ProductRepository products, ShopOrderRepository orders, CustomerAccountService customers,
                        PaymentOptionsService paymentOptions) {
        this.products = products;
        this.orders = orders;
        this.customers = customers;
        this.paymentOptions = paymentOptions;
    }

    @Transactional
    public OrderResponse create(CheckoutRequest request, Map<Long, Integer> cart, String customerEmail) {
        boolean bankPayment = request.paymentMethod() == PaymentMethod.BANK_TRANSFER
                || request.paymentMethod() == PaymentMethod.BANK_TRANSFER_DEMO;
        if (request.paymentMethod() == PaymentMethod.BANK_TRANSFER
                && !paymentOptions.getOptions().bankTransferConfigured()) {
            throw new IllegalArgumentException("Cửa hàng chưa cấu hình tài khoản nhận chuyển khoản.");
        }
        if (bankPayment
                && (request.payerAccountName() == null || request.payerAccountName().isBlank()
                || request.payerAccountLastFour() == null || request.payerAccountLastFour().isBlank())) {
            throw new IllegalArgumentException("Vui lòng nhập tên người chuyển và 4 số cuối tài khoản.");
        }
        if (request.paymentMethod() == PaymentMethod.BANK_TRANSFER_DEMO
                && (request.payerBankName() == null || request.payerBankName().isBlank())) {
            throw new IllegalArgumentException("Vui lòng nhập tên ngân hàng demo.");
        }
        CustomerAccount customer = customers.findByEmail(customerEmail);
        CheckoutRequest customerRequest = new CheckoutRequest(customer.getFullName(), customer.getEmail(),
                request.phone(), request.shippingAddress(), request.paymentMethod(),
                bankPayment && request.payerBankName() != null ? request.payerBankName().trim() : null,
                bankPayment ? request.payerAccountName().trim() : null,
                bankPayment ? request.payerAccountLastFour() : null);
        ShopOrder order = new ShopOrder(customerRequest, customer);
        BigDecimal total = BigDecimal.ZERO;
        for (var entry : cart.entrySet()) {
            Product product = products.findByIdAndActiveTrue(entry.getKey())
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy balo."));
            int quantity = entry.getValue();
            if (quantity > product.getStock()) {
                throw new IllegalArgumentException("Balo không đủ tồn kho: " + product.getName());
            }
            product.setStock(product.getStock() - quantity);
            order.addItem(new OrderItem(product.getId(), product.getName(), quantity, product.getPrice()));
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }
        order.setTotal(total);
        return OrderResponse.from(orders.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id, String customerEmail) {
        CustomerAccount customer = customers.findByEmail(customerEmail);
        ShopOrder order = orders.findByIdAndCustomer_Id(id, customer.getId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn hàng."));
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public java.util.List<OrderResponse> listAll() {
        return orders.findAllByOrderByCreatedAtDesc().stream().map(OrderResponse::from).toList();
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus status) {
        ShopOrder order = findOrder(id);
        boolean cancelling = status == OrderStatus.CANCELLED && !"CANCELLED".equals(order.getStatus());
        if ((status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED)
                && (order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER
                || order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER_DEMO
                || order.getPaymentMethod() == PaymentMethod.VNPAY_DEMO)
                && !"PAID".equals(order.getPaymentStatus())) {
            throw new IllegalStateException("Cần xác nhận thanh toán trước khi giao đơn hàng.");
        }
        order.updateStatus(status);
        if (cancelling) {
            order.getItems().forEach(item -> products.findById(item.getProductId()).ifPresent(product ->
                    product.setStock(product.getStock() + item.getQuantity())));
        }
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse markTransferReceived(Long id) {
        ShopOrder order = findOrder(id);
        order.markTransferReceived();
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse markDemoPaymentReceived(Long id, String customerEmail) {
        CustomerAccount customer = customers.findByEmail(customerEmail);
        ShopOrder order = orders.findByIdAndCustomer_Id(id, customer.getId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn hàng."));
        order.markDemoPaymentReceived();
        return OrderResponse.from(order);
    }

    private ShopOrder findOrder(Long id) {
        return orders.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn hàng."));
    }
}
