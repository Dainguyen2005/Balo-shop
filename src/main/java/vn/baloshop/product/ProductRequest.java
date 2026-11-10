package vn.baloshop.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 1000) String description,
        @Size(max = 1000) String imageUrl,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @NotNull @PositiveOrZero Integer stock
) {
}
