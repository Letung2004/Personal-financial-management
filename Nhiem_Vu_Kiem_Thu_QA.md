# CHI TIẾT NHIỆM VỤ: KIỂM THỬ QA/QC (SV4)
> **Lưu ý:** File này là tài liệu nội bộ cho SV4. Nội dung được tổng hợp vào `Bao_Cao_Do_An.md` để nộp báo cáo chính thức.

---

## 1. Sinh dữ liệu mẫu (Data Generation)
Để kiểm tra tính ổn định và tốc độ của cấu trúc Cây, bạn cần tạo ra một lượng dữ liệu lớn mà không thể nhập tay.

### Cách 1: Dùng hàm `generateTestData()` của SV2 (khuyến nghị)

SV2 đã cài đặt hàm này trong `FinanceManager`. Bạn chỉ cần gọi:

```java
manager.generateTestData(10000, true);  // true = phân bổ đều
manager.generateTestData(10000, false); // false = phân bổ ngẫu nhiên không đều
```

### Cách 2: Tự viết script sinh dữ liệu (để có file CSV kiểm tra `loadData`)

Mục đích của cách này là tạo ra file `test_data.csv` theo đúng định dạng SV1 quy định để kiểm tra `loadData()` của SV3.

**⚠️ Định dạng file phải đúng chuẩn (SV1 — Section 5):**
```
YYYY-MM-DD,amount,CategoryPath,note
```
Ví dụ:
```
2024-03-15,150000,CHI/Nhu cầu thiết yếu/Ăn uống/Ăn sáng,Test GD #1
2024-07-22,8000000,THU/Lương/Lương chính,Test GD #2
```
> **Lưu ý quan trọng:** Path PHẢI bắt đầu bằng `THU` hoặc `CHI`, không phải `Root` hay `Thu nhập`. Nếu sai format, `loadData()` của SV3 sẽ bỏ qua dòng đó.

```java
// Ví dụ code sinh file CSV đúng chuẩn (Java):
import java.io.*;
import java.util.*;
import java.time.LocalDate;

public class DataGenerator {
    public static void generate(FinanceManager manager) throws IOException {
        List<String> leafPaths = manager.listLeafCategories(); // Lấy tất cả nút lá từ SV2
        Random random = new Random();
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("test_data.csv"), "UTF-8"))) {
            for (int i = 0; i < 10000; i++) {
                String path = leafPaths.get(random.nextInt(leafPaths.size()));
                double amount = 10000 + random.nextDouble() * 9990000;
                LocalDate date = startDate.plusDays(random.nextInt(365));
                String note = "Test GD #" + i;
                writer.write(String.format(Locale.US, "%s,%.2f,%s,%s\n", date, amount, path, note));
            }
        }
    }
}
```

### ⚠️ Yêu cầu phân bổ đều theo chiều sâu cây (quan trọng):
> Nếu 10.000 giao dịch chỉ tập trung vào 1-2 nút lá, bài kiểm thử DFS sẽ **không phản ánh đúng thực tế**.

**Quy tắc phân bổ bắt buộc:**
*   Lấy danh sách **tất cả nút lá** trong cây (dùng `getAllLeafPaths()`).
*   Phân bổ 10.000 giao dịch **đều theo tỷ lệ** vào các nút lá.
*   **Kiểm tra sau sinh:** Mỗi nút lá phải nhận ít nhất `10.000 / tổng_nút_lá × 0.5` giao dịch (không được lệch quá 50% so với mức trung bình).

---

## 2. Kiểm thử hiệu năng (Performance Test)
Sau khi có 10.000 dữ liệu, bạn cần đo đạc các thông số để đưa vào báo cáo cuối cùng.

### Các kịch bản đo đạc:
1.  **Thời gian tải dữ liệu (Load):** Đo từ lúc bắt đầu đọc file 10.000 bản ghi đến khi dựng xong Cây trong bộ nhớ.
2.  **Thời gian báo cáo (Report):** Đo thời gian thuật toán DFS tính tổng toàn bộ cây với 10.000 bản ghi.
3.  **Thời gian tìm kiếm (Search):**
    - Tìm danh mục theo **đường dẫn** (phải là O(1) nhờ `nodeIndex`).
    - Tìm giao dịch theo **từ khóa** (phải là O(N+M)).

### Yêu cầu:
*   Lập bảng so sánh thời gian thực thi giữa các mức: **100 bản ghi**, **1.000 bản ghi** và **10.000 bản ghi**.
*   Sử dụng các thư viện đo thời gian của ngôn ngữ (VD: `System.nanoTime()` trong Java).

---

## 3. Kiểm thử biên và Logic (Edge Cases)
Đảm bảo phần mềm không bị "văng" (crash) khi người dùng nhập sai.

### Các trường hợp cần kiểm tra:

#### Nhóm A: Dữ liệu nhập tay
*   **Số tiền không hợp lệ:** Nhập số tiền âm (`-50000`), nhập chữ (`"abc"`), nhập rỗng.
*   **Danh mục không hợp lệ:** Xóa danh mục vẫn đang có giao dịch con, thêm danh mục vào một path không tồn tại.
*   **Ngày tháng lỗi:** Nhập ngày `31/02`, ngày trong tương lai (`2099-01-01`), định dạng sai (`11-05-2024`).
*   **Tên trùng lặp:** Thêm danh mục có tên đã tồn tại trong cùng cấp cha (phải báo lỗi, không crash).

#### Nhóm B: ⚠️ Dữ liệu file bị hỏng (Thầy cô hay cố tình test cái này)
> Đây là kịch bản **thầy cô thường cố tình làm** khi chấm bài: xóa file, sửa nội dung file, hoặc nhập sai đường dẫn. Chương trình **không được crash (văng)** trong bất kỳ trường hợp nào.

| Kịch bản | Hành vi mong đợi |
|---|---|
| **File không tồn tại** (`data.csv` bị xóa) | Thông báo "Không tìm thấy file, bắt đầu với dữ liệu trống" và chạy bình thường |
| **File rỗng** (0 byte) | Load thành công với 0 giao dịch, không crash |
| **Dòng thiếu dấu phân cách** `/` trong path | Bỏ qua dòng đó, ghi log cảnh báo, đọc tiếp các dòng sau |
| **Số tiền không phải số** (`abc` thay vì `150000`) | Bỏ qua dòng đó, không crash |
| **Path không bắt đầu bằng THU hoặc CHI** | Bỏ qua dòng đó, in cảnh báo |
| **File bị sửa tay thêm ký tự lạ** | Xử lý gracefully, không crash |
| **Cây rỗng khi gọi generateReport()** | Hiển thị "Chưa có dữ liệu", không crash |
| **Cây rỗng khi gọi search()** | Trả về danh sách rỗng, không crash |

#### Nhóm C: Điều kiện biên cấu trúc cây
*   Xóa nút gốc `ROOT` (phải từ chối, không được xóa).
*   Xóa nút `THU` hoặc `CHI` (nếu có quy định — phải từ chối hoặc hỏi xác nhận).
*   Thêm nút con trùng tên trong cùng một cha (phải báo lỗi).
*   Tính tổng của nút lá không có giao dịch (kết quả phải là 0.0, không crash).
*   Xóa `CASCADE` một nút có 1000 giao dịch con (không crash, giải phóng các đối tượng con cháu đúng cách).
*   Xóa `REPARENT` — kiểm tra đường dẫn trong `nodeIndex` đã cập nhật đúng chưa.

---

## 4. Lập bảng báo cáo lỗi (Bug Report)
Mỗi khi phát hiện lỗi, bạn cần lập bảng gửi cho SV2 và SV3 sửa:

| Mã lỗi | Mô tả lỗi | Thành phần lỗi | Mức độ | Trạng thái |
|---|---|---|---|---|
| BUG-001 | Nhập tiền âm chương trình vẫn tính | Core (SV2) | Nghiêm trọng | Đã sửa |
| BUG-002 | loadData crash khi file rỗng | File I/O (SV3) | Nghiêm trọng | Đã sửa |
| BUG-003 | Xóa CASCADE không cập nhật nodeIndex | Core (SV2) | Nghiêm trọng | Đã sửa |

---

## 5. Mẫu bảng kết quả Performance (Tham khảo)

| Số lượng bản ghi | Thời gian Load (ms) | Thời gian Tính tổng DFS (ms) | Thời gian Tìm theo path (ms) | Thời gian Tìm theo từ khóa (ms) |
| :--- | :--- | :--- | :--- | :--- |
| 100 | 5 | 1 | <1 | 0.5 |
| 1.000 | 45 | 8 | <1 | 2 |
| 10.000 | 420 | 75 | <1 | 15 |

> **Lưu ý khi nhận xét bảng:**
> - Tìm theo path phải gần như hằng số (O(1) nhờ `nodeIndex`) — nếu không phải O(1), cần báo SV2 kiểm tra lại.
> - Thời gian tìm kiếm từ khóa tăng tuyến tính theo số bản ghi (O(M)).
> - Thời gian tính tổng DFS tăng theo số nút N + số giao dịch M (O(N+M)).
> - So sánh với lý thuyết Big O từ SV5 để kiểm chứng.
