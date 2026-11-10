package vn.baloshop.product;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    java.util.List<Product> findAllByActiveTrueOrderByIdAsc();

    java.util.Optional<Product> findByIdAndActiveTrue(Long id);

    boolean existsByName(String name);
}
