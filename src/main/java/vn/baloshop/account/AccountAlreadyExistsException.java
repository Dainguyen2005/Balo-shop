package vn.baloshop.account;

public class AccountAlreadyExistsException extends RuntimeException {
    public AccountAlreadyExistsException() {
        super("Email này đã được đăng ký.");
    }
}
