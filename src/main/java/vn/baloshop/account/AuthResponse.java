package vn.baloshop.account;

public record AuthResponse(boolean authenticated, CustomerResponse customer) {
    public static AuthResponse authenticated(CustomerAccount customer) {
        return new AuthResponse(true, CustomerResponse.from(customer));
    }

    public static AuthResponse anonymous() {
        return new AuthResponse(false, null);
    }
}
