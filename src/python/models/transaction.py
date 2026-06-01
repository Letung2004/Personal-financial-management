from datetime import date


class Transaction:
    """Biểu diễn một giao dịch tài chính."""
    _id_counter = 0

    def __init__(self, amount: float, date: date, note: str = "", category_path: str = ""):
        if amount <= 0:
            raise ValueError(f"Số tiền giao dịch phải lớn hơn 0. Nhận được: {amount}")
        Transaction._id_counter += 1
        self.transaction_id: int = Transaction._id_counter
        self.amount: float = amount
        self.date: date = date
        self.note: str = note or ""
        self.category_path: str = category_path or ""

    @classmethod
    def reset_counter(cls):
        cls._id_counter = 0

    def get_details(self) -> str:
        return (
            f"  [ID: {self.transaction_id:4d}] "
            f"Ngày: {self.date.strftime('%d/%m/%Y')} | "
            f"Số tiền: {self.amount:>12,.0f} VND | "
            f"Danh mục: {self.category_path:<30s} | "
            f"Ghi chú: {self.note}"
        )

    def to_csv_row(self) -> str:
        safe_note = self.note.replace(",", ";")
        return f"{self.transaction_id},{self.amount:.2f},{self.date.strftime('%Y-%m-%d')},{self.category_path},{safe_note}"

    @classmethod
    def from_csv_row(cls, row: str) -> "Transaction":
        parts = row.split(",", 4)
        if len(parts) < 4:
            raise ValueError(f"Dòng CSV không hợp lệ: '{row}'")
        txn_id = int(parts[0].strip())
        amount = float(parts[1].strip())
        txn_date = date.fromisoformat(parts[2].strip())
        category_path = parts[3].strip()
        note = parts[4].strip().replace(";", ",") if len(parts) > 4 else ""

        txn = cls(amount, txn_date, note, category_path)
        txn.transaction_id = txn_id
        if txn_id > cls._id_counter:
            cls._id_counter = txn_id
        return txn

    def __repr__(self) -> str:
        return f"Transaction(id={self.transaction_id}, amount={self.amount}, date={self.date}, note='{self.note}')"

    def __eq__(self, other) -> bool:
        if not isinstance(other, Transaction):
            return False
        return self.transaction_id == other.transaction_id

    def __hash__(self) -> int:
        return hash(self.transaction_id)
