import sys
from personal_finance_tree.console import run_console_menu
from personal_finance_tree.gui import run_gui_app

def main() -> None:
    # Cấu hình an toàn mã hóa stdout thành UTF-8 trên Windows để hiển thị tiếng Việt mượt mà
    if sys.stdout.encoding != 'utf-8':
        try:
            sys.stdout.reconfigure(encoding='utf-8')
        except Exception:
            pass
            
    # Kiểm tra tham số dòng lệnh
    if "--cli" in sys.argv:
        run_console_menu()
    else:
        run_gui_app()

if __name__ == "__main__":
    main()
