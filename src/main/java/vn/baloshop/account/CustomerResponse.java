package vn.baloshop.account;

public record CustomerResponse(Long id, String email, String fullName, AccountRole role) {
    public static CustomerResponse from(CustomerAccount customer) {
        return new CustomerResponse(customer.getId(), customer.getEmail(), customer.getFullName(), customer.getRole());
    }
}
