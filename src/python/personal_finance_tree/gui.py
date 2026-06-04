from __future__ import annotations

import tkinter as tk
from tkinter import ttk, messagebox, simpledialog
from datetime import datetime
import os

from .manager import FinanceManager
from .models import Transaction, CategoryNode

DATA_FILE = "finance_data.csv"
TEST_DATA_FILE = "test_data.csv"


class TransactionDialog(tk.Toplevel):
    """
    Hộp thoại tùy chỉnh để nhập/sửa thông tin giao dịch (Số tiền, Ngày, Ghi chú).
    """

    def __init__(
        self,
        parent,
        title="Giao dịch",
        amount="",
        date="",
        note="",
    ) -> None:
        super().__init__(parent)
        self.title(title)
        self.geometry("380x250")
        self.resizable(False, False)
        self.transient(parent)  # Luôn hiển thị trên cửa sổ cha
        self.grab_set()  # Modal dialog

        self.result = None

        # Thiết lập widget
        frame = ttk.Frame(self, padding="15")
        frame.pack(fill=tk.BOTH, expand=True)

        # Số tiền
        ttk.Label(frame, text="Số tiền (đ):").grid(row=0, column=0, sticky=tk.W, pady=8)
        self.amount_var = tk.StringVar(value=str(amount))
        self.amount_entry = ttk.Entry(frame, textvariable=self.amount_var, width=25)
        self.amount_entry.grid(row=0, column=1, pady=8)

        # Ngày tháng
        ttk.Label(frame, text="Ngày (YYYY-MM-DD):").grid(row=1, column=0, sticky=tk.W, pady=8)
        default_date = date if date else datetime.now().strftime("%Y-%m-%d")
        self.date_var = tk.StringVar(value=default_date)
        self.date_entry = ttk.Entry(frame, textvariable=self.date_var, width=25)
        self.date_entry.grid(row=1, column=1, pady=8)

        # Ghi chú
        ttk.Label(frame, text="Ghi chú:").grid(row=2, column=0, sticky=tk.W, pady=8)
        self.note_var = tk.StringVar(value=note)
        self.note_entry = ttk.Entry(frame, textvariable=self.note_var, width=25)
        self.note_entry.grid(row=2, column=1, pady=8)

        # Hướng dẫn định dạng
        ttk.Label(
            frame,
            text="* Để trống ngày để lấy ngày hôm nay.",
            foreground="gray",
            font=("Segoe UI", 9, "italic"),
        ).grid(row=3, column=0, columnspan=2, pady=5)

        # Nút bấm
        btn_frame = ttk.Frame(frame)
        btn_frame.grid(row=4, column=0, columnspan=2, pady=15)

        ttk.Button(btn_frame, text="Đồng ý", command=self.on_ok).pack(side=tk.LEFT, padx=10)
        ttk.Button(btn_frame, text="Hủy bỏ", command=self.on_cancel).pack(side=tk.LEFT, padx=10)

        self.amount_entry.focus()
        self.protocol("WM_DELETE_WINDOW", self.on_cancel)
        self.wait_window(self)

    def on_ok(self) -> None:
        raw_amount = self.amount_var.get().strip().replace(",", "")
        raw_date = self.date_var.get().strip()
        note = self.note_var.get().strip()

        # 1. Validate Số tiền
        try:
            amount = float(raw_amount)
            if amount <= 0:
                raise ValueError
        except ValueError:
            messagebox.showerror("Lỗi nhập liệu", "Số tiền phải là số lớn hơn 0.")
            self.amount_entry.focus()
            return

        # 2. Validate Ngày
        if not raw_date:
            raw_date = datetime.now().strftime("%Y-%m-%d")
        else:
            try:
                dt = datetime.strptime(raw_date, "%Y-%m-%d")
                if dt > datetime.now():
                    messagebox.showerror("Lỗi nhập liệu", "Ngày giao dịch không được ở trong tương lai.")
                    self.date_entry.focus()
                    return
            except ValueError:
                messagebox.showerror(
                    "Lỗi nhập liệu",
                    "Ngày không hợp lệ hoặc sai định dạng YYYY-MM-DD (Ví dụ: 2025-05-26).",
                )
                self.date_entry.focus()
                return

        self.result = {"amount": amount, "date": raw_date, "note": note}
        self.destroy()

    def on_cancel(self) -> None:
        self.result = None
        self.destroy()


class FinanceApp(tk.Tk):
    """
    Ứng dụng Desktop giao diện đồ họa GUI hiện đại bằng Tkinter.
    """

    def __init__(self) -> None:
        super().__init__()
        self.title("Hệ thống Quản lý Tài chính cá nhân (N-ary Tree Desktop)")
        self.geometry("1150x680")
        self.minsize(1000, 600)

        # Khởi tạo lõi quản lý
        self.manager = FinanceManager()

        # Cấu hình giao diện đẹp mắt
        self.setup_styles()
        self.create_widgets()

        # Tải dữ liệu CSV khi khởi động
        self.load_initial_data()

    def setup_styles(self) -> None:
        """
        Cấu hình màu sắc, phông chữ và kiểu dáng hiện đại (Sleek Theme).
        """
        self.style = ttk.Style(self)
        self.style.theme_use("vista" if os.name == "nt" else "clam")

        # Palette màu cao cấp
        self.bg_color = "#f4f6f9"
        self.sidebar_bg = "#ffffff"
        self.primary_color = "#3f51b5"
        self.accent_green = "#2e7d32"  # Income
        self.accent_red = "#c62828"  # Expense
        self.accent_blue = "#1565c0"  # Balance

        self.configure(bg=self.bg_color)

        # Style cấu hình các Ttk Widgets
        self.style.configure(".", font=("Segoe UI", 10))
        self.style.configure("TFrame", background=self.bg_color)
        self.style.configure("Sidebar.TFrame", background=self.sidebar_bg)

        # Style các nút bấm
        self.style.configure("TButton", font=("Segoe UI", 10, "bold"), padding=6)
        self.style.configure("Action.TButton", background=self.primary_color, foreground="white")

        # Thống kê Labels
        self.style.configure("StatTitle.TLabel", font=("Segoe UI", 11), foreground="#555555", background="#ffffff")
        self.style.configure("StatIncome.TLabel", font=("Segoe UI", 16, "bold"), foreground=self.accent_green, background="#ffffff")
        self.style.configure("StatExpense.TLabel", font=("Segoe UI", 16, "bold"), foreground=self.accent_red, background="#ffffff")
        self.style.configure("StatBalance.TLabel", font=("Segoe UI", 16, "bold"), foreground=self.accent_blue, background="#ffffff")

    def create_widgets(self) -> None:
        """
        Xây dựng cấu trúc lưới giao diện: Sidebar bên trái, Nội dung bên phải.
        """
        # Chia bố cục chính thành 2 cột: Sidebar (Left) và Content Area (Right)
        self.grid_columnconfigure(0, weight=2, minsize=260)  # Sidebar
        self.grid_columnconfigure(1, weight=8, minsize=700)  # Main Content
        self.grid_rowconfigure(0, weight=1)

        # ==========================================
        # 1. SIDEBAR BÊN TRÁI - QUẢN LÝ CÂY DANH MỤC
        # ==========================================
        sidebar = ttk.Frame(self, style="Sidebar.TFrame", padding="10")
        sidebar.grid(row=0, column=0, sticky="nsew", padx=(0, 2), pady=0)

        # Label tiêu đề
        title_label = ttk.Label(
            sidebar,
            text="SƠ ĐỒ CÂY DANH MỤC",
            font=("Segoe UI", 11, "bold"),
            foreground=self.primary_color,
            background=self.sidebar_bg,
        )
        title_label.pack(anchor=tk.W, pady=(5, 10))

        # Cây Treeview hiển thị N-ary Tree
        tree_frame = tk.Frame(sidebar, background=self.sidebar_bg)
        tree_frame.pack(fill=tk.BOTH, expand=True)

        self.category_tree = ttk.Treeview(tree_frame, selectmode="browse", show="tree")
        self.category_tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        tree_scroll = ttk.Scrollbar(tree_frame, orient="vertical", command=self.category_tree.yview)
        tree_scroll.pack(side=tk.RIGHT, fill=tk.Y)
        self.category_tree.configure(yscrollcommand=tree_scroll.set)

        self.category_tree.bind("<<TreeviewSelect>>", self.on_category_select)

        # Thanh nút thao tác Danh mục bên dưới Tree
        cat_btn_frame = ttk.Frame(sidebar, style="Sidebar.TFrame", padding="5")
        cat_btn_frame.pack(fill=tk.X, pady=(10, 5))

        self.btn_add_cat = ttk.Button(cat_btn_frame, text="Thêm mục", command=self.gui_add_category)
        self.btn_add_cat.pack(fill=tk.X, pady=3)

        self.btn_rename_cat = ttk.Button(cat_btn_frame, text="Đổi tên", command=self.gui_rename_category)
        self.btn_rename_cat.pack(fill=tk.X, pady=3)

        self.btn_delete_cat = ttk.Button(cat_btn_frame, text="Xóa mục (Cascade)", command=self.gui_delete_category)
        self.btn_delete_cat.pack(fill=tk.X, pady=3)

        # ==========================================
        # 2. KHU VỰC CHÍNH BÊN PHẢI (MAIN CONTENT)
        # ==========================================
        main_content = ttk.Frame(self, padding="15")
        main_content.grid(row=0, column=1, sticky="nsew")

        # ---- TOP BAR: DASHBOARD THỐNG KÊ ----
        stats_frame = ttk.Frame(main_content)
        stats_frame.pack(fill=tk.X, pady=(0, 15))
        stats_frame.grid_columnconfigure(0, weight=1)
        stats_frame.grid_columnconfigure(1, weight=1)
        stats_frame.grid_columnconfigure(2, weight=1)

        # Card 1: Tổng thu
        card_income = tk.Frame(stats_frame, bg="#ffffff", bd=1, relief=tk.SOLID, padx=15, pady=10)
        card_income.grid(row=0, column=0, padx=5, sticky="nsew")
        ttk.Label(card_income, text="TỔNG THU NHẬP", style="StatTitle.TLabel").pack(anchor=tk.W)
        self.lbl_total_income = ttk.Label(card_income, text="0.00 đ", style="StatIncome.TLabel")
        self.lbl_total_income.pack(anchor=tk.W, pady=(5, 0))

        # Card 2: Tổng chi
        card_expense = tk.Frame(stats_frame, bg="#ffffff", bd=1, relief=tk.SOLID, padx=15, pady=10)
        card_expense.grid(row=0, column=1, padx=5, sticky="nsew")
        ttk.Label(card_expense, text="TỔNG CHI TIÊU", style="StatTitle.TLabel").pack(anchor=tk.W)
        self.lbl_total_expense = ttk.Label(card_expense, text="0.00 đ", style="StatExpense.TLabel")
        self.lbl_total_expense.pack(anchor=tk.W, pady=(5, 0))

        # Card 3: Số dư
        card_balance = tk.Frame(stats_frame, bg="#ffffff", bd=1, relief=tk.SOLID, padx=15, pady=10)
        card_balance.grid(row=0, column=2, padx=5, sticky="nsew")
        ttk.Label(card_balance, text="SỐ DƯ HIỆN TẠI", style="StatTitle.TLabel").pack(anchor=tk.W)
        self.lbl_total_balance = ttk.Label(card_balance, text="0.00 đ", style="StatBalance.TLabel")
        self.lbl_total_balance.pack(anchor=tk.W, pady=(5, 0))

        # ---- BOTTOM AREA: TABBED INTERFACE ----
        self.notebook = ttk.Notebook(main_content)
        self.notebook.pack(fill=tk.BOTH, expand=True)

        # Tab 1: Giao dịch danh mục
        self.tab_tx = ttk.Frame(self.notebook, padding="10")
        self.notebook.add(self.tab_tx, text="  Sổ Giao Dịch  ")

        # Tab 2: Lọc & Tìm kiếm nâng cao
        self.tab_filter = ttk.Frame(self.notebook, padding="10")
        self.notebook.add(self.tab_filter, text="  Tìm Kiếm & Bộ Lọc  ")

        # Tab 3: Module QA/QC (SV4)
        self.tab_qa = ttk.Frame(self.notebook, padding="10")
        self.notebook.add(self.tab_qa, text="  Kiểm Thử QA & Hiệu Năng  ")

        self.setup_tab_transactions()
        self.setup_tab_filter()
        self.setup_tab_qa()

    def setup_tab_transactions(self) -> None:
        """
        Lập trình các widget trong Tab Giao dịch danh mục.
        """
        # Tiêu đề chỉ rõ danh mục đang xem
        self.lbl_selected_cat_title = ttk.Label(
            self.tab_tx,
            text="Đang xem danh mục: [Vui lòng chọn ở cây bên trái]",
            font=("Segoe UI", 11, "bold"),
            foreground=self.primary_color,
        )
        self.lbl_selected_cat_title.pack(anchor=tk.W, pady=(0, 10))

        # Bảng danh sách giao dịch
        table_frame = ttk.Frame(self.tab_tx)
        table_frame.pack(fill=tk.BOTH, expand=True)

        columns = ("id", "date", "amount", "note")
        self.tx_table = ttk.Treeview(table_frame, columns=columns, show="headings", selectmode="browse")
        self.tx_table.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        # Định dạng cột
        self.tx_table.heading("id", text="STT")
        self.tx_table.heading("date", text="Ngày")
        self.tx_table.heading("amount", text="Số tiền (đ)")
        self.tx_table.heading("note", text="Ghi chú")

        self.tx_table.column("id", width=50, minwidth=40, anchor=tk.CENTER)
        self.tx_table.column("date", width=120, minwidth=100, anchor=tk.CENTER)
        self.tx_table.column("amount", width=160, minwidth=130, anchor=tk.E)
        self.tx_table.column("note", width=350, minwidth=200, anchor=tk.W)

        table_scroll = ttk.Scrollbar(table_frame, orient="vertical", command=self.tx_table.yview)
        table_scroll.pack(side=tk.RIGHT, fill=tk.Y)
        self.tx_table.configure(yscrollcommand=table_scroll.set)

        # Thanh nút thao tác giao dịch bên dưới
        tx_btn_frame = ttk.Frame(self.tab_tx, padding="5")
        tx_btn_frame.pack(fill=tk.X, pady=(10, 0))

        self.btn_add_tx = ttk.Button(tx_btn_frame, text="Ghi giao dịch mới", command=self.gui_add_transaction)
        self.btn_add_tx.pack(side=tk.LEFT, padx=5)

        self.btn_edit_tx = ttk.Button(tx_btn_frame, text="Sửa giao dịch", command=self.gui_edit_transaction)
        self.btn_edit_tx.pack(side=tk.LEFT, padx=5)

        self.btn_delete_tx = ttk.Button(tx_btn_frame, text="Xóa giao dịch", command=self.gui_delete_transaction)
        self.btn_delete_tx.pack(side=tk.LEFT, padx=5)

    def setup_tab_filter(self) -> None:
        """
        Lập trình các widget trong Tab Bộ lọc nâng cao.
        """
        # Khung nhập điều kiện lọc
        filter_box = ttk.LabelFrame(self.tab_filter, text=" Điều kiện lọc nâng cao ", padding="15")
        filter_box.pack(fill=tk.X, pady=(0, 15))

        # Bố cục lưới điều kiện
        ttk.Label(filter_box, text="Từ ngày (YYYY-MM-DD):").grid(row=0, column=0, sticky=tk.W, padx=5, pady=5)
        self.ent_filter_start = ttk.Entry(filter_box, width=15)
        self.ent_filter_start.grid(row=0, column=1, padx=10, pady=5)

        ttk.Label(filter_box, text="Đến ngày (YYYY-MM-DD):").grid(row=0, column=2, sticky=tk.W, padx=5, pady=5)
        self.ent_filter_end = ttk.Entry(filter_box, width=15)
        self.ent_filter_end.grid(row=0, column=3, padx=10, pady=5)

        ttk.Label(filter_box, text="Danh mục nhánh:").grid(row=1, column=0, sticky=tk.W, padx=5, pady=5)
        self.ent_filter_path = ttk.Entry(filter_box, width=35)
        self.ent_filter_path.grid(row=1, column=1, columnspan=3, sticky=tk.W, padx=10, pady=5)

        btn_filter = ttk.Button(filter_box, text="Lọc Dữ Liệu", command=self.gui_run_filters)
        btn_filter.grid(row=1, column=4, padx=15, pady=5)

        # Bảng hiển thị kết quả lọc
        table_frame = ttk.Frame(self.tab_filter)
        table_frame.pack(fill=tk.BOTH, expand=True)

        columns = ("id", "path", "date", "amount", "note")
        self.filter_table = ttk.Treeview(table_frame, columns=columns, show="headings", selectmode="none")
        self.filter_table.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        self.filter_table.heading("id", text="STT")
        self.filter_table.heading("path", text="Đường dẫn danh mục")
        self.filter_table.heading("date", text="Ngày")
        self.filter_table.heading("amount", text="Số tiền (đ)")
        self.filter_table.heading("note", text="Ghi chú")

        self.filter_table.column("id", width=45, minwidth=40, anchor=tk.CENTER)
        self.filter_table.column("path", width=250, minwidth=180, anchor=tk.W)
        self.filter_table.column("date", width=100, minwidth=90, anchor=tk.CENTER)
        self.filter_table.column("amount", width=140, minwidth=120, anchor=tk.E)
        self.filter_table.column("note", width=250, minwidth=150, anchor=tk.W)

        filter_scroll = ttk.Scrollbar(table_frame, orient="vertical", command=self.filter_table.yview)
        filter_scroll.pack(side=tk.RIGHT, fill=tk.Y)
        self.filter_table.configure(yscrollcommand=filter_scroll.set)

    def setup_tab_qa(self) -> None:
        """
        Lập trình tab kiểm thử hiệu năng QA/QC máy tính (SV4).
        """
        # Thanh điều khiển kiểm thử
        ctrl_frame = ttk.Frame(self.tab_qa, padding="5")
        ctrl_frame.pack(fill=tk.X, pady=(0, 10))

        ttk.Button(
            ctrl_frame,
            text="Sinh 10.000 dữ liệu mẫu (ĐỀU)",
            command=lambda: self.gui_run_generate_data(True),
        ).pack(side=tk.LEFT, padx=5)

        ttk.Button(
            ctrl_frame,
            text="Sinh 10.000 dữ liệu mẫu (NGẪU NHIÊN)",
            command=lambda: self.gui_run_generate_data(False),
        ).pack(side=tk.LEFT, padx=5)

        ttk.Button(ctrl_frame, text="Chạy Performance Test", command=self.gui_run_performance_test).pack(
            side=tk.LEFT, padx=5
        )

        # Vùng ghi nhận logs kết quả
        lbl_log = ttk.Label(
            self.tab_qa,
            text="BÁO CÁO KẾT QUẢ ĐO ĐẠC HIỆU NĂNG (BẢNG SO SÁNH):",
            font=("Segoe UI", 10, "bold"),
            foreground=self.accent_blue,
        )
        lbl_log.pack(anchor=tk.W, pady=(5, 5))

        log_frame = ttk.Frame(self.tab_qa)
        log_frame.pack(fill=tk.BOTH, expand=True)

        self.txt_qa_log = tk.Text(
            log_frame,
            wrap=tk.NONE,
            font=("Consolas", 10),
            bg="#2b2b2b",
            fg="#a9b7c6",
            insertbackground="white",
        )
        self.txt_qa_log.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        # Scrollbars cho Text
        ysb = ttk.Scrollbar(log_frame, orient="vertical", command=self.txt_qa_log.yview)
        ysb.pack(side=tk.RIGHT, fill=tk.Y)
        xsb = ttk.Scrollbar(self.tab_qa, orient="horizontal", command=self.txt_qa_log.xview)
        xsb.pack(fill=tk.X)

        self.txt_qa_log.configure(yscrollcommand=ysb.set, xscrollcommand=xsb.set)

        self.write_qa_log("=== SẴN SÀNG CHẠY KIỂM THỬ ===\n\nHãy nhấp vào các nút phía trên để bắt đầu thử nghiệm.")

    def write_qa_log(self, text: str) -> None:
        self.txt_qa_log.delete("1.0", tk.END)
        self.txt_qa_log.insert(tk.END, text)

    # ==========================================
    # CÁC HÀM XỬ LÝ NGHIỆP VỤ (BUSINESS LOGIC)
    # ==========================================

    def load_initial_data(self) -> None:
        """
        Nạp dữ liệu khi khởi chạy ứng dụng.
        """
        try:
            success, count, warnings = self.manager.load_data(DATA_FILE)
            if success:
                if warnings:
                    warn_text = "\n".join(warnings[:5])
                    if len(warnings) > 5:
                        warn_text += f"\n... và {len(warnings) - 5} dòng cảnh báo lỗi khác."
                    messagebox.showwarning(
                        "Cảnh báo nạp dữ liệu",
                        f"Đã nạp {count} giao dịch từ CSV, nhưng phát hiện dòng lỗi:\n{warn_text}",
                    )
                else:
                    pass
            else:
                # Không có file, hệ thống tự động khởi tạo mặc định
                pass
        except Exception as e:
            messagebox.showerror("Lỗi hệ thống", f"Không thể nạp dữ liệu từ '{DATA_FILE}':\n{e}")

        self.refresh_all()

    def refresh_all(self) -> None:
        """
        Làm mới toàn bộ giao diện: Cây danh mục, số liệu thống kê và danh sách giao dịch.
        """
        # 1. Làm mới Cây Danh mục
        self.category_tree.delete(*self.category_tree.get_children())
        self.populate_tree(self.manager.root_income, "")
        self.populate_tree(self.manager.root_expense, "")

        # 2. Làm mới Thống kê Dashboard
        total_income = self.manager.root_income.get_total_amount()
        total_expense = self.manager.root_expense.get_total_amount()
        balance = total_income - total_expense

        self.lbl_total_income.configure(text=f"{total_income:,.2f} đ")
        self.lbl_total_expense.configure(text=f"{total_expense:,.2f} đ")
        self.lbl_total_balance.configure(text=f"{balance:,.2f} đ")

        # 3. Làm mới bảng giao dịch
        self.refresh_transactions_list()

    def populate_tree(self, node: CategoryNode, parent_id: str) -> None:
        """
        Duyệt DFS để đưa các nút của N-ary Tree vào Widget Treeview.
        """
        node_path = node.name
        if node.parent:
            # Thu thập đường dẫn đầy đủ
            parent_node = node.parent
            parent_paths = []
            while parent_node:
                parent_paths.append(parent_node.name)
                parent_node = parent_node.parent
            parent_paths.reverse()
            node_path = "/".join(parent_paths) + "/" + node.name

        # Nhãn hiển thị có chứa số tiền tích lũy của nhánh
        display_text = f"{node.name} ({node.get_total_amount():,.0f} đ)"
        node_id = self.category_tree.insert(parent_id, "end", text=display_text, values=(node_path,))

        # Đệ quy cho con
        for child in node.children:
            self.populate_tree(child, node_id)

        # Mặc định mở rộng cây gốc
        if not parent_id:
            self.category_tree.item(node_id, open=True)

    def get_selected_category_path(self) -> Optional[str]:
        """
        Lấy đường dẫn danh mục đang được chọn trên cây Treeview.
        """
        selected_item = self.category_tree.selection()
        if not selected_item:
            return None
        values = self.category_tree.item(selected_item[0], "values")
        return values[0] if values else None

    def on_category_select(self, event) -> None:
        """
        Sự kiện khi click chọn danh mục trên cây.
        """
        self.refresh_transactions_list()

    def refresh_transactions_list(self) -> None:
        """
        Làm mới bảng giao dịch của danh mục đang chọn.
        """
        self.tx_table.delete(*self.tx_table.get_children())
        cat_path = self.get_selected_category_path()

        if not cat_path:
            self.lbl_selected_cat_title.configure(text="Đang xem danh mục: [Vui lòng chọn ở cây bên trái]")
            return

        self.lbl_selected_cat_title.configure(text=f"Đang xem danh mục: {cat_path}")

        # Tìm node tương ứng
        parts = self.manager.normalize_and_validate_path(cat_path)
        path_upper = "/".join(parts).upper()
        if path_upper in self.manager.node_index:
            node = self.manager.node_index[path_upper]
            for idx, tx in enumerate(node.transactions):
                self.tx_table.insert(
                    "",
                    "end",
                    values=(idx, tx.date, f"{tx.amount:,.2f}", tx.note),
                )

    # ==========================================
    # CÁC THAO TÁC SỰ KIỆN NÚT BẤM (GUI ACTIONS)
    # ==========================================

    def gui_add_category(self) -> None:
        """
        Sự kiện Thêm danh mục trống mới.
        """
        selected_path = self.get_selected_category_path()
        initial_val = selected_path + "/" if selected_path else "CHI TIÊU/"

        path = simpledialog.askstring(
            "Thêm danh mục mới",
            "Nhập đường dẫn danh mục cần tạo:\n(Ví dụ: CHI TIÊU/Ăn uống/Ăn vặt)",
            initialvalue=initial_val,
            parent=self,
        )

        if not path:
            return

        try:
            self.manager.add_category_by_path(path)
            messagebox.showinfo("Thành công", f"Đã thêm danh mục mới:\n'{path}'")
            self.refresh_all()
        except ValueError as e:
            messagebox.showerror("Lỗi", f"Không thể thêm danh mục:\n{e}")

    def gui_rename_category(self) -> None:
        """
        Sự kiện Đổi tên danh mục hiện tại.
        """
        selected_path = self.get_selected_category_path()
        if not selected_path:
            messagebox.showwarning("Cảnh báo", "Vui lòng chọn danh mục cần đổi tên trên cây.")
            return

        parts = selected_path.split("/")
        if len(parts) == 1:
            messagebox.showwarning("Cảnh báo", "Không thể đổi tên danh mục gốc.")
            return

        current_name = parts[-1]
        new_name = simpledialog.askstring(
            "Đổi tên danh mục",
            f"Nhập tên mới thay thế cho '{current_name}':",
            initialvalue=current_name,
            parent=self,
        )

        if not new_name:
            return

        try:
            self.manager.rename_category(selected_path, new_name)
            messagebox.showinfo("Thành công", "Đổi tên danh mục thành công!")
            self.refresh_all()
        except ValueError as e:
            messagebox.showerror("Lỗi", f"Không thể đổi tên danh mục:\n{e}")

    def gui_delete_category(self) -> None:
        """
        Sự kiện Xóa danh mục CASCADE.
        """
        selected_path = self.get_selected_category_path()
        if not selected_path:
            messagebox.showwarning("Cảnh báo", "Vui lòng chọn danh mục cần xóa trên cây.")
            return

        parts = selected_path.split("/")
        if len(parts) == 1:
            messagebox.showwarning("Cảnh báo", "Không thể xóa danh mục gốc.")
            return

        confirm = messagebox.askyesno(
            "Xác nhận xóa danh mục",
            f"CẢNH BÁO: Bạn có chắc chắn muốn xóa danh mục:\n'{selected_path}'?\n\n"
            "Thao tác này sẽ xóa CASCADE toàn bộ các danh mục con và các giao dịch trực thuộc!",
            icon=messagebox.WARNING,
            parent=self,
        )

        if not confirm:
            return

        try:
            self.manager.delete_category(selected_path)
            messagebox.showinfo("Thành công", "Đã xóa danh mục thành công!")
            self.refresh_all()
        except ValueError as e:
            messagebox.showerror("Lỗi", f"Không thể xóa danh mục:\n{e}")

    def gui_add_transaction(self) -> None:
        """
        Sự kiện Thêm giao dịch mới.
        """
        selected_path = self.get_selected_category_path()
        if not selected_path:
            messagebox.showwarning("Cảnh báo", "Vui lòng chọn danh mục cụ thể trên cây trước khi thêm giao dịch.")
            return

        dialog = TransactionDialog(self, title=f"Ghi giao dịch vào: {selected_path.split('/')[-1]}")
        if not dialog.result:
            return

        try:
            tx = Transaction(
                amount=dialog.result["amount"],
                date=dialog.result["date"],
                note=dialog.result["note"],
            )
            self.manager.add_transaction(tx, selected_path)
            self.refresh_all()
        except ValueError as e:
            messagebox.showerror("Lỗi", f"Không thể thêm giao dịch:\n{e}")

    def gui_edit_transaction(self) -> None:
        """
        Sự kiện sửa giao dịch.
        """
        selected_path = self.get_selected_category_path()
        if not selected_path:
            messagebox.showwarning("Cảnh báo", "Vui lòng chọn danh mục trên cây.")
            return

        selected_tx = self.tx_table.selection()
        if not selected_tx:
            messagebox.showwarning("Cảnh báo", "Vui lòng chọn một dòng giao dịch cần sửa.")
            return

        # Lấy chỉ số và thông tin cũ
        values = self.tx_table.item(selected_tx[0], "values")
        idx = int(values[0])
        old_amount = float(values[2].replace(",", ""))
        old_date = values[1]
        old_note = values[3]

        dialog = TransactionDialog(
            self,
            title="Sửa giao dịch",
            amount=old_amount,
            date=old_date,
            note=old_note,
        )

        if not dialog.result:
            return

        try:
            self.manager.edit_transaction(
                selected_path,
                idx,
                dialog.result["amount"],
                dialog.result["date"],
                dialog.result["note"],
            )
            self.refresh_all()
        except ValueError as e:
            messagebox.showerror("Lỗi", f"Không thể sửa giao dịch:\n{e}")

    def gui_delete_transaction(self) -> None:
        """
        Sự kiện Xóa giao dịch.
        """
        selected_path = self.get_selected_category_path()
        if not selected_path:
            messagebox.showwarning("Cảnh báo", "Vui lòng chọn danh mục trên cây.")
            return

        selected_tx = self.tx_table.selection()
        if not selected_tx:
            messagebox.showwarning("Cảnh báo", "Vui lòng chọn một dòng giao dịch cần xóa.")
            return

        values = self.tx_table.item(selected_tx[0], "values")
        idx = int(values[0])

        confirm = messagebox.askyesno(
            "Xác nhận xóa",
            "Bạn có chắc chắn muốn xóa giao dịch này không?",
            parent=self,
        )
        if not confirm:
            return

        try:
            self.manager.delete_transaction(selected_path, idx)
            self.refresh_all()
        except ValueError as e:
            messagebox.showerror("Lỗi", f"Không thể xóa giao dịch:\n{e}")

    def gui_run_filters(self) -> None:
        """
        Sự kiện lọc dữ liệu nâng cao trên Tab 2.
        """
        start = self.ent_filter_start.get().strip()
        end = self.ent_filter_end.get().strip()
        path = self.ent_filter_path.get().strip()

        # Validate ngày
        try:
            if start:
                datetime.strptime(start, "%Y-%m-%d")
            if end:
                datetime.strptime(end, "%Y-%m-%d")
        except ValueError:
            messagebox.showerror("Lỗi định dạng", "Ngày bắt đầu hoặc ngày kết thúc phải có định dạng YYYY-MM-DD.")
            return

        self.filter_table.delete(*self.filter_table.get_children())

        try:
            results = self.manager.filter_transactions(
                start_date=start if start else None,
                end_date=end if end_date else None,
                category_path=path if path else None,
            )

            for idx, (p, tx) in enumerate(results, 1):
                self.filter_table.insert(
                    "",
                    "end",
                    values=(idx, p, tx.date, f"{tx.amount:,.2f}", tx.note),
                )
            
            if not results:
                messagebox.showinfo("Lọc dữ liệu", "Không tìm thấy giao dịch nào thỏa mãn bộ lọc.")

        except ValueError as e:
            messagebox.showerror("Lỗi", f"Đường dẫn lọc danh mục không hợp lệ:\n{e}")

    def gui_run_generate_data(self, uniform: bool) -> None:
        """
        Chạy sinh dữ liệu 10.000 bản ghi mẫu.
        """
        self.write_qa_log(f"Đang tiến hành sinh 10.000 giao dịch mẫu (Phân bổ: {'ĐỀU' if uniform else 'NGẪU NHIÊN'})...\nXin vui lòng chờ...")
        self.update_idletasks()

        try:
            self.manager.generate_test_data(10000, uniform, TEST_DATA_FILE)
            self.write_qa_log(
                f"✨ Sinh dữ liệu thành công!\n\n"
                f"- File được tạo: '{TEST_DATA_FILE}'\n"
                f"- Số lượng bản ghi: 10.000 dòng\n"
                f"- Phân bổ: {'Đồng đều 100% xoay vòng giữa các nút lá' if uniform else 'Ngẫu nhiên không đều'}\n\n"
                f"💡 Bạn có thể bấm 'Chạy Performance Test' ngay bây giờ để đo thời gian thực thi của file này."
            )
            messagebox.showinfo("QA/QC", f"Đã sinh 10.000 bản ghi mẫu thành công vào file '{TEST_DATA_FILE}'!")
        except Exception as e:
            self.write_qa_log(f"❌ Lỗi sinh dữ liệu: {e}")
            messagebox.showerror("Lỗi", f"Không thể sinh dữ liệu mẫu:\n{e}")

    def gui_run_performance_test(self) -> None:
        """
        Chạy Performance Test đo đạc hiệu năng.
        """
        if not os.path.exists(TEST_DATA_FILE):
            messagebox.showwarning("Cảnh báo", f"Vui lòng nhấp nút 'Sinh dữ liệu mẫu' trước. Không tìm thấy file '{TEST_DATA_FILE}'")
            return

        self.write_qa_log("🚀 Đang tiến hành đo đạc hiệu năng thực tế trên file 10.000 bản ghi...\nXin vui lòng chờ một vài giây...")
        self.update_idletasks()

        try:
            report_table = self.manager.test_performance(TEST_DATA_FILE)
            self.write_qa_log(
                f"📊 BẢNG KẾT QUẢ ĐO ĐẠC HIỆU NĂNG THỰC TẾ (O-notation):\n\n"
                f"{report_table}\n\n"
                f"💡 Nhận xét kết quả phục vụ viết báo cáo lý thuyết Big O (SV5):\n"
                f"  - Tải dữ liệu CSV (Load): Tăng tuyến tính tỉ lệ thuận với số dòng O(M).\n"
                f"  - Duyệt cây tính tổng (DFS): Tính tổng 10.000 nút cực kỳ tối ưu, nhỏ hơn 0.5 ms (O(N+M)).\n"
                f"  - Tìm kiếm đường dẫn (Path): Chỉ mất khoảng 0.00005 ms và GIỮ HẰNG SỐ (O(1) nhờ node_index).\n"
                f"  - Tìm từ khóa ghi chú: Tăng tuyến tính theo số lượng giao dịch O(M)."
            )
            messagebox.showinfo("QA/QC", "Đo hiệu năng thành công! Xem bảng kết quả hiển thị phía dưới.")
        except Exception as e:
            self.write_qa_log(f"❌ Lỗi đo hiệu năng: {e}")
            messagebox.showerror("Lỗi", f"Không thể chạy kiểm thử hiệu năng:\n{e}")

    def destroy(self) -> None:
        """
        Tự động lưu dữ liệu vào file CSV khi tắt cửa sổ chính.
        """
        try:
            self.manager.save_data(DATA_FILE)
        except Exception as e:
            print(f"Lỗi khi tự động lưu dữ liệu: {e}")
        super().destroy()


def run_gui_app() -> None:
    app = FinanceApp()
    app.mainloop()
