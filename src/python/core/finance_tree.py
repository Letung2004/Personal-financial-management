from __future__ import annotations
from collections import deque
from datetime import date
from typing import Dict, List, Optional, Tuple

from models.category_node import CategoryNode
from models.transaction import Transaction


class FinanceTree:
    """Cây tài chính N-ary kết hợp HashMap để tối ưu tra cứu O(1)."""

    def __init__(self):
        self.root: CategoryNode = CategoryNode("Root", "Root", parent=None)
        self._node_map: Dict[str, CategoryNode] = {}
        self._initialize_default_structure()

    # =========================================================================
    # PHẦN NỘI BỘ: Đăng ký / Hủy đăng ký nút khỏi bảng băm
    # =========================================================================

    def _register_node(self, node: CategoryNode) -> None:
        path = node.get_path()
        self._node_map[path] = node
        for txn in node.transactions:
            txn.category_path = path

    def _unregister_subtree(self, node: CategoryNode) -> None:
        self._node_map.pop(node.get_path(), None)
        for child in node.children:
            self._unregister_subtree(child)

    def _re_register_subtree(self, node: CategoryNode) -> None:
        self._register_node(node)
        for child in node.children:
            self._re_register_subtree(child)

    def _initialize_default_structure(self) -> None:
        self._register_node(self.root)

        # --- Nhánh THU NHẬP ---
        income_root = CategoryNode("Thu nhập", "Income", self.root)
        self.root.children.append(income_root)
        self._register_node(income_root)
        for name in ["Lương", "Thưởng", "Đầu tư", "Thu nhập khác"]:
            node = CategoryNode(name, "Income", income_root)
            income_root.children.append(node)
            self._register_node(node)

        # --- Nhánh CHI TIÊU ---
        expense_root = CategoryNode("Chi tiêu", "Expense", self.root)
        self.root.children.append(expense_root)
        self._register_node(expense_root)

        default_expenses = {
            "Ăn uống": ["Đi chợ", "Ăn ngoài", "Cà phê"],
            "Di chuyển": ["Xăng xe", "Sửa xe", "Taxi/Grab"],
            "Nhà cửa": ["Tiền thuê", "Điện nước", "Internet"],
            "Sức khỏe": ["Thuốc", "Khám bệnh", "Thể thao"],
            "Giải trí": ["Phim ảnh", "Du lịch", "Mua sắm"],
            "Giáo dục": ["Học phí", "Sách vở"],
            "Chi tiêu khác": [],
        }

        for parent_name, child_names in default_expenses.items():
            parent_node = CategoryNode(parent_name, "Expense", expense_root)
            expense_root.children.append(parent_node)
            self._register_node(parent_node)
            for child_name in child_names:
                child_node = CategoryNode(child_name, "Expense", parent_node)
                parent_node.children.append(child_node)
                self._register_node(child_node)

    # =========================================================================
    # PHẦN 1: QUẢN LÝ NÚT CÂY
    # =========================================================================

    def insert_node(self, parent_path: str, new_name: str, category_type: str = None) -> Optional[CategoryNode]:
        parent = self.get_node_by_path(parent_path)
        if not parent:
            print(f"  [LỖI] Không tìm thấy danh mục cha: '{parent_path}'")
            return None

        actual_type = category_type or parent.category_type
        if actual_type == "Root":
            actual_type = "Expense"

        new_node = CategoryNode(new_name, actual_type, parent)
        success = parent.add_child(new_node)
        if success:
            self._register_node(new_node)
            print(f"  [OK] Đã thêm danh mục '{new_name}' vào '{parent.name}'.")
            return new_node
        return None

    def delete_node(self, node_path: str, mode: str = "CASCADE") -> bool:
        node = self.get_node_by_path(node_path)
        if not node:
            print(f"  [LỖI] Không tìm thấy danh mục: '{node_path}'")
            return False
        if node.parent is None or node.name == "Root":
            print("  [LỖI] Không thể xóa nút gốc.")
            return False

        parent = node.parent

        if mode.upper() == "CASCADE":
            self._unregister_subtree(node)
            parent.remove_child(node.name)
            print(f"  [OK] Đã xóa danh mục '{node.name}' (Cascade - và toàn bộ con cháu).")
            return True

        elif mode.upper() == "REPARENT":
            children_to_move = list(node.children)
            for child in children_to_move:
                self._unregister_subtree(child)
                child.parent = parent
                parent.children.append(child)
                self._re_register_subtree(child)

            if node.transactions:
                parent.transactions.extend(node.transactions)
                for txn in node.transactions:
                    txn.category_path = parent.get_path()
                node.transactions.clear()

            parent.remove_child(node.name)
            node.children.clear()
            self._node_map.pop(node_path, None)
            print(f"  [OK] Đã xóa danh mục '{node.name}' (Reparent - chuyển con và giao dịch lên '{parent.name}').")
            return True

        else:
            print(f"  [LỖI] Chế độ xóa không hợp lệ: '{mode}'. Phải là CASCADE hoặc REPARENT.")
            return False

    def rename_node(self, node_path: str, new_name: str) -> bool:
        node = self.get_node_by_path(node_path)
        if not node:
            print(f"  [LỖI] Không tìm thấy danh mục: '{node_path}'")
            return False
        if node.parent and node.parent.find_child_by_name(new_name):
            print(f"  [LỖI] Tên '{new_name}' đã tồn tại trong '{node.parent.name}'.")
            return False
        self._unregister_subtree(node)
        node.name = new_name
        self._re_register_subtree(node)
        print(f"  [OK] Đã đổi tên thành '{new_name}'.")
        return True

    # =========================================================================
    # PHẦN 2: THUẬT TOÁN DFS - TÍNH TỔNG TIỀN THEO NHÁNH (Đệ quy)
    # =========================================================================

    def calculate_total_dfs(self, node: CategoryNode = None) -> float:
        if node is None:
            node = self.root
        total = node.get_direct_total()
        for child in node.children:
            total += self.calculate_total_dfs(child)
        return total

    def calculate_total_by_path(self, path: str) -> float:
        node = self.get_node_by_path(path)
        if not node:
            print(f"  [LỖI] Không tìm thấy danh mục: '{path}'")
            return 0.0
        return self.calculate_total_dfs(node)

    def calculate_income_and_expense(self) -> Tuple[float, float]:
        income_node = self.get_node_by_path("Root/Thu nhập")
        expense_node = self.get_node_by_path("Root/Chi tiêu")
        total_income = self.calculate_total_dfs(income_node) if income_node else 0.0
        total_expense = self.calculate_total_dfs(expense_node) if expense_node else 0.0
        return total_income, total_expense

    # =========================================================================
    # PHẦN 3: THUẬT TOÁN TÌM KIẾM
    # =========================================================================

    def get_node_by_path(self, path: str) -> Optional[CategoryNode]:
        return self._node_map.get(path)

    def search_by_name(self, name: str) -> List[CategoryNode]:
        """BFS - tìm tất cả các nút có tên khớp."""
        results = []
        name_lower = name.strip().lower()
        queue = deque([self.root])
        while queue:
            current = queue.popleft()
            if current.name.lower() == name_lower:
                results.append(current)
            queue.extend(current.children)
        return results

    def search_by_name_dfs(self, name: str, start_node: CategoryNode = None) -> Optional[CategoryNode]:
        """DFS - tìm nút đầu tiên có tên khớp."""
        if start_node is None:
            start_node = self.root
        if start_node.name.lower() == name.strip().lower():
            return start_node
        for child in start_node.children:
            result = self.search_by_name_dfs(name, child)
            if result:
                return result
        return None

    def search_transactions_by_keyword(self, keyword: str) -> List[Transaction]:
        results = []
        keyword_lower = keyword.strip().lower()
        self._search_txn_recursive(self.root, keyword_lower, results)
        return results

    def _search_txn_recursive(self, node: CategoryNode, keyword: str, results: list) -> None:
        for txn in node.transactions:
            if keyword in txn.note.lower():
                results.append(txn)
        for child in node.children:
            self._search_txn_recursive(child, keyword, results)

    def search_transactions_by_date_range(self, start_date: date, end_date: date) -> List[Transaction]:
        results = []
        self._search_by_date_recursive(self.root, start_date, end_date, results)
        return results

    def _search_by_date_recursive(self, node: CategoryNode, start: date, end: date, results: list) -> None:
        for txn in node.transactions:
            if start <= txn.date <= end:
                results.append(txn)
        for child in node.children:
            self._search_by_date_recursive(child, start, end, results)

    # =========================================================================
    # PHẦN 4: PHÂN LOẠI GIAO DỊCH
    # =========================================================================

    def classify_and_add_transaction(self, txn: Transaction, category_path: str) -> bool:
        target_node = self.get_node_by_path(category_path)
        if not target_node:
            results = self.search_by_name(category_path)
            if results:
                target_node = results[0]
                print(f"  [THÔNG TIN] Tìm theo tên, dùng: '{target_node.get_path()}'")
            else:
                print(f"  [LỖI] Không tìm thấy danh mục: '{category_path}'")
                return False
        target_node.add_transaction(txn)
        return True

    # =========================================================================
    # PHẦN 5: DUYỆT VÀ HIỂN THỊ CÂY
    # =========================================================================

    def traverse_tree(self, node: CategoryNode = None, indent: int = 0) -> None:
        if node is None:
            node = self.root
            print("\n=======================================================")
            print("  SƠ ĐỒ CÂY DANH MỤC TÀI CHÍNH")
            print("=======================================================")

        prefix = "    " * indent
        total = self.calculate_total_dfs(node)
        txn_count = len(node.transactions)
        is_leaf_marker = " (*)" if (node.is_leaf() and txn_count > 0) else ""
        info = ""
        if txn_count > 0:
            info = f"[Tổng: {total:,.0f} VND, {txn_count} GD{is_leaf_marker}]"
        elif total > 0:
            info = f"[Tổng: {total:,.0f} VND]"

        print(f"{prefix}{'└── ' if indent > 0 else ''}📁 {node.name}  {info}")
        for child in node.children:
            self.traverse_tree(child, indent + 1)

    def get_all_transactions(self) -> List[Transaction]:
        all_txns: List[Transaction] = []
        self._collect_transactions_recursive(self.root, all_txns)
        all_txns.sort(key=lambda t: t.date, reverse=True)
        return all_txns

    def _collect_transactions_recursive(self, node: CategoryNode, result: list) -> None:
        result.extend(node.transactions)
        for child in node.children:
            self._collect_transactions_recursive(child, result)

    def get_node_count(self) -> int:
        return len(self._node_map)

    def get_all_leaf_paths(self) -> List[str]:
        paths = []
        self._get_leaf_paths_recursive(self.root, paths)
        return paths

    def _get_leaf_paths_recursive(self, node: CategoryNode, paths: list) -> None:
        if node.is_leaf():
            paths.append(node.get_path())
        for child in node.children:
            self._get_leaf_paths_recursive(child, paths)
