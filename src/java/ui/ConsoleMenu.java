package ui;

import core.FinanceManager;
import core.FinanceTree;
import models.CategoryNode;
import models.Transaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class ConsoleMenu {
    private final FinanceManager manager;
    private final Scanner scanner;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ConsoleMenu() {
        this(new FinanceManager());
    }

    public ConsoleMenu(FinanceManager manager) {
        this.manager = manager;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        // Tự động tải dữ liệu khi khởi chạy
        manager.loadData();
        for (String warning : manager.getLoadWarnings()) {
            System.out.println("  [CẢNH BÁO] " + warning);
        }

        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = readLine().trim();
            switch (choice) {
                case "1":
                    manageCategoriesMenu();
                    break;
                case "2":
                    addTransactionMenu();
                    break;
                case "3":
                    System.out.print(manager.generateReport());
                    waitEnter();
                    break;
                case "4":
                    searchTransactionsMenu();
                    break;
                case "5":
                    saveLoadMenu();
                    break;
                case "6":
                    System.out.println("\n  [HỆ THỐNG] Đang tự động lưu dữ liệu trước khi thoát...");
                    manager.saveData();
                    System.out.println("  [OK] Đã thoát chương trình. Tạm biệt!");
                    running = false;
                    break;
                default:
                    System.out.println("  [LỖI] Lựa chọn không hợp lệ, vui lòng chọn lại (1-6).");
                    waitEnter();
            }
        }
    }

    private void printMainMenu() {
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║         HỆ THỐNG QUẢN LÝ TÀI CHÍNH CÁ NHÂN (SV3)         ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  1. 📁 Quản lý Danh mục (Thêm / Xóa / Đổi tên)           ║");
        System.out.println("║  2. 📝 Nhập liệu Giao dịch mới                           ║");
        System.out.println("║  3. 📊 Xem Báo cáo Tổng quan Tài chính                   ║");
        System.out.println("║  4. 🔍 Tìm kiếm & Lọc Giao dịch                          ║");
        System.out.println("║  5. 💾 Lưu trữ & Tải lại dữ liệu (CSV)                   ║");
        System.out.println("║  6. ❌ Thoát chương trình & Tự động lưu                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.print("  👉 Nhập lựa chọn của bạn (1-6): ");
    }

    // =========================================================================
    // 1. QUẢN LÝ DANH MỤC
    // =========================================================================
    private void manageCategoriesMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n╔══════════════════════════════════════════════════════════╗");
            System.out.println("║                  📁 QUẢN LÝ DANH MỤC                    ║");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  1.1 Xem cấu trúc cây danh mục hiện tại                  ║");
            System.out.println("║  1.2 Thêm danh mục mới                                   ║");
            System.out.println("║  1.3 Xóa danh mục (CASCADE / REPARENT)                   ║");
            System.out.println("║  1.4 Đổi tên danh mục                                    ║");
            System.out.println("║  1.5 Quay lại Menu chính                                 ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.print("  👉 Nhập lựa chọn của bạn (1.1-1.5): ");
            String choice = readLine().trim();
            switch (choice) {
                case "1.1":
                case "1":
                    System.out.print(manager.listCategories());
                    waitEnter();
                    break;
                case "1.2":
                case "2":
                    addCategoryFlow();
                    break;
                case "1.3":
                case "3":
                    deleteCategoryFlow();
                    break;
                case "1.4":
                case "4":
                    renameCategoryFlow();
                    break;
                case "1.5":
                case "5":
                    back = true;
                    break;
                default:
                    System.out.println("  [LỖI] Lựa chọn không hợp lệ.");
                    waitEnter();
            }
        }
    }

    private void addCategoryFlow() {
        System.out.println("\n  --- THÊM DANH MỤC MỚI ---");
        System.out.print("  Nhập đường dẫn danh mục cha (ví dụ: 'THU' hoặc 'CHI/Nhu cầu thiết yếu'): ");
        String parentPath = readLine().trim();
        System.out.print("  Nhập tên danh mục mới muốn tạo: ");
        String name = readLine().trim();
        System.out.print("  Nhập loại danh mục (THU / CHI, để trống để tự động kế thừa từ cha): ");
        String typeInput = readLine().trim().toUpperCase();

        if (name.isEmpty()) {
            System.out.println("  [LỖI] Tên danh mục không được để trống.");
            waitEnter();
            return;
        }

        String type = typeInput.isEmpty() ? null : typeInput;
        boolean success = manager.addCategory(parentPath, name, type);
        if (success) {
            System.out.printf("  [OK] Đã thêm thành công danh mục '%s' vào '%s'.\n", name, parentPath);
        } else {
            System.out.println("  [LỖI] Thêm danh mục thất bại. Vui lòng kiểm tra lại đường dẫn cha hoặc tính trùng lặp tên.");
        }
        waitEnter();
    }

    private void deleteCategoryFlow() {
        System.out.println("\n  --- XÓA DANH MỤC ---");
        System.out.print("  Nhập đường dẫn danh mục muốn xóa (ví dụ: 'CHI/Làm đẹp'): ");
        String path = readLine().trim();

        if (path.isEmpty() || path.equals("THU") || path.equals("CHI") || path.equals("ROOT")) {
            System.out.println("  [LỖI] Không được phép xóa nút ROOT hoặc các nhánh gốc mặc định (THU / CHI).");
            waitEnter();
            return;
        }

        System.out.println("  Chọn chế độ xóa:");
        System.out.println("  - CASCADE : Xóa danh mục này và toàn bộ danh mục con + giao dịch của nó.");
        System.out.println("  - REPARENT: Xóa danh mục này, chuyển các con và giao dịch trực tiếp lên cha.");
        System.out.print("  Nhập chế độ (CASCADE hoặc REPARENT): ");
        String mode = readLine().trim().toUpperCase();

        if (!mode.equals("CASCADE") && !mode.equals("REPARENT")) {
            System.out.println("  [LỖI] Chế độ xóa không hợp lệ.");
            waitEnter();
            return;
        }

        System.out.printf("  [WARNING] Bạn có chắc chắn muốn xóa danh mục '%s' với chế độ '%s'? (y/n): ", path, mode);
        String confirm = readLine().trim().toLowerCase();
        if (!confirm.equals("y") && !confirm.equals("yes")) {
            System.out.println("  [HỦY] Hủy bỏ thao tác xóa.");
            waitEnter();
            return;
        }

        boolean success = manager.removeCategory(path, mode);
        if (success) {
            System.out.println("  [OK] Đã xóa danh mục thành công.");
        } else {
            System.out.println("  [LỖI] Xóa danh mục thất bại (đường dẫn không tồn tại hoặc lỗi trong quá trình xử lý).");
        }
        waitEnter();
    }

    private void renameCategoryFlow() {
        System.out.println("\n  --- ĐỔI TÊN DANH MỤC ---");
        System.out.print("  Nhập đường dẫn danh mục muốn đổi tên: ");
        String path = readLine().trim();
        System.out.print("  Nhập tên mới: ");
        String newName = readLine().trim();

        if (path.isEmpty() || newName.isEmpty()) {
            System.out.println("  [LỖI] Đường dẫn và tên mới không được để trống.");
            waitEnter();
            return;
        }

        if (path.equals("THU") || path.equals("CHI") || path.equals("ROOT")) {
            System.out.println("  [LỖI] Không thể đổi tên các danh mục mặc định cốt lõi.");
            waitEnter();
            return;
        }

        boolean success = manager.getTree().renameNode(path, newName);
        if (success) {
            System.out.println("  [OK] Đổi tên danh mục thành công.");
        } else {
            System.out.println("  [LỖI] Đổi tên thất bại. Có thể trùng tên ở cùng cấp hoặc đường dẫn sai.");
        }
        waitEnter();
    }

    // =========================================================================
    // 2. NHẬP LIỆU GIAO DỊCH
    // =========================================================================
    private void addTransactionMenu() {
        System.out.println("\n  --- THÊM GIAO DỊCH MỚI ---");
        System.out.println("  Danh sách các danh mục lá khả dụng:");
        List<String> leaves = manager.listLeafCategories();
        for (int i = 0; i < leaves.size(); i++) {
            System.out.printf("   [%d] %s\n", i + 1, leaves.get(i));
        }

        System.out.print("  👉 Nhập đường dẫn danh mục (ví dụ: 'THU/Lương' hoặc chọn số thứ tự ở trên): ");
        String pathInput = readLine().trim();
        String categoryPath = "";

        try {
            int index = Integer.parseInt(pathInput);
            if (index >= 1 && index <= leaves.size()) {
                categoryPath = leaves.get(index - 1);
            } else {
                System.out.println("  [LỖI] Số thứ tự lựa chọn vượt quá phạm vi.");
                waitEnter();
                return;
            }
        } catch (NumberFormatException e) {
            categoryPath = pathInput;
        }

        if (categoryPath.isEmpty()) {
            System.out.println("  [LỖI] Đường dẫn danh mục không được để trống.");
            waitEnter();
            return;
        }

        System.out.print("  Nhập số tiền giao dịch (VND): ");
        double amount;
        try {
            amount = Double.parseDouble(readLine().trim());
            if (amount <= 0) {
                System.out.println("  [LỖI] Số tiền giao dịch phải lớn hơn 0.");
                waitEnter();
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("  [LỖI] Số tiền nhập vào không phải là số hợp lệ.");
            waitEnter();
            return;
        }

        System.out.print("  Nhập ngày giao dịch (YYYY-MM-DD, nhấn ENTER để chọn ngày hôm nay): ");
        String dateInput = readLine().trim();
        LocalDate date;
        if (dateInput.isEmpty()) {
            date = LocalDate.now();
        } else {
            try {
                date = LocalDate.parse(dateInput, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                System.out.println("  [LỖI] Định dạng ngày không hợp lệ (phải là YYYY-MM-DD).");
                waitEnter();
                return;
            }
        }

        System.out.print("  Nhập ghi chú giao dịch: ");
        String note = readLine().trim();

        boolean success = manager.addTransaction(amount, date, note, categoryPath);
        if (success) {
            System.out.printf("  [OK] Đã ghi nhận giao dịch thành công vào '%s'.\n", categoryPath);
        } else {
            System.out.println("  [LỖI] Ghi nhận giao dịch thất bại. Hãy chắc chắn đường dẫn danh mục tồn tại.");
        }
        waitEnter();
    }

    // =========================================================================
    // 4. TÌM KIẾM GIAO DỊCH
    // =========================================================================
    private void searchTransactionsMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n╔══════════════════════════════════════════════════════════╗");
            System.out.println("║                 🔍 TÌM KIẾM GIAO DỊCH                   ║");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  4.1 Tìm theo từ khóa trong ghi chú                      ║");
            System.out.println("║  4.2 Lọc theo khoảng ngày (Từ ngày -> Đến ngày)          ║");
            System.out.println("║  4.3 Quay lại Menu chính                                 ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.print("  👉 Nhập lựa chọn của bạn (4.1-4.3): ");
            String choice = readLine().trim();
            switch (choice) {
                case "4.1":
                case "1":
                    System.out.print("  Nhập từ khóa cần tìm kiếm: ");
                    String keyword = readLine().trim();
                    List<Transaction> keywordResults = manager.searchTransactions(keyword);
                    displayTransactionList(keywordResults, "Kết quả tìm kiếm cho từ khóa: '" + keyword + "'");
                    waitEnter();
                    break;
                case "4.2":
                case "2":
                    try {
                        System.out.print("  Nhập từ ngày (YYYY-MM-DD): ");
                        LocalDate fromDate = LocalDate.parse(readLine().trim(), DATE_FORMATTER);
                        System.out.print("  Nhập đến ngày (YYYY-MM-DD): ");
                        LocalDate toDate = LocalDate.parse(readLine().trim(), DATE_FORMATTER);
                        List<Transaction> dateResults = manager.filterByDate(fromDate, toDate);
                        displayTransactionList(dateResults, "Kết quả lọc từ " + fromDate + " đến " + toDate);
                    } catch (DateTimeParseException e) {
                        System.out.println("  [LỖI] Định dạng ngày không hợp lệ. Vui lòng nhập đúng YYYY-MM-DD.");
                    }
                    waitEnter();
                    break;
                case "4.3":
                case "3":
                    back = true;
                    break;
                default:
                    System.out.println("  [LỖI] Lựa chọn không hợp lệ.");
                    waitEnter();
            }
        }
    }

    private void displayTransactionList(List<Transaction> list, String title) {
        System.out.println("\n  --- " + title.toUpperCase() + " ---");
        if (list.isEmpty()) {
            System.out.println("  (Không có giao dịch nào thỏa mãn điều kiện)");
            return;
        }
        for (Transaction txn : list) {
            System.out.println(txn.getDetails());
        }
        System.out.println("  -------------------------------------------------------");
        System.out.printf("  Tổng cộng: %d giao dịch.\n", list.size());
    }

    // =========================================================================
    // 5. LƯU/TẢI DỮ LIỆU
    // =========================================================================
    private void saveLoadMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n╔══════════════════════════════════════════════════════════╗");
            System.out.println("║                 💾 LƯU TRỮ & TẢI DỮ LIỆU                ║");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  5.1 Lưu dữ liệu hiện tại vào file CSV                   ║");
            System.out.println("║  5.2 Tải lại dữ liệu từ file CSV                         ║");
            System.out.println("║  5.3 Quay lại Menu chính                                 ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.print("  👉 Nhập lựa chọn của bạn (5.1-5.3): ");
            String choice = readLine().trim();
            switch (choice) {
                case "5.1":
                case "1":
                    System.out.print("  Nhập tên file để lưu (để trống sẽ dùng mặc định 'data.csv'): ");
                    String saveFile = readLine().trim();
                    boolean saveSuccess = manager.saveData(saveFile);
                    if (saveSuccess) {
                        System.out.println("  [OK] Đã lưu trữ dữ liệu thành công.");
                    } else {
                        System.out.println("  [LỖI] Lưu trữ thất bại.");
                    }
                    waitEnter();
                    break;
                case "5.2":
                case "2":
                    System.out.print("  Nhập tên file để tải (để trống sẽ dùng mặc định 'data.csv'): ");
                    String loadFile = readLine().trim();
                    boolean loadSuccess = manager.loadData(loadFile);
                    for (String warning : manager.getLoadWarnings()) {
                        System.out.println("  [CẢNH BÁO] " + warning);
                    }
                    if (loadSuccess) {
                        System.out.println("  [OK] Đã tải dữ liệu thành công.");
                    } else {
                        System.out.println("  [LỖI] Tải dữ liệu thất bại hoặc file không có sẵn.");
                    }
                    waitEnter();
                    break;
                case "5.3":
                case "3":
                    back = true;
                    break;
                default:
                    System.out.println("  [LỖI] Lựa chọn không hợp lệ.");
                    waitEnter();
            }
        }
    }

    private String readLine() {
        try {
            return scanner.nextLine();
        } catch (NoSuchElementException e) {
            return "";
        }
    }

    private void waitEnter() {
        System.out.print("\n  [Nhấn ENTER để tiếp tục...]");
        readLine();
    }
}
