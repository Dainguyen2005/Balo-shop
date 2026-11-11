package vn.baloshop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import vn.baloshop.account.CustomerAccountService;
import vn.baloshop.account.CustomerAccountRepository;
import vn.baloshop.account.AccountRole;
import vn.baloshop.order.ShopOrderRepository;
import vn.baloshop.order.PaymentOptionsService;
import vn.baloshop.product.Product;
import vn.baloshop.product.ProductRepository;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:baloshop-test;DB_CLOSE_DELAY=-1",
        "shop.payment.bank-name=Test Bank",
        "shop.payment.account-number=123456789",
        "shop.payment.account-holder=BALO SHOP",
        "shop.admin.email=bootstrap-admin@example.com",
        "shop.admin.password=bootstrapAdmin123",
        "shop.admin.full-name=Bootstrap Admin"
})
@AutoConfigureMockMvc
class BackpackShopApiTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository products;

    @Autowired
    private ShopOrderRepository orders;

    @Autowired
    private CustomerAccountRepository customers;

    @Autowired
    private CustomerAccountService customerAccountService;

    @BeforeEach
    void resetData() {
        orders.deleteAll();
        products.deleteAll();
        products.save(new Product("Balo test", "Mô tả", "https://example.com/balo.jpg",
                new BigDecimal("500000"), 5));
    }

    @Test
    void listsProducts() throws Exception {
        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Balo test"));
    }

    @Test
    void createsInitialAdminFromEnvironmentConfiguration() {
        org.junit.jupiter.api.Assertions.assertEquals(AccountRole.ADMIN,
                customers.findByEmailIgnoreCase("bootstrap-admin@example.com").orElseThrow().getRole());
    }

    @Test
    void addsItemChecksOutAndReducesStock() throws Exception {
        MockHttpSession session = registerCustomer();
        JsonNode cart = objectMapper.readTree(mvc.perform(post("/api/carts")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
        String cartId = cart.get("cartId").asText();
        Long productId = products.findAll().get(0).getId();

        mvc.perform(post("/api/carts/{cartId}/items", cartId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": %d, "quantity": 2}
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.total").value(1000000));

        String orderJson = mvc.perform(post("/api/carts/{cartId}/checkout", cartId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Nguyen Van A",
                                  "email": "a@example.com",
                                  "phone": "0900000000",
                                  "shippingAddress": "1 Le Loi, TP HCM",
                                  "paymentMethod": "BANK_TRANSFER",
                                  "payerAccountName": "Nguyen Van A",
                                  "payerAccountLastFour": "1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.paymentMethod").value("BANK_TRANSFER"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.payerAccountName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.payerAccountLastFour").value("1234"))
                .andExpect(jsonPath("$.transferReference").value(startsWith("BALO-")))
                .andExpect(jsonPath("$.total").value(1000000))
                .andReturn().getResponse().getContentAsString();

        long orderId = objectMapper.readTree(orderJson).get("id").asLong();
        org.junit.jupiter.api.Assertions.assertEquals("BALO-" + orderId,
                objectMapper.readTree(orderJson).get("transferReference").asText());
        mvc.perform(get("/api/orders/{id}", orderId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.paymentMethod").value("BANK_TRANSFER"))
                .andExpect(jsonPath("$.payerAccountLastFour").value("1234"))
                .andExpect(jsonPath("$.transferReference").value("BALO-" + orderId));
        org.junit.jupiter.api.Assertions.assertEquals(3, products.findById(productId).orElseThrow().getStock());
    }

    @Test
    void rejectsQuantityAboveAvailableStock() throws Exception {
        String cartId = objectMapper.readTree(mvc.perform(post("/api/carts")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                        .andReturn().getResponse().getContentAsString())
                .get("cartId").asText();
        Long productId = products.findAll().get(0).getId();

        mvc.perform(post("/api/carts/{cartId}/items", cartId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": %d, "quantity": 6}
                                """.formatted(productId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void bankTransferRequiresSenderNameAndAccountSuffix() throws Exception {
        MockHttpSession session = registerCustomer();
        Long productId = products.findAll().get(0).getId();
        String cartId = createCartWithItem(session, productId);

        mvc.perform(post("/api/carts/{cartId}/checkout", cartId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Nguyen Van A",
                                  "email": "a@example.com",
                                  "phone": "0900000000",
                                  "shippingAddress": "1 Le Loi, TP HCM",
                                  "paymentMethod": "BANK_TRANSFER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Vui lòng nhập tên người chuyển và 4 số cuối tài khoản."));
    }

    @Test
    void registersAndAuthenticatesCustomerWithoutReturningPasswordHash() throws Exception {
        String email = "customer-" + UUID.randomUUID() + "@example.com";
        String password = "safePass123";

        mvc.perform(post("/api/auth/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "fullName": "Nguyen Van A", "password": "%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername(email))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.customer.email").value(email))
                .andExpect(jsonPath("$.customer.passwordHash").doesNotExist());

        assertNotEquals(password, customers.findByEmailIgnoreCase(email).orElseThrow().getPasswordHash());
    }

    @Test
    void rejectsDuplicateRegistrationAndInvalidLogin() throws Exception {
        String email = "duplicate-" + UUID.randomUUID() + "@example.com";
        String registerBody = """
                {"email": "%s", "fullName": "Nguyen Van A", "password": "safePass123"}
                """.formatted(email);
        mvc.perform(post("/api/auth/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
        mvc.perform(post("/api/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "incorrect"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void requiresLoginToCheckout() throws Exception {
        JsonNode cart = objectMapper.readTree(mvc.perform(post("/api/carts")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        mvc.perform(post("/api/carts/{cartId}/checkout", cart.get("cartId").asText())
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Nguyen Van A",
                                  "email": "a@example.com",
                                  "phone": "0900000000",
                                  "shippingAddress": "1 Le Loi, TP HCM",
                                  "paymentMethod": "CASH_ON_DELIVERY"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsCashOnDeliveryAndExposesBankConfigurationStatus() throws Exception {
        MockHttpSession session = registerCustomer();
        JsonNode cart = objectMapper.readTree(mvc.perform(post("/api/carts")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        Long productId = products.findAll().get(0).getId();
        mvc.perform(post("/api/carts/{cartId}/items", cart.get("cartId").asText())
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": %d, "quantity": 1}
                                """.formatted(productId)))
                .andExpect(status().isOk());

        mvc.perform(post("/api/carts/{cartId}/checkout", cart.get("cartId").asText())
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Nguyen Van A",
                                  "email": "a@example.com",
                                  "phone": "0900000000",
                                  "shippingAddress": "1 Le Loi, TP HCM",
                                  "paymentMethod": "CASH_ON_DELIVERY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethod").value("CASH_ON_DELIVERY"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING_ON_DELIVERY"))
                .andExpect(jsonPath("$.transferReference").doesNotExist());

        mvc.perform(get("/api/payments/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bankTransferConfigured").value(true))
                .andExpect(jsonPath("$.bankName").value("Test Bank"))
                .andExpect(jsonPath("$.accountNumber").value("123456789"))
                .andExpect(jsonPath("$.demoMode").value(true))
                .andExpect(jsonPath("$.demoAccountNumber").value("0000000000"));
    }

    @Test
    void bankTransferIsNotEnabledWithoutCompleteAccountDetails() {
        var options = new PaymentOptionsService("", "123456789", "");
        org.junit.jupiter.api.Assertions.assertFalse(options.getOptions().bankTransferConfigured());
    }

    @Test
    void rejectsUnknownPaymentMethod() throws Exception {
        MockHttpSession session = registerCustomer();
        JsonNode cart = objectMapper.readTree(mvc.perform(post("/api/carts")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        mvc.perform(post("/api/carts/{cartId}/checkout", cart.get("cartId").asText())
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Nguyen Van A",
                                  "email": "a@example.com",
                                  "phone": "0900000000",
                                  "shippingAddress": "1 Le Loi, TP HCM",
                                  "paymentMethod": "BITCOIN"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customerCannotManageProductsOrOrders() throws Exception {
        MockHttpSession session = registerCustomer();
        mvc.perform(get("/api/admin/products").session(session))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/orders").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateUpdateAndHideProducts() throws Exception {
        MockHttpSession session = registerAdmin();
        String productBody = """
                {
                  "name": "Balo Admin",
                  "description": "Sản phẩm do admin tạo",
                  "imageUrl": "https://example.com/balo.jpg",
                  "price": 750000,
                  "stock": 8
                }
                """;
        String created = mvc.perform(post("/api/admin/products")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        long productId = objectMapper.readTree(created).get("id").asLong();

        mvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Balo Admin"));
        mvc.perform(put("/api/admin/products/{id}", productId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody.replace("Balo Admin", "Balo Admin Đã Sửa")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Balo Admin Đã Sửa"));

        mvc.perform(delete("/api/admin/products/{id}", productId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanConfirmTransferAndCancelUnpaidOrderRestoringStock() throws Exception {
        MockHttpSession customerSession = registerCustomer();
        MockHttpSession adminSession = registerAdmin();
        Long productId = products.findAll().get(0).getId();
        String bankCartId = createCartWithItem(customerSession, productId);
        String bankOrder = placeOrder(customerSession, bankCartId, "BANK_TRANSFER");
        long bankOrderId = objectMapper.readTree(bankOrder).get("id").asLong();
        mvc.perform(post("/api/admin/orders/{id}/payment-received", bankOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));

        String codCartId = createCartWithItem(customerSession, productId);
        String codOrder = placeOrder(customerSession, codCartId, "CASH_ON_DELIVERY");
        long codOrderId = objectMapper.readTree(codOrder).get("id").asLong();
        mvc.perform(patch("/api/admin/orders/{id}/status", codOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CANCELLED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        org.junit.jupiter.api.Assertions.assertEquals(4, products.findById(productId).orElseThrow().getStock());
    }

    @Test
    void customerCanCompleteDemoBankAndVnpayPaymentsWithoutSendingFullAccountNumber() throws Exception {
        MockHttpSession customerSession = registerCustomer();
        MockHttpSession otherCustomerSession = registerCustomer();
        Long productId = products.findAll().get(0).getId();

        String bankCartId = createCartWithItem(customerSession, productId);
        JsonNode bankOrder = objectMapper.readTree(placeOrder(customerSession, bankCartId, "BANK_TRANSFER_DEMO"));
        long bankOrderId = bankOrder.get("id").asLong();
        org.junit.jupiter.api.Assertions.assertFalse(bankOrder.has("payerAccountNumber"));
        org.junit.jupiter.api.Assertions.assertEquals("1234", bankOrder.get("payerAccountLastFour").asText());
        org.junit.jupiter.api.Assertions.assertEquals("Demo Bank", bankOrder.get("payerBankName").asText());

        String qrSvg = mvc.perform(get("/api/orders/{id}/demo-transfer-qr", bankOrderId)
                        .session(customerSession))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(qrSvg.contains("<svg"));

        mvc.perform(get("/api/orders/{id}/demo-transfer-qr", bankOrderId)
                        .session(otherCustomerSession))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/orders/{id}/demo-payment", bankOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(otherCustomerSession))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/orders/{id}/demo-payment", bankOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(customerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));

        String vnpayCartId = createCartWithItem(customerSession, productId);
        JsonNode vnpayOrder = objectMapper.readTree(placeOrder(customerSession, vnpayCartId, "VNPAY_DEMO"));
        long vnpayOrderId = vnpayOrder.get("id").asLong();
        mvc.perform(post("/api/orders/{id}/demo-payment", vnpayOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(customerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethod").value("VNPAY_DEMO"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
    }

    private MockHttpSession registerCustomer() throws Exception {
        String email = "customer-" + UUID.randomUUID() + "@example.com";
        var result = mvc.perform(post("/api/auth/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "fullName": "Nguyen Van A", "password": "safePass123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private MockHttpSession registerAdmin() throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@example.com";
        customerAccountService.configureAdmin(email, "Admin Test", "adminPass123");
        var result = mvc.perform(post("/api/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "adminPass123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.role").value("ADMIN"))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String createCartWithItem(MockHttpSession session, Long productId) throws Exception {
        JsonNode cart = objectMapper.readTree(mvc.perform(post("/api/carts")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        String cartId = cart.get("cartId").asText();
        mvc.perform(post("/api/carts/{cartId}/items", cartId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId": %d, "quantity": 1}
                                """.formatted(productId)))
                .andExpect(status().isOk());
        return cartId;
    }

    private String placeOrder(MockHttpSession session, String cartId, String paymentMethod) throws Exception {
        String senderDetails = "BANK_TRANSFER".equals(paymentMethod)
                ? ",\n                                  \"payerAccountName\": \"Nguyen Van A\","
                + "\n                                  \"payerAccountLastFour\": \"1234\""
                : "BANK_TRANSFER_DEMO".equals(paymentMethod)
                ? ",\n                                  \"payerBankName\": \"Demo Bank\","
                + "\n                                  \"payerAccountName\": \"Nguyen Van A\","
                + "\n                                  \"payerAccountLastFour\": \"1234\""
                : "";
        return mvc.perform(post("/api/carts/{cartId}/checkout", cartId)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Nguyen Van A",
                                  "email": "a@example.com",
                                  "phone": "0900000000",
                                  "shippingAddress": "1 Le Loi, TP HCM",
                                  "paymentMethod": "%s"%s
                                }
                                """.formatted(paymentMethod, senderDetails)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
