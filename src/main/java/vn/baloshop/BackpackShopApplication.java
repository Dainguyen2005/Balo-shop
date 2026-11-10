package vn.baloshop;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import vn.baloshop.account.CustomerAccountService;
import vn.baloshop.product.Product;
import vn.baloshop.product.ProductRepository;

import java.math.BigDecimal;
import java.util.List;

@SpringBootApplication
public class BackpackShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackpackShopApplication.class, args);
    }

    @Bean
    ApplicationRunner seedProducts(ProductRepository products) {
        return args -> {
            List<Product> catalog = List.of(
                    new Product("Balo City Explorer", "Balo đi học, đi làm gọn nhẹ.", "https://images.unsplash.com/photo-1553062407-98eeb64c6a62", new BigDecimal("590000"), 20),
                    new Product("Balo Trekking 35L", "Balo du lịch chống nước, dung tích 35L.", "https://images.unsplash.com/photo-1622260614153-03223fb72052", new BigDecimal("1250000"), 12),
                    new Product("Balo Laptop Urban", "Ngăn chống sốc cho laptop đến 15.6 inch.", "https://images.unsplash.com/photo-1581605405669-fuzea2d83c7b", new BigDecimal("890000"), 15),
                    new Product("Balo Canvas Daily", "Thiết kế canvas tối giản, phù hợp đi học và dạo phố.", "https://images.unsplash.com/photo-1622560480654-d96214fdc887", new BigDecimal("450000"), 18),
                    new Product("Balo Mini Weekend", "Balo mini nhỏ gọn cho những chuyến đi cuối tuần.", "https://images.unsplash.com/photo-1546938576-6e6a64f317cc", new BigDecimal("390000"), 14),
                    new Product("Balo Travel Pro 40L", "Sức chứa rộng, nhiều ngăn tiện dụng cho chuyến đi dài.", "https://images.unsplash.com/photo-1622260614153-03223fb72052", new BigDecimal("1490000"), 9),
                    new Product("Balo Sport Active", "Chất liệu nhẹ, thoáng khí, dành cho tập luyện và hoạt động ngoài trời.", "https://images.unsplash.com/photo-1553062407-98eeb64c6a62", new BigDecimal("690000"), 16),
                    new Product("Balo Laptop Executive", "Phong cách thanh lịch, ngăn đệm bảo vệ laptop và phụ kiện.", "https://images.unsplash.com/photo-1581605405669-fuzea2d83c7b", new BigDecimal("1090000"), 11)
            );
            for (Product product : catalog) {
                if (!products.existsByName(product.getName())) {
                    products.save(product);
                }
            }
        };
    }

    @Bean
    ApplicationRunner configureInitialAdmin(
            CustomerAccountService customers,
            @Value("${shop.admin.email:}") String email,
            @Value("${shop.admin.password:}") String password,
            @Value("${shop.admin.full-name:Quản trị cửa hàng}") String fullName
    ) {
        return args -> {
            if (email.isBlank() && password.isBlank()) {
                return;
            }
            if (email.isBlank() || password.isBlank()) {
                throw new IllegalStateException("Cần cấu hình đồng thời SHOP_ADMIN_EMAIL và SHOP_ADMIN_PASSWORD.");
            }
            customers.configureAdmin(email, fullName, password);
        };
    }
}
