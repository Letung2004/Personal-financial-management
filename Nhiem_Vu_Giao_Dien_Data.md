# CHI TIẾT NHIỆM VỤ: LẬP TRÌNH VIÊN 2 - GIAO DIỆN & DỮ LIỆU (SV3)
> **Lưu ý:** File này là tài liệu nội bộ cho SV3. Nội dung được tổng hợp vào `Bao_Cao_Do_An.md` để nộp báo cáo chính thức.

---

## 1. Xử lý Dữ liệu (File I/O)
Nhiệm vụ này đảm bảo dữ liệu không bị mất sau khi đóng chương trình. Bạn cần phối hợp chặt chẽ với SV2 để biết cách "dựng lại" cây từ file.

### ⚠️ Hợp đồng định dạng file CSV (BẮT BUỘC — SV1 quy định)

Đây là chuẩn **bất biến** để SV2, SV3, SV4 làm việc độc lập mà vẫn tương thích:

```
# Format mỗi dòng trong data.csv:
YYYY-MM-DD,amount,CategoryPath,note

# Ví dụ thực tế:
2024-05-11,150000,CHI/Nhu cầu thiết yếu/Ăn uống/Ăn sáng,Phở buổi sáng
2024-05-11,8000000,THU/Lương/Lương chính,Lương tháng 5
2024-05-12,500000,CHI/Nhu cầu thiết yếu/Di chuyển/Xăng xe,Đổ xăng xe máy
```

**Quy ước bắt buộc:**
- Thứ tự cột: `ngày → số tiền → đường dẫn danh mục → ghi chú`
- `CategoryPath` dùng `/` để phân cách cấp bậc.
- Cấp đầu tiên **luôn là `THU` hoặc `CHI`** (không phải `Root`, `Thu nhập`, `Income`...).
- Khi `loadData()` đọc từng dòng: nếu một cấp trong path chưa tồn tại → **tự động tạo mới** bằng `addCategory()` của SV2.
- **Xử lý dấu phẩy (,) trong Ghi chú (note):** 
  * Khi xuất dữ liệu (`saveData()`): Nếu trường `note` có chứa dấu phẩy `,`, bắt buộc phải bọc nó trong dấu nháy kép `""` (Ví dụ: `2024-05-11,150000,CHI/Nhu cầu thiết yếu/Ăn uống/Ăn sáng,"Ăn sáng, mua thêm nước"`) và tiến hành escape các ký tự nháy kép `"` bên trong ghi chú thành nháy kép kép `""`.
  * Khi nạp dữ liệu (`loadData()`): Sử dụng biểu thức chính quy Regex Lookahead thông minh `line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 4)` để phân tách chuỗi thay vì dùng `split(",")` thô sơ, nhằm tránh việc tách nhầm dấu phẩy bên trong ghi chú được bọc nháy kép thành cột mới (gây lệch cột và bỏ qua dòng theo bảng xử lý lỗi).

### Các hàm cần viết:

*   **`saveData(filename)`:**
    *   Duyệt qua toàn bộ cây (dùng DFS từ SV2).
    *   Lưu thông tin giao dịch theo đúng format: `YYYY-MM-DD,amount,THU/...,note`. Nếu trường `note` có chứa dấu phẩy `,`, bắt buộc phải bọc nó trong dấu nháy kép `""` và escape dấu nháy kép bên trong (thay thế `"` thành `""`).
    *   Lưu thêm phần cấu trúc cây (các path chưa có giao dịch) để khi load lại không bị mất nhánh rỗng.

*   **`loadData(filename)`:**
    *   Mở file, đọc từng dòng.
    *   Với mỗi dòng, tách dữ liệu ra bằng Regex Lookahead `line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 4)` và gọi hàm từ SV2 để chèn vào cây. Nếu danh mục cha chưa tồn tại → tự động tạo mới.
    *   Nếu cột ghi chú bị bọc bởi dấu nháy kép `""`, hãy tiến hành loại bỏ dấu nháy kép ở hai đầu và thay thế các dấu nháy kép kép `""` bên trong thành một dấu nháy kép đơn `"`.
    *   **Không được để crash chương trình** khi gặp file lỗi.
    *   **Trả về số dòng đọc thành công** (int), không ném exception ra ngoài.

### Bảng xử lý lỗi bắt buộc trong `loadData()`:

| Trường hợp | Hành vi yêu cầu |
|---|---|
| File không tồn tại | Thông báo "File chưa có, bắt đầu mới"; trả về `false`; không crash |
| File rỗng | Trả về `true` với 0 giao dịch; không crash |
| Dòng thiếu dấu `/` trong `CategoryPath` | Bỏ qua dòng đó, in cảnh báo `"[WARN] Line X: invalid path"` |
| Số tiền không phải số (`"abc"`) | Bỏ qua dòng đó, in cảnh báo |
| Dòng thiếu cột (< 4 phần) | Bỏ qua dòng đó, in cảnh báo |
| Số tiền âm trong file | Bỏ qua dòng đó (SV2 sẽ từ chối), in cảnh báo |
| Đường dẫn không bắt đầu bằng `THU` hoặc `CHI` | Bỏ qua dòng đó, in cảnh báo |

> **Nguyên tắc:** `loadData()` phải được bao bọc trong `try-catch` (Java) ở mức toàn cục. Phải bắt **tất cả** lỗi bên trong hàm, không để crash.

---

## 2. Xây dựng Giao diện người dùng
Bạn có thể chọn làm Console (dễ hơn) hoặc GUI (đẹp hơn, điểm cộng cao hơn).

### ⚠️ Nguyên tắc bắt buộc — Tách biệt UI khỏi Core (SV1 quy định):
> Module UI (SV3) **chỉ được hiển thị dữ liệu, không xử lý logic cây**. Module Core (SV2) **không in gì ra màn hình** — chỉ trả về kết quả. SV3 nhận kết quả đó và quyết định hiển thị như thế nào.

**Ví dụ đúng:**
```java
// SV3 (UI) gọi SV2 (Core) và tự hiển thị kết quả:
boolean result = manager.addCategory(parentPath, name, "CHI");
if (result) {
    System.out.println("[OK] Đã thêm danh mục.");
} else {
    System.out.println("[LỖI] Tên đã tồn tại trong cùng cấp.");
}
```

### Lưu ý phối hợp với SV2 — `nodeIndex`:
> Khi người dùng gõ tên danh mục để nhập giao dịch, bạn (SV3) **không cần tự duyệt cây**. Hãy gọi `manager.addTransaction(path, ...)` — SV2 sẽ dùng `nodeIndex` để tra cứu O(1). Bạn chỉ cần truyền đúng chuỗi đường dẫn `path` theo chuẩn `THU/...` hoặc `CHI/...`.

### Luồng chức năng (Menu):
1.  **Quản lý danh mục:** Thêm/Xóa/Đổi tên danh mục. Khi xóa — hỏi người dùng chọn `CASCADE` hay `REPARENT`.
2.  **Nhập liệu:** Nhập thu nhập hoặc chi tiêu mới. Hiển thị danh sách path gợi ý từ `getAllLeafPaths()`.
3.  **Báo cáo:** Xem tổng thu, tổng chi, số dư. Hiển thị chi tiết từng nhánh.
4.  **Tìm kiếm:** Tìm giao dịch theo từ khóa ghi chú hoặc theo khoảng ngày.
5.  **Lưu/Tải:** Lưu xuống file, tải lại từ file.
6.  **Thoát & Lưu tự động.**

---

## 3. Hướng dẫn làm GUI và cách kết nối

Nếu bạn không muốn làm màn hình Console đen trắng mà muốn làm giao diện cửa sổ (GUI):

### Lựa chọn công nghệ:
*   **Java:** Sử dụng **Swing** (chuyên nghiệp, tích hợp sẵn) hoặc **JavaFX**.

### Cách kết nối Giao diện với Logic (Backend):

1.  **Khai báo đối tượng:** Trong lớp Giao diện, hãy khai báo một biến đại diện cho lớp `FinanceManager` (do SV2 viết).
    *   *Ví dụ (Java):* `private FinanceManager manager = new FinanceManager();`
2.  **Bắt sự kiện (Event Handling):** Khi người dùng nhấn nút "Lưu Giao Dịch":
    *   Bước 1: Lấy dữ liệu từ các ô nhập liệu (TextBox/Entry).
    *   Bước 2: Gọi hàm của SV2: `manager.addTransaction(amount, date, note, path)`
    *   Bước 3: Hiển thị thông báo thành công hoặc lỗi dựa trên kết quả trả về.
3.  **Hiển thị cấu trúc cây:** Dùng **JTree** hoặc **TreeView** của Java (Swing `JTree` hoặc JavaFX `TreeView`) và "đổ" dữ liệu từ SV2 vào.

**Lưu ý:** Vì SV2 không có lệnh `print` trong Core (theo quy định của SV1), bạn có thể gọi Core từ GUI một cách dễ dàng mà không lo bị "lẫn" output.

---

## 4. Coding Convention (Áp dụng cho SV3)

Theo quy định của SV1 cho toàn nhóm:

| Quy tắc | Nội dung |
|---|---|
| Tên hàm | camelCase: `loadData()`, `saveData()`, `generateReport()` |
| Tên biến | camelCase: `categoryPath`, `totalIncome` |
| Quản lý lỗi | Hàm trả `bool` hoặc `int` (số dòng thành công). Không ném exception ra ngoài. |
| Encoding file | UTF-8 (bắt buộc để tránh lỗi tiếng Việt) |
| Comment | Mỗi hàm phải có comment mô tả Input, Output, và điều kiện biên |

---

## 5. Danh mục kiểm tra của SV3 (Checklist)

Trước khi bàn giao module UI & Dữ liệu, SV3 cần tự kiểm tra các tiêu chí sau:
- [x] **Xử lý Giao dịch chứa dấu phẩy trong note:** 
    * Khi xuất file (`saveData()`): Bọc ghi chú có dấu phẩy trong dấu nháy kép `""` và escape dấu nháy kép bên trong thành `""`.
    * Khi nạp file (`loadData()`): Sử dụng Regex Lookahead `line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 4)` để không bị lệch cột. Sau đó giải phóng dấu nháy kép ở hai đầu và unescape `""` thành `"`.
- [x] **Quản lý lỗi và tính chịu lỗi (Fault Tolerance):**
    * Bắt mọi exception bên trong `loadData()` để đảm bảo chương trình không bao giờ bị sập (crash) khi đọc phải file dữ liệu bị hư hỏng, thiếu cột, sai định dạng tiền hoặc sai cấu trúc đường dẫn.
    * Ghi nhận đầy đủ các cảnh báo dòng lỗi vào danh sách cảnh báo (không in trực tiếp bằng `System.out`) để UI có thể lấy và hiển thị.
- [x] **Giao diện người dùng đồ họa (Swing GUI):**
    * Hiển thị cấu trúc cây danh mục trực quan bằng `JTree`.
    * Cung cấp các thao tác thêm, xóa (CASCADE / REPARENT), đổi tên danh mục và tự động cập nhật lại giao diện.
    * Tích hợp bảng danh sách giao dịch, lọc theo khoảng thời gian và tìm kiếm theo từ khóa.
    * Hiển thị tổng thu, tổng chi và số dư ví rõ ràng.
