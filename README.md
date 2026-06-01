# Hệ thống Quản lý Tài chính Cá nhân Phân cấp (Java)

Dự án môn học **Cấu trúc dữ liệu & Giải thuật** triển khai hệ thống quản lý tài chính cá nhân sử dụng cấu trúc dữ liệu cây kết hợp bảng băm, hỗ trợ giao diện tương tác dòng lệnh (Console) và giao diện cửa sổ đồ họa (Swing GUI) trực quan.

---

## 🌟 Thành viên & Vai trò
* **SV1 (Trưởng nhóm):** Thiết lập quy định, đặc tả CSV và viết mã nguồn Python tham chiếu.
* **SV2 (Core Logic):** Cài đặt cấu trúc cây danh mục N-ary, DFS tính tổng đệ quy, và BFS tìm kiếm.
* **SV3 (Giao diện & Dữ liệu):** Xây dựng I/O lưu/tải CSV chống lỗi crash, menu Console và **Premium Swing GUI**.
* **SV4 (Kiểm thử QA/QC):** Lập bộ kiểm thử tự động `PerformanceAndEdgeTest` đo hiệu năng và kiểm thử biên.
* **SV5 (Tài liệu):** Viết báo cáo khoa học và phân tích độ phức tạp Big O.

---

## 🛠️ Công nghệ sử dụng
* **Ngôn ngữ:** Java (JDK 8 trở lên)
* **Giao diện:** Java Swing (sử dụng thư viện chuẩn, không phụ thuộc bên thứ ba)
* **Lưu trữ dữ liệu:** File CSV mã hóa UTF-8

---

## ✨ Các tính năng chính

### 1. Quản lý Danh mục tài chính phân cấp (Tree structure)
* Khởi tạo cấu trúc danh mục mặc định chuẩn 4 nhánh `THU` (Lương, Thưởng, Đầu tư, Thu nhập khác) và nhánh `CHI` phân cấp 3 tầng (Nhu cầu thiết yếu, Giáo dục & Phát triển, Hưởng thụ).
* Thêm danh mục con vào vị trí bất kỳ trên cây.
* Đổi tên danh mục (tự động cập nhật lại toàn bộ đường dẫn của các danh mục con cháu trong bảng băm).
* Xóa danh mục với hai chế độ nâng cao:
  * **CASCADE:** Xóa đệ quy toàn bộ danh mục con cháu và các giao dịch trực thuộc.
  * **REPARENT:** Chuyển giao toàn bộ nút con và giao dịch của nút bị xóa lên nút cha trực tiếp trước khi xóa.

### 2. Quản lý Giao dịch & Phân loại
* Ghi nhận giao dịch với các thông tin: số tiền (validation > 0), ngày tháng, ghi chú, và đường dẫn phân cấp.
* Tự động phân loại giao dịch vào đúng nút danh mục tương ứng trên cây chỉ với độ phức tạp $O(1)$.

### 3. Báo cáo & Thống kê Phân cấp
* Tính tổng thu nhập, tổng chi tiêu và số dư ví.
* Phân tích tỷ lệ phần trăm đóng góp của các danh mục chi tiêu con so với danh mục cha hoặc tổng chi tiêu toàn hệ thống.

### 4. Tìm kiếm & Lọc nâng cao
* Tìm kiếm danh mục theo tên (sử dụng thuật toán BFS).
* Tra cứu danh mục theo đường dẫn đầy đủ (O(1) nhờ nodeIndex).
* Tìm kiếm giao dịch theo từ khóa trong ghi chú và lọc giao dịch theo khoảng ngày tháng.

### 5. Lưu trữ & Khôi phục (CSV I/O)
* Lưu trữ tự động toàn bộ cây và giao dịch xuống file `data.csv` khi thoát.
* Tải dữ liệu an toàn khi khởi động với cơ chế kiểm tra định dạng dòng lỗi (thiếu cột, sai định dạng số, tiền âm, sai tiền tố đường dẫn) mà không làm sập ứng dụng.

---

## 📈 Kiến trúc & Thuật toán
Hệ thống sử dụng thiết kế cấu trúc dữ liệu kép (Hybrid DS) để tối ưu hóa hiệu năng:
1. **Cây N-ary (N-ary Tree):** Dùng để biểu diễn phân cấp các danh mục và tính toán tổng số tiền của một nhánh đệ quy bằng **DFS (Depth-First Search)** với độ phức tạp $O(N_{sub} + M_{sub})$.
2. **Bảng băm (HashMap - `nodeIndex`):** Ánh xạ từ đường dẫn tuyệt đối (ví dụ: `CHI/Nhu cầu thiết yếu/Ăn uống`) đến tham chiếu nút `CategoryNode`. Giúp hoạt động nhập giao dịch và kiểm tra nút tồn tại đạt tốc độ **$O(1)$** thay vì phải duyệt cây mất $O(N)$ thời gian.

---

## 🚀 Hướng dẫn biên dịch và Chạy chương trình

Mở Terminal (PowerShell hoặc CMD trên Windows) tại thư mục gốc của dự án và chạy các lệnh sau:

### 1. Biên dịch dự án:
```bash
# Tạo thư mục bin chứa file class sau biên dịch
mkdir bin

# Biên dịch toàn bộ file java
javac -encoding UTF-8 -d bin src/java/models/*.java src/java/core/*.java src/java/ui/*.java src/java/PerformanceAndEdgeTest.java src/java/Main.java
```

### 2. Chạy ứng dụng chính (Main):
```bash
java -cp bin Main
```
Sau khi chạy, menu khởi động sẽ xuất hiện:
* **Nhập 1:** Chạy tự động kịch bản Demo.
* **Nhập 2:** Khởi chạy Giao diện tương tác Console.
* **Nhập 3:** Khởi chạy **Giao diện cửa sổ đồ họa Premium Swing GUI** (khuyên dùng).
* **Nhập 4:** Thoát.

### 3. Chạy bộ kiểm thử QA/QC tự động:
Chạy lớp đo đạc hiệu năng và kiểm tra lỗi biên tự động:
```bash
java -cp bin PerformanceAndEdgeTest
```
