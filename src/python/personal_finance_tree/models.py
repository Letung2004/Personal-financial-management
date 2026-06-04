from __future__ import annotations

from datetime import datetime
from typing import List, Optional, Union, Tuple


DateValue = Union[str, datetime]


class Transaction:
    """
    Lớp biểu diễn một giao dịch tài chính.
    """

    def __init__(self, amount: float, date: DateValue, note: str = "") -> None:
        if amount <= 0:
            raise ValueError("Số tiền phải lớn hơn 0.")

        # Chuẩn hóa và xác thực ngày tháng
        if isinstance(date, datetime):
            date_str = date.strftime("%Y-%m-%d")
        else:
            date_str = str(date).strip()

        try:
            dt = datetime.strptime(date_str, "%Y-%m-%d")
        except ValueError:
            raise ValueError(f"Ngày '{date_str}' không hợp lệ hoặc không đúng định dạng YYYY-MM-DD.")

        if dt > datetime.now():
            raise ValueError("Ngày giao dịch không được ở trong tương lai.")

        self.amount: float = amount
        self.date: str = date_str
        self.note: str = note

    def get_date_string(self) -> str:
        """
        Trả về chuỗi ngày YYYY-MM-DD.
        """
        return self.date

    def get_details(self) -> str:
        """
        Trả về chuỗi thông tin chi tiết của giao dịch.
        """
        return (
            f"Số tiền: {self.amount:,.2f}, "
            f"Ngày: {self.get_date_string()}, "
            f"Ghi chú: {self.note}"
        )

    def to_dict(self) -> dict:
        """
        Chuyển giao dịch thành dict (nếu cần tương thích ngược JSON).
        """
        return {
            "amount": self.amount,
            "date": self.get_date_string(),
            "note": self.note,
        }

    @classmethod
    def from_dict(cls, data: dict) -> Transaction:
        """
        Dựng lại đối tượng Transaction từ dict.
        """
        return cls(
            amount=float(data["amount"]),
            date=str(data["date"]),
            note=str(data.get("note", "")),
        )


class CategoryNode:
    """
    Lớp biểu diễn một nút danh mục trong cây tổng quát N-ary Tree.
    """

    def __init__(
        self,
        name: str,
        type: str,
        parent: Optional[CategoryNode] = None,
    ) -> None:
        if type not in ("Income", "Expense"):
            raise ValueError("type phải là 'Income' hoặc 'Expense'")

        self.name: str = name
        self.type: str = type
        self.parent: Optional[CategoryNode] = parent
        self.children: List[CategoryNode] = []
        self.transactions: List[Transaction] = []

    def add_child(self, child_node: CategoryNode) -> None:
        """
        Thêm một nút con và tự động gán parent của nút con là nút hiện tại.
        """
        child_node.parent = self
        self.children.append(child_node)

    def remove_child(self, name: str) -> bool:
        """
        Xóa nút con trực tiếp theo tên.
        Trả về True nếu xóa thành công, False nếu không tìm thấy.
        """
        for child in self.children:
            if child.name == name:
                child.parent = None
                self.children.remove(child)
                return True

        return False

    def find_child(self, name: str) -> Optional[CategoryNode]:
        """
        Tìm một nút con trực tiếp theo tên (không phân biệt hoa thường khi so sánh để tránh trùng lặp).
        """
        for child in self.children:
            if child.name.upper() == name.upper():
                return child

        return None

    def add_transaction(self, transaction: Transaction) -> None:
        """
        Thêm một giao dịch trực tiếp vào danh mục hiện tại.
        """
        self.transactions.append(transaction)

    def get_total_amount(self) -> float:
        """
        Tính tổng số tiền của nút hiện tại và tất cả nút con bằng DFS đệ quy.
        """
        total: float = sum(transaction.amount for transaction in self.transactions)

        for child in self.children:
            total += child.get_total_amount()

        return total

    def get_all_transactions_recursive(self, current_path: str) -> List[Tuple[str, Transaction]]:
        """
        Duyệt đệ quy (DFS) để lấy toàn bộ các giao dịch của nhánh kèm đường dẫn đầy đủ.
        """
        results: List[Tuple[str, Transaction]] = []
        for transaction in self.transactions:
            results.append((current_path, transaction))

        for child in self.children:
            child_path = f"{current_path}/{child.name}"
            results.extend(child.get_all_transactions_recursive(child_path))

        return results

    def to_dict(self) -> dict:
        """
        Chuyển toàn bộ nhánh cây thành dict (tương thích ngược).
        """
        return {
            "name": self.name,
            "type": self.type,
            "transactions": [
                transaction.to_dict()
                for transaction in self.transactions
            ],
            "children": [
                child.to_dict()
                for child in self.children
            ],
        }

    @classmethod
    def from_dict(
        cls,
        data: dict,
        parent: Optional[CategoryNode] = None,
    ) -> CategoryNode:
        """
        Dựng lại một nhánh cây từ dict (tương thích ngược).
        """
        node = cls(
            name=str(data["name"]),
            type=str(data["type"]),
            parent=parent,
        )

        node.transactions = [
            Transaction.from_dict(transaction_data)
            for transaction_data in data.get("transactions", [])
        ]

        for child_data in data.get("children", []):
            child_node = cls.from_dict(child_data, parent=node)
            node.children.append(child_node)

        return node
