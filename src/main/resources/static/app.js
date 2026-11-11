(() => {
  "use strict";

  const $ = (selector) => document.querySelector(selector);
  const elements = {
    productGrid: $("#product-grid"),
    productCount: $("#product-count"),
    emptyProducts: $("#empty-products"),
    search: $("#product-search"),
    sort: $("#sort-products"),
    cartCount: $("#cart-count"),
    drawerCount: $("#drawer-count"),
    drawer: $("#cart-drawer"),
    backdrop: $("#drawer-backdrop"),
    cartItems: $("#cart-items"),
    cartEmpty: $("#cart-empty"),
    cartTotal: $("#cart-total"),
    drawerFooter: $("#drawer-footer"),
    checkoutForm: $("#checkout-form"),
    checkoutMessage: $("#checkout-message"),
    checkoutSubmit: $("#checkout-submit"),
    bankTransferRadio: $('input[name="paymentMethod"][value="BANK_TRANSFER_DEMO"]'),
    bankAccountDetails: $("#bank-account-details"),
    bankUnconfigured: $("#bank-unconfigured"),
    bankDetails: $("#bank-details"),
    bankPayerDetails: $("#bank-payer-details"),
    authDialog: $("#auth-dialog"),
    authBackdrop: $("#auth-backdrop"),
    authForm: $("#auth-form"),
    authMessage: $("#auth-message"),
    authNameField: $("#auth-name-field"),
    authSubmit: $("#auth-submit"),
    orderConfirmation: $("#order-confirmation"),
    orderConfirmationBackdrop: $("#order-confirmation-backdrop"),
    demoPaymentConfirmation: $("#demo-payment-confirmation"),
    demoPaymentSuccess: $("#demo-payment-success"),
    toast: $("#toast")
  };
  const state = {
    products: [],
    cartId: localStorage.getItem("baloShopCartId"),
    cart: null,
    customer: null,
    paymentOptions: null,
    authMode: "login",
    pendingCheckout: false,
    pendingDemoOrderId: null,
    csrfToken: null,
    toastTimer: null
  };
  const money = new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 });

  function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, (character) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[character]);
  }

  async function api(path, options = {}) {
    const method = (options.method || (options.body ? "POST" : "GET")).toUpperCase();
    const headers = { ...(options.body ? { "Content-Type": "application/json" } : {}), ...options.headers };
    if (!["GET", "HEAD", "OPTIONS", "TRACE"].includes(method)) {
      if (!state.csrfToken) await loadCsrfToken();
      headers["X-XSRF-TOKEN"] = state.csrfToken;
    }
    const response = await fetch(path, {
      ...options,
      headers
    });
    const data = await response.json().catch(() => null);
    if (!response.ok) {
      if (response.status === 401) throw new Error("Bạn cần đăng nhập để tiếp tục.");
      throw new Error(data?.error || `Có lỗi xảy ra (${response.status}).`);
    }
    return data;
  }

  async function loadCsrfToken() {
    const response = await fetch("/api/auth/csrf");
    const data = await response.json().catch(() => null);
    if (!response.ok || !data?.token) {
      throw new Error("Chưa kết nối được bảo mật cửa hàng. Vui lòng tải lại trang.");
    }
    state.csrfToken = data.token;
  }

  function showToast(message) {
    elements.toast.textContent = message;
    elements.toast.classList.add("is-visible");
    clearTimeout(state.toastTimer);
    state.toastTimer = setTimeout(() => elements.toast.classList.remove("is-visible"), 3000);
  }

  function imageMarkup(url, name, className = "") {
    const safeUrl = escapeHtml(url || "");
    return `<img class="${className}" src="${safeUrl}" alt="${escapeHtml(name)}" loading="lazy" onerror="this.onerror=null;this.src='data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 400 420%22%3E%3Crect width=%22400%22 height=%22420%22 fill=%22%23e9e8df%22/%3E%3Ctext x=%22200%22 y=%22220%22 text-anchor=%22middle%22 fill=%22%23355747%22 font-family=%22sans-serif%22 font-size=%2250%22%3EB.%3C/text%3E%3C/svg%3E'">`;
  }

  function filteredProducts() {
    const query = elements.search.value.trim().toLocaleLowerCase("vi");
    const products = state.products.filter((product) =>
      `${product.name} ${product.description}`.toLocaleLowerCase("vi").includes(query));
    switch (elements.sort.value) {
      case "price-low": return products.sort((a, b) => a.price - b.price);
      case "price-high": return products.sort((a, b) => b.price - a.price);
      case "name": return products.sort((a, b) => a.name.localeCompare(b.name, "vi"));
      default: return products;
    }
  }

  function renderProducts() {
    const products = filteredProducts();
    elements.productCount.textContent = `${products.length} chiếc balo đang chờ bạn`;
    elements.productGrid.hidden = products.length === 0;
    elements.emptyProducts.hidden = products.length !== 0;
    elements.productGrid.innerHTML = products.map((product) => {
      const badge = product.stock < 5 ? "SẮP HẾT" : product.id === state.products[0]?.id ? "ĐƯỢC YÊU THÍCH" : "NHÀ MÌNH CHỌN";
      return `<article class="product-card">
        <div class="product-photo">
          ${imageMarkup(product.imageUrl, product.name)}
          <span class="product-badge">${badge}</span>
        </div>
        <div class="product-details">
          <div class="product-title-row"><h3>${escapeHtml(product.name)}</h3><span class="product-price">${money.format(product.price)}</span></div>
          <p class="product-description">${escapeHtml(product.description)}</p>
          <div class="product-action-row">
            <span class="stock-note">${product.stock > 0 ? `Còn ${product.stock} chiếc` : "Tạm hết hàng"}</span>
            <button class="add-button" type="button" data-add-product="${escapeHtml(product.id)}" ${product.stock < 1 ? "disabled" : ""}>
              <span aria-hidden="true">+</span> Thêm vào giỏ
            </button>
          </div>
        </div>
      </article>`;
    }).join("");
  }

  function cartQuantity() {
    return state.cart?.items.reduce((sum, item) => sum + item.quantity, 0) || 0;
  }

  function renderCart() {
    const items = state.cart?.items || [];
    const quantity = cartQuantity();
    elements.cartCount.textContent = String(quantity);
    elements.drawerCount.textContent = `(${quantity})`;
    elements.cartTotal.textContent = money.format(state.cart?.total || 0);
    elements.cartEmpty.hidden = items.length > 0;
    elements.cartItems.hidden = items.length === 0;
    elements.drawerFooter.hidden = items.length === 0;
    if (!elements.checkoutForm.hidden) {
      elements.checkoutForm.hidden = items.length === 0;
    }
    elements.cartItems.innerHTML = items.map((item) => `
      <article class="cart-item">
        ${imageMarkup(item.imageUrl, item.name)}
        <div>
          <h3>${escapeHtml(item.name)}</h3>
          <span class="cart-item-price">${money.format(item.unitPrice)}</span>
          <div class="quantity-control" aria-label="Số lượng ${escapeHtml(item.name)}">
            <button type="button" data-cart-action="decrease" data-product-id="${escapeHtml(item.productId)}" aria-label="Giảm số lượng">−</button>
            <span>${item.quantity}</span>
            <button type="button" data-cart-action="increase" data-product-id="${escapeHtml(item.productId)}" aria-label="Tăng số lượng">+</button>
          </div>
        </div>
        <div class="cart-item-end">
          <strong>${money.format(item.subtotal)}</strong>
          <button class="remove-item" type="button" data-cart-action="remove" data-product-id="${escapeHtml(item.productId)}">Xóa</button>
        </div>
      </article>`).join("");
  }

  function renderPaymentOptions() {
    const options = state.paymentOptions;
    const demoEnabled = Boolean(options?.demoMode);
    elements.bankTransferRadio.disabled = !demoEnabled;
    elements.bankUnconfigured.hidden = demoEnabled;
    elements.bankAccountDetails.hidden = !demoEnabled;
    $("#bank-name").textContent = options?.demoBankName || "";
    $("#bank-account-number").textContent = options?.demoAccountNumber || "";
    $("#bank-account-holder").textContent = options?.demoAccountHolder || "";
    updateBankPayerDetails();
  }

  function updateBankPayerDetails() {
    const show = !elements.bankTransferRadio.disabled && elements.bankTransferRadio.checked;
    elements.bankDetails.hidden = !show;
    elements.bankPayerDetails.hidden = !show;
    elements.bankPayerDetails.querySelectorAll("input").forEach((input) => { input.required = show; });
  }

  async function loadPaymentOptions() {
    state.paymentOptions = await api("/api/payments/options");
    renderPaymentOptions();
  }

  function renderAccount() {
    const accountButton = $("#account-trigger");
    const logoutButton = $("#logout-trigger");
    if (state.customer) {
      accountButton.textContent = `Xin chào, ${state.customer.fullName}`;
      logoutButton.hidden = false;
      $("#admin-link").hidden = state.customer.role !== "ADMIN";
      $("#admin-link-mobile").hidden = state.customer.role !== "ADMIN";
    } else {
      accountButton.textContent = "Đăng nhập";
      logoutButton.hidden = true;
      $("#admin-link").hidden = true;
      $("#admin-link-mobile").hidden = true;
    }
  }

  async function loadCurrentUser() {
    const response = await api("/api/auth/me");
    state.customer = response.authenticated ? response.customer : null;
    renderAccount();
  }

  function setAuthMode(mode) {
    state.authMode = mode;
    const isRegister = mode === "register";
    $("#auth-title").innerHTML = isRegister ? "Mình làm quen <em>nhé?</em>" : "Rất vui gặp lại <em>bạn.</em>";
    $("#auth-description").textContent = isRegister
      ? "Tạo tài khoản để đặt hàng và đồng hành cùng nhà mình."
      : "Đăng nhập để tiếp tục đặt chiếc balo bạn yêu thích.";
    elements.authNameField.hidden = !isRegister;
    elements.authNameField.querySelector("input").required = isRegister;
    elements.authForm.elements.password.autocomplete = isRegister ? "new-password" : "current-password";
    elements.authSubmit.innerHTML = isRegister
      ? 'Tạo tài khoản <span aria-hidden="true">↗</span>'
      : 'Đăng nhập <span aria-hidden="true">↗</span>';
    $("#login-tab").classList.toggle("is-active", !isRegister);
    $("#login-tab").setAttribute("aria-selected", String(!isRegister));
    $("#register-tab").classList.toggle("is-active", isRegister);
    $("#register-tab").setAttribute("aria-selected", String(isRegister));
    elements.authMessage.textContent = "";
  }

  function openAuth(mode = "login", pendingCheckout = false) {
    state.pendingCheckout = pendingCheckout;
    setAuthMode(mode);
    elements.authDialog.hidden = false;
    elements.authDialog.setAttribute("aria-hidden", "false");
    elements.authBackdrop.hidden = false;
    document.body.classList.add("auth-open");
    elements.authDialog.querySelector(isRegisterMode() ? '[name="fullName"]' : '[name="email"]').focus();
  }

  function isRegisterMode() {
    return state.authMode === "register";
  }

  function closeAuth() {
    elements.authDialog.hidden = true;
    elements.authDialog.setAttribute("aria-hidden", "true");
    elements.authBackdrop.hidden = true;
    document.body.classList.remove("auth-open");
    state.pendingCheckout = false;
    $("#account-trigger").focus();
  }

  function startCheckout() {
    if (!state.customer) {
      openAuth("login", true);
      return;
    }
    elements.checkoutForm.hidden = false;
    elements.drawerFooter.hidden = true;
    elements.checkoutForm.elements.customerName.value = state.customer.fullName;
    elements.checkoutForm.elements.email.value = state.customer.email;
    renderPaymentOptions();
    elements.checkoutForm.elements.phone.focus();
  }

  function closeOrderConfirmation() {
    elements.orderConfirmation.hidden = true;
    elements.orderConfirmation.setAttribute("aria-hidden", "true");
    elements.orderConfirmationBackdrop.hidden = true;
    document.body.classList.remove("order-confirmation-open");
    $("#open-cart").focus();
  }

  function showOrderConfirmation(order) {
    const demoBankTransfer = order.paymentMethod === "BANK_TRANSFER_DEMO";
    const bankTransfer = demoBankTransfer || order.paymentMethod === "BANK_TRANSFER";
    const demoPayment = demoBankTransfer || order.paymentMethod === "VNPAY_DEMO";
    state.pendingDemoOrderId = demoPayment ? order.id : null;
    $("#confirmation-order-id").textContent = `#${order.id}`;
    $("#confirmation-message").textContent = demoPayment
      ? "Đơn hàng demo đã được ghi nhận. Bạn có thể mô phỏng thanh toán thành công bên dưới."
      : bankTransfer
        ? "Đơn hàng đang chờ thanh toán. Vui lòng chuyển khoản đúng số tiền và nội dung bên dưới."
        : "Đơn hàng COD đã được ghi nhận. Bạn thanh toán cho đơn vị giao hàng khi nhận balo.";
    $("#transfer-confirmation").hidden = !bankTransfer;
    $("#demo-transfer-qr").hidden = !demoBankTransfer;
    elements.demoPaymentConfirmation.hidden = !demoPayment;
    elements.demoPaymentSuccess.hidden = false;
    elements.demoPaymentSuccess.disabled = false;
    if (bankTransfer) {
      $("#confirmation-total").textContent = money.format(order.total);
      $("#confirmation-bank-name").textContent = demoBankTransfer
        ? state.paymentOptions.demoBankName : state.paymentOptions.bankName;
      $("#confirmation-account-number").textContent = demoBankTransfer
        ? state.paymentOptions.demoAccountNumber : state.paymentOptions.accountNumber;
      $("#confirmation-account-holder").textContent = demoBankTransfer
        ? state.paymentOptions.demoAccountHolder : state.paymentOptions.accountHolder;
      $("#confirmation-transfer-reference").textContent = order.transferReference;
    }
    if (demoBankTransfer) {
      $("#confirmation-transfer-qr").src = `/api/orders/${encodeURIComponent(order.id)}/demo-transfer-qr`;
    }
    elements.orderConfirmation.hidden = false;
    elements.orderConfirmation.setAttribute("aria-hidden", "false");
    elements.orderConfirmationBackdrop.hidden = false;
    document.body.classList.add("order-confirmation-open");
    $("#close-order-confirmation").focus();
  }

  async function confirmDemoPayment() {
    if (!state.pendingDemoOrderId) return;
    elements.demoPaymentSuccess.disabled = true;
    try {
      const order = await api(`/api/orders/${state.pendingDemoOrderId}/demo-payment`, { method: "POST" });
      state.pendingDemoOrderId = null;
      elements.demoPaymentConfirmation.querySelector("p").textContent =
        "Thanh toán demo thành công. Không có giao dịch tiền thật.";
      elements.demoPaymentSuccess.hidden = true;
      $("#confirmation-message").textContent = `Đơn hàng #${order.id} đã được đánh dấu thanh toán (demo).`;
    } catch (error) {
      elements.demoPaymentSuccess.disabled = false;
      showToast(error.message);
    }
  }

  async function submitAuth(event) {
    event.preventDefault();
    if (!elements.authForm.reportValidity()) return;
    elements.authSubmit.disabled = true;
    elements.authMessage.textContent = "";
    const formData = new FormData(elements.authForm);
    const request = {
      email: formData.get("email"),
      password: formData.get("password")
    };
    const isRegister = isRegisterMode();
    if (isRegister) request.fullName = formData.get("fullName");
    try {
      const response = await api(`/api/auth/${isRegister ? "register" : "login"}`, {
        method: "POST",
        body: JSON.stringify(request)
      });
      state.customer = response.customer;
      renderAccount();
      const shouldCheckout = state.pendingCheckout;
      closeAuth();
      elements.authForm.reset();
      showToast(isRegister ? "Tạo tài khoản thành công. Chào mừng bạn!" : "Đăng nhập thành công!");
      if (shouldCheckout) startCheckout();
    } catch (error) {
      elements.authMessage.textContent = error.message;
    } finally {
      elements.authSubmit.disabled = false;
    }
  }

  async function logout() {
    try {
      await api("/api/auth/logout", { method: "POST" });
      state.customer = null;
      renderAccount();
      showToast("Bạn đã đăng xuất khỏi tài khoản.");
    } catch (error) {
      showToast(error.message);
    }
  }

  async function loadProducts() {
    elements.productCount.textContent = "Đang tìm những chiếc balo phù hợp...";
    try {
      state.products = await api("/api/products");
      renderProducts();
    } catch (error) {
      elements.productGrid.hidden = true;
      elements.emptyProducts.hidden = false;
      elements.emptyProducts.innerHTML = `<span aria-hidden="true">!</span><h3>Chưa kết nối được cửa hàng</h3><p>${escapeHtml(error.message)} Hãy tải lại trang sau ít phút nhé.</p>`;
      elements.productCount.textContent = "Không tải được sản phẩm";
    }
  }

  async function loadCart() {
    if (!state.cartId) {
      const cart = await api("/api/carts", { method: "POST" });
      state.cartId = cart.cartId;
      localStorage.setItem("baloShopCartId", state.cartId);
      state.cart = cart;
      renderCart();
      return;
    }
    try {
      state.cart = await api(`/api/carts/${encodeURIComponent(state.cartId)}`);
    } catch (error) {
      if (!error.message.startsWith("Không tìm thấy giỏ hàng")) throw error;
      localStorage.removeItem("baloShopCartId");
      state.cartId = null;
      return loadCart();
    }
    renderCart();
  }

  async function ensureCart() {
    if (!state.cartId) await loadCart();
  }

  function setDrawerOpen(open) {
    elements.drawer.classList.toggle("is-open", open);
    elements.drawer.setAttribute("aria-hidden", String(!open));
    elements.backdrop.hidden = !open;
    document.body.classList.toggle("no-scroll", open);
    if (open) {
      requestAnimationFrame(() => elements.backdrop.classList.add("is-visible"));
      $("#close-cart").focus();
    } else {
      elements.backdrop.classList.remove("is-visible");
      $("#open-cart").focus();
    }
  }

  async function addProduct(productId) {
    try {
      await ensureCart();
      state.cart = await api(`/api/carts/${encodeURIComponent(state.cartId)}/items`, {
        method: "POST",
        body: JSON.stringify({ productId: Number(productId), quantity: 1 })
      });
      renderCart();
      showToast("Đã thêm vào giỏ. Đi thôi!");
    } catch (error) {
      showToast(error.message);
    }
  }

  async function changeCartItem(productId, action) {
    const item = state.cart?.items.find((entry) => entry.productId === Number(productId));
    if (!item) return;
    try {
      if (action === "remove" || (action === "decrease" && item.quantity === 1)) {
        state.cart = await api(`/api/carts/${encodeURIComponent(state.cartId)}/items/${productId}`, { method: "DELETE" });
      } else {
        const quantity = item.quantity + (action === "increase" ? 1 : -1);
        state.cart = await api(`/api/carts/${encodeURIComponent(state.cartId)}/items/${productId}?quantity=${quantity}`, { method: "PUT" });
      }
      renderCart();
    } catch (error) {
      showToast(error.message);
      try {
        state.cart = await api(`/api/carts/${encodeURIComponent(state.cartId)}`);
        renderCart();
      } catch {
        state.cart = null;
        renderCart();
      }
    }
  }

  async function submitOrder(event) {
    event.preventDefault();
    if (!elements.checkoutForm.reportValidity()) return;
    elements.checkoutSubmit.disabled = true;
    elements.checkoutMessage.textContent = "";
    const request = Object.fromEntries(new FormData(elements.checkoutForm).entries());
    if (request.paymentMethod === "BANK_TRANSFER_DEMO") {
      const demoAccountNumber = request.demoAccountNumber.trim();
      request.payerAccountLastFour = demoAccountNumber.slice(-4);
      delete request.demoAccountNumber;
    } else {
      delete request.payerBankName;
      delete request.payerAccountName;
      delete request.payerAccountLastFour;
      delete request.demoAccountNumber;
    }
    try {
      const order = await api(`/api/carts/${encodeURIComponent(state.cartId)}/checkout`, {
        method: "POST",
        body: JSON.stringify(request)
      });
      state.cartId = null;
      state.cart = { items: [], total: 0 };
      localStorage.removeItem("baloShopCartId");
      elements.checkoutForm.reset();
      elements.checkoutForm.hidden = true;
      renderCart();
      setDrawerOpen(false);
      showOrderConfirmation(order);
    } catch (error) {
      elements.checkoutMessage.textContent = error.message;
      if (error.message.includes("đăng nhập")) {
        state.customer = null;
        renderAccount();
        openAuth("login", true);
      }
    } finally {
      elements.checkoutSubmit.disabled = false;
    }
  }

  $("#open-cart").addEventListener("click", () => setDrawerOpen(true));
  $("#close-cart").addEventListener("click", () => setDrawerOpen(false));
  elements.backdrop.addEventListener("click", () => setDrawerOpen(false));
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && !elements.orderConfirmation.hidden) closeOrderConfirmation();
    else if (event.key === "Escape" && !elements.authDialog.hidden) closeAuth();
    else if (event.key === "Escape" && elements.drawer.classList.contains("is-open")) setDrawerOpen(false);
    if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
      event.preventDefault();
      elements.search.focus();
    }
  });
  elements.productGrid.addEventListener("click", (event) => {
    const button = event.target.closest("[data-add-product]");
    if (button) addProduct(button.dataset.addProduct);
  });
  elements.cartItems.addEventListener("click", (event) => {
    const button = event.target.closest("[data-cart-action]");
    if (button) changeCartItem(button.dataset.productId, button.dataset.cartAction);
  });
  elements.search.addEventListener("input", renderProducts);
  elements.sort.addEventListener("change", renderProducts);
  $("#account-trigger").addEventListener("click", () => openAuth("login"));
  $("#logout-trigger").addEventListener("click", logout);
  $("#close-auth").addEventListener("click", closeAuth);
  elements.authBackdrop.addEventListener("click", closeAuth);
  $("#login-tab").addEventListener("click", () => setAuthMode("login"));
  $("#register-tab").addEventListener("click", () => setAuthMode("register"));
  elements.authForm.addEventListener("submit", submitAuth);
  $("#checkout-start").addEventListener("click", startCheckout);
  $("#back-to-cart").addEventListener("click", () => {
    elements.checkoutForm.hidden = true;
    elements.drawerFooter.hidden = false;
    elements.checkoutMessage.textContent = "";
  });
  elements.checkoutForm.addEventListener("change", (event) => {
    if (event.target.name === "paymentMethod") {
      elements.checkoutMessage.textContent = "";
      updateBankPayerDetails();
    }
  });
  elements.checkoutForm.addEventListener("submit", submitOrder);
  elements.demoPaymentSuccess.addEventListener("click", confirmDemoPayment);
  $("#close-order-confirmation").addEventListener("click", closeOrderConfirmation);
  $("#confirmation-done").addEventListener("click", closeOrderConfirmation);
  elements.orderConfirmationBackdrop.addEventListener("click", closeOrderConfirmation);

  loadCsrfToken()
    .then(() => Promise.all([loadProducts(), loadCart(), loadCurrentUser(), loadPaymentOptions()]))
    .catch((error) => showToast(error.message));
})();
