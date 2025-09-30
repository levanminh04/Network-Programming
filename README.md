README - Kiến trúc Multi-module

Mục đích
- Giải thích nguyên lý hoạt động và cách sử dụng cấu trúc multi-module cho dự án Network-Programming.

Tổng quan kiến trúc
- Dự án được tổ chức thành nhiều module Maven con dưới một aggregator (root) pom:
  - core: chứa logic lõi, xử lý server, networking, xử lý dữ liệu (jar library).
  - shared: chứa các model, protocol, util dùng chung giữa các module (jar library).
  - gateway: ứng dụng Spring Boot (entry point) triển khai web/REST/WebSocket, phụ thuộc vào core và shared.
  - frontend: (tách riêng, không phải module Maven) phần client (vite/react) nằm ở thư mục riêng.

Nguyên lý hoạt động chính
1) Aggregator (root pom)
   - Loại pom: packaging = pom. Đóng vai trò tổng hợp (reactor) để build nhiều module cùng lúc.
   - Định nghĩa modules: <modules> liệt kê các thư mục con (core, gateway, shared).
   - Có thể chứa dependencyManagement và pluginManagement để đồng bộ phiên bản thư viện và plugin cho toàn bộ modules.
   - Trong dự án hiện tại, root cũng kế thừa spring-boot-starter-parent để tận dụng dependency/plugin mặc định của Spring Boot.

2) Module con (child modules)
   - Mỗi module có pom riêng và khai báo <parent> trỏ về root pom (relativePath) để kế thừa groupId, version, và quản lý dependency/plugin.
   - Module dạng thư viện (core, shared): packaging = jar, không cần spring-boot parent riêng.
   - Module ứng dụng (gateway): vẫn dùng spring-boot-maven-plugin để repackage thành executable jar, nhưng parent là root để giữ nhất quán phiên bản.

3) Kế thừa và quản lý phiên bản
   - groupId và version được định nghĩa ở root và được truyền xuống các module thông qua <parent>, tránh xung đột phiên bản.
   - dependencyManagement ở root (nếu cần) cho phép định nghĩa phiên bản một lần cho mọi module.

Quy trình build và chạy
- Build toàn bộ dự án (tại thư mục gốc):
  mvn -DskipTests package
  Kết quả: Maven sẽ build theo reactor order, tạo jar cho core, gateway, shared. Gateway sẽ được repackage (executable jar).

- Chạy ứng dụng gateway:
  java -jar gateway/target/gateway-0.1.0-SNAPSHOT.jar

Khi thêm module mới
- Thêm thư mục module
- Thêm <module>new-module</module> vào root pom.xml
- Trong new-module/pom.xml khai báo <parent> trỏ về root (relativePath ../pom.xml)

Lợi ích của cấu trúc này
- Tách biệt trách nhiệm: code dùng chung vs ứng dụng triển khai.
- Quản lý phiên bản/phiên bản plugin tập trung (giảm lỗi do không đồng bộ).
- Build nhanh và có thể tái sử dụng module dưới dạng thư viện.

Lưu ý và best-practices
- Không cho từng module kế thừa spring-boot-starter-parent riêng nếu bạn muốn quản lý tập trung; thay vào đó, cho root kế thừa Spring Boot hoặc dùng dependencyManagement để kiểm soát phiên bản Spring Boot.
- Đặt dependency phiên bản trong dependencyManagement của root khi nhiều module dùng cùng thư viện.
- Sử dụng pluginManagement ở root để cấu hình plugin chung (maven-compiler-plugin, spring-boot-maven-plugin).
- Giữ pom module gọn: chỉ khai báo phần đặc thù (dependencies, build) còn lại để root quản lý.

Khắc phục sự cố thường gặp
- Module không được nhận dạng: kiểm tra <modules> trong root và relativePath của parent trong module pom.
- Phiên bản không đồng nhất: kiểm tra groupId/version ở root và đảm bảo module kế thừa.
- Spring Boot repackage lỗi: đảm bảo spring-boot-maven-plugin có trong build/plugins của module ứng dụng (gateway) hoặc được cấu hình trong pluginManagement của root.

Kết luận
- Kiến trúc multi-module phù hợp cho dự án có nhiều thành phần tách biệt (library + ứng dụng).
- Hiện tại dự án đã được cấu hình để root làm aggregator và Spring Boot parent, các module con kế thừa root, đảm bảo nhất quán và dễ bảo trì.


