# CHI TIẾT NHIỆM VỤ: TÀI LIỆU & BÁO CÁO (SV5)
> **Lưu ý:** File này là tài liệu nội bộ cho SV5. Nội dung được tổng hợp vào `Bao_Cao_Do_An.md` để nộp báo cáo chính thức.

---

## 1. Viết báo cáo chi tiết (.pdf)
Bạn là người tổng hợp toàn bộ chất xám của nhóm thành một văn bản hoàn chỉnh. Bản báo cáo phải chuyên nghiệp và tuân thủ đúng yêu cầu 40 trang của đề bài.

### Cấu trúc báo cáo cần đảm bảo:

*   **Mở đầu:** Giới thiệu đề tài, lý do chọn cấu trúc Cây cho quản lý tài chính.
*   **Phân tích bài toán (lấy từ SV1):**
    *   Bảng đặc tả đầu vào (Input): Tên danh mục, Loại (`THU`/`CHI`), Danh mục cha, Số tiền, Ngày, Ghi chú.
    *   Bảng đặc tả đầu ra (Output): Cây phân cấp, Tổng theo nhánh, Số dư, Kết quả tìm kiếm.
    *   Bảng so sánh cấu trúc dữ liệu (Mảng / Danh sách / **Cây N-ary** ✅) — lấy từ SV1.
*   **Thiết kế hệ thống (lấy từ SV1):**
    *   Chụp ảnh và giải thích Sơ đồ Use Case (6 UC: Quản lý danh mục, Ghi giao dịch, Xem báo cáo, Tìm kiếm, Lưu file, Tải file).
    *   Chụp ảnh và giải thích Sơ đồ lớp (Class Diagram) với 3 lớp: `CategoryNode`, `Transaction`, `FinanceManager`.
    *   Trình bày Kiến trúc Module: UI (SV3) → Core (SV2) → File I/O (SV3).
*   **Triển khai (lấy từ SV2, SV3):**
    *   Trình bày Pseudocode cho thuật toán DFS tính tổng nhánh.
    *   Trình bày 2 chế độ xóa nút: **CASCADE** và **REPARENT** (có ví dụ minh họa từng bước).
    *   Trình bày hợp đồng định dạng CSV (format `YYYY-MM-DD,amount,THU/.../...,note`).
    *   Chèn các đoạn code quan trọng (Snippet) và giải thích logic.
*   **Kết quả kiểm thử (lấy từ SV4):**
    *   Chèn bảng Performance Test (100 / 1.000 / 10.000 bản ghi).
    *   Chụp ảnh giao diện phần mềm đang chạy.
    *   Tóm tắt Bug Report: số lỗi tìm thấy, số lỗi đã sửa.

---

## 2. Phân tích độ phức tạp Big O
Đây là phần "ăn điểm" về mặt kỹ thuật của môn Cấu trúc dữ liệu & Giải thuật.

### Các nội dung cần phân tích:

*   **Độ phức tạp thời gian (Time Complexity):**

    | Thao tác | Độ phức tạp | Diễn giải |
    |---|---|---|
    | Thêm nút vào cây | O(1) | Đã biết tham chiếu cha + cập nhật `nodeIndex` |
    | Xóa nút (Cascade) | O(N_sub + M_sub) | Phải dùng đệ quy giải phóng toàn bộ cây con |
    | Tìm danh mục theo **đường dẫn** | **O(1)** | Nhờ bảng băm `nodeIndex` |
    | Tìm danh mục theo **tên** | O(N) | Phải BFS/DFS toàn cây |
    | Tính tổng nhánh (DFS) | O(N_sub + M_sub) | N_sub nút, M_sub giao dịch trong nhánh |
    | Duyệt toàn cây (`traverseTree`) | O(N) | N = tổng số nút |
    | Tìm kiếm giao dịch theo từ khóa | O(N + M) | Duyệt cây + lọc giao dịch |

*   **Phân tích thiết kế kết hợp 2 cấu trúc (quan trọng — ăn điểm báo cáo):**
    > Hệ thống sử dụng **đồng thời** cả Cây N-ary và Bảng băm (`HashMap`). Mỗi cấu trúc đảm nhiệm một vai trò riêng:
    > *   **Cây N-ary:** Phản ánh quan hệ phân cấp cha-con, hỗ trợ tính tổng nhánh đệ quy (DFS). Thế mạnh **độc nhất** của cây.
    > *   **Bảng băm `nodeIndex`:** Tăng tốc tra cứu từ O(N) → **O(1)** khi nhập giao dịch mới. Ánh xạ đường dẫn đầy đủ (VD: `"CHI/Nhu cầu thiết yếu/Ăn uống"`) → đối tượng nút.
    > *   Hai cấu trúc **bổ sung** cho nhau, không thay thế nhau.

*   **Đồng bộ hóa `nodeIndex`:**
    > Mỗi khi thêm/xóa/đổi tên nút, `nodeIndex` **phải** được cập nhật tương ứng. SV5 cần giải thích rõ điều này trong báo cáo và liên kết với kết quả kiểm thử của SV4 (kiểm tra đường dẫn sau khi xóa REPARENT).

*   **Độ phức tạp không gian (Space Complexity):**
    *   Bộ nhớ cây: O(N + M) cho N nút và M giao dịch.
    *   Bộ nhớ `nodeIndex`: O(N) cho N cặp (key, pointer).
    *   Stack đệ quy khi DFS: O(D) với D là chiều cao cây.
    *   **Tổng cộng:** O(N + M).

*   **Mối liên hệ lý thuyết ↔ thực tế:** So sánh giữa lý thuyết Big O và kết quả đo thực tế từ SV4 (Ví dụ: Khi dữ liệu tăng gấp 10 thì thời gian tăng bao nhiêu lần? Có khớp với Big O không?).

---

## 3. Làm Slide thuyết trình
Slide cần súc tích, tránh đưa quá nhiều chữ. Tập trung vào hình ảnh sơ đồ và demo.

### Dàn ý Slide mẫu (khoảng 10-15 slides):
1.  **Tiêu đề & Thành viên.**
2.  **Vấn đề:** Khó khăn khi quản lý tài chính không phân cấp.
3.  **Giải pháp:** Ứng dụng Cấu trúc Cây N-ary.
4.  **Kiến trúc hệ thống:** Sơ đồ lớp (Class Diagram từ SV1).
5.  **Cấu trúc cây mặc định:** Show sơ đồ cây THU/CHI phân cấp 3-4 cấp.
6.  **Thuật toán then chốt 1:** Giải thích nhanh DFS tính tổng nhánh.
7.  **Thuật toán then chốt 2:** 2 chế độ xóa nút — CASCADE vs REPARENT.
8.  **Thiết kế kết hợp:** Cây N-ary + Bảng băm `nodeIndex` — tại sao O(1).
9.  **Kết quả kiểm thử:** Show bảng Performance từ SV4.
10. **Demo:** Video hoặc demo trực tiếp.
11. **Bài học kinh nghiệm & Kết luận.**

---

## 4. Quay Video thuyết trình & Demo
Video tối đa 20 phút và dung lượng dưới 100MB.

### Kế hoạch quay:
*   **Phần 1: Giới thiệu (2 phút):** Tất cả thành viên xuất hiện (qua camera hoặc giới thiệu tên).
*   **Phần 2: Thuyết trình (10 phút):** Chia nhau nói về các phần mình phụ trách (Thiết kế, Code Core, Code UI, Test, Báo cáo).
*   **Phần 3: Demo phần mềm (8 phút):**
    *   Thực hiện thao tác thêm danh mục, nhập giao dịch.
    *   Show kết quả báo cáo tính tổng nhánh (DFS).
    *   **Demo xóa CASCADE và REPARENT** — show cấu trúc cây trước và sau.
    *   Thực hiện load file 10.000 bản ghi để chứng minh tốc độ.
    *   **Demo cố tình làm file lỗi** (xóa file, sửa nội dung sai) và chứng minh không crash.
*   **Mẹo giảm dung lượng:** Sử dụng các phần mềm nén video hoặc giảm độ phân giải xuống 720p để đảm bảo dưới 100MB.

---

## 5. Danh mục kiểm tra cuối cùng (Checklist)

### Nội dung báo cáo:
- [ ] Báo cáo đã xuất sang định dạng PDF chưa? (SV5 thực hiện thủ công từ Bao_Cao_Do_An.md)
- [ ] Các sơ đồ Mermaid đã được chụp ảnh sắc nét chèn vào chưa? (SV5 chụp từ Bao_Cao_Do_An.md)
- [x] Báo cáo đã trình bày rõ **cấu trúc cây mặc định THU/CHI 3-4 cấp** chưa? (Đã hoàn thiện)
- [x] Báo cáo đã trình bày rõ **2 chế độ xóa nút** (Cascade Delete và Re-parent) chưa? (Đã hoàn thiện)
- [x] Báo cáo đã giải thích sự kết hợp Cây + Bảng băm (`nodeIndex`) chưa? (Đã hoàn thiện)
- [x] Bảng Big O đã phân biệt rõ tìm kiếm O(1) (theo path) và O(N) (theo tên) chưa? (Đã hoàn thiện)
- [x] Báo cáo đã trình bày hợp đồng định dạng CSV (`YYYY-MM-DD,amount,THU/...,note`) chưa? (Đã hoàn thiện)
- [ ] Nếu viết bằng **C++**: đã đề cập quản lý bộ nhớ (`delete`, Destructor) chưa? (N/A - Dự án viết bằng Java)
- [x] Báo cáo đã ghi rõ nguyên tắc Separation of Concern (Core không print, UI quyết định hiển thị) chưa? (Đã hoàn thiện)

### Video và Demo:
- [ ] Video có nghe rõ giọng của tất cả thành viên không? (Cả nhóm thực hiện)
- [x] Demo đã chạy kịch bản **file bị xóa / sửa tay lỗi** mà không crash chưa? (Đã tích hợp trong PerformanceAndEdgeTest & loadData)
- [x] Demo đã chạy load **10.000 bản ghi** và show bảng Performance Test chưa? (Đã đo đạc thực tế trong báo cáo)
- [x] Demo đã show **cả 2 chế độ xóa** CASCADE và REPARENT chưa? (Đã kiểm tra tự động qua bộ Test)

### Nộp bài:
- [ ] File mã nguồn đã được nén thành `.zip` hoặc `.rar` chưa? (Trưởng nhóm thực hiện)
- [ ] File báo cáo PDF đã đặt tên đúng theo yêu cầu giảng viên chưa? (SV5 thực hiện)
- [ ] File video đã kiểm tra dung lượng < 100MB chưa? (Cả nhóm thực hiện)
