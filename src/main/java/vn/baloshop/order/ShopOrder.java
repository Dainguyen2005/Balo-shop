package vn.baloshop.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import vn.baloshop.account.CustomerAccount;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shop_orders")
public class ShopOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;
    private String email;
    private String phone;
    private String shippingAddress;
    private BigDecimal total;
    private String status;
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    private String paymentStatus;
    private String payerBankName;
    private String payerAccountName;
    private String payerAccountLastFour;
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerAccount customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected ShopOrder() {
    }

    public ShopOrder(CheckoutRequest request, CustomerAccount customer) {
        customerName = request.customerName();
        email = request.email();
        phone = request.phone();
        shippingAddress = request.shippingAddress();
        this.customer = customer;
        paymentMethod = request.paymentMethod();
        if (paymentMethod == PaymentMethod.BANK_TRANSFER || paymentMethod == PaymentMethod.BANK_TRANSFER_DEMO) {
            payerBankName = request.payerBankName();
            payerAccountName = request.payerAccountName().trim();
            payerAccountLastFour = request.payerAccountLastFour();
        }
        status = "PLACED";
        paymentStatus = paymentMethod == PaymentMethod.BANK_TRANSFER
                || paymentMethod == PaymentMethod.BANK_TRANSFER_DEMO
                || paymentMethod == PaymentMethod.VNPAY_DEMO ? "PENDING" : "PENDING_ON_DELIVERY";
        createdAt = Instant.now();
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public void updateStatus(OrderStatus nextStatus) {
        if ("CANCELLED".equals(status) || "DELIVERED".equals(status)) {
            throw new IllegalStateException("Đơn đã kết thúc, không thể đổi trạng thái.");
        }
        if (nextStatus == OrderStatus.CANCELLED && "PAID".equals(paymentStatus)) {
            throw new IllegalStateException("Đơn đã thanh toán; cần xử lý hoàn tiền trước khi hủy.");
        }
        status = nextStatus.name();
    }

    public void markTransferReceived() {
        if ("CANCELLED".equals(status)) {
            throw new IllegalStateException("Không thể xác nhận thanh toán cho đơn đã hủy.");
        }
        if (paymentMethod != PaymentMethod.BANK_TRANSFER) {
            throw new IllegalStateException("Chỉ xác nhận chuyển khoản cho đơn thanh toán bằng chuyển khoản.");
        }
        if ("PAID".equals(paymentStatus)) {
            throw new IllegalStateException("Đơn hàng này đã được xác nhận thanh toán.");
        }
        paymentStatus = "PAID";
    }

    public void markDemoPaymentReceived() {
        if ("CANCELLED".equals(status)) {
            throw new IllegalStateException("Không thể thanh toán đơn hàng đã hủy.");
        }
        if (paymentMethod != PaymentMethod.BANK_TRANSFER_DEMO && paymentMethod != PaymentMethod.VNPAY_DEMO) {
            throw new IllegalStateException("Đơn hàng này không sử dụng thanh toán demo.");
        }
        if ("PAID".equals(paymentStatus)) {
            throw new IllegalStateException("Thanh toán demo đã được xác nhận.");
        }
        paymentStatus = "PAID";
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getStatus() {
        return status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getPayerAccountName() {
        return payerAccountName;
    }

    public String getPayerBankName() {
        return payerBankName;
    }

    public String getPayerAccountLastFour() {
        return payerAccountLastFour;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public CustomerAccount getCustomer() {
        return customer;
    }
}
