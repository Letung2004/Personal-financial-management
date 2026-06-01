from __future__ import annotations
from typing import List, Optional
from .transaction import Transaction

VALID_TYPES = ("Income", "Expense", "Root")


class CategoryNode:
    """Biểu diễn một nút danh mục trong cây tài chính phân cấp."""

    def __init__(self, name: str, category_type: str, parent: Optional["CategoryNode"] = None):
        if not name or not name.strip():
            raise ValueError("Tên danh mục không được để trống.")
        if category_type not in VALID_TYPES:
            raise ValueError(f"Loại danh mục không hợp lệ: '{category_type}'. Phải là một trong {VALID_TYPES}")

        self.name: str = name.strip()
        self.category_type: str = category_type
        self.parent: Optional[CategoryNode] = parent
        self.children: List[CategoryNode] = []
        self.transactions: List[Transaction] = []

    def add_child(self, child_node: "CategoryNode") -> bool:
        if self.find_child_by_name(child_node.name):
            print(f"  [!] Danh mục '{child_node.name}' đã tồn tại trong '{self.name}'.")
            return False
        child_node.parent = self
        self.children.append(child_node)
        return True

    def remove_child(self, name: str) -> Optional["CategoryNode"]:
        for i, child in enumerate(self.children):
            if child.name == name:
                removed = self.children.pop(i)
                removed.parent = None
                return removed
        print(f"  [!] Không tìm thấy danh mục '{name}' trong '{self.name}'.")
        return None

    def find_child_by_name(self, name: str) -> Optional["CategoryNode"]:
        for child in self.children:
            if child.name == name:
                return child
        return None

    def is_leaf(self) -> bool:
        return len(self.children) == 0

    def add_transaction(self, txn: Transaction) -> None:
        txn.category_path = self.get_path()
        self.transactions.append(txn)

    def get_direct_total(self) -> float:
        return sum(txn.amount for txn in self.transactions)

    def get_path(self) -> str:
        if self.parent is None:
            return self.name
        return f"{self.parent.get_path()}/{self.name}"

    def get_depth(self) -> int:
        if self.parent is None:
            return 0
        return 1 + self.parent.get_depth()

    def __repr__(self) -> str:
        return (
            f"CategoryNode(name='{self.name}', type='{self.category_type}', "
            f"children={len(self.children)}, txns={len(self.transactions)})"
        )
