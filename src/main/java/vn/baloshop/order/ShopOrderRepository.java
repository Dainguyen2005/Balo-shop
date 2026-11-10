package vn.baloshop.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {
    java.util.Optional<ShopOrder> findByIdAndCustomer_Id(Long id, Long customerId);

    java.util.List<ShopOrder> findAllByOrderByCreatedAtDesc();
}
