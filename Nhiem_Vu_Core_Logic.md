# CHI TIẾT NHIỆM VỤ: LẬP TRÌNH VIÊN 1 - CORE LOGIC (SV2)
> **Lưu ý:** File này là tài liệu nội bộ cho SV2. Nội dung được tổng hợp vào `Bao_Cao_Do_An.md` để nộp báo cáo chính thức.
> **Ngôn ngữ:** **Java** (toàn bộ nhóm thống nhất dùng Java).

---

## 0. Cấu trúc thư mục & File code

Toàn bộ mã nguồn được đặt trong `D:\Project\MI_3060\src\java\`. Dưới đây là sơ đồ cấu trúc **bắt buộc** — mọi thành viên phải đặt file đúng vị trí này để `javac` biên dịch thành công.

```
D:\Project\MI_3060\
│
├── src\
│   └── java\                        ← Thư mục gốc mã nguồn Java
│       │
│       ├── models\                  ← Package "models" — các lớp dữ liệu (SV2)
│       │   ├── CategoryNode.java    ← Lớp nút cây danh mục
│       │   └── Transaction.java     ← Lớp giao dịch tài chính
│       │
│       ├── core\                    ← Package "core" — logic nghiệp vụ (SV2)
│       │   ├── FinanceTree.java     ← Cấu trúc cây N-ary + HashMap nodeIndex + mọi thuật toán
│       │   └── FinanceManager.java  ← Facade: SV3 chỉ gọi qua lớp này
│       │
│       └── Main.java                ← Entry point — chạy demo / tích hợp (SV2 viết demo)
│
├── bin\                             ← Thư mục output sau khi biên dịch (javac -d bin ...)
│   ├── models\
│   │   ├── CategoryNode.class
│   │   └── Transaction.class
│   ├── core\
│   │   ├── FinanceTree.class
│   │   └── FinanceManager.class
│   └── Main.class
│
└── data.csv                         ← File dữ liệu lưu/tải (SV3 quản lý)
```

### Vai trò từng file — SV2 phụ trách:

| File | Package | Vai trò | Người viết |
|---|---|---|---|
| `CategoryNode.java` | `models` | Định nghĩa nút cây: name, type, parent, children, transactions | **SV2** |
| `Transaction.java` | `models` | Định nghĩa giao dịch: amount, date, note, categoryPath | **SV2** |
| `FinanceTree.java` | `core` | Toàn bộ logic cây: insert, delete, rename, DFS, BFS, nodeIndex | **SV2** |
| `FinanceManager.java` | `core` | Facade điều phối — giao tiếp với SV3; có `generateTestData()` | **SV2** |
| `Main.java` | _(default)_ | Demo tích hợp, kiểm tra kịch bản | **SV2** (SV3 thêm vào khi ghép UI) |

### Lệnh biên dịch và chạy:

```bash
# Biên dịch toàn bộ (chạy từ thư mục D:\Project\MI_3060):
javac -encoding UTF-8 -d bin src\java\models\*.java src\java\core\*.java src\java\Main.java

# Chạy chương trình:
java -cp bin Main
```

### Quy tắc khai báo package trong từng file:

```java
// CategoryNode.java và Transaction.java — đầu file phải có:
package models;

// FinanceTree.java và FinanceManager.java — đầu file phải có:
package core;

// Main.java — KHÔNG khai báo package (nằm ở default package):
import models.CategoryNode;
import models.Transaction;
import core.FinanceTree;
import core.FinanceManager;
```

> **Lưu ý phân công:** SV3 sẽ thêm file `ui/ConsoleMenu.java` hoặc `ui/MainGUI.java` vào thư mục `src\java\ui\` — **không được** đặt code UI vào package `core` hay `models`.

---

## 1. Cài đặt Cấu trúc dữ liệu Cây (Tree)
Đây là phần quan trọng nhất của đồ án Cấu trúc dữ liệu & Giải thuật. Bạn cần tự cài đặt cấu trúc cây thay vì dùng thư viện có sẵn.

### Các hạng mục cần code:

**Xây dựng lớp `CategoryNode`** — file `src/java/models/CategoryNode.java`:
*   Quản lý danh sách các nút con bằng `ArrayList<CategoryNode>`.
*   Lưu trữ tham chiếu về nút cha: `CategoryNode parent`.
*   Lưu trữ danh sách giao dịch: `ArrayList<Transaction> transactions`.
*   **Trường `type`:** Chỉ nhận `"THU"` hoặc `"CHI"` (theo quy định của SV1). **Không dùng `"Income"`, `"Expense"` hay các giá trị khác.**

```java
// Ví dụ khai báo lớp CategoryNode (Java):
public class CategoryNode {
    private String name;
    private String type;           // "THU", "CHI", hoặc "ROOT"
    private CategoryNode parent;
    private ArrayList<CategoryNode> children;
    private ArrayList<Transaction> transactions;

    public CategoryNode(String name, String type, CategoryNode parent) { ... }
    public boolean addChild(CategoryNode child) { ... }
    public CategoryNode removeChild(String name) { ... }
    public CategoryNode findChildByName(String name) { ... }
    public double getDirectTotal() { ... }
    public String getPath() { ... }   // Đệ quy lên cha để tạo đường dẫn
}
```

**Xây dựng lớp `Transaction`** — file `src/java/models/Transaction.java`:

```java
public class Transaction {
    private int transactionId;
    private double amount;
    private LocalDate date;        // dùng java.time.LocalDate
    private String note;
    private String categoryPath;
}
```

**Xây dựng lớp `FinanceTree`** — file `src/java/core/FinanceTree.java`:
*   `insertNode(String parentPath, String newName, String type)` — thêm nút, cập nhật `nodeIndex`.
*   `deleteNode(String path, String mode)` — xóa nút với `mode = "CASCADE"` hoặc `"REPARENT"`. Khi xóa với chế độ `"REPARENT"`, bắt buộc phải đệ quy cập nhật lại toàn bộ khóa (Key) của các nút con cháu trong `nodeIndex` (vì đường dẫn cha của chúng đã thay đổi).
*   `renameNode(String path, String newName)` — đổi tên nút, bắt buộc phải đệ quy cập nhật lại toàn bộ khóa (Key) của tất cả các nút con cháu trong `nodeIndex` tương ứng với đường dẫn mới.
*   `traverseTree(CategoryNode node, int indent)` — in cây dạng thụt đầu dòng (dùng cho SV3 hiển thị).

**Xây dựng lớp `FinanceManager`** — file `src/java/core/FinanceManager.java`:
*   Lớp điều phối (facade) — SV3 chỉ gọi qua lớp này, không gọi thẳng vào `FinanceTree`.
*   Phải có hàm `generateTestData(int count, boolean uniformDistrib)` — xem chi tiết ở mục 6.

**⚠️ Nguyên tắc bắt buộc (SV1 quy định):**
> Module Core (SV2) **không được chứa bất kỳ lệnh `System.out.println()` hay `System.out.printf()` nào**. Chỉ xử lý dữ liệu và trả về kết quả (`boolean` / `Object` / `List`). Mọi hiển thị là trách nhiệm của SV3 (UI Module).

---

## 2. Viết các thuật toán xử lý chính

### Thuật toán Duyệt cây (DFS - Depth First Search):
*   **Mục tiêu:** Tính tổng số tiền của một nhánh (Ví dụ: tính tổng `"CHI/Nhu cầu thiết yếu/Ăn uống"` bao gồm cả `"Ăn sáng"`, `"Ăn trưa"`, `"Ăn tối"`).
*   **Yêu cầu:** Viết hàm **đệ quy** duyệt từ nút hiện tại xuống tất cả các nút lá, cộng dồn số tiền.
*   **Pseudocode:**
    ```
    Function calculateTotal(Node):
        sum = sum(Node.transactions.amounts)   // Tổng giao dịch TRỰC TIẾP tại nút
        For each child in Node.children:
            sum += calculateTotal(child)       // ĐỆ QUY xuống các nút con
        Return sum
    ```
*   **Cài đặt Java:**
    ```java
    // Trong FinanceTree.java
    public double calculateTotalDfs(CategoryNode node) {
        double total = node.getDirectTotal();            // Tổng trực tiếp tại nút
        for (CategoryNode child : node.getChildren()) {
            total += calculateTotalDfs(child);           // Đệ quy
        }
        return total;
    }
    ```

### Thuật toán Tìm kiếm (Search) — Kết hợp Cây + Bảng Băm:
*   **Mục tiêu:** Tìm nhanh một danh mục dựa trên đường dẫn (Path) hoặc tên.
*   **Thiết kế kép (quan trọng — cần trình bày rõ trong báo cáo):**

    | Trường hợp | Cấu trúc dùng (Java) | Độ phức tạp |
    |---|---|---|
    | Tìm danh mục theo **đường dẫn đầy đủ** | `HashMap<String, CategoryNode> nodeIndex` | **O(1)** trung bình |
    | Tìm danh mục theo **tên** (không rõ path) | BFS dùng `Queue<CategoryNode>` | **O(N)** |
    | Tìm giao dịch theo **từ khóa / ngày** | DFS đệ quy toàn cây + lọc | **O(N + M)** |

    > **Giải thích cho báo cáo:** Cây N-ary được dùng để **phân cấp và tính tổng nhánh** (thế mạnh độc nhất của cây). Bảng băm `nodeIndex` (`HashMap`) được dùng để **tối ưu tốc độ tra cứu danh mục khi nhập giao dịch mới** — thay vì phải duyệt O(N) qua cây mỗi lần, ta chỉ cần 1 thao tác hash O(1). Đây là sự **kết hợp hai cấu trúc dữ liệu** bổ sung cho nhau.

*   **Cài đặt BFS tìm theo tên (Java):**
    ```java
    public List<CategoryNode> searchByName(String name) {
        List<CategoryNode> results = new ArrayList<>();
        Queue<CategoryNode> queue = new LinkedList<>();
        queue.add(this.root);
        while (!queue.isEmpty()) {
            CategoryNode current = queue.poll();
            if (current.getName().equalsIgnoreCase(name)) results.add(current);
            queue.addAll(current.getChildren());
        }
        return results;
    }
    ```

### Thuật toán Phân loại (Classification):
*   **Mục tiêu:** Khi có giao dịch mới, xác định nút đích và đẩy vào danh sách giao dịch của nút đó.
*   **Cơ chế Java:** `nodeIndex.get(categoryPath)` → O(1) → `node.addTransaction(txn)`.

---

## 3. ⚠️ Cơ chế Xóa Nút (deleteNode) — Trọng tâm thầy cô hay hỏi

Khi xóa `"CHI/Nhu cầu thiết yếu/Ăn uống"` (đang có con `"Ăn sáng"`, `"Ăn tối"`), hệ thống hỗ trợ **2 chế độ xóa**:

### Chế độ 1: CASCADE (Xóa Tầng)
> Xóa toàn bộ nút và **kéo theo tất cả con cháu + giao dịch** bên trong.

```
deleteNode("CHI/Nhu cầu thiết yếu/Ăn uống", mode="CASCADE"):
    1. Đệ quy xóa tất cả nút con của "Ăn uống":
       - Xóa "Ăn sáng" (và giao dịch của nó) khỏi nodeIndex
       - Xóa "Ăn trưa" (và giao dịch của nó) khỏi nodeIndex
       - Xóa "Ăn tối"  (và giao dịch của nó) khỏi nodeIndex
    2. Xóa giao dịch trực tiếp của "Ăn uống"
    3. Xóa "Ăn uống" khỏi danh sách children của "Nhu cầu thiết yếu"
    4. Xóa key "CHI/Nhu cầu thiết yếu/Ăn uống" khỏi nodeIndex

Kết quả: Mọi dữ liệu trong nhánh bị mất hoàn toàn.
```

*   **Cài đặt Java — hàm hỗ trợ đệ quy:**
    ```java
    private void unregisterSubtree(CategoryNode node) {
        nodeIndex.remove(node.getPath());
        for (CategoryNode child : node.getChildren()) {
            unregisterSubtree(child);   // Đệ quy xóa key của tất cả con cháu
        }
    }
    ```

*   **Khi nào dùng?** Khi người dùng xác nhận "Xóa toàn bộ danh mục này".

### Chế độ 2: REPARENT (Chuyển về cha)
> Xóa nút hiện tại nhưng **chuyển giao dịch và con cái lên cho nút cha**.

```
deleteNode("CHI/Nhu cầu thiết yếu/Ăn uống", mode="REPARENT"):
    1. Lấy danh sách con: childrenToMove = new ArrayList<>(node.getChildren())
    2. Với mỗi child trong childrenToMove:
         - Hủy đăng ký cây con khỏi nodeIndex (unregisterSubtree)
         - Gán child.setParent(parent)  // "Nhu cầu thiết yếu"
         - Thêm child vào parent.getChildren()
         - Đăng ký lại cây con vào nodeIndex (reRegisterSubtree) với đường dẫn mới
    3. Chuyển giao dịch trực tiếp của "Ăn uống" sang parent
    4. Xóa "Ăn uống" khỏi children của parent
    5. Xóa key "CHI/Nhu cầu thiết yếu/Ăn uống" khỏi nodeIndex

Kết quả: Dữ liệu được bảo toàn, cấu trúc cây được làm phẳng 1 cấp.
```

*   **Cài đặt Java — hàm đăng ký lại sau khi đổi cha:**
    ```java
    private void reRegisterSubtree(CategoryNode node) {
        nodeIndex.put(node.getPath(), node);   // getPath() tính lại từ cha mới
        for (CategoryNode child : node.getChildren()) {
            reRegisterSubtree(child);           // Đệ quy cập nhật toàn bộ cây con
        }
    }
    ```

*   **Khi nào dùng?** Khi người dùng muốn giữ lại dữ liệu giao dịch.

> **Lưu ý cho báo cáo:** Phải hỏi người dùng xác nhận trước khi thực hiện xóa. Chế độ mặc định đề xuất là **CASCADE**.

---

## 4. Cấu trúc cây Danh mục mặc định — BẮT BUỘC đúng theo SV1

> **Quan trọng:** Cấu trúc cây khởi tạo trong hàm `initializeDefaultStructure()` phải đúng **chính xác** theo thiết kế sau. SV3 và SV4 phụ thuộc vào tên đường dẫn này.

```
ROOT
├── THU (Thu nhập)
│   ├── Lương
│   │   ├── Lương chính
│   │   └── Làm thêm
│   ├── Kinh doanh
│   │   ├── Bán hàng online
│   │   └── Freelance
│   └── Khác
│       ├── Quà tặng
│       └── Lãi tiết kiệm
└── CHI (Chi tiêu)
    ├── Nhu cầu thiết yếu
    │   ├── Ăn uống
    │   │   ├── Ăn sáng
    │   │   ├── Ăn trưa
    │   │   └── Ăn tối
    │   ├── Nhà ở
    │   │   ├── Tiền thuê
    │   │   └── Điện nước
    │   └── Di chuyển
    │       ├── Xăng xe
    │       └── Giao thông công cộng
    ├── Giáo dục & Phát triển
    │   ├── Sách vở
    │   └── Khóa học
    └── Hưởng thụ
        ├── Du lịch
        └── Giải trí
```

**Ví dụ đường dẫn hợp lệ:**
- `"THU/Lương/Lương chính"` ← đúng
- `"CHI/Nhu cầu thiết yếu/Ăn uống/Ăn sáng"` ← đúng
- `"Root/Thu nhập/Lương"` ← **sai**, không được dùng

**Cài đặt Java mẫu:**
```java
// Trong FinanceTree.java — hàm initializeDefaultStructure()
private void initializeDefaultStructure() {
    this.root = new CategoryNode("ROOT", "ROOT", null);
    registerNode(root);

    CategoryNode thuNode = new CategoryNode("THU", "THU", root);
    root.getChildren().add(thuNode);
    registerNode(thuNode);

    CategoryNode luongNode = new CategoryNode("Lương", "THU", thuNode);
    thuNode.getChildren().add(luongNode);
    registerNode(luongNode);

    // ... tiếp tục theo sơ đồ cây trên
}
```

---

## 5. Phân tích độ phức tạp (Big O)
Bạn cần phối hợp với SV5 để cung cấp các thông số:

| Thao tác | Thời gian | Không gian | Ghi chú |
|---|---|---|---|
| Thêm danh mục (`insertNode`) | O(1) | O(1) | Biết cha qua `nodeIndex` |
| Xóa danh mục CASCADE | O(N_sub + M_sub) | O(D) call stack | N_sub nút con, M_sub giao dịch |
| Tìm theo đường dẫn (`HashMap.get`) | **O(1)** | — | Nhờ `nodeIndex` |
| Tìm theo tên (BFS với `LinkedList`) | O(N) | O(N) | N = tổng nút |
| Tính tổng nhánh (DFS đệ quy) | O(N_sub + M_sub) | O(D) call stack | |
| Duyệt toàn cây (`traverseTree`) | O(N) | O(D) call stack | |
| Tìm giao dịch theo từ khóa | O(N + M) | O(D) | M = tổng giao dịch |

> **Ghi chú Java:** Java không cần `delete` thủ công — JVM Garbage Collector tự giải phóng các đối tượng không còn được tham chiếu. Chỉ cần đảm bảo xóa khỏi `nodeIndex` (HashMap) và khỏi `children` (ArrayList) để tránh tham chiếu "mồ côi".

---

## 6. Hàm `generateTestData()` — Phục vụ SV4 kiểm thử

SV1 yêu cầu **bắt buộc** phải có hàm này trong `FinanceManager`:

```java
// Trong FinanceManager.java
/**
 * Sinh dữ liệu thử nghiệm ngẫu nhiên.
 * @param count          Số lượng giao dịch cần sinh (thường là 10.000)
 * @param uniformDistrib true = phân bổ đều theo tất cả nút lá
 *                       false = phân bổ ngẫu nhiên không đều
 */
public void generateTestData(int count, boolean uniformDistrib) {
    List<String> leafPaths = tree.getAllLeafPaths();  // Lấy tất cả nút lá
    Random random = new Random();
    LocalDate startDate = LocalDate.of(2024, 1, 1);

    for (int i = 0; i < count; i++) {
        // 1. Chọn path đích
        String path;
        if (uniformDistrib) {
            path = leafPaths.get(i % leafPaths.size());       // Phân bổ đều tuần tự
        } else {
            path = leafPaths.get(random.nextInt(leafPaths.size())); // Ngẫu nhiên
        }

        // 2. Sinh dữ liệu ngẫu nhiên
        double amount = 10_000 + random.nextDouble() * (10_000_000 - 10_000);
        LocalDate date = startDate.plusDays(random.nextInt(365));
        String note = "Test GD #" + i;

        // 3. Thêm giao dịch (KHÔNG in gì ra màn hình — để SV3 xử lý)
        addTransaction(amount, date, note, path);
    }
    // Không có System.out.println ở đây!
}
```

---

## 7. Giải đáp về việc dùng MySQL hay Spring Boot

**Lời khuyên:** **KHÔNG NÊN** sử dụng MySQL / Spring Boot / Hibernate vì:

1.  **Trọng tâm môn học:** Thầy cô muốn thấy bạn tự tay code cấu trúc **Cây (Tree)** bằng Java trong bộ nhớ RAM với `ArrayList` và `HashMap`. Nếu dùng database, bạn không thể hiện được kỹ năng quản lý cây.
2.  **Yêu cầu đề bài:** Đề bài yêu cầu có hàm `loadData()` và `saveData()` làm việc với **file CSV**.
3.  **Độ phức tạp không cần thiết:** Cài đặt MySQL / Spring tốn nhiều thời gian cấu hình, trong khi điểm số chủ yếu nằm ở phần **giải thuật Cây**.

**Thay vào đó:** Hãy tập trung vào việc đọc/ghi file CSV bằng `BufferedReader`/`BufferedWriter` và thể hiện kỹ năng thao tác trên tham chiếu (reference) của các đối tượng trong cây.

---

## 8. Danh mục kiểm tra của SV2 (Checklist)

Trước khi bàn giao module Core Logic, SV2 cần tự kiểm tra các tiêu chí sau:
- [x] **Cài đặt Cây N-ary và HashMap:** Cây danh mục phân cấp hoạt động tốt, kết hợp với bảng băm `nodeIndex` (`nodeMap` trong `FinanceTree`) để tra cứu danh mục nhanh $O(1)$.
- [x] **Đồng bộ hóa nodeIndex khi thay đổi cấu trúc cây:** 
    * Khi đổi tên nút (`renameNode`): Bắt buộc phải đệ quy cập nhật lại toàn bộ khóa (Key) trong `nodeIndex` của chính nút đó và tất cả các nút con cháu của nó tương ứng với đường dẫn mới.
    * Khi xóa danh mục ở chế độ `REPARENT`: Bắt buộc phải dịch chuyển các con của nút bị xóa lên nút cha trực tiếp, đồng thời đệ quy cập nhật lại toàn bộ khóa (Key) của tất cả các nút con cháu dịch chuyển trong `nodeIndex` (vì đường dẫn cha của chúng thay đổi).
    * Khi xóa danh mục ở chế độ `CASCADE`: Đệ quy xóa tất cả các nút con cháu khỏi `nodeIndex` để đảm bảo bảng băm không còn lưu tham chiếu mồ côi.
- [x] **Separation of Concerns (Tuân thủ SV1):** Không sử dụng `System.out` để in ấn trong bất kỳ lớp nào thuộc package `core` hoặc `models`. Mọi kết quả duyệt cây và báo cáo được trả về dưới dạng chuỗi hoặc thu thập lỗi qua danh sách cảnh báo.
- [x] **Kiểm tra đầu vào (Edge Cases):**
    * Từ chối giao dịch có số tiền $\le 0$.
    * Không cho phép xóa nút gốc (`ROOT`, `THU`, `CHI`).
    * Không cho phép tạo các danh mục trùng tên trong cùng một cấp cha.
