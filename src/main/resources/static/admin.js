(() => {
  "use strict";

  const $ = (selector) => document.querySelector(selector);
  const state = { csrfToken: null, products: [], orders: [], editingId: null, toastTimer: null };
  const money = new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 });
  const orderStatuses = {
    PLACED: "Mới đặt",
    PROCESSING: "Đang chuẩn bị",
    SHIPPED: "Đang giao",
    DELIVERED: "Đã giao",
    CANCELLED: "Đã hủy"
  };
  const paymentMethods = {
    BANK_TRANSFER: "Chuyển khoản",
    BANK_TRANSFER_DEMO: "Chuyển khoản (demo)",
    VNPAY_DEMO: "VNPay (demo)",
    CASH_ON_DELIVERY: "COD"
  };

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
    const response = await fetch(path, { ...options, headers });
    const data = await response.json().catch(() => null);
    if (response.status === 401) {
      window.location.assign("/");
      throw new Error("Phiên đăng nhập đã hết hạn.");
    }
    if (response.status === 403) {
      window.location.assign("/");
      throw new Error("Tài khoản này không có quyền quản trị.");
    }
    if (!response.ok) throw new Error(data?.error || `Có lỗi xảy ra (${response.status}).`);
    return data;
  }

  async function loadCsrfToken() {
    const response = await fetch("/api/auth/csrf");
    const data = await response.json().catch(() => null);
    if (!response.ok || !data?.token) throw new Error("Không lấy được mã bảo mật CSRF.");
    state.csrfToken = data.token;
  }

  function notify(message) {
    const toast = $("#toast");
    toast.textContent = message;
    toast.classList.add("is-visible");
    clearTimeout(state.toastTimer);
    state.toastTimer = setTimeout(() => toast.classList.remove("is-visible"), 3500);
  }

  function showMessage(message, isError = false) {
    const element = $("#admin-message");
    element.textContent = message;
    element.classList.toggle("is-error", isError);
  }

  function renderProducts() {
    $("#admin-product-count").textContent = String(state.products.length);
    $("#products-empty").hidden = state.products.length !== 0;
    $("#admin-products").innerHTML = state.products.map((product) => `
      <tr>
        <td><div class="admin-product-name"><strong>${escapeHtml(product.name)}</strong><small>${escapeHtml(product.description)}</small></div></td>
        <td>${money.format(product.price)}</td>
        <td>${product.stock}</td>
        <td><span class="status-pill ${product.active ? "status-active" : "status-inactive"}">${product.active ? "Đang bán" : "Đã ẩn"}</span></td>
        <td><div class="table-actions">
          <button class="small-action" type="button" data-product-edit="${product.id}">Sửa</button>
          <button class="small-action ${product.active ? "action-danger" : ""}" type="button" data-product-active="${product.id}" data-next-active="${!product.active}">${product.active ? "Ẩn" : "Hiện lại"}</button>
        </div></td>
      </tr>`).join("");
  }

  function renderOrders() {
    $("#admin-order-count").textContent = String(state.orders.length);
    $("#orders-empty").hidden = state.orders.length !== 0;
    $("#admin-orders").innerHTML = state.orders.map((order) => {
      const canConfirmPayment = order.paymentMethod === "BANK_TRANSFER" && order.paymentStatus !== "PAID";
      const options = Object.entries(orderStatuses)
        .map(([value, label]) => `<option value="${value}" ${order.status === value ? "selected" : ""}>${label}</option>`).join("");
      return `<tr>
        <td><strong>#${order.id}</strong><small class="table-subtext">${new Date(order.createdAt).toLocaleString("vi-VN")}</small></td>
        <td><div class="admin-product-name"><strong>${escapeHtml(order.customerName)}</strong><small>${escapeHtml(order.email)} · ${escapeHtml(order.phone)}</small><small>${escapeHtml(order.shippingAddress)}</small></div></td>
        <td><strong>${paymentMethods[order.paymentMethod] || escapeHtml(order.paymentMethod)}</strong><small class="table-subtext">${order.paymentStatus === "PAID" ? "Đã thanh toán" : order.paymentStatus === "PENDING" ? "Chờ thanh toán" : "Thu tiền khi giao"}</small>${order.payerAccountName ? `<small class="table-subtext">Người chuyển: ${escapeHtml(order.payerAccountName)} · ${order.payerBankName ? `${escapeHtml(order.payerBankName)} · ` : ""}TK ****${escapeHtml(order.payerAccountLastFour)}</small>` : ""}</td>
        <td>${money.format(order.total)}</td>
        <td><select class="order-status-select" data-order-status="${order.id}" aria-label="Trạng thái đơn hàng ${order.id}">${options}</select></td>
        <td><div class="table-actions">
          <button class="small-action" type="button" data-save-status="${order.id}" ${order.status === "DELIVERED" || order.status === "CANCELLED" ? "disabled" : ""}>Cập nhật</button>
          ${canConfirmPayment ? `<button class="small-action payment-confirm" type="button" data-confirm-payment="${order.id}">Đã nhận tiền</button>` : ""}
        </div></td>
      </tr>`;
    }).join("");
  }

  async function loadProducts() {
    state.products = await api("/api/admin/products");
    renderProducts();
  }

  async function loadOrders() {
    state.orders = await api("/api/admin/orders");
    renderOrders();
  }

  function openProductModal(product = null) {
    state.editingId = product?.id || null;
    $("#product-modal-title").textContent = product ? "Sửa sản phẩm" : "Thêm sản phẩm";
    $("#save-product").innerHTML = product ? 'Lưu thay đổi <span aria-hidden="true">↗</span>' : 'Lưu sản phẩm <span aria-hidden="true">↗</span>';
    $("#product-form-error").textContent = "";
    $("#product-form").reset();
    if (product) {
      for (const [key, value] of Object.entries({
        name: product.name,
        description: product.description,
        imageUrl: product.imageUrl,
        price: product.price,
        stock: product.stock
      })) {
        $("#product-form").elements[key].value = value;
      }
    }
    $("#product-modal").hidden = false;
    $("#product-modal-backdrop").hidden = false;
    $("#product-form").elements.name.focus();
  }

  function closeProductModal() {
    $("#product-modal").hidden = true;
    $("#product-modal-backdrop").hidden = true;
    $("#new-product").focus();
  }

  async function saveProduct(event) {
    event.preventDefault();
    if (!$("#product-form").reportValidity()) return;
    $("#save-product").disabled = true;
    $("#product-form-error").textContent = "";
    const form = new FormData($("#product-form"));
    const request = {
      name: form.get("name").trim(),
      description: form.get("description").trim(),
      imageUrl: form.get("imageUrl").trim(),
      price: Number(form.get("price")),
      stock: Number(form.get("stock"))
    };
    try {
      const url = state.editingId ? `/api/admin/products/${state.editingId}` : "/api/admin/products";
      const product = await api(url, { method: state.editingId ? "PUT" : "POST", body: JSON.stringify(request) });
      if (state.editingId) {
        state.products = state.products.map((item) => item.id === product.id ? product : item);
      } else {
        state.products.push(product);
      }
      renderProducts();
      closeProductModal();
      notify(state.editingId ? "Đã cập nhật sản phẩm." : "Đã thêm sản phẩm mới.");
    } catch (error) {
      $("#product-form-error").textContent = error.message;
    } finally {
      $("#save-product").disabled = false;
    }
  }

  async function setProductActive(id, active) {
    try {
      const product = await api(`/api/admin/products/${id}/active`, {
        method: "PATCH",
        body: JSON.stringify({ active })
      });
      state.products = state.products.map((item) => item.id === product.id ? product : item);
      renderProducts();
      notify(active ? "Sản phẩm đã được hiện lại." : "Sản phẩm đã được ẩn khỏi cửa hàng.");
    } catch (error) {
      showMessage(error.message, true);
    }
  }

  async function saveOrderStatus(id) {
    const status = $(`[data-order-status="${id}"]`).value;
    if (status === "CANCELLED" && !window.confirm(`Xác nhận hủy đơn #${id}? Tồn kho sẽ được hoàn lại.`)) return;
    try {
      await api(`/api/admin/orders/${id}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status })
      });
      await Promise.all([loadOrders(), loadProducts()]);
      notify(`Đã cập nhật trạng thái đơn #${id}.`);
    } catch (error) {
      showMessage(error.message, true);
    }
  }

  async function confirmPayment(id) {
    if (!window.confirm(`Xác nhận cửa hàng đã nhận đủ tiền chuyển khoản cho đơn #${id}?`)) return;
    try {
      await api(`/api/admin/orders/${id}/payment-received`, { method: "POST" });
      await loadOrders();
      notify(`Đã xác nhận thanh toán đơn #${id}.`);
    } catch (error) {
      showMessage(error.message, true);
    }
  }

  async function initialize() {
    await loadCsrfToken();
    const current = await api("/api/auth/me");
    if (!current.authenticated) {
      window.location.assign("/");
      return;
    }
    if (current.customer.role !== "ADMIN") {
      document.body.innerHTML = '<main class="admin-denied"><h1>Khu vực dành cho quản trị viên</h1><p>Tài khoản này không có quyền truy cập.</p><a class="button button-dark" href="/">Quay lại cửa hàng</a></main>';
      return;
    }
    $("#admin-user").textContent = current.customer.fullName;
    showMessage("Đang tải dữ liệu quản trị...");
    try {
      await Promise.all([loadProducts(), loadOrders()]);
      showMessage("");
    } catch (error) {
      showMessage(error.message, true);
    }
  }

  document.querySelectorAll(".admin-tab").forEach((tab) => tab.addEventListener("click", () => {
    document.querySelectorAll(".admin-tab").forEach((item) => {
      item.classList.toggle("is-active", item === tab);
      item.setAttribute("aria-selected", String(item === tab));
    });
    document.querySelectorAll(".admin-panel").forEach((panel) => {
      panel.hidden = panel.id !== tab.dataset.panel;
    });
  }));
  $("#new-product").addEventListener("click", () => openProductModal());
  $("#close-product-modal").addEventListener("click", closeProductModal);
  $("#product-modal-backdrop").addEventListener("click", closeProductModal);
  $("#product-form").addEventListener("submit", saveProduct);
  $("#admin-products").addEventListener("click", (event) => {
    const edit = event.target.closest("[data-product-edit]");
    const active = event.target.closest("[data-product-active]");
    if (edit) openProductModal(state.products.find((product) => product.id === Number(edit.dataset.productEdit)));
    if (active) setProductActive(Number(active.dataset.productActive), active.dataset.nextActive === "true");
  });
  $("#admin-orders").addEventListener("click", (event) => {
    const status = event.target.closest("[data-save-status]");
    const payment = event.target.closest("[data-confirm-payment]");
    if (status) saveOrderStatus(Number(status.dataset.saveStatus));
    if (payment) confirmPayment(Number(payment.dataset.confirmPayment));
  });
  $("#refresh-orders").addEventListener("click", () => loadOrders().catch((error) => showMessage(error.message, true)));
  $("#admin-logout").addEventListener("click", async () => {
    try {
      await api("/api/auth/logout", { method: "POST" });
      window.location.assign("/");
    } catch (error) {
      showMessage(error.message, true);
    }
  });
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && !$("#product-modal").hidden) closeProductModal();
  });

  initialize().catch((error) => showMessage(error.message, true));
})();
