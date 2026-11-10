package vn.baloshop.product;

import jakarta.validation.constraints.NotNull;

public record ProductActiveRequest(@NotNull Boolean active) {
}
