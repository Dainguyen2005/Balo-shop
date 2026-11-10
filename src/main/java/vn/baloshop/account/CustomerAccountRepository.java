package vn.baloshop.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, Long> {
    Optional<CustomerAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    java.util.List<CustomerAccount> findAllByOrderByIdAsc();
}
