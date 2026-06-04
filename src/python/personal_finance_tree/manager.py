from __future__ import annotations

import csv
import random
import time
from datetime import datetime, timedelta
from pathlib import Path
from typing import List, Optional, Tuple, Dict

from .models import CategoryNode, Transaction


class FinanceManager:
    """
    Lớp quản lý hai cây danh mục: Thu nhập và Chi tiêu.
    Tích hợp bảng băm tra cứu O(1), lưu trữ CSV, quản lý toàn diện và QA Performance Test.
    """

    def __init__(self) -> None:
        self.root_income: CategoryNode = CategoryNode("THU NHẬP", "Income")
        self.root_expense: CategoryNode = CategoryNode("CHI TIÊU", "Expense")
        self.node_index: Dict[str, CategoryNode] = {}
        self.rebuild_node_index()
        self.initialize_default_categories()

    def initialize_default_categories(self) -> None:
        """
        Khởi tạo cây danh mục mặc định theo đề tài (Section 7 của PDF).
        """
        # Thu nhập
        for sub in ["Lương", "Thưởng", "Đầu tư"]:
            self.add_category_by_path(f"THU NHẬP/{sub}")

        # Chi tiêu
        for path in [
            "CHI TIÊU/Ăn uống/Đi chợ",
            "CHI TIÊU/Ăn uống/Ăn ngoài",
            "CHI TIÊU/Di chuyển/Xăng xe",
            "CHI TIÊU/Di chuyển/Sửa xe",
            "CHI TIÊU/Nhà cửa/Tiền thuê",
            "CHI TIÊU/Nhà cửa/Điện nước"
        ]:
            self.add_category_by_path(path)

    def rebuild_node_index(self) -> None:
        """
        Duyệt cây DFS để dựng lại toàn bộ bảng băm node_index.
        Đảm bảo tra cứu O(1) và giải quyết triệt để lỗi đồng bộ chỉ mục (BUG-003).
        """
        self.node_index = {}
        self._rebuild_index_for_node(self.root_income, self.root_income.name)
        self._rebuild_index_for_node(self.root_expense, self.root_expense.name)

    def _rebuild_index_for_node(self, node: CategoryNode, current_path: str) -> None:
        self.node_index[current_path.upper()] = node
        for child in node.children:
            self._rebuild_index_for_node(child, f"{current_path}/{child.name}")

    def normalize_and_validate_path(self, path: str) -> List[str]:
        """
        Chuẩn hóa đường dẫn danh mục. Ánh xạ các biến thể ROOT viết tắt (THU -> THU NHẬP, CHI -> CHI TIÊU).
        """
        parts = [part.strip() for part in path.split("/") if part.strip()]
        if not parts:
            raise ValueError("Đường dẫn danh mục không được rỗng.")

        root_name = parts[0].upper()
        if root_name in ("THU NHẬP", "THU NHAP", "INCOME", "THU"):
            parts[0] = "THU NHẬP"
        elif root_name in ("CHI TIÊU", "CHI TIEU", "EXPENSE", "CHI"):
            parts[0] = "CHI TIÊU"
        else:
            raise ValueError("Đường dẫn bắt đầu phải là THU (THU NHẬP) hoặc CHI (CHI TIÊU).")

        return parts

    def add_category_by_path(self, category_path: str) -> CategoryNode:
        """
        Thêm danh mục trống theo đường dẫn. Nếu chưa có, tự động tạo mới dọc đường.
        Trả về nút danh mục cuối cùng.
        """
        parts = self.normalize_and_validate_path(category_path)
        current_node = self.root_income if parts[0] == "THU NHẬP" else self.root_expense
        category_type = current_node.type

        current_path = parts[0]
        for name in parts[1:]:
            current_path = f"{current_path}/{name}"
            child_node = current_node.find_child(name)
            if child_node is None:
                child_node = CategoryNode(name, category_type, parent=current_node)
                current_node.add_child(child_node)
            current_node = child_node

        self.rebuild_node_index()
        return current_node

    def add_transaction(self, transaction: Transaction, category_path: str) -> None:
        """
        Thêm giao dịch mới vào một đường dẫn danh mục (tự tạo danh mục nếu chưa tồn tại).
        """
        node = self.add_category_by_path(category_path)
        node.add_transaction(transaction)

    def delete_category(self, category_path: str) -> None:
        """
        Xóa danh mục theo đường dẫn (CASCADE xóa toàn bộ các nút con và giao dịch bên trong).
        Không cho phép xóa nút Gốc.
        """
        parts = self.normalize_and_validate_path(category_path)
        if len(parts) == 1:
            raise ValueError("Không thể xóa nút gốc 'THU NHẬP' hoặc 'CHI TIÊU'.")

        path_upper = "/".join(parts).upper()
        if path_upper not in self.node_index:
            raise ValueError(f"Không tìm thấy danh mục: {category_path}")

        node = self.node_index[path_upper]
        parent = node.parent
        if parent:
            parent.remove_child(node.name)

        self.rebuild_node_index()

    def rename_category(self, category_path: str, new_name: str) -> None:
        """
        Đổi tên một danh mục và đồng bộ lại toàn bộ bảng băm chỉ mục con.
        """
        new_name_clean = new_name.strip()
        if not new_name_clean:
            raise ValueError("Tên danh mục mới không được để trống.")
        if "/" in new_name_clean:
            raise ValueError("Tên danh mục không được chứa ký tự phân cấp '/'.")

        parts = self.normalize_and_validate_path(category_path)
        if len(parts) == 1:
            raise ValueError("Không thể đổi tên nút gốc 'THU NHẬP' hoặc 'CHI TIÊU'.")

        path_upper = "/".join(parts).upper()
        if path_upper not in self.node_index:
            raise ValueError(f"Không tìm thấy danh mục: {category_path}")

        node = self.node_index[path_upper]
        parent = node.parent
        if parent and parent.find_child(new_name_clean) is not None:
            raise ValueError(f"Tên danh mục '{new_name_clean}' đã tồn tại trong cùng cấp cha.")

        node.name = new_name_clean
        self.rebuild_node_index()

    def edit_transaction(self, category_path: str, transaction_idx: int, amount: float, date: str, note: str) -> None:
        """
        Sửa giao dịch trong một danh mục tại vị trí chỉ định.
        """
        parts = self.normalize_and_validate_path(category_path)
        path_upper = "/".join(parts).upper()
        if path_upper not in self.node_index:
            raise ValueError(f"Không tìm thấy danh mục: {category_path}")

        node = self.node_index[path_upper]
        if transaction_idx < 0 or transaction_idx >= len(node.transactions):
            raise ValueError("Vị trí giao dịch không hợp lệ.")

        # Thao tác tạo Transaction mới để kích hoạt cơ chế validation số tiền và ngày tháng
        updated_tx = Transaction(amount, date, note)
        node.transactions[transaction_idx] = updated_tx

    def delete_transaction(self, category_path: str, transaction_idx: int) -> None:
        """
        Xóa giao dịch tại danh mục và vị trí chỉ định.
        """
        parts = self.normalize_and_validate_path(category_path)
        path_upper = "/".join(parts).upper()
        if path_upper not in self.node_index:
            raise ValueError(f"Không tìm thấy danh mục: {category_path}")

        node = self.node_index[path_upper]
        if transaction_idx < 0 or transaction_idx >= len(node.transactions):
            raise ValueError("Vị trí giao dịch không hợp lệ.")

        node.transactions.pop(transaction_idx)

    def save_data(self, filename: str) -> None:
        """
        Lưu toàn bộ dữ liệu ra file CSV theo chuẩn của đề tài:
        YYYY-MM-DD,amount,CategoryPath,note
        """
        all_txs: List[Tuple[str, Transaction]] = []
        all_txs.extend(self.root_income.get_all_transactions_recursive(self.root_income.name))
        all_txs.extend(self.root_expense.get_all_transactions_recursive(self.root_expense.name))

        path = Path(filename)
        with path.open("w", encoding="utf-8", newline="") as f:
            writer = csv.writer(f)
            # Viết tiêu đề cột giống QA quy định
            writer.writerow(["date", "amount", "CategoryPath", "note"])
            for category_path, tx in all_txs:
                # Đổi tên ROOT THU NHẬP -> THU và CHI TIÊU -> CHI trong file lưu trữ
                save_path = category_path
                if save_path.startswith("THU NHẬP"):
                    save_path = save_path.replace("THU NHẬP", "THU", 1)
                elif save_path.startswith("CHI TIÊU"):
                    save_path = save_path.replace("CHI TIÊU", "CHI", 1)

                writer.writerow([tx.get_date_string(), f"{tx.amount:.2f}", save_path, tx.note])

    def load_data(self, filename: str) -> Tuple[bool, int, List[str]]:
        """
        Tải dữ liệu từ file CSV. 
        Đồng thời bắt lỗi chặt chẽ (Graceful Error Handling) tránh crash chương trình khi gặp dữ liệu lỗi.
        Trả về: (Thành công, Số dòng đã nạp, Danh sách cảnh báo lỗi)
        """
        path = Path(filename)
        if not path.exists():
            return False, 0, []

        warnings: List[str] = []
        loaded_count = 0

        # Làm rỗng cây trước khi nạp nhưng vẫn giữ lại các danh mục mặc định để tránh mất khung xương
        self.root_income = CategoryNode("THU NHẬP", "Income")
        self.root_expense = CategoryNode("CHI TIÊU", "Expense")
        self.rebuild_node_index()
        self.initialize_default_categories()

        try:
            with path.open("r", encoding="utf-8") as f:
                reader = csv.reader(f)
                header = next(reader, None)  # Bỏ qua dòng tiêu đề

                # Kiểm tra nếu là file trống
                if header is None:
                    return True, 0, []

                line_num = 1
                for row in reader:
                    line_num += 1
                    if not row:
                        continue  # Bỏ qua dòng trống

                    if len(row) < 3:
                        warnings.append(f"Dòng {line_num}: Thiếu cột thông tin (yêu cầu ít nhất 3 cột).")
                        continue

                    # Đọc các trường thông tin
                    date_val = row[0].strip()
                    amount_str = row[1].strip()
                    cat_path = row[2].strip()
                    note_val = row[3].strip() if len(row) > 3 else ""

                    # 1. Validate Số tiền
                    try:
                        amount = float(amount_str)
                    except ValueError:
                        warnings.append(f"Dòng {line_num}: Số tiền không hợp lệ ('{amount_str}').")
                        continue

                    # 2. Validate Đường dẫn danh mục
                    try:
                        normalized_parts = self.normalize_and_validate_path(cat_path)
                    except ValueError as e:
                        warnings.append(f"Dòng {line_num}: {e}")
                        continue

                    # 3. Khởi tạo đối tượng và Validate Ngày tháng
                    try:
                        tx = Transaction(amount=amount, date=date_val, note=note_val)
                    except ValueError as e:
                        warnings.append(f"Dòng {line_num}: {e}")
                        continue

                    # Nạp vào cây
                    self.add_transaction(tx, cat_path)
                    loaded_count += 1

        except Exception as e:
            warnings.append(f"Lỗi đọc file nghiêm trọng: {e}")
            return False, 0, warnings

        self.rebuild_node_index()
        return True, loaded_count, warnings

    def print_tree(self, root: CategoryNode, level: int = 0) -> None:
        """
        Hiển thị cây danh mục ra màn hình trực quan với thụt dòng.
        """
        indent = "  " * level
        print(f"{indent}- {root.name}: {root.get_total_amount():,.2f}")

        # In giao dịch trực tiếp tại nút
        for idx, transaction in enumerate(root.transactions):
            print(f"{indent}  [{idx}] + {transaction.get_details()}")

        # Duyệt DFS qua các nút con
        for child in root.children:
            self.print_tree(child, level + 1)

    def search_transactions(self, keyword: str) -> List[Tuple[str, Transaction]]:
        """
        Tìm kiếm giao dịch chứa từ khóa trong ghi chú (không phân biệt hoa thường).
        """
        results: List[Tuple[str, Transaction]] = []
        normalized_keyword = keyword.strip().lower()
        if not normalized_keyword:
            return results

        self._search_in_node(self.root_income, self.root_income.name, normalized_keyword, results)
        self._search_in_node(self.root_expense, self.root_expense.name, normalized_keyword, results)
        return results

    def _search_in_node(
        self,
        node: CategoryNode,
        path: str,
        keyword: str,
        results: List[Tuple[str, Transaction]],
    ) -> None:
        for transaction in node.transactions:
            if keyword in transaction.note.lower():
                results.append((path, transaction))

        for child in node.children:
            self._search_in_node(child, f"{path}/{child.name}", keyword, results)

    def filter_transactions(
        self,
        start_date: Optional[str] = None,
        end_date: Optional[str] = None,
        category_path: Optional[str] = None
    ) -> List[Tuple[str, Transaction]]:
        """
        Lọc giao dịch nâng cao: Theo danh mục nhánh, theo khoảng thời gian (Từ ngày -> Đến ngày).
        """
        source_txs: List[Tuple[str, Transaction]] = []

        if category_path:
            # Tra cứu nhanh O(1) danh mục bằng bảng băm
            parts = self.normalize_and_validate_path(category_path)
            path_upper = "/".join(parts).upper()
            if path_upper in self.node_index:
                target_node = self.node_index[path_upper]
                source_txs = target_node.get_all_transactions_recursive("/".join(parts))
        else:
            # Nếu không chỉ định danh mục, thu thập toàn bộ cây
            source_txs.extend(self.root_income.get_all_transactions_recursive(self.root_income.name))
            source_txs.extend(self.root_expense.get_all_transactions_recursive(self.root_expense.name))

        filtered_results: List[Tuple[str, Transaction]] = []
        for path, tx in source_txs:
            tx_date = tx.get_date_string()
            if start_date and tx_date < start_date:
                continue
            if end_date and tx_date > end_date:
                continue
            filtered_results.append((path, tx))

        return filtered_results

    def generate_report_data(
        self,
        filter_year: Optional[int] = None,
        filter_month: Optional[int] = None,
        filter_day: Optional[int] = None
    ) -> Tuple[float, float, float]:
        """
        Tính toán tổng thu, tổng chi và số dư theo khoảng lọc Ngày/Tháng/Năm tùy chọn.
        """
        all_income = self.root_income.get_all_transactions_recursive(self.root_income.name)
        all_expense = self.root_expense.get_all_transactions_recursive(self.root_expense.name)

        total_income = 0.0
        total_expense = 0.0

        for _, tx in all_income:
            dt = datetime.strptime(tx.get_date_string(), "%Y-%m-%d")
            if filter_year and dt.year != filter_year:
                continue
            if filter_month and dt.month != filter_month:
                continue
            if filter_day and dt.day != filter_day:
                continue
            total_income += tx.amount

        for _, tx in all_expense:
            dt = datetime.strptime(tx.get_date_string(), "%Y-%m-%d")
            if filter_year and dt.year != filter_year:
                continue
            if filter_month and dt.month != filter_month:
                continue
            if filter_day and dt.day != filter_day:
                continue
            total_expense += tx.amount

        return total_income, total_expense, total_income - total_expense

    def get_all_leaf_paths(self) -> List[str]:
        """
        Lấy tất cả đường dẫn của các nút lá trong cây (Mục 1.2 QA).
        Nút lá là nút không có children.
        """
        leaf_paths: List[str] = []
        self._collect_leaf_paths(self.root_income, self.root_income.name, leaf_paths)
        self._collect_leaf_paths(self.root_expense, self.root_expense.name, leaf_paths)
        return leaf_paths

    def _collect_leaf_paths(self, node: CategoryNode, current_path: str, leaf_paths: List[str]) -> None:
        if not node.children:
            leaf_paths.append(current_path)
            return
        for child in node.children:
            self._collect_leaf_paths(child, f"{current_path}/{child.name}", leaf_paths)

    def generate_test_data(self, count: int, uniform_distrib: bool, filename: str) -> None:
        """
        Sinh ngẫu nhiên số lượng bản ghi lớn phân bổ đều/không đều vào các nút lá (Mục 1 QA).
        Ghi trực tiếp ra file CSV chuẩn.
        """
        leaf_paths = self.get_all_leaf_paths()
        if not leaf_paths:
            raise ValueError("Không có nút lá nào trong hệ thống danh mục để sinh giao dịch mẫu.")

        # Định nghĩa khoảng ngày sinh ngẫu nhiên trong năm 2025
        start_date = datetime(2025, 1, 1)

        path = Path(filename)
        with path.open("w", encoding="utf-8", newline="") as f:
            writer = csv.writer(f)
            writer.writerow(["date", "amount", "CategoryPath", "note"])

            for i in range(count):
                # 1. Chọn đường dẫn danh mục theo quy tắc phân bổ
                if uniform_distrib:
                    # Để đảm bảo phân bổ đều toán học 100%, ta xoay vòng qua danh sách nút lá
                    cat_path = leaf_paths[i % len(leaf_paths)]
                else:
                    # Phân bổ không đều ngẫu nhiên
                    cat_path = random.choice(leaf_paths)

                # Viết tắt ROOT THU NHẬP -> THU và CHI TIÊU -> CHI
                save_path = cat_path
                if save_path.startswith("THU NHẬP"):
                    save_path = save_path.replace("THU NHẬP", "THU", 1)
                elif save_path.startswith("CHI TIÊU"):
                    save_path = save_path.replace("CHI TIÊU", "CHI", 1)

                # 2. Sinh ngẫu nhiên số tiền (từ 10,000đ đến 5,000,000đ)
                amount = round(random.uniform(10000, 5000000), -3)

                # 3. Sinh ngày ngẫu nhiên
                random_days = random.randint(0, 364)
                tx_date = (start_date + timedelta(days=random_days)).strftime("%Y-%m-%d")

                note = f"Giao dịch tự động #{i+1}"
                writer.writerow([tx_date, f"{amount:.2f}", save_path, note])

    def test_performance(self, filename: str) -> str:
        """
        Chạy kiểm thử hiệu năng QA/QC chính thức (Mục 2 QA).
        Đo đạc 4 chỉ số trên các kích thước 100, 1.000 và 10.000 bản ghi.
        Trả về bảng kết quả so sánh dạng chuỗi ASCII.
        """
        path = Path(filename)
        if not path.exists():
            raise FileNotFoundError(f"Vui lòng tạo dữ liệu mẫu trước. Không tìm thấy file {filename}")

        # Đọc toàn bộ các dòng hợp lệ từ file
        records: List[Tuple[str, float, str, str]] = []
        with path.open("r", encoding="utf-8") as f:
            reader = csv.reader(f)
            next(reader, None)  # Bỏ qua tiêu đề
            for row in reader:
                if len(row) >= 3:
                    records.append((row[0].strip(), float(row[1].strip()), row[2].strip(), row[3].strip() if len(row) > 3 else ""))

        sizes = [100, 1000, 10000]
        results_rows = []

        # Lưu trạng thái cây hiện tại để khôi phục sau kiểm thử
        original_income = self.root_income
        original_expense = self.root_expense

        for size in sizes:
            # Chỉ lấy số lượng bản ghi tương ứng
            subset = records[:size]
            if len(subset) < size:
                # Nếu file không đủ bản ghi, sinh tạm thời
                continue

            # 1. Đo thời gian tải dữ liệu (Load CSV & Dựng cây)
            # Tạo file tạm cho kích thước tương ứng
            temp_filename = f"temp_perf_{size}.csv"
            temp_path = Path(temp_filename)
            with temp_path.open("w", encoding="utf-8", newline="") as tf:
                tw = csv.writer(tf)
                tw.writerow(["date", "amount", "CategoryPath", "note"])
                for r in subset:
                    tw.writerow(r)

            t_start = time.perf_counter()
            self.load_data(temp_filename)
            t_load = (time.perf_counter() - t_start) * 1000  # Đổi sang ms

            # Xóa file tạm
            try:
                temp_path.unlink()
            except OSError:
                pass

            # 2. Đo thời gian tính tổng DFS (Duyệt toàn bộ cây)
            t_start = time.perf_counter()
            _ = self.root_income.get_total_amount()
            _ = self.root_expense.get_total_amount()
            t_dfs = (time.perf_counter() - t_start) * 1000  # ms

            # 3. Đo thời gian tìm kiếm danh mục O(1) qua node_index (chạy 100 lần lấy trung bình)
            # Chọn ngẫu nhiên một đường dẫn lá
            leaf_paths = self.get_all_leaf_paths()
            test_path = random.choice(leaf_paths).upper() if leaf_paths else "CHI TIÊU/ĂN UỐNG"
            
            t_start = time.perf_counter()
            for _ in range(100):
                _ = self.node_index.get(test_path)
            t_path = ((time.perf_counter() - t_start) * 1000) / 100  # ms trung bình

            # 4. Đo thời gian tìm kiếm từ khóa ghi chú O(M) (chạy 10 lần lấy trung bình)
            t_start = time.perf_counter()
            for _ in range(10):
                _ = self.search_transactions("Giao dịch tự động #50")
            t_search = ((time.perf_counter() - t_start) * 1000) / 10  # ms trung bình

            results_rows.append((size, t_load, t_dfs, t_path, t_search))

        # Khôi phục dữ liệu ban đầu
        self.root_income = original_income
        self.root_expense = original_expense
        self.rebuild_node_index()

        # Tạo bảng kết quả đẹp mắt
        output = []
        output.append("=" * 100)
        output.append(f"{'Số lượng bản ghi':<18} | {'Thời gian Load (ms)':<22} | {'Thời gian DFS (ms)':<20} | {'Tìm theo Path O(1) (ms)':<25} | {'Tìm Từ khóa O(M) (ms)':<20}")
        output.append("-" * 100)
        for r in results_rows:
            output.append(f"{r[0]:<18} | {r[1]:<22.4f} | {r[2]:<20.4f} | {r[3]:<25.6f} | {r[4]:<20.4f}")
        output.append("=" * 100)

        return "\n".join(output)
