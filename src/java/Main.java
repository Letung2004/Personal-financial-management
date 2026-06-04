import core.FinanceManager;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import models.CategoryNode;
import models.Transaction;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);

    private static void separator(String title) {
        System.out.println("\n============================================================");
        if (title != null && !title.isEmpty()) {
            System.out.println("  🔷 " + title);
            System.out.println("============================================================");
        }
    }

    private static void waitEnter() {
        System.out.print("\n  [Nhấn ENTER để tiếp tục...]");
        scanner.nextLine();
    }

    public static void main(String[] args) {

/*
        System.out.println("  [HỆ THỐNG] Đang mở giao diện đồ họa Swing GUI...");
        javax.swing.SwingUtilities.invokeLater(() -> {
            ui.MainGUI gui = new ui.MainGUI();
            gui.setVisible(true);
        });

*/

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║        HỆ THỐNG QUẢN LÝ TÀI CHÍNH CÁ NHÂN (JAVA)         ║");
        System.out.println("║        Môn: Cấu trúc dữ liệu & Giải thuật                ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println("  1. Chạy Dữ Liệu Demo Tự Động (Kịch bản kiểm thử SV2)");
        System.out.println("  2. Mở Giao diện Tương tác Console (Interactive SV3 UI)");
        System.out.println("  3. Mở Giao diện Cửa sổ Đồ họa (Premium Swing GUI)");
        System.out.println("  4. Thoát");
        System.out.print("  👉 Nhập lựa chọn của bạn (1-4): ");
        String choice = "";
        if (scanner.hasNextLine()) {
            choice = scanner.nextLine().trim();
        }
        
        if (choice.equals("1")) {
            demo1TreeStructure();
            waitEnter();

            demo2InsertDelete();
            waitEnter();

            demo3AddTransactions();
            waitEnter();

            demo4DfsCalculation();
            waitEnter();

            demo5Search();
            waitEnter();

            demo6Report();
            waitEnter();

            demo7SaveLoad();

            System.out.println("\n\n  ✅ HOÀN THÀNH TOÀN BỘ DEMO CORE LOGIC (JAVA)!");
            System.out.println("  Tất cả các module đã được kiểm tra thành công.\n");
        } else if (choice.equals("2")) {
            new ui.ConsoleMenu().run();
        } else if (choice.equals("3")) {
            System.out.println("  [HỆ THỐNG] Đang mở giao diện đồ họa Swing GUI...");
            javax.swing.SwingUtilities.invokeLater(() -> {
                ui.MainGUI gui = new ui.MainGUI();
                gui.setVisible(true);
            });
        } else {
            System.out.println("  Tạm biệt!");
        }
    }

    private static void demo1TreeStructure() {
        separator("DEMO 1: CẤU TRÚC CÂY DANH MỤC MẶC ĐỊNH");
        FinanceManager manager = new FinanceManager();
        System.out.print(manager.listCategories());
        System.out.println("\n  Tổng số danh mục: " + manager.getTree().getNodeCount());
    }

    private static void demo2InsertDelete() {
        separator("DEMO 2: THÊM VÀ XÓA DANH MỤC (CASCADE VS REPARENT)");
        FinanceManager manager = new FinanceManager();

        // Thêm danh mục con
        System.out.println("\n  [+] Thêm danh mục mới:");
        manager.addCategory("CHI/Nhu cầu thiết yếu/Ăn uống", "Đặt đồ ăn online", "CHI");
        manager.addCategory("THU", "Freelance", "THU");
        manager.addCategory("CHI", "Làm đẹp", "CHI");
        manager.addCategory("CHI/Làm đẹp", "Cắt tóc", "CHI");
        manager.addCategory("CHI/Làm đẹp", "Mỹ phẩm", "CHI");

        // Thử thêm trùng tên
        System.out.println("\n  [!] Thử thêm tên đã tồn tại:");
        manager.addCategory("CHI/Nhu cầu thiết yếu/Ăn uống", "Ăn sáng", "CHI");

        System.out.println("\n  --- Cây danh mục ban đầu trước khi xóa ---");
        System.out.print(manager.listCategories());

        // Demo 2.1: Xóa CASCADE
        System.out.println("\n  [-] Thêm một số danh mục phụ để chuẩn bị xóa CASCADE:");
        manager.addCategory("CHI", "Mua sắm xa xỉ", "CHI");
        manager.addCategory("CHI/Mua sắm xa xỉ", "Đồng hồ hiệu", "CHI");
        manager.addCategory("CHI/Mua sắm xa xỉ", "Túi xách hiệu", "CHI");
        System.out.println("  --- Cây trước khi xóa CASCADE 'Mua sắm xa xỉ' ---");
        System.out.print(manager.listCategories());
        
        System.out.println("\n  [-] Thực hiện xóa CASCADE 'Mua sắm xa xỉ':");
        manager.removeCategory("CHI/Mua sắm xa xỉ", "CASCADE");
        System.out.print(manager.listCategories());

        // Demo 2.2: Xóa REPARENT
        System.out.println("\n  [-] Thực hiện xóa REPARENT 'Làm đẹp' (chuyển các con 'Cắt tóc', 'Mỹ phẩm' lên cho 'CHI'):");
        manager.removeCategory("CHI/Làm đẹp", "REPARENT");
        System.out.print(manager.listCategories());
    }

    private static void demo3AddTransactions() {
        separator("DEMO 3: THÊM GIAO DỊCH VÀ PHÂN LOẠI");
        FinanceManager manager = new FinanceManager();

        // Thêm giao dịch thu nhập
        System.out.println("\n  [+] Giao dịch Thu nhập:");
        manager.addTransaction(8500000, LocalDate.of(2026, 5, 1), "Lương tháng 5", "THU/Lương");
        manager.addTransaction(2000000, LocalDate.of(2026, 5, 10), "Thưởng dự án", "THU/Thưởng");
        manager.addTransaction(500000, LocalDate.of(2026, 5, 15), "Tiền lãi gửi tiết kiệm", "THU/Đầu tư");

        // Thêm giao dịch chi tiêu
        System.out.println("\n  [+] Giao dịch Chi tiêu:");
        manager.addTransaction(150000, LocalDate.of(2026, 5, 2), "Đi chợ sáng thứ 2", "CHI/Nhu cầu thiết yếu/Ăn uống/Ăn sáng");
        manager.addTransaction(85000, LocalDate.of(2026, 5, 3), "Phở bò buổi sáng", "CHI/Nhu cầu thiết yếu/Ăn uống/Ăn trưa");
        manager.addTransaction(45000, LocalDate.of(2026, 5, 4), "Cà phê sữa đá", "CHI/Nhu cầu thiết yếu/Ăn uống/Ăn tối");
        manager.addTransaction(250000, LocalDate.of(2026, 5, 5), "Xăng xe đầy bình", "CHI/Nhu cầu thiết yếu/Di chuyển/Xăng xe");
        manager.addTransaction(3200000, LocalDate.of(2026, 5, 1), "Tiền thuê nhà tháng 5", "CHI/Nhu cầu thiết yếu/Nhà ở/Tiền thuê");
        manager.addTransaction(320000, LocalDate.of(2026, 5, 5), "Tiền điện tháng 5", "CHI/Nhu cầu thiết yếu/Nhà ở/Điện nước");
        manager.addTransaction(180000, LocalDate.of(2026, 5, 6), "Ăn tối nhà hàng", "CHI/Nhu cầu thiết yếu/Ăn uống/Ăn tối");

        // Thử thêm số tiền âm (kiểm tra validation)
        System.out.println("\n  [!] Thử thêm giao dịch không hợp lệ (tiền âm):");
        manager.addTransaction(-500000, LocalDate.of(2026, 5, 7), "Giao dịch lỗi", "CHI/Nhu cầu thiết yếu/Ăn uống");

        // Hiển thị cây sau khi thêm giao dịch
        System.out.println("\n  --- Cây danh mục sau khi phân loại giao dịch ---");
        System.out.print(manager.listCategories());
    }

    private static void demo4DfsCalculation() {
        separator("DEMO 4: THUẬT TOÁN DFS - TÍNH TỔNG TIỀN THEO NHÁNH");
        FinanceManager manager = new FinanceManager();

        // Chuẩn bị dữ liệu
        manager.addTransaction(8500000, LocalDate.of(2026, 5, 1), "Lương tháng 5", "THU/Lương");
        manager.addTransaction(2000000, LocalDate.of(2026, 5, 10), "Thưởng dự án", "THU/Thưởng");
        manager.addTransaction(150000, LocalDate.of(2026, 5, 2), "Đi chợ sáng", "CHI/Nhu cầu thiết yếu/Ăn uống/Ăn sáng");
        manager.addTransaction(85000, LocalDate.of(2026, 5, 3), "Phở bò", "CHI/Nhu cầu thiết yếu/Ăn uống/Ăn trưa");
        manager.addTransaction(45000, LocalDate.of(2026, 5, 4), "Cà phê", "CHI/Nhu cầu thiết yếu/Ăn uống/Ăn tối");
        manager.addTransaction(250000, LocalDate.of(2026, 5, 5), "Xăng xe", "CHI/Nhu cầu thiết yếu/Di chuyển/Xăng xe");
        manager.addTransaction(3200000, LocalDate.of(2026, 5, 1), "Tiền nhà", "CHI/Nhu cầu thiết yếu/Nhà ở/Tiền thuê");

        System.out.println("\n  📊 Kết quả tính tổng bằng DFS:");
        System.out.println("  ├─ Tổng 'Ăn uống' (gồm Ăn sáng + Ăn trưa + Ăn tối):");
        double totalFood = manager.getTree().calculateTotalByPath("CHI/Nhu cầu thiết yếu/Ăn uống");
        System.out.printf("  │   = 150,000 + 85,000 + 45,000 = %,.0f VND  ✓\n", totalFood);

        System.out.println("\n  ├─ Tổng 'Di chuyển':");
        double totalTransport = manager.getTree().calculateTotalByPath("CHI/Nhu cầu thiết yếu/Di chuyển");
        System.out.printf("  │   = %,.0f VND\n", totalTransport);

        System.out.println("\n  ├─ Tổng 'Chi tiêu' (toàn bộ nhánh - DFS đệ quy):");
        double totalExpense = manager.getTree().calculateTotalByPath("CHI");
        System.out.printf("  │   = %,.0f VND\n", totalExpense);

        System.out.println("\n  ├─ Tổng 'Thu nhập' (toàn bộ nhánh):");
        double totalIncome = manager.getTree().calculateTotalByPath("THU");
        System.out.printf("  │   = %,.0f VND\n", totalIncome);

        System.out.printf("\n  └─ Số dư = %,.0f - %,.0f = %,.0f VND\n", totalIncome, totalExpense, totalIncome - totalExpense);
    }

    private static void demo5Search() {
        separator("DEMO 5: THUẬT TOÁN TÌM KIẾM");
        FinanceManager manager = new FinanceManager();

        // Chuẩn bị dữ liệu
        manager.addTransaction(150000, LocalDate.of(2026, 5, 2), "Ăn sáng phở bò", "CHI/Nhu cầu thiết yếu/Ăn uống/Ăn sáng");
        manager.addTransaction(180000, LocalDate.of(2026, 5, 7), "Ăn trưa cùng đồng nghiệp", "CHI/Nhu cầu thiết yếu/Ăn uống/Ăn trưa");
        manager.addTransaction(85000, LocalDate.of(2026, 5, 3), "Ăn tối bún chả", "CHI/Nhu cầu thiết yếu/Ăn uống/Ăn tối");
        manager.addTransaction(250000, LocalDate.of(2026, 5, 5), "Đổ xăng đầy bình", "CHI/Nhu cầu thiết yếu/Di chuyển/Xăng xe");
        manager.addTransaction(8500000, LocalDate.of(2026, 5, 1), "Lương tháng 5 công ty ABC", "THU/Lương");

        // Test 1: Tìm kiếm nút theo tên
        System.out.println("\n  [1] Tìm danh mục theo tên 'Ăn sáng':");
        List<CategoryNode> results = manager.getTree().searchByName("Ăn sáng");
        for (CategoryNode node : results) {
            System.out.println("      ✓ Tìm thấy: '" + node.getPath() + "'");
        }

        // Test 2: Tìm theo đường dẫn (O(1))
        System.out.println("\n  [2] Tìm theo đường dẫn (O(1) - Hash Map):");
        CategoryNode node = manager.getTree().getNodeByPath("CHI/Nhu cầu thiết yếu/Ăn uống/Ăn tối");
        System.out.println("      ✓ Tìm 'CHI/Nhu cầu thiết yếu/Ăn uống/Ăn tối': " + (node != null ? "Tìm thấy" : "Không tìm thấy"));

        // Test 3: Tìm giao dịch theo từ khóa
        System.out.println("\n  [3] Tìm giao dịch chứa từ khóa 'phở':");
        List<Transaction> txns = manager.searchTransactions("phở");
        for (Transaction txn : txns) {
            System.out.println("      ✓ " + txn.getDetails());
        }

        // Test 4: Lọc theo khoảng thời gian
        System.out.println("\n  [4] Lọc giao dịch từ 01/05 đến 04/05:");
        List<Transaction> filtered = manager.filterByDate(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 4));
        for (Transaction txn : filtered) {
            System.out.println("      ✓ " + txn.getDetails());
        }
    }

    private static void demo6Report() {
        separator("DEMO 6: BÁO CÁO TỔNG QUAN TÀI CHÍNH");
        FinanceManager manager = new FinanceManager();

        // Tạo bộ dữ liệu đầy đủ
        manager.addTransaction(8500000, LocalDate.of(2026, 5, 1), "Lương tháng 5", "THU/Lương");
        manager.addTransaction(2000000, LocalDate.of(2026, 5, 10), "Thưởng quý 1", "THU/Thưởng");
        manager.addTransaction(500000, LocalDate.of(2026, 5, 15), "Lãi tiết kiệm", "THU/Đầu tư");
        manager.addTransaction(150000, LocalDate.of(2026, 5, 2), "Đi chợ sáng", "CHI/Nhu cầu thiết yếu/Ăn uống/Ăn sáng");
        manager.addTransaction(85000, LocalDate.of(2026, 5, 3), "Phở buổi sáng", "CHI/Nhu cầu thiết yếu/Ăn uống/Ăn sáng");
        manager.addTransaction(45000, LocalDate.of(2026, 5, 4), "Cà phê sữa đá", "CHI/Nhu cầu thiết yếu/Ăn uống/Ăn tối");
        manager.addTransaction(180000, LocalDate.of(2026, 5, 7), "Đi chợ chiều", "CHI/Nhu cầu thiết yếu/Ăn uống/Ăn tối");
        manager.addTransaction(250000, LocalDate.of(2026, 5, 5), "Xăng xe đầy bình", "CHI/Nhu cầu thiết yếu/Di chuyển/Xăng xe");
        manager.addTransaction(3200000, LocalDate.of(2026, 5, 1), "Tiền thuê nhà T5", "CHI/Nhu cầu thiết yếu/Nhà ở/Tiền thuê");
        manager.addTransaction(320000, LocalDate.of(2026, 5, 5), "Hóa đơn điện T5", "CHI/Nhu cầu thiết yếu/Nhà ở/Điện nước");
        manager.addTransaction(180000, LocalDate.of(2026, 5, 6), "Hóa đơn internet", "CHI/Nhu cầu thiết yếu/Nhà ở/Tiền thuê");
        manager.addTransaction(50000, LocalDate.of(2026, 5, 8), "Thuốc cảm cúm", "CHI/Nhu cầu thiết yếu/Di chuyển/Xăng xe");

        System.out.print(manager.generateReport());

        // Chi tiết một danh mục
        System.out.print(manager.getCategorySummary("CHI/Nhu cầu thiết yếu/Ăn uống"));
    }

    private static void demo7SaveLoad() {
        separator("DEMO 7: LƯU VÀ TẢI DỮ LIỆU (saveData / loadData)");

        String DATA_FILE = "demo_data.csv";

        // --- Bước 1: Tạo và lưu ---
        System.out.println("\n  [Bước 1] Tạo dữ liệu và lưu xuống file:");
        FinanceManager manager1 = new FinanceManager(DATA_FILE);
        manager1.addTransaction(8500000, LocalDate.of(2026, 5, 1), "Lương tháng 5", "THU/Lương");
        manager1.addTransaction(150000, LocalDate.of(2026, 5, 2), "Đi chợ", "CHI/Nhu cầu thiết yếu/Ăn uống/Ăn sáng");
        manager1.addCategory("CHI/Nhu cầu thiết yếu/Ăn uống", "Bữa trưa công sở", "CHI");
        manager1.addTransaction(75000, LocalDate.of(2026, 5, 3), "Cơm trưa văn phòng", "CHI/Nhu cầu thiết yếu/Ăn uống/Bữa trưa công sở");

        double totalBefore = manager1.getTree().calculateTotalByPath("CHI");
        System.out.printf("\n  Tổng chi tiêu TRƯỚC khi lưu: %,.0f VND\n", totalBefore);
        manager1.saveData();

        // --- Bước 2: Tải lại ---
        System.out.println("\n  [Bước 2] Khởi tạo manager mới và tải lại từ file:");
        FinanceManager manager2 = new FinanceManager(DATA_FILE);
        manager2.loadData();

        double totalAfter = manager2.getTree().calculateTotalByPath("CHI");
        System.out.printf("\n  Tổng chi tiêu SAU khi tải lại: %,.0f VND\n", totalAfter);
        System.out.println("\n  " + (Math.abs(totalBefore - totalAfter) < 0.01 ? "✅ DỮ LIỆU KHỚP NHAU!" : "❌ DỮ LIỆU KHÔNG KHỚP!"));

        // Dọn dẹp file demo
        File file = new File(DATA_FILE);
        if (file.exists()) {
            file.delete();
        }
    }
}
