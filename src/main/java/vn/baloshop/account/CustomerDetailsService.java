package vn.baloshop.account;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomerDetailsService implements UserDetailsService {
    private final CustomerAccountRepository customers;

    public CustomerDetailsService(CustomerAccountRepository customers) {
        this.customers = customers;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        CustomerAccount customer = customers.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản."));
        return User.withUsername(customer.getEmail())
                .password(customer.getPasswordHash())
                .roles(customer.getRole().name())
                .build();
    }
}
