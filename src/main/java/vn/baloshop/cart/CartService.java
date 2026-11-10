package vn.baloshop.cart;

import org.springframework.stereotype.Service;
import vn.baloshop.order.CheckoutRequest;
import vn.baloshop.order.OrderResponse;
import vn.baloshop.order.OrderService;
import vn.baloshop.product.Product;
import vn.baloshop.product.ProductRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CartService {
    private final Map<UUID, Map<Long, Integer>> carts = new ConcurrentHashMap<>();
    private final ProductRepository products;
    private final OrderService orders;

    public CartService(ProductRepository products, OrderService orders) {
        this.products = products;
        this.orders = orders;
    }

    public CartResponse create() {
        UUID id = UUID.randomUUID();
        carts.put(id, new ConcurrentHashMap<>());
        return response(id);
    }

    public CartResponse get(UUID id) {
        requireCart(id);
        return response(id);
    }

    public CartResponse add(UUID id, CartItemRequest request) {
        Map<Long, Integer> cart = requireCart(id);
        Product product = findAvailableProduct(request.productId());
        int quantity = cart.getOrDefault(product.getId(), 0) + request.quantity();
        requireAvailable(product, quantity);
        cart.put(product.getId(), quantity);
        return response(id);
    }

    public CartResponse update(UUID id, Long productId, int quantity) {
        Map<Long, Integer> cart = requireCart(id);
        Product product = findAvailableProduct(productId);
        requireAvailable(product, quantity);
        if (!cart.containsKey(productId)) {
            throw new NoSuchElementException("Balo không có trong giỏ hàng.");
        }
        cart.put(productId, quantity);
        return response(id);
    }

    public CartResponse remove(UUID id, Long productId) {
        Map<Long, Integer> cart = requireCart(id);
        if (cart.remove(productId) == null) {
            throw new NoSuchElementException("Balo không có trong giỏ hàng.");
        }
        return response(id);
    }

    public OrderResponse checkout(UUID id, CheckoutRequest request, String customerEmail) {
        Map<Long, Integer> cart = requireCart(id);
        if (cart.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng đang trống.");
        }
        OrderResponse order = orders.create(request, Map.copyOf(cart), customerEmail);
        carts.remove(id);
        return order;
    }

    private CartResponse response(UUID id) {
        Map<Long, Integer> cart = requireCart(id);
        var items = cart.entrySet().stream().map(entry -> {
            Product product = findProduct(entry.getKey());
            int quantity = entry.getValue();
            return new CartItemResponse(product.getId(), product.getName(), product.getImageUrl(),
                    product.getPrice(), quantity, product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }).toList();
        BigDecimal total = items.stream().map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(id, items, total);
    }

    private Map<Long, Integer> requireCart(UUID id) {
        Map<Long, Integer> cart = carts.get(id);
        if (cart == null) {
            throw new NoSuchElementException("Không tìm thấy giỏ hàng.");
        }
        return cart;
    }

    private Product findProduct(Long id) {
        return products.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy balo."));
    }

    private Product findAvailableProduct(Long id) {
        return products.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy balo."));
    }

    private void requireAvailable(Product product, int quantity) {
        if (quantity > product.getStock()) {
            throw new IllegalArgumentException("Số lượng vượt quá tồn kho của balo: " + product.getName());
        }
    }
}
