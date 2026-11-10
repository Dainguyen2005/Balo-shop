package vn.baloshop.account;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
public class CustomerAccountService {
    private final CustomerAccountRepository customers;
    private final PasswordEncoder passwordEncoder;

    public CustomerAccountService(CustomerAccountRepository customers, PasswordEncoder passwordEncoder) {
        this.customers = customers;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CustomerAccount register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (customers.existsByEmailIgnoreCase(email)) {
            throw new AccountAlreadyExistsException();
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu không được vượt quá 72 byte UTF-8.");
        }
        try {
            return customers.saveAndFlush(new CustomerAccount(email, request.fullName().trim(),
                    passwordEncoder.encode(request.password())));
        } catch (DataIntegrityViolationException exception) {
            throw new AccountAlreadyExistsException();
        }
    }

    @Transactional(readOnly = true)
    public CustomerAccount findByEmail(String email) {
        return customers.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy tài khoản."));
    }

    @Transactional
    public CustomerAccount configureAdmin(String email, String fullName, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu admin không được vượt quá 72 byte UTF-8.");
        }
        CustomerAccount customer = customers.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> new CustomerAccount(normalizedEmail, fullName.trim(),
                        passwordEncoder.encode(rawPassword)));
        customer.setRole(AccountRole.ADMIN);
        return customers.save(customer);
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
