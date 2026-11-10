package vn.baloshop.order;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
        @NotBlank String customerName,
        @NotBlank @Email String email,
        @NotBlank String phone,
        @NotBlank String shippingAddress,
        @NotNull PaymentMethod paymentMethod,
        @Size(max = 80) String payerBankName,
        @Size(max = 120) String payerAccountName,
        @Pattern(regexp = "\\d{4}", message = "Vui lòng nhập đúng 4 số cuối tài khoản.")
        String payerAccountLastFour
) {
}
