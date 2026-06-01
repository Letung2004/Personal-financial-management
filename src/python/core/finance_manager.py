from __future__ import annotations
import os
from datetime import date, datetime
from typing import List, Optional

from core.finance_tree import FinanceTree
from models.category_node import CategoryNode
from models.transaction import Transaction


class FinanceManager:
    """Lớp điều phối: kết nối UI/File I/O với FinanceTree."""

    DEFAULT_DATA_FILE = "data.csv"
    SEPARATOR = "---TREE---"

    def __init__(self, data_file: str = None):
        self.tree = FinanceTree()
        self.data_file = data_file or self.DEFAULT_DATA_FILE

    # =========================================================================
    # PHẦN 1: QUẢN LÝ DANH MỤC
    # =========================================================================

    def add_category(self, parent_path: str, name: str, category_type: str = None) -> bool:
        result = self.tree.insert_node(parent_path, name, category_type)
        return result is not None

    def remove_category(self, path: str, mode: str = "CASCADE") -> bool:
        return self.tree.delete_node(path, mode)

    def list_categories(self) -> None:
        self.tree.traverse_tree()

    def list_leaf_categories(self) -> List[str]:
        return self.tree.get_all_leaf_paths()

    # =========================================================================
    # PHẦN 2: QUẢN LÝ GIAO DỊCH
    # =========================================================================

    def add_transaction(self, amount: float, txn_date: date, note: str, category_path: str) -> bool:
        try:
            txn = Transaction(amount, txn_date, note, category_path)
            success = self.tree.classify_and_add_transaction(txn, category_path)
            if success:
                print(f"  [OK] Đã thêm giao dịch: {amount:,.0f} VND vào '{category_path}'.")
            return success
        except ValueError as e:
            print(f"  [LỖI] {e}")
            return False

    def get_all_transactions(self, limit: int = 0) -> List[Transaction]:
        all_txns = self.tree.get_all_transactions()
        return all_txns[:limit] if limit > 0 else all_txns

    def search_transactions(self, keyword: str) -> List[Transaction]:
        return self.tree.search_transactions_by_keyword(keyword)

    def filter_by_date(self, start_date: date, end_date: date) -> List[Transaction]:
        return self.tree.search_transactions_by_date_range(start_date, end_date)

    # =========================================================================
    # PHẦN 3: BÁO CÁO VÀ THỐNG KÊ
    # =========================================================================

    def generate_report(self) -> None:
        print("\n=======================================================")
        print("  BÁO CÁO TỔNG QUAN TÀI CHÍNH")
        print("=======================================================")

        total_income, total_expense = self.tree.calculate_income_and_expense()
        balance = total_income - total_expense

        print(f"  📈 Tổng Thu nhập  : {total_income:>15,.0f} VND")
        print(f"  📉 Tổng Chi tiêu  : {total_expense:>15,.0f} VND")
        print("-------------------------------------------------------")
        balance_symbol = "✅" if balance >= 0 else "❌"
        print(f"  {balance_symbol} Số dư còn lại  : {balance:>15,.0f} VND")

        print("\n  --- Chi tiết theo danh mục chính ---")
        income_node = self.tree.get_node_by_path("Root/Thu nhập")
        expense_node = self.tree.get_node_by_path("Root/Chi tiêu")

        if income_node:
            print(f"\n  📁 Thu nhập ({total_income:,.0f} VND):")
            for child in income_node.children:
                amount = self.tree.calculate_total_dfs(child)
                if amount > 0:
                    print(f"     ├─ {child.name:<20s}: {amount:>12,.0f} VND")

        if expense_node:
            print(f"\n  📁 Chi tiêu ({total_expense:,.0f} VND):")
            for child in expense_node.children:
                amount = self.tree.calculate_total_dfs(child)
                if amount > 0:
                    pct = (amount / total_expense * 100) if total_expense > 0 else 0
                    print(f"     ├─ {child.name:<20s}: {amount:>12,.0f} VND  ({pct:.1f}%)")

        total_txns = len(self.tree.get_all_transactions())
        print(f"\n  📊 Tổng số giao dịch: {total_txns}")
        print(f"  🌳 Tổng số danh mục : {self.tree.get_node_count()}")
        print("=======================================================")

    def get_category_summary(self, category_path: str) -> None:
        node = self.tree.get_node_by_path(category_path)
        if not node:
            print(f"  [LỖI] Không tìm thấy: '{category_path}'")
            return

        total = self.tree.calculate_total_dfs(node)
        print(f"\n  📁 Danh mục: {node.get_path()}")
        print(f"  Tổng tiền (bao gồm cả con): {total:,.0f} VND")
        print(f"  Số danh mục con trực tiếp : {len(node.children)}")
        print(f"  Số giao dịch trực tiếp    : {len(node.transactions)}")

        if node.transactions:
            print("\n  --- Các giao dịch trực tiếp ---")
            for txn in sorted(node.transactions, key=lambda t: t.date, reverse=True):
                print(txn.get_details())

    # =========================================================================
    # PHẦN 4: LƯU TRỮ DỮ LIỆU (File I/O - CSV)
    # =========================================================================

    def save_data(self, filename: str = None) -> bool:
        filename = filename or self.data_file
        try:
            with open(filename, "w", encoding="utf-8") as f:
                f.write(f"# FinanceManager Data File - Saved: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
                f.write("# Version: 1.0\n")
                f.write(f"# {self.SEPARATOR}\n")

                self._save_tree_structure(f, self.tree.root)

                f.write("---TRANSACTIONS---\n")
                all_txns = sorted(self.tree.get_all_transactions(), key=lambda t: t.transaction_id)
                for txn in all_txns:
                    f.write(txn.to_csv_row() + "\n")

            print(f"  [OK] Đã lưu {len(all_txns)} giao dịch vào file '{filename}'.")
            return True
        except IOError as e:
            print(f"  [LỖI] Không thể ghi file: {e}")
            return False

    def _save_tree_structure(self, f, node: CategoryNode) -> None:
        if node.name != "Root":
            f.write(f"{node.get_path()},{node.category_type}\n")
        for child in node.children:
            self._save_tree_structure(f, child)

    def load_data(self, filename: str = None) -> bool:
        filename = filename or self.data_file
        if not os.path.exists(filename):
            print(f"  [THÔNG TIN] File '{filename}' chưa tồn tại. Bắt đầu với dữ liệu mới.")
            return False

        Transaction.reset_counter()
        in_transactions_section = False
        nodes_created = 0
        txns_loaded = 0

        try:
            with open(filename, "r", encoding="utf-8") as f:
                for line_num, line in enumerate(f, 1):
                    line = line.strip()
                    if not line or line.startswith("#"):
                        continue

                    if line == "---TRANSACTIONS---":
                        in_transactions_section = True
                        continue

                    if not in_transactions_section:
                        try:
                            last_comma = line.rfind(",")
                            if last_comma == -1:
                                print(f"  [WARN] Line {line_num}: invalid path format (missing comma)")
                                continue
                            path = line[:last_comma].strip()
                            cat_type = line[last_comma + 1:].strip()

                            if not path.startswith("Root/"):
                                print(f"  [WARN] Line {line_num}: invalid path '{path}'")
                                continue

                            if not self.tree.get_node_by_path(path):
                                last_slash = path.rfind("/")
                                parent_path = path[:last_slash]
                                node_name = path[last_slash + 1:]
                                if self.tree.insert_node(parent_path, node_name, cat_type):
                                    nodes_created += 1
                        except Exception as e:
                            print(f"  [WARN] Lỗi dòng {line_num} (cây): {e}")
                    else:
                        try:
                            txn = Transaction.from_csv_row(line)
                            if "/" not in txn.category_path:
                                print(f"  [WARN] Line {line_num}: invalid path '{txn.category_path}'")
                                continue
                            if self.tree.classify_and_add_transaction(txn, txn.category_path):
                                txns_loaded += 1
                            else:
                                print(f"  [WARN] Line {line_num}: could not classify transaction into '{txn.category_path}'")
                        except Exception as e:
                            print(f"  [WARN] Lỗi dòng {line_num} (giao dịch): {e}")

            print(f"  [OK] Đã tải: {nodes_created} danh mục mới, {txns_loaded} giao dịch từ '{filename}'.")
            return True
        except IOError as e:
            print(f"  [LỖI] Không thể đọc file: {e}")
            return False
