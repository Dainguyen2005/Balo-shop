package vn.baloshop.order;

public record PaymentOptionsResponse(
        boolean bankTransferConfigured,
        String bankName,
        String accountNumber,
        String accountHolder,
        boolean demoMode,
        String demoBankName,
        String demoAccountNumber,
        String demoAccountHolder
) {
}
