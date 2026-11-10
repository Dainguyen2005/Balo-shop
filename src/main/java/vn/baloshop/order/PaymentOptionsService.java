package vn.baloshop.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentOptionsService {
    private final String bankName;
    private final String accountNumber;
    private final String accountHolder;

    public PaymentOptionsService(
            @Value("${shop.payment.bank-name:}") String bankName,
            @Value("${shop.payment.account-number:}") String accountNumber,
            @Value("${shop.payment.account-holder:}") String accountHolder
    ) {
        this.bankName = bankName.trim();
        this.accountNumber = accountNumber.trim();
        this.accountHolder = accountHolder.trim();
    }

    public PaymentOptionsResponse getOptions() {
        boolean configured = !bankName.isBlank() && !accountNumber.isBlank() && !accountHolder.isBlank();
        return new PaymentOptionsResponse(configured, bankName, accountNumber, accountHolder,
                true, "Ngân hàng Demo", "0000000000", "BALO SHOP DEMO");
    }
}
