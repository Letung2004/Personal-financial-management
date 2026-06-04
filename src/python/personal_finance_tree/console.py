from __future__ import annotations

from datetime import datetime
from typing import Optional

from .manager import FinanceManager
from .models import Transaction


DATA_FILE = "finance_data.csv"


def run_console_menu() -> None:
    """
    Chạy vòng lặp menu console nâng cấp cho hệ thống quản lý tài chính cá nhân.
    """
    manager = FinanceManager()

    # Tải dữ liệu từ file CSV khi khởi động
    try:
        success, count, warnings = manager.load_data(DATA_FILE)
        if success:
            print(f"Đã tải thành công {count} giao dịch từ '{DATA_FILE}'.")
            if warnings:
                print(f"⚠️ Phát hiện {len(warnings)} cảnh báo khi đọc file:")
                for w in warnings[:10]:
                    print(f"  - {w}")
                if len(warnings) > 10:
                    print(f"  ... và {len(warnings) - 10} cảnh báo khác.")
        else:
            print(f"Chưa có file dữ liệu '{DATA_FILE}', bắt đầu với cây danh mục mặc định.")
    except Exception as error:
        print(f"Không thể tải dữ liệu: {error}")
        print("Bắt đầu với cây danh mục mặc định mới.")

    while True:
        print("\n=============================================")
        print("     HỆ THỐNG QUẢN LÝ TÀI CHÍNH CÁ NHÂN")
        print("=============================================")
        print("1. Xem sơ đồ cây danh mục tài chính")
        print("2. Ghi chép giao dịch mới")
        print("3. Xem báo cáo tổng kết & Chi tiết")
        print("4. Tìm kiếm & Lọc giao dịch (Nâng cao)")
        print("5. Menu: Quản lý Danh mục (Thêm/Sửa/Xóa)")
        print("6. Menu: Quản lý Giao dịch (Sửa/Xóa)")
        print("7. Menu: Kiểm thử QA/QC & Hiệu năng (SV4)")
        print("8. Thoát và lưu dữ liệu")
        print("=============================================")

        choice = input("Chọn chức năng (1-8): ").strip()

        if choice == "1":
            show_category_trees(manager)
        elif choice == "2":
            add_new_transaction(manager)
        elif choice == "3":
            show_summary_report(manager)
        elif choice == "4":
            search_and_filter_menu(manager)
        elif choice == "5":
            category_management_menu(manager)
        elif choice == "6":
            transaction_management_menu(manager)
        elif choice == "7":
            qa_performance_menu(manager)
        elif choice == "8":
            save_and_exit(manager)
            break
        else:
            print("Lựa chọn không hợp lệ. Vui lòng nhập số từ 1 đến 8.")


def show_category_trees(manager: FinanceManager) -> None:
    """
    Hiển thị sơ đồ cây thu nhập và chi tiêu.
    """
    print("\n--- CÂY THU NHẬP (INCOME TREE) ---")
    manager.print_tree(manager.root_income)

    print("\n--- CÂY CHI TIÊU (EXPENSE TREE) ---")
    manager.print_tree(manager.root_expense)


def add_new_transaction(manager: FinanceManager) -> None:
    """
    Ghi chép giao dịch mới từ console.
    """
    print("\n--- GHI CHÉP GIAO DỊCH MỚI ---")
    amount = read_amount()
    date = read_date()
    note = input("Nhập ghi chú: ").strip()

    print("Ví dụ đường dẫn: CHI TIÊU/Ăn uống/Đi chợ")
    print("Hoặc viết tắt: CHI/Ăn uống/Đi chợ, THU/Lương")
    category_path = input("Nhập đường dẫn danh mục: ").strip()

    try:
        transaction = Transaction(amount=amount, date=date, note=note)
        manager.add_transaction(transaction, category_path)
        print("✨ Đã thêm giao dịch thành công!")
    except ValueError as error:
        print(f"❌ Không thể thêm giao dịch: {error}")


def read_amount() -> float:
    """
    Đọc số tiền và xác thực.
    """
    while True:
        raw_value = input("Nhập số tiền (đ): ").strip().replace(",", "")
        try:
            amount = float(raw_value)
            if amount <= 0:
                print("Số tiền phải lớn hơn 0.")
                continue
            return amount
        except ValueError:
            print("Số tiền không hợp lệ. Vui lòng nhập số (ví dụ: 150000).")


def read_date() -> str:
    """
    Đọc ngày dạng YYYY-MM-DD và kiểm tra tính hợp lệ.
    """
    while True:
        date_value = input("Nhập ngày (YYYY-MM-DD) [Để trống lấy ngày hiện tại]: ").strip()
        if not date_value:
            return datetime.now().strftime("%Y-%m-%d")

        try:
            # Kiểm tra xem ngày tháng nhập vào có thực sự tồn tại trong lịch hay không
            dt = datetime.strptime(date_value, "%Y-%m-%d")
            if dt > datetime.now():
                print("Ngày giao dịch không được ở tương lai. Vui lòng nhập lại.")
                continue
            return date_value
        except ValueError:
            print("Ngày không hợp lệ (Ví dụ: YYYY-MM-DD và phải là ngày thực tế, không nhập 30/02).")


def show_summary_report(manager: FinanceManager) -> None:
    """
    Hiển thị báo cáo tổng kết hỗ trợ bộ lọc Ngày/Tháng/Năm.
    """
    print("\n--- XEM BÁO CÁO TỔNG KẾT ---")
    print("Chọn bộ lọc báo cáo:")
    print("0. Xem toàn bộ thời gian (mặc định)")
    print("1. Lọc theo Năm")
    print("2. Lọc theo Tháng (trong năm)")
    print("3. Lọc theo Ngày cụ thể")
    
    choice = input("Lựa chọn (0-3): ").strip()
    
    year: Optional[int] = None
    month: Optional[int] = None
    day: Optional[int] = None

    if choice == "1":
        year = read_int_range("Nhập năm cần lọc: ", 2000, 2100)
    elif choice == "2":
        year = read_int_range("Nhập năm cần lọc: ", 2000, 2100)
        month = read_int_range("Nhập tháng (1-12): ", 1, 12)
    elif choice == "3":
        date_str = read_date()
        dt = datetime.strptime(date_str, "%Y-%m-%d")
        year, month, day = dt.year, dt.month, dt.day

    total_income, total_expense, balance = manager.generate_report_data(year, month, day)

    # Hiển thị tiêu đề báo cáo
    filter_desc = "TOÀN BỘ THỜI GIAN"
    if day:
        filter_desc = f"NGÀY {year:04d}-{month:02d}-{day:02d}"
    elif month:
        filter_desc = f"THÁNG {month:02d}/{year:04d}"
    elif year:
        filter_desc = f"NĂM {year:04d}"

    print(f"\n===== BÁO CÁO TỔNG KẾT ({filter_desc}) =====")
    print(f"Tổng Thu (Income)  : {total_income:,.2f} đ")
    print(f"Tổng Chi (Expense) : {total_expense:,.2f} đ")
    print(f"Số dư (Balance)    : {balance:,.2f} đ")
    print("==================================================")
    
    view_details = input("Bạn có muốn xem chi tiết cây phân bổ giao dịch? (y/n): ").strip().lower()
    if view_details == "y":
        show_category_trees(manager)


def read_int_range(prompt: str, min_val: int, max_val: int) -> int:
    while True:
        try:
            val = int(input(prompt).strip())
            if min_val <= val <= max_val:
                return val
            print(f"Giá trị phải nằm trong khoảng từ {min_val} đến {max_val}.")
        except ValueError:
            print("Vui lòng nhập một số nguyên hợp lệ.")


def search_and_filter_menu(manager: FinanceManager) -> None:
    """
    Menu con Tìm kiếm & Lọc giao dịch nâng cao.
    """
    while True:
        print("\n--- MENU: TÌM KIẾM & LỌC GIAO DỊCH ---")
        print("1. Tìm kiếm giao dịch theo từ khóa trong ghi chú")
        print("2. Lọc giao dịch nâng cao (Khoảng ngày & Nhánh danh mục)")
        print("3. Quay lại menu chính")
        
        choice = input("Lựa chọn (1-3): ").strip()
        
        if choice == "1":
            keyword = input("Nhập từ khóa ghi chú cần tìm: ").strip()
            results = manager.search_transactions(keyword)
            print_search_results(results)
        elif choice == "2":
            print("Nhập khoảng ngày lọc (YYYY-MM-DD). Để trống nếu không muốn lọc.")
            start_date = input("Từ ngày (YYYY-MM-DD): ").strip()
            end_date = input("Đến ngày (YYYY-MM-DD): ").strip()
            cat_path = input("Đường dẫn danh mục nhánh (ví dụ: CHI TIÊU/Ăn uống) [Để trống = Tất cả]: ").strip()
            
            # Validate khoảng ngày nếu nhập
            try:
                if start_date:
                    datetime.strptime(start_date, "%Y-%m-%d")
                if end_date:
                    datetime.strptime(end_date, "%Y-%m-%d")
            except ValueError:
                print("❌ Định dạng ngày tháng không hợp lệ (yêu cầu YYYY-MM-DD).")
                continue

            try:
                results = manager.filter_transactions(
                    start_date=start_date if start_date else None,
                    end_date=end_date if end_date else None,
                    category_path=cat_path if cat_path else None
                )
                print_search_results(results)
            except ValueError as e:
                print(f"❌ Lỗi lọc dữ liệu: {e}")
        elif choice == "3":
            break
        else:
            print("Lựa chọn không hợp lệ.")


def print_search_results(results: list) -> None:
    if not results:
        print("Không tìm thấy giao dịch nào phù hợp với bộ lọc.")
        return

    print(f"\nKẾT QUẢ TÌM THẤY ({len(results)} giao dịch):")
    print("-" * 80)
    for idx, (path, tx) in enumerate(results, 1):
        print(f"{idx:02d}. [{path}] - Số tiền: {tx.amount:,.2f} đ | Ngày: {tx.date} | Ghi chú: {tx.note}")
    print("-" * 80)


def category_management_menu(manager: FinanceManager) -> None:
    """
    Menu con quản lý danh mục (Thêm, Đổi tên, Xóa CASCADE).
    """
    while True:
        print("\n--- MENU: QUẢN LÝ DANH MỤC (N-ARY TREE) ---")
        print("1. Thêm danh mục trống mới")
        print("2. Đổi tên danh mục hiện có")
        print("3. Xóa danh mục (CASCADE xóa con & giao dịch)")
        print("4. Quay lại menu chính")

        choice = input("Lựa chọn (1-4): ").strip()

        if choice == "1":
            path = input("Nhập đường dẫn danh mục mới cần tạo (VD: CHI/Ăn uống/Ăn vặt): ").strip()
            try:
                manager.add_category_by_path(path)
                print("✨ Đã tạo danh mục thành công!")
            except ValueError as e:
                print(f"❌ Lỗi: {e}")
        elif choice == "2":
            path = input("Nhập đường dẫn danh mục cần đổi tên: ").strip()
            new_name = input("Nhập tên mới cho danh mục: ").strip()
            try:
                manager.rename_category(path, new_name)
                print("✨ Đổi tên danh mục thành công!")
            except ValueError as e:
                print(f"❌ Lỗi: {e}")
        elif choice == "3":
            path = input("Nhập đường dẫn danh mục cần xóa: ").strip()
            print("⚠️ CẢNH BÁO: Thao tác này sẽ xóa CASCADE toàn bộ danh mục con và giao dịch thuộc nhánh này!")
            confirm = input("Bạn có chắc chắn muốn xóa? (y/n): ").strip().lower()
            if confirm == "y":
                try:
                    manager.delete_category(path)
                    print("✨ Đã xóa danh mục thành công!")
                except ValueError as e:
                    print(f"❌ Lỗi: {e}")
            else:
                print("Đã hủy bỏ thao tác xóa.")
        elif choice == "4":
            break
        else:
            print("Lựa chọn không hợp lệ.")


def transaction_management_menu(manager: FinanceManager) -> None:
    """
    Menu con quản lý giao dịch (Liệt kê, Sửa, Xóa).
    """
    while True:
        print("\n--- MENU: QUẢN LÝ GIAO DỊCH ---")
        print("1. Liệt kê giao dịch của một danh mục")
        print("2. Sửa giao dịch")
        print("3. Xóa giao dịch")
        print("4. Quay lại menu chính")

        choice = input("Lựa chọn (1-4): ").strip()

        if choice in ("1", "2", "3"):
            path = input("Nhập đường dẫn danh mục (ví dụ: CHI TIÊU/Ăn uống): ").strip()
            try:
                parts = manager.normalize_and_validate_path(path)
                path_upper = "/".join(parts).upper()
                if path_upper not in manager.node_index:
                    print("❌ Không tìm thấy danh mục được yêu cầu.")
                    continue

                node = manager.node_index[path_upper]
                if not node.transactions:
                    print(f"Danh mục '{path}' hiện tại không có giao dịch nào.")
                    if choice == "1":
                        continue
                    else:
                        # Không thể sửa/xóa nếu không có giao dịch
                        continue

                # Liệt kê danh sách
                print(f"\nDanh sách giao dịch tại '{path}':")
                for i, tx in enumerate(node.transactions):
                    print(f"  [{i}] - Số tiền: {tx.amount:,.2f}đ | Ngày: {tx.date} | Ghi chú: {tx.note}")

                if choice == "1":
                    continue

                # Lấy chỉ số giao dịch cần thao tác
                idx = read_int_range("Chọn chỉ số giao dịch cần sửa/xóa: ", 0, len(node.transactions) - 1)

                if choice == "2":
                    print("\nNhập các thông tin mới:")
                    amount = read_amount()
                    date = read_date()
                    note = input("Nhập ghi chú mới: ").strip()
                    manager.edit_transaction(path, idx, amount, date, note)
                    print("✨ Cập nhật giao dịch thành công!")
                elif choice == "3":
                    manager.delete_transaction(path, idx)
                    print("✨ Đã xóa giao dịch thành công!")

            except ValueError as e:
                print(f"❌ Lỗi: {e}")

        elif choice == "4":
            break
        else:
            print("Lựa chọn không hợp lệ.")


def qa_performance_menu(manager: FinanceManager) -> None:
    """
    Menu kiểm thử QA/QC dành riêng cho SV4.
    """
    test_file = "test_data.csv"
    while True:
        print("\n--- MENU: KIỂM THỬ QA/QC & HIỆU NĂNG (SV4) ---")
        print("1. Sinh 10.000 dữ liệu mẫu (test_data.csv) - Phân bổ ĐỀU (Uniform)")
        print("2. Sinh 10.000 dữ liệu mẫu (test_data.csv) - Phân bổ NGẪU NHIÊN")
        print("3. Chạy Performance Test (Đo đạc 100, 1.000, 10.000 bản ghi)")
        print("4. Quay lại menu chính")

        choice = input("Lựa chọn (1-4): ").strip()

        if choice in ("1", "2"):
            uniform = (choice == "1")
            print(f"Đang sinh 10.000 bản ghi giao dịch mẫu vào '{test_file}'...")
            try:
                manager.generate_test_data(10000, uniform, test_file)
                print(f"✨ Sinh dữ liệu mẫu thành công!")
                print(f"  - File lưu trữ: '{test_file}'")
                print(f"  - Phân bổ: {'ĐỀU THEO CHIỀU SÂU' if uniform else 'NGẪU NHIÊN'}")
                print(f"  - Kiểm tra chất lượng: Bạn có thể chọn mục 3 để chạy Performance Test ngay.")
            except ValueError as e:
                print(f"❌ Lỗi: {e}")
        elif choice == "3":
            print(f"Đang tiến hành đo đạc hiệu năng với dữ liệu trong '{test_file}'...")
            try:
                table_result = manager.test_performance(test_file)
                print("\n📊 BẢNG KẾT QUẢ KIỂM THỬ HIỆU NĂNG:")
                print(table_result)
                print("\n💡 Nhận xét kết quả:")
                print("  - Thời gian nạp dữ liệu (Load) tăng tuyến tính theo số bản ghi O(M).")
                print("  - Thời gian duyệt DFS tính tổng tiền cực kỳ nhanh nhờ đệ quy tối ưu O(N+M).")
                print("  - Tra cứu danh mục theo đường dẫn (Path) gần như HẰNG SỐ O(1) nhờ Bảng băm node_index.")
                print("  - Tìm kiếm theo từ khóa ghi chú tăng tuyến tính theo số lượng giao dịch O(M).")
            except Exception as e:
                print(f"❌ Không thể chạy kiểm thử hiệu năng: {e}")
                print("  (Hãy đảm bảo bạn đã sinh 10.000 dữ liệu mẫu ở mục 1 hoặc 2 trước).")
        elif choice == "4":
            break
        else:
            print("Lựa chọn không hợp lệ.")


def save_and_exit(manager: FinanceManager) -> None:
    """
    Tự động lưu toàn bộ dữ liệu ra CSV khi thoát.
    """
    try:
        manager.save_data(DATA_FILE)
        print(f"💾 Dữ liệu đã được lưu trữ an toàn vào file CSV '{DATA_FILE}'.")
        print("👋 Tạm biệt! Hẹn gặp lại bạn lần sau.")
    except Exception as error:
        print(f"❌ Lỗi nghiêm trọng khi lưu dữ liệu: {error}")
