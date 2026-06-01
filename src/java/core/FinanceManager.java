package core;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.List;
import models.CategoryNode;
import models.Transaction;

public class FinanceManager {
    public static final String DEFAULT_DATA_FILE = "data.csv";
    public static final String SEPARATOR = "---TREE---";

    private FinanceTree tree;
    private String dataFile;
    private final List<String> loadWarnings = new ArrayList<>();
    private final List<String> testWarnings = new ArrayList<>();

    public List<String> getLoadWarnings() {
        return new ArrayList<>(loadWarnings);
    }

    public List<String> getTestWarnings() {
        return new ArrayList<>(testWarnings);
    }

    public FinanceManager() {
        this(DEFAULT_DATA_FILE);
    }

    public FinanceManager(String dataFile) {
        this.tree = new FinanceTree();
        this.dataFile = dataFile;
    }

    public FinanceTree getTree() {
        return tree;
    }

    // =========================================================================
    // PHẦN 1: QUẢN LÝ DANH MỤC
    // =========================================================================

    public boolean addCategory(String parentPath, String name, String categoryType) {
        CategoryNode result = tree.insertNode(parentPath, name, categoryType);
        return result != null;
    }

    public boolean removeCategory(String path, String mode) {
        return tree.deleteNode(path, mode);
    }

    public String listCategories() {
        return tree.traverseTree(null, 0);
    }

    public List<String> listLeafCategories() {
        return tree.getAllLeafPaths();
    }

    // =========================================================================
    // PHẦN 2: QUẢN LÝ GIAO DỊCH
    // =========================================================================

    public boolean addTransaction(double amount, LocalDate date, String note, String categoryPath) {
        try {
            Transaction txn = new Transaction(amount, date, note, categoryPath);
            return tree.classifyAndAddTransaction(txn, categoryPath);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public List<Transaction> getAllTransactions(int limit) {
        List<Transaction> allTxns = tree.getAllTransactions();
        if (limit > 0 && limit < allTxns.size()) {
            return allTxns.subList(0, limit);
        }
        return allTxns;
    }

    public List<Transaction> searchTransactions(String keyword) {
        return tree.searchTransactionsByKeyword(keyword);
    }

    public List<Transaction> filterByDate(LocalDate startDate, LocalDate endDate) {
        return tree.searchTransactionsByDateRange(startDate, endDate);
    }

    // =========================================================================
    // PHẦN 3: BÁO CÁO VÀ THỐNG KÊ
    // =========================================================================

    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=======================================================\n");
        sb.append("  BÁO CÁO TỔNG QUAN TÀI CHÍNH\n");
        sb.append("=======================================================\n");

        double[] totals = tree.calculateIncomeAndExpense();
        double totalIncome = totals[0];
        double totalExpense = totals[1];
        double balance = totalIncome - totalExpense;

        sb.append(String.format("  📈 Tổng Thu nhập  : %,15.0f VND\n", totalIncome));
        sb.append(String.format("  📉 Tổng Chi tiêu  : %,15.0f VND\n", totalExpense));
        sb.append("-------------------------------------------------------\n");
        String balanceSymbol = balance >= 0 ? "✅" : "❌";
        sb.append(String.format("  %s Số dư còn lại  : %,15.0f VND\n", balanceSymbol, balance));

        // Chi tiết từng danh mục lớn
        sb.append("\n  --- Chi tiết theo danh mục chính ---\n");
        CategoryNode incomeNode = tree.getNodeByPath("THU");
        CategoryNode expenseNode = tree.getNodeByPath("CHI");

        if (incomeNode != null) {
            sb.append(String.format("\n  📁 Thu nhập (%,.0f VND):\n", totalIncome));
            for (CategoryNode child : incomeNode.getChildren()) {
                double amount = tree.calculateTotalDfs(child);
                if (amount > 0) {
                    sb.append(String.format("     ├─ %-20s: %,12.0f VND\n", child.getName(), amount));
                }
            }
        }

        if (expenseNode != null) {
            sb.append(String.format("\n  📁 Chi tiêu (%,.0f VND):\n", totalExpense));
            for (CategoryNode child : expenseNode.getChildren()) {
                double amount = tree.calculateTotalDfs(child);
                if (amount > 0) {
                    double pct = totalExpense > 0 ? (amount / totalExpense * 100) : 0;
                    sb.append(String.format("     ├─ %-20s: %,12.0f VND  (%.1f%%)\n", child.getName(), amount, pct));
                }
            }
        }

        int totalTxns = tree.getAllTransactions().size();
        sb.append(String.format("\n  📊 Tổng số giao dịch: %d\n", totalTxns));
        sb.append(String.format("  🌳 Tổng số danh mục : %d\n", tree.getNodeCount()));
        sb.append("=======================================================\n");
        return sb.toString();
    }

    public String getCategorySummary(String categoryPath) {
        StringBuilder sb = new StringBuilder();
        CategoryNode node = tree.getNodeByPath(categoryPath);
        if (node == null) {
            return "  [LỖI] Không tìm thấy: '" + categoryPath + "'\n";
        }

        double total = tree.calculateTotalDfs(node);
        sb.append("\n  📁 Danh mục: ").append(node.getPath()).append("\n");
        sb.append(String.format("  Tổng tiền (bao gồm cả con): %,.0f VND\n", total));
        sb.append("  Số danh mục con trực tiếp : ").append(node.getChildren().size()).append("\n");
        sb.append("  Số giao dịch trực tiếp    : ").append(node.getTransactions().size()).append("\n");

        if (!node.getTransactions().isEmpty()) {
            sb.append("\n  --- Các giao dịch trực tiếp ---\n");
            List<Transaction> sortedTxns = new ArrayList<>(node.getTransactions());
            sortedTxns.sort((t1, t2) -> t2.getDate().compareTo(t1.getDate()));
            for (Transaction txn : sortedTxns) {
                sb.append(txn.getDetails()).append("\n");
            }
        }
        return sb.toString();
    }

    // =========================================================================
    // PHẦN 4: LƯU TRỮ DỮ LIỆU (File I/O)
    // =========================================================================

    public boolean saveData() {
        return saveData(this.dataFile);
    }

    public boolean saveData(String filename) {
        if (filename == null || filename.isEmpty()) {
            filename = this.dataFile;
        }

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"))) {
            // -- Header --
            writer.write("# FinanceManager Data File - Saved: " + 
                         LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
            writer.write("# Version: 1.0\n");

            // -- Phần 1: Cấu trúc cây --
            writer.write("# " + SEPARATOR + "\n");
            saveTreeStructure(writer, tree.getRoot());

            // -- Phần 2: Giao dịch --
            writer.write("---TRANSACTIONS---\n");
            List<Transaction> allTxns = tree.getAllTransactions();
            // Sắp xếp theo ID tăng dần khi lưu
            allTxns.sort((t1, t2) -> Integer.compare(t1.getTransactionId(), t2.getTransactionId()));
            for (Transaction txn : allTxns) {
                writer.write(txn.toCsvRow() + "\n");
            }

            return true;

        } catch (IOException e) {
            return false;
        }
    }

    private void saveTreeStructure(BufferedWriter writer, CategoryNode node) throws IOException {
        if (!node.getName().equals("ROOT")) {
            String path = node.getPath();
            writer.write(path + "," + node.getCategoryType() + "\n");
        }
        for (CategoryNode child : node.getChildren()) {
            saveTreeStructure(writer, child);
        }
    }

    public boolean loadData() {
        return loadData(this.dataFile);
    }

    public boolean loadData(String filename) {
        if (filename == null || filename.isEmpty()) {
            filename = this.dataFile;
        }

        loadWarnings.clear();
        File file = new File(filename);
        if (!file.exists()) {
            loadWarnings.add(String.format("File '%s' chưa có, bắt đầu mới.", filename));
            return false;
        }

        this.tree = new FinanceTree();
        Transaction.resetCounter();
        boolean inTransactionsSection = false;
        int nodesCreated = 0;
        int txnsLoaded = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();

                // Bỏ qua dòng comment và dòng trống
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (line.equals("---TRANSACTIONS---")) {
                    inTransactionsSection = true;
                    continue;
                }

                if (!inTransactionsSection) {
                    // -- Xử lý cấu trúc cây --
                    // Format: "Root/Chi tiêu/Ăn uống,Expense"
                    try {
                        int lastCommaIdx = line.lastIndexOf(',');
                        if (lastCommaIdx != -1) {
                            String path = line.substring(0, lastCommaIdx).trim();
                            String catType = line.substring(lastCommaIdx + 1).trim();

                            // Kiểm tra path phải bắt đầu bằng THU hoặc CHI
                            if (!path.equals("THU") && !path.equals("CHI") && !path.startsWith("THU/") && !path.startsWith("CHI/")) {
                                loadWarnings.add(String.format("Line %d: invalid path '%s' (phải bắt đầu bằng THU/ hoặc CHI/)", lineNum, path));
                                continue;
                            }

                            // Tạo nút nếu chưa tồn tại
                            if (tree.getNodeByPath(path) == null) {
                                int lastSlashIdx = path.lastIndexOf('/');
                                String parentPath = lastSlashIdx != -1 ? path.substring(0, lastSlashIdx) : "ROOT";
                                String nodeName = lastSlashIdx != -1 ? path.substring(lastSlashIdx + 1) : path;

                                if (tree.insertNode(parentPath, nodeName, catType) != null) {
                                    nodesCreated++;
                                }
                            }
                        } else {
                            loadWarnings.add(String.format("Line %d: invalid path format (missing comma)", lineNum));
                        }
                    } catch (Exception e) {
                        loadWarnings.add(String.format("Lỗi dòng %d (cây): %s", lineNum, e.getMessage()));
                    }

                } else {
                    // -- Xử lý giao dịch --
                    try {
                        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 4);
                        if (parts.length < 4) {
                            loadWarnings.add(String.format("Line %d: invalid columns count", lineNum));
                            continue;
                        }
                        
                        LocalDate date;
                        try {
                            date = LocalDate.parse(parts[0].trim(), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        } catch (java.time.format.DateTimeParseException e) {
                            loadWarnings.add(String.format("Line %d: invalid date format", lineNum));
                            continue;
                        }
                        
                        double amount;
                        try {
                            amount = Double.parseDouble(parts[1].trim());
                        } catch (NumberFormatException e) {
                            loadWarnings.add(String.format("Line %d: invalid amount format", lineNum));
                            continue;
                        }
                        
                        if (amount <= 0) {
                            loadWarnings.add(String.format("Line %d: invalid amount <= 0", lineNum));
                            continue;
                        }
                        
                        String categoryPath = parts[2].trim();
                        if (!categoryPath.contains("/")) {
                            loadWarnings.add(String.format("Line %d: invalid path", lineNum));
                            continue;
                        }
                        
                        if (!categoryPath.startsWith("THU/") && !categoryPath.startsWith("CHI/")) {
                            loadWarnings.add(String.format("Line %d: invalid path prefix", lineNum));
                            continue;
                        }
                        
                        String noteField = parts[3].trim();
                        String note = "";
                        if (noteField.startsWith("\"") && noteField.endsWith("\"")) {
                            note = noteField.substring(1, noteField.length() - 1).replace("\"\"", "\"");
                        } else {
                            note = noteField.replace(";", ",");
                        }
                        
                        Transaction txn = new Transaction(amount, date, note, categoryPath);
                        boolean added = tree.classifyAndAddTransaction(txn, categoryPath);
                        if (added) {
                            txnsLoaded++;
                        } else {
                            loadWarnings.add(String.format("Line %d: could not classify transaction into '%s'", lineNum, categoryPath));
                        }
                    } catch (Exception e) {
                        loadWarnings.add(String.format("Lỗi dòng %d (giao dịch): %s", lineNum, e.getMessage()));
                    }
                }
            }

            return true;

        } catch (IOException e) {
            return false;
        }
    }

    // =========================================================================
    // PHẦN 5: SINH DỮ LIỆU THỬ NGHIỆM (Test Data Generation)
    // =========================================================================

    /**
     * Sinh ngẫu nhiên các giao dịch thử nghiệm và phân bổ vào cây danh mục.
     *
     * @param count          Số lượng giao dịch cần sinh (ví dụ: 10_000)
     * @param uniformDistrib true  = phân bổ tuần tự đều vào tất cả nút lá
     *                       false = chọn ngẫu nhiên hoàn toàn (có thể lệch)
     * @return Số giao dịch đã thêm thành công
     */
    public int generateTestData(int count, boolean uniformDistrib) {
        List<String> leafPaths = tree.getAllLeafPaths();
        if (leafPaths.isEmpty()) {
            return 0;
        }

        Random random = new Random();
        LocalDate startOfYear = LocalDate.of(2024, 1, 1);
        int successCount = 0;

        for (int i = 0; i < count; i++) {
            // 1. Chọn đường dẫn danh mục đích
            String path;
            if (uniformDistrib) {
                // Phân bổ đều tuần tự: lần lượt qua từng nút lá
                path = leafPaths.get(i % leafPaths.size());
            } else {
                // Chọn hoàn toàn ngẫu nhiên
                path = leafPaths.get(random.nextInt(leafPaths.size()));
            }

            // 2. Sinh số tiền ngẫu nhiên trong khoảng [10.000 .. 10.000.000]
            double amount = 10_000 + random.nextDouble() * (10_000_000 - 10_000);

            // 3. Sinh ngày ngẫu nhiên trong năm 2024 (0..365 ngày kể từ 01/01/2024)
            LocalDate date = startOfYear.plusDays(random.nextInt(365));

            // 4. Tạo ghi chú
            String note = "Test GD #" + (i + 1);

            // 5. Thêm giao dịch vào cây (KHÔNG in ra màn hình — để SV3/SV4 xử lý)
            try {
                Transaction txn = new Transaction(amount, date, note, path);
                if (tree.classifyAndAddTransaction(txn, path)) {
                    successCount++;
                }
            } catch (IllegalArgumentException ignored) {
                // amount luôn > 0 nên không xảy ra, nhưng bắt để an toàn
            }
        }

        testWarnings.clear();
        // 6. Kiểm tra phân bổ: mỗi nút lá phải có ít nhất half của mức trung bình
        if (uniformDistrib && !leafPaths.isEmpty()) {
            double avg = (double) successCount / leafPaths.size();
            double minExpected = avg * 0.5;
            for (String path : leafPaths) {
                CategoryNode node = tree.getNodeByPath(path);
                if (node != null && node.getTransactions().size() < minExpected) {
                    testWarnings.add(String.format("generateTestData: nút '%s' chỉ có %d GD (< %.0f mức tối thiểu)",
                            path, node.getTransactions().size(), minExpected));
                }
            }
        }

        return successCount;
    }
}
