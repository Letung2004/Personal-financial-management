"""
Demo Core Logic (Python) - Quản lý Tài chính Cá nhân
Môn: Cấu trúc dữ liệu & Giải thuật
"""
import sys
import os
from datetime import date

# Đảm bảo stdout dùng UTF-8 khi chạy trên Windows
if sys.stdout.encoding and sys.stdout.encoding.lower() != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

# Thêm thư mục hiện tại vào sys.path để import package
sys.path.insert(0, os.path.dirname(__file__))

from core.finance_manager import FinanceManager


def separator(title: str = ""):
    print("\n============================================================")
    if title:
        print(f"  🔷 {title}")
        print("============================================================")


def demo_1_tree_structure():
    separator("DEMO 1: CẤU TRÚC CÂY DANH MỤC MẶC ĐỊNH")
    manager = FinanceManager()
    manager.list_categories()
    print(f"\n  Tổng số danh mục: {manager.tree.get_node_count()}")


def demo_2_insert_delete():
    separator("DEMO 2: THÊM VÀ XÓA DANH MỤC (CASCADE VS REPARENT)")
    manager = FinanceManager()

    print("\n  [+] Thêm danh mục mới:")
    manager.add_category("Root/Chi tiêu/Ăn uống", "Đặt đồ ăn online", "Expense")
    manager.add_category("Root/Thu nhập", "Freelance", "Income")
    manager.add_category("Root/Chi tiêu", "Làm đẹp", "Expense")
    manager.add_category("Root/Chi tiêu/Làm đẹp", "Cắt tóc", "Expense")
    manager.add_category("Root/Chi tiêu/Làm đẹp", "Mỹ phẩm", "Expense")

    print("\n  [!] Thử thêm tên đã tồn tại:")
    manager.add_category("Root/Chi tiêu/Ăn uống", "Đi chợ", "Expense")

    # Demo xóa CASCADE
    manager.add_category("Root/Chi tiêu", "Mua sắm xa xỉ", "Expense")
    manager.add_category("Root/Chi tiêu/Mua sắm xa xỉ", "Đồng hồ hiệu", "Expense")
    manager.add_category("Root/Chi tiêu/Mua sắm xa xỉ", "Túi xách hiệu", "Expense")
    print("\n  [-] Xóa CASCADE 'Mua sắm xa xỉ':")
    manager.remove_category("Root/Chi tiêu/Mua sắm xa xỉ", "CASCADE")
    manager.list_categories()

    # Demo xóa REPARENT
    print("\n  [-] Xóa REPARENT 'Làm đẹp' (chuyển 'Cắt tóc', 'Mỹ phẩm' lên 'Chi tiêu'):")
    manager.remove_category("Root/Chi tiêu/Làm đẹp", "REPARENT")
    manager.list_categories()


def demo_3_add_transactions():
    separator("DEMO 3: THÊM GIAO DỊCH VÀ PHÂN LOẠI")
    manager = FinanceManager()

    print("\n  [+] Giao dịch Thu nhập:")
    manager.add_transaction(8_500_000, date(2026, 5, 1), "Lương tháng 5", "Root/Thu nhập/Lương")
    manager.add_transaction(2_000_000, date(2026, 5, 10), "Thưởng dự án", "Root/Thu nhập/Thưởng")
    manager.add_transaction(500_000, date(2026, 5, 15), "Tiền lãi gửi tiết kiệm", "Root/Thu nhập/Đầu tư")

    print("\n  [+] Giao dịch Chi tiêu:")
    manager.add_transaction(150_000, date(2026, 5, 2), "Đi chợ sáng thứ 2", "Root/Chi tiêu/Ăn uống/Đi chợ")
    manager.add_transaction(85_000, date(2026, 5, 3), "Phở bò buổi sáng", "Root/Chi tiêu/Ăn uống/Ăn ngoài")
    manager.add_transaction(45_000, date(2026, 5, 4), "Cà phê sữa đá", "Root/Chi tiêu/Ăn uống/Cà phê")
    manager.add_transaction(250_000, date(2026, 5, 5), "Xăng xe đầy bình", "Root/Chi tiêu/Di chuyển/Xăng xe")
    manager.add_transaction(3_200_000, date(2026, 5, 1), "Tiền thuê nhà tháng 5", "Root/Chi tiêu/Nhà cửa/Tiền thuê")
    manager.add_transaction(320_000, date(2026, 5, 5), "Tiền điện tháng 5", "Root/Chi tiêu/Nhà cửa/Điện nước")
    manager.add_transaction(180_000, date(2026, 5, 6), "Đi chợ chiều thứ 5", "Root/Chi tiêu/Ăn uống/Đi chợ")

    print("\n  [!] Thử thêm giao dịch không hợp lệ (tiền âm):")
    manager.add_transaction(-500_000, date(2026, 5, 7), "Giao dịch lỗi", "Root/Chi tiêu/Ăn uống")

    print("\n  --- Cây danh mục sau khi phân loại giao dịch ---")
    manager.list_categories()


def demo_4_dfs_calculation():
    separator("DEMO 4: THUẬT TOÁN DFS - TÍNH TỔNG TIỀN THEO NHÁNH")
    manager = FinanceManager()

    manager.add_transaction(8_500_000, date(2026, 5, 1), "Lương tháng 5", "Root/Thu nhập/Lương")
    manager.add_transaction(2_000_000, date(2026, 5, 10), "Thưởng dự án", "Root/Thu nhập/Thưởng")
    manager.add_transaction(150_000, date(2026, 5, 2), "Đi chợ sáng", "Root/Chi tiêu/Ăn uống/Đi chợ")
    manager.add_transaction(85_000, date(2026, 5, 3), "Phở bò", "Root/Chi tiêu/Ăn uống/Ăn ngoài")
    manager.add_transaction(45_000, date(2026, 5, 4), "Cà phê", "Root/Chi tiêu/Ăn uống/Cà phê")
    manager.add_transaction(250_000, date(2026, 5, 5), "Xăng xe", "Root/Chi tiêu/Di chuyển/Xăng xe")
    manager.add_transaction(3_200_000, date(2026, 5, 1), "Tiền nhà", "Root/Chi tiêu/Nhà cửa/Tiền thuê")

    print("\n  📊 Kết quả tính tổng bằng DFS:")
    total_food = manager.tree.calculate_total_by_path("Root/Chi tiêu/Ăn uống")
    print(f"  ├─ Tổng 'Ăn uống': 150,000 + 85,000 + 45,000 = {total_food:,.0f} VND  ✓")
    total_transport = manager.tree.calculate_total_by_path("Root/Chi tiêu/Di chuyển")
    print(f"  ├─ Tổng 'Di chuyển': {total_transport:,.0f} VND")
    total_expense = manager.tree.calculate_total_by_path("Root/Chi tiêu")
    print(f"  ├─ Tổng 'Chi tiêu' (DFS đệ quy): {total_expense:,.0f} VND")
    total_income = manager.tree.calculate_total_by_path("Root/Thu nhập")
    print(f"  ├─ Tổng 'Thu nhập': {total_income:,.0f} VND")
    print(f"  └─ Số dư = {total_income:,.0f} - {total_expense:,.0f} = {total_income - total_expense:,.0f} VND")


def demo_5_search():
    separator("DEMO 5: THUẬT TOÁN TÌM KIẾM")
    manager = FinanceManager()

    manager.add_transaction(150_000, date(2026, 5, 2), "Đi chợ sáng thứ 2", "Root/Chi tiêu/Ăn uống/Đi chợ")
    manager.add_transaction(180_000, date(2026, 5, 7), "Đi chợ chiều thứ 6", "Root/Chi tiêu/Ăn uống/Đi chợ")
    manager.add_transaction(85_000, date(2026, 5, 3), "Ăn phở bò buổi sáng", "Root/Chi tiêu/Ăn uống/Ăn ngoài")
    manager.add_transaction(250_000, date(2026, 5, 5), "Đổ xăng đầy bình", "Root/Chi tiêu/Di chuyển/Xăng xe")
    manager.add_transaction(8_500_000, date(2026, 5, 1), "Lương tháng 5 công ty ABC", "Root/Thu nhập/Lương")

    print("\n  [1] Tìm danh mục theo tên 'Đi chợ' (BFS):")
    for node in manager.tree.search_by_name("Đi chợ"):
        print(f"      ✓ Tìm thấy: '{node.get_path()}'")

    print("\n  [2] Tìm theo đường dẫn (O(1) - Hash Map):")
    node = manager.tree.get_node_by_path("Root/Chi tiêu/Ăn uống/Cà phê")
    print(f"      ✓ Tìm 'Root/Chi tiêu/Ăn uống/Cà phê': {'Tìm thấy' if node else 'Không tìm thấy'}")

    print("\n  [3] Tìm giao dịch chứa từ khóa 'chợ':")
    for txn in manager.search_transactions("chợ"):
        print(f"      ✓ {txn.get_details()}")

    print(f"\n  [4] Lọc giao dịch từ 01/05 đến 04/05:")
    for txn in manager.filter_by_date(date(2026, 5, 1), date(2026, 5, 4)):
        print(f"      ✓ {txn.get_details()}")


def demo_6_report():
    separator("DEMO 6: BÁO CÁO TỔNG QUAN TÀI CHÍNH")
    manager = FinanceManager()

    manager.add_transaction(8_500_000, date(2026, 5, 1), "Lương tháng 5", "Root/Thu nhập/Lương")
    manager.add_transaction(2_000_000, date(2026, 5, 10), "Thưởng quý 1", "Root/Thu nhập/Thưởng")
    manager.add_transaction(500_000, date(2026, 5, 15), "Lãi tiết kiệm", "Root/Thu nhập/Đầu tư")
    manager.add_transaction(150_000, date(2026, 5, 2), "Đi chợ sáng", "Root/Chi tiêu/Ăn uống/Đi chợ")
    manager.add_transaction(85_000, date(2026, 5, 3), "Phở buổi sáng", "Root/Chi tiêu/Ăn uống/Ăn ngoài")
    manager.add_transaction(45_000, date(2026, 5, 4), "Cà phê sữa đá", "Root/Chi tiêu/Ăn uống/Cà phê")
    manager.add_transaction(180_000, date(2026, 5, 7), "Đi chợ chiều", "Root/Chi tiêu/Ăn uống/Đi chợ")
    manager.add_transaction(250_000, date(2026, 5, 5), "Xăng xe đầy bình", "Root/Chi tiêu/Di chuyển/Xăng xe")
    manager.add_transaction(3_200_000, date(2026, 5, 1), "Tiền thuê nhà T5", "Root/Chi tiêu/Nhà cửa/Tiền thuê")
    manager.add_transaction(320_000, date(2026, 5, 5), "Hóa đơn điện T5", "Root/Chi tiêu/Nhà cửa/Điện nước")
    manager.add_transaction(180_000, date(2026, 5, 6), "Hóa đơn internet", "Root/Chi tiêu/Nhà cửa/Internet")
    manager.add_transaction(50_000, date(2026, 5, 8), "Thuốc cảm cúm", "Root/Chi tiêu/Sức khỏe/Thuốc")

    manager.generate_report()
    manager.get_category_summary("Root/Chi tiêu/Ăn uống")


def demo_7_save_load():
    separator("DEMO 7: LƯU VÀ TẢI DỮ LIỆU (save_data / load_data)")
    DATA_FILE = "demo_data.csv"

    print("\n  [Bước 1] Tạo dữ liệu và lưu xuống file:")
    manager1 = FinanceManager(DATA_FILE)
    manager1.add_transaction(8_500_000, date(2026, 5, 1), "Lương tháng 5", "Root/Thu nhập/Lương")
    manager1.add_transaction(150_000, date(2026, 5, 2), "Đi chợ", "Root/Chi tiêu/Ăn uống/Đi chợ")
    manager1.add_category("Root/Chi tiêu/Ăn uống", "Bữa trưa công sở", "Expense")
    manager1.add_transaction(75_000, date(2026, 5, 3), "Cơm trưa văn phòng", "Root/Chi tiêu/Ăn uống/Bữa trưa công sở")

    total_before = manager1.tree.calculate_total_by_path("Root/Chi tiêu")
    print(f"\n  Tổng chi tiêu TRƯỚC khi lưu: {total_before:,.0f} VND")
    manager1.save_data()

    print("\n  [Bước 2] Khởi tạo manager mới và tải lại từ file:")
    manager2 = FinanceManager(DATA_FILE)
    manager2.load_data()

    total_after = manager2.tree.calculate_total_by_path("Root/Chi tiêu")
    print(f"\n  Tổng chi tiêu SAU khi tải lại: {total_after:,.0f} VND")
    match = "✅ DỮ LIỆU KHỚP NHAU!" if abs(total_before - total_after) < 0.01 else "❌ DỮ LIỆU KHÔNG KHỚP!"
    print(f"\n  {match}")

    if os.path.exists(DATA_FILE):
        os.remove(DATA_FILE)


if __name__ == "__main__":
    print("╔══════════════════════════════════════════════════════════╗")
    print("║  DEMO CORE LOGIC (PYTHON) - QUẢN LÝ TÀI CHÍNH CÁ NHÂN   ║")
    print("║  Môn: Cấu trúc dữ liệu & Giải thuật                      ║")
    print("╚══════════════════════════════════════════════════════════╝")

    demo_1_tree_structure()
    input("\n  [Nhấn ENTER để tiếp tục...]")

    demo_2_insert_delete()
    input("\n  [Nhấn ENTER để tiếp tục...]")

    demo_3_add_transactions()
    input("\n  [Nhấn ENTER để tiếp tục...]")

    demo_4_dfs_calculation()
    input("\n  [Nhấn ENTER để tiếp tục...]")

    demo_5_search()
    input("\n  [Nhấn ENTER để tiếp tục...]")

    demo_6_report()
    input("\n  [Nhấn ENTER để tiếp tục...]")

    demo_7_save_load()

    print("\n\n  ✅ HOÀN THÀNH TOÀN BỘ DEMO CORE LOGIC (PYTHON)!")
    print("  Tất cả các module đã được kiểm tra thành công.\n")
