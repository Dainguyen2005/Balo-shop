package vn.baloshop.order;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentOptionsController {
    private final PaymentOptionsService paymentOptions;

    public PaymentOptionsController(PaymentOptionsService paymentOptions) {
        this.paymentOptions = paymentOptions;
    }

    @GetMapping("/options")
    public PaymentOptionsResponse getOptions() {
        return paymentOptions.getOptions();
    }
}
