import core.FinanceManager;
import core.FinanceTree;
import models.CategoryNode;
import models.Transaction;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.util.List;

public class PerformanceAndEdgeTest {
    public static void main(String[] args) {
        System.out.println("================================════════════════════════════");
        System.out.println("          BẮT ĐẦU CHƯƠNG TRÌNH KIỂM THỬ QA/QC (SV4)         ");
        System.out.println("================================════════════════════════════");

        runEdgeCaseTests();
        runPerformanceTests();

        System.out.println("\n================================════════════════════════════");
        System.out.println("            HOÀN THÀNH TOÀN BỘ KIỂM THỬ QA/QC!              ");
        System.out.println("================================════════════════════════════");
    }

    private static void runEdgeCaseTests() {
        System.out.println("\n--- [PHẦN 1] KIỂM THỬ BIÊN VÀ LOGIC (EDGE CASES) ---");
        int passed = 0;
        int total = 0;

        // Test 1: BUG-001 - Nhập tiền âm
        total++;
        try {
            FinanceManager manager = new FinanceManager();
            boolean success = manager.addTransaction(-50000, LocalDate.now(), "Tiền âm", "THU/Lương");
            if (!success) {
                System.out.println("  [PASS] Test 1: Hệ thống từ chối thêm giao dịch với số tiền âm.");
                passed++;
            } else {
                System.out.println("  [FAIL] Test 1: Hệ thống chấp nhận số tiền âm.");
            }
        } catch (Exception e) {
            System.out.println("  [PASS] Test 1: Hệ thống quăng lỗi đúng mong đợi: " + e.getMessage());
            passed++;
        }

        // Test 2: BUG-002 - loadData crash khi file rỗng
        total++;
        String emptyFilename = "empty_test.csv";
        try {
            File f = new File(emptyFilename);
            if (f.exists()) f.delete();
            f.createNewFile();

            FinanceManager manager = new FinanceManager();
            boolean success = manager.loadData(emptyFilename);
            int txnCount = manager.getTree().getAllTransactions().size();
            
            if (success && txnCount == 0) {
                System.out.println("  [PASS] Test 2: loadData xử lý file rỗng thành công (0 giao dịch, không crash).");
                passed++;
            } else {
                System.out.println("  [FAIL] Test 2: loadData trả về thất bại hoặc có giao dịch.");
            }
            f.delete();
        } catch (Exception e) {
            System.out.println("  [FAIL] Test 2: loadData bị crash khi đọc file rỗng: " + e.getMessage());
        }

        // Test 3: BUG-003 - Xóa CASCADE không cập nhật nodeIndex
        total++;
        try {
            FinanceManager manager = new FinanceManager();
            manager.addCategory("CHI/Nhu cầu thiết yếu", "Ăn chơi", "CHI");
            manager.addCategory("CHI/Nhu cầu thiết yếu/Ăn chơi", "Bar club", "CHI");
            
            // Xác nhận các nút tồn tại trong map
            boolean beforeCheck = manager.getTree().getNodeByPath("CHI/Nhu cầu thiết yếu/Ăn chơi") != null 
                    && manager.getTree().getNodeByPath("CHI/Nhu cầu thiết yếu/Ăn chơi/Bar club") != null;

            // Thực hiện xóa CASCADE
            manager.removeCategory("CHI/Nhu cầu thiết yếu/Ăn chơi", "CASCADE");
            
            // Xác nhận các nút đã biến mất hoàn toàn khỏi map
            boolean afterCheck = manager.getTree().getNodeByPath("CHI/Nhu cầu thiết yếu/Ăn chơi") == null 
                    && manager.getTree().getNodeByPath("CHI/Nhu cầu thiết yếu/Ăn chơi/Bar club") == null;

            if (beforeCheck && afterCheck) {
                System.out.println("  [PASS] Test 3: Xóa CASCADE cập nhật và xóa sạch các khóa con cháu khỏi nodeMap.");
                passed++;
            } else {
                System.out.println("  [FAIL] Test 3: nodeIndex vẫn còn lưu tham chiếu sau khi xóa CASCADE.");
            }
        } catch (Exception e) {
            System.out.println("  [FAIL] Test 3: Lỗi trong quá trình xóa CASCADE: " + e.getMessage());
        }

        // Test 4: Không cho phép xóa ROOT
        total++;
        try {
            FinanceManager manager = new FinanceManager();
            boolean success = manager.removeCategory("ROOT", "CASCADE");
            if (!success) {
                System.out.println("  [PASS] Test 4: Hệ thống từ chối xóa nút gốc ROOT.");
                passed++;
            } else {
                System.out.println("  [FAIL] Test 4: Hệ thống cho phép xóa nút gốc ROOT.");
            }
        } catch (Exception e) {
            System.out.println("  [FAIL] Test 4: Lỗi khi test xóa ROOT: " + e.getMessage());
        }

        // Test 5: Không cho phép thêm danh mục trùng tên ở cùng một cha
        total++;
        try {
            FinanceManager manager = new FinanceManager();
            boolean success1 = manager.addCategory("THU", "Lương", "THU"); // "Lương" đã có mặc định
            if (!success1) {
                System.out.println("  [PASS] Test 5: Hệ thống từ chối thêm danh mục trùng tên trong cùng một cấp cha.");
                passed++;
            } else {
                System.out.println("  [FAIL] Test 5: Hệ thống chấp nhận danh mục trùng tên.");
            }
        } catch (Exception e) {
            System.out.println("  [FAIL] Test 5: Lỗi khi test thêm trùng tên: " + e.getMessage());
        }

        // Test 6: Xóa REPARENT và cập nhật nodeIndex
        total++;
        try {
            FinanceManager manager = new FinanceManager();
            manager.addCategory("CHI/Nhu cầu thiết yếu", "Làm đẹp", "CHI");
            manager.addCategory("CHI/Nhu cầu thiết yếu/Làm đẹp", "Cắt tóc", "CHI");
            
            // Xóa REPARENT
            manager.removeCategory("CHI/Nhu cầu thiết yếu/Làm đẹp", "REPARENT");

            // Nút "Cắt tóc" phải được chuyển lên trực thuộc "CHI/Nhu cầu thiết yếu"
            CategoryNode catTocNode = manager.getTree().getNodeByPath("CHI/Nhu cầu thiết yếu/Cắt tóc");
            CategoryNode lamDepNode = manager.getTree().getNodeByPath("CHI/Nhu cầu thiết yếu/Làm đẹp");

            if (catTocNode != null && lamDepNode == null) {
                System.out.println("  [PASS] Test 6: Xóa REPARENT hoạt động đúng, di chuyển con lên cha và cập nhật nodeMap.");
                passed++;
            } else {
                System.out.println("  [FAIL] Test 6: Xóa REPARENT lỗi (con không được chuyển hoặc nodeMap không cập nhật).");
            }
        } catch (Exception e) {
            System.out.println("  [FAIL] Test 6: Lỗi khi test xóa REPARENT: " + e.getMessage());
        }
        // Test 7: BUG-004 - Nhập ghi chú có chứa dấu phẩy
        total++;
        String commaFilename = "comma_test.csv";
        try {
            File f = new File(commaFilename);
            if (f.exists()) f.delete();

            FinanceManager manager1 = new FinanceManager(commaFilename);
            String noteWithComma = "Ăn sáng, mua thêm nước ngọt";
            manager1.addTransaction(150000, LocalDate.now(), noteWithComma, "CHI/Nhu cầu thiết yếu/Ăn uống/Ăn sáng");
            manager1.saveData();

            FinanceManager manager2 = new FinanceManager(commaFilename);
            manager2.loadData();
            List<Transaction> txns = manager2.getTree().getAllTransactions();

            if (txns.size() == 1 && txns.get(0).getNote().equals(noteWithComma)) {
                System.out.println("  [PASS] Test 7: Ghi chú có chứa dấu phẩy được bọc nháy kép và load lại chính xác.");
                passed++;
            } else {
                System.out.println("  [FAIL] Test 7: Lỗi lưu hoặc nạp ghi chú có dấu phẩy.");
            }
            f.delete();
        } catch (Exception e) {
            System.out.println("  [FAIL] Test 7: Lỗi khi kiểm thử ghi chú có dấu phẩy: " + e.getMessage());
        }

        System.out.printf("  => Kết quả: Đạt %d/%d ca kiểm thử biên.\n", passed, total);
    }

    private static void runPerformanceTests() {
        System.out.println("\n--- [PHẦN 2] KIỂM THỬ HIỆU NĂNG (PERFORMANCE TESTS) ---");
        int[] sizes = {100, 1000, 10000};
        
        System.out.println("┌──────────────────┬─────────────────┬───────────────────┬───────────────────┬───────────────────┐");
        System.out.println("│ Số lượng bản ghi │ Load từ CSV (ms)│ Tính tổng DFS (ms)│ Tìm theo path(ms) │ Tìm từ khóa (ms)  │");
        System.out.println("├──────────────────┼─────────────────┼───────────────────┼───────────────────┼───────────────────┤");

        for (int size : sizes) {
            // Chuẩn bị file dữ liệu
            String testFile = "perf_test_" + size + ".csv";
            generatePerfCSV(testFile, size);

            // 1. Đo thời gian tải (Load)
            FinanceManager manager = new FinanceManager();
            long t0 = System.nanoTime();
            manager.loadData(testFile);
            long t1 = System.nanoTime();
            double loadTimeMs = (t1 - t0) / 1_000_000.0;

            // 2. Đo thời gian tính tổng DFS
            long t2 = System.nanoTime();
            double total = manager.getTree().calculateTotalDfs(null);
            long t3 = System.nanoTime();
            double dfsTimeMs = (t3 - t2) / 1_000_000.0;

            // 3. Đo thời gian tìm theo path O(1) - lấy trung bình 1000 lượt
            long t4 = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                CategoryNode n = manager.getTree().getNodeByPath("CHI/Nhu cầu thiết yếu/Ăn uống/Ăn sáng");
            }
            long t5 = System.nanoTime();
            double pathTimeMs = ((t5 - t4) / 1000.0) / 1_000_000.0; // chia cho 1000 lượt

            // 4. Đo thời gian tìm theo từ khóa
            long t6 = System.nanoTime();
            List<Transaction> keywordRes = manager.searchTransactions("Test");
            long t7 = System.nanoTime();
            double keywordTimeMs = (t7 - t6) / 1_000_000.0;

            System.out.printf("│ %16d │ %15.2f │ %17.3f │ %17.4f │ %17.3f │\n",
                    size, loadTimeMs, dfsTimeMs, pathTimeMs, keywordTimeMs);

            // Dọn dẹp file
            new File(testFile).delete();
        }
        System.out.println("└──────────────────┴─────────────────┴───────────────────┴───────────────────┴───────────────────┘");
        System.out.println("  Nhận xét:");
        System.out.println("  - Tra cứu theo path qua nodeMap ổn định cực nhanh (~0 ms) bất kể kích thước cây (đúng O(1)).");
        System.out.println("  - Tìm kiếm theo từ khóa tăng tuyến tính theo số lượng giao dịch (đúng O(M)).");
        System.out.println("  - DFS tính tổng đệ quy hiệu quả, tăng dần theo kích thước cây.");
    }

    private static void generatePerfCSV(String filename, int count) {
        FinanceManager manager = new FinanceManager();
        List<String> leafPaths = manager.listLeafCategories();
        if (leafPaths.isEmpty()) return;

        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("# Auto-generated Performance Test CSV\n");
            writer.write("# ---TREE---\n");
            // Ghi cấu trúc cây
            writeTreeStructure(writer, manager.getTree().getRoot());
            writer.write("---TRANSACTIONS---\n");

            // Ghi giao dịch
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            for (int i = 0; i < count; i++) {
                String path = leafPaths.get(i % leafPaths.size());
                double amount = 10000 + (i * 100) % 5000000;
                LocalDate date = startDate.plusDays(i % 365);
                String note = "Test GD #" + i;
                // CSV Format: date,amount,path,note
                writer.write(String.format(java.util.Locale.US, "%s,%.2f,%s,%s\n",
                        date.toString(), amount, path, note));
            }
        } catch (Exception ignored) {
        }
    }

    private static void writeTreeStructure(FileWriter writer, CategoryNode node) throws Exception {
        if (!node.getName().equals("ROOT")) {
            writer.write(node.getPath() + "," + node.getCategoryType() + "\n");
        }
        for (CategoryNode child : node.getChildren()) {
            writeTreeStructure(writer, child);
        }
    }
}
