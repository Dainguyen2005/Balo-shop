# Balo Nhà Mình — Website bán balo

Website bán balo với giao diện responsive và REST API, viết bằng Java 21, Spring Boot và Spring Data JPA. Mặc định dự án vẫn dùng H2; có thể kết nối MySQL và chuyển dữ liệu H2 hiện tại theo hướng dẫn bên dưới. Khi khởi động, ứng dụng tự bổ sung các sản phẩm mẫu còn thiếu mà không tạo trùng sản phẩm đã có. Giao diện cửa hàng và API được phục vụ cùng một địa chỉ.

## Chạy ứng dụng

Cần JDK 21 và Maven 3.8+:

```powershell
mvn spring-boot:run
```

Nếu Maven chưa có trong PATH nhưng đã cài theo hướng dẫn của project:

```powershell
& "$env:USERPROFILE\.maven\apache-maven-3.9.11\bin\mvn.cmd" spring-boot:run
```

Mở website tại `http://localhost:8082`; API chạy cùng host dưới `/api`. Khi dùng H2, console ở `http://localhost:8082/h2-console` với JDBC URL `jdbc:h2:file:./data/baloshop`, username `sa`, mật khẩu để trống. Ảnh mẫu lấy từ Unsplash nên cần kết nối internet để hiển thị.

Website hỗ trợ tìm kiếm, sắp xếp sản phẩm, thêm/xóa sản phẩm trong giỏ, cập nhật số lượng và gửi thông tin đặt hàng. Giỏ hàng được giữ trong bộ nhớ của ứng dụng nên bị xóa khi ứng dụng khởi động lại; sản phẩm, tồn kho và đơn hàng được lưu trong database đang cấu hình.

## Kết nối MySQL và chuyển dữ liệu từ H2

MySQL Connector/J đã được thêm vào dự án. Trên máy phát triển hiện tại, MySQL đang chạy tại `127.0.0.1:3307`. Cấu hình database bằng file `.env` cục bộ (đã được `.gitignore` loại trừ):

1. Sao chép `.env.example` thành `.env`.
2. Điền `SHOP_DB_USERNAME` và `SHOP_DB_PASSWORD` cho tài khoản MySQL có quyền tạo database/bảng và ghi dữ liệu vào `balo_db`. Không gửi mật khẩu qua chat hoặc commit file `.env`.
3. Giữ `SHOP_DB_URL` trỏ đến `jdbc:mysql://127.0.0.1:3307/balo_db?...`; `createDatabaseIfNotExist=true` sẽ tạo database nếu tài khoản có quyền.
4. Để chuyển dữ liệu hiện có từ H2, thêm `SHOP_DB_MIGRATE_FROM_H2=true` vào `.env`, dừng ứng dụng cũ để giải phóng file H2 rồi chạy `mvn spring-boot:run`. Lệnh chuyển sao chép sản phẩm, tài khoản (kể cả password hash), đơn hàng và dòng hàng; bản H2 được giữ nguyên. Việc sao chép chỉ chạy khi các bảng MySQL đích đang trống và được rollback nếu gặp lỗi.
5. Khi log xác nhận các bảng đã chuyển xong, xóa `SHOP_DB_MIGRATE_FROM_H2` khỏi `.env` và khởi động lại ứng dụng bình thường. Không giữ cờ này sau lần chuyển đầu tiên.

Ứng dụng dùng `spring.jpa.hibernate.ddl-auto=update` để tạo/cập nhật bảng trên MySQL khi khởi động. Nếu chưa muốn chuyển dữ liệu, chỉ cần cấu hình `.env` kết nối MySQL và bỏ qua cờ migration; ứng dụng sẽ tạo các sản phẩm mẫu trong database mới. Khi không có `SHOP_DB_URL`, dự án tiếp tục dùng H2 như trước.

Khách hàng có thể đăng ký/đăng nhập bằng email và mật khẩu (tối thiểu 8 ký tự). Mật khẩu được băm bằng BCrypt, phiên đăng nhập được giữ bằng cookie HttpOnly/SameSite và đặt hàng yêu cầu đăng nhập. Đơn hàng gắn với tài khoản để khách khác không thể xem đơn bằng cách đoán mã đơn.

## Quản trị cửa hàng

Trang quản trị ở `http://localhost:8082/admin.html`. Tài khoản đăng ký trên cửa hàng mặc định chỉ có quyền `CUSTOMER`; API quản trị yêu cầu role `ADMIN` ở phía server.

Tạo tài khoản admin ban đầu bằng cách mở **PowerShell** tại thư mục project và đặt email/mật khẩu riêng trong cửa sổ đó trước khi khởi động:

```powershell
$env:SHOP_ADMIN_EMAIL = "admin@example.com"
$env:SHOP_ADMIN_PASSWORD = "ThayBangMatKhauManhCuaBan"
$env:SHOP_ADMIN_FULL_NAME = "Quản trị cửa hàng"
& "$env:USERPROFILE\.maven\apache-maven-3.9.11\bin\mvn.cmd" spring-boot:run
```

Trong **Command Prompt (cmd)**, dùng cú pháp tương ứng:

```cmd
set SHOP_ADMIN_EMAIL=admin@example.com
set SHOP_ADMIN_PASSWORD=ThayBangMatKhauManhCuaBan
set SHOP_ADMIN_FULL_NAME=Quan tri cua hang
"%USERPROFILE%\.maven\apache-maven-3.9.11\bin\mvn.cmd" spring-boot:run
```

Chỉ cần cấu hình admin ở lần chạy đầu để tạo tài khoản. Nếu email đã tồn tại, ứng dụng nâng tài khoản đó lên admin nhưng giữ nguyên mật khẩu hiện có; cần đăng nhập bằng mật khẩu của tài khoản đó. Không dùng chung mật khẩu khách hàng, không chia sẻ terminal có chứa mật khẩu và không đưa mật khẩu vào mã nguồn.

Admin có thể thêm/sửa/ẩn/hiện lại sản phẩm, xem đơn hàng, cập nhật trạng thái và xác nhận đã nhận tiền chuyển khoản. Ẩn sản phẩm là thao tác mềm để giữ lịch sử đơn. Khi hủy đơn chưa thanh toán, tồn kho được hoàn lại; đơn đã thanh toán cần xử lý hoàn tiền trước khi hủy. Đơn chuyển khoản cần được xác nhận đã nhận tiền trước khi chuyển sang trạng thái giao hàng.

## Thanh toán demo

Khách chọn VNPay (demo), chuyển khoản ngân hàng (demo) hoặc COD. Hai lựa chọn demo chỉ mô phỏng giao dịch, không kết nối VNPay/ngân hàng và không chuyển/thu tiền thật. Sau khi tạo đơn, khách có thể bấm “Mô phỏng thanh toán thành công” để cập nhật đơn demo thành `PAID`. Thông tin ngân hàng hiển thị là dữ liệu giả lập.

Với chuyển khoản demo, khách nhập tên ngân hàng, tên chủ tài khoản và một số tài khoản giả lập (6–20 chữ số). Trình duyệt chỉ gửi 4 số cuối lên máy chủ; số đầy đủ không được lưu. Không yêu cầu/nhập số tài khoản thật, mật khẩu ngân hàng, PIN, OTP hoặc thông tin thẻ. VNPay demo không yêu cầu thông tin tài khoản/thẻ. COD vẫn ở trạng thái `PENDING_ON_DELIVERY`.

## API chính

| Method | Đường dẫn | Mô tả |
|---|---|---|
| GET | `/api/auth/csrf` | Lấy mã CSRF cho các yêu cầu thay đổi dữ liệu |
| GET | `/api/auth/me` | Kiểm tra trạng thái đăng nhập |
| POST | `/api/auth/register` | Đăng ký `{ "email": "ban@example.com", "fullName": "Tên bạn", "password": "matkhau123" }` |
| POST | `/api/auth/login` | Đăng nhập bằng email và mật khẩu |
| POST | `/api/auth/logout` | Đăng xuất |
| GET | `/api/products` | Danh sách balo |
| GET | `/api/products/{id}` | Chi tiết balo |
| POST | `/api/carts` | Tạo giỏ hàng |
| GET | `/api/carts/{cartId}` | Xem giỏ hàng |
| POST | `/api/carts/{cartId}/items` | Thêm `{ "productId": 1, "quantity": 2 }` |
| PUT | `/api/carts/{cartId}/items/{productId}?quantity=1` | Cập nhật số lượng |
| DELETE | `/api/carts/{cartId}/items/{productId}` | Xóa sản phẩm khỏi giỏ |
| POST | `/api/carts/{cartId}/checkout` | Đặt hàng |
| GET | `/api/orders/{id}` | Xem đơn hàng |
| POST | `/api/orders/{id}/demo-payment` | Mô phỏng thanh toán thành công cho đơn VNPay/chuyển khoản demo |
| GET | `/api/payments/options` | Các phương thức thanh toán và trạng thái cấu hình tài khoản |
| GET/POST/PUT/DELETE | `/api/admin/products` | Quản lý sản phẩm (ADMIN) |
| GET/PATCH | `/api/admin/orders` | Xem đơn và cập nhật trạng thái (ADMIN) |
| POST | `/api/admin/orders/{id}/payment-received` | Xác nhận đã nhận chuyển khoản (ADMIN) |

Các API ghi dữ liệu dùng cookie phiên và CSRF. Trình duyệt tự xử lý phiên; với client API, gọi `/api/auth/csrf`, gửi cookie phiên và gửi giá trị `token` trong header `X-XSRF-TOKEN`. Đặt hàng cần phiên đăng nhập hợp lệ.

Body đặt hàng:

```json
{
  "customerName": "Nguyen Van A",
  "email": "a@example.com",
  "phone": "0900000000",
  "shippingAddress": "1 Le Loi, Quan 1, TP HCM",
  "paymentMethod": "CASH_ON_DELIVERY"
}
```

`paymentMethod` nhận `BANK_TRANSFER_DEMO`, `VNPAY_DEMO` hoặc `CASH_ON_DELIVERY`. Với `BANK_TRANSFER_DEMO`, gửi `payerBankName`, `payerAccountName` và `payerAccountLastFour` (4 chữ số cuối từ dữ liệu tài khoản giả lập); không gửi toàn bộ số tài khoản. Dùng endpoint `/api/orders/{id}/demo-payment` để mô phỏng hoàn tất thanh toán.

Giỏ hàng được giữ trong bộ nhớ của ứng dụng nên bị xóa khi khởi động lại; sản phẩm, tồn kho và đơn hàng được lưu trong H2.
