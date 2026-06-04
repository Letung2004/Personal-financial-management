package ui;

import core.FinanceManager;
import core.FinanceTree;
import models.CategoryNode;
import models.Transaction;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;



///////////// fix bảng (thêm padding tí) /////////////

class PaddedTableCellRenderer extends DefaultTableCellRenderer {
    private final int padding;

    public PaddedTableCellRenderer(int padding) {
        this.padding = padding;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {

        // hàm gốc để lấy giao diện mặc định
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // padding
        if (c instanceof JComponent) {
            ((JComponent) c).setBorder(new EmptyBorder(0, padding, 0, padding));
        }

        return c;
    }
}

//================================================================================================================
//================================================================================================================

public class MainGUI extends JFrame {
    private final FinanceManager manager;
    private final CardLayout cardLayout;
    private final JPanel mainContentPanel;
    private DefaultTreeModel treeModel;
    private JTree categoryTree;
    private DefaultMutableTreeNode rootTreeNode;

    //  các nút ở Sidebar và nút đang được chọn
    ///trong motion của nút navigate
    private final java.util.List<JButton> navButtons = new java.util.ArrayList<>();
    private JButton currentActiveBtn = null;

    // BẢNG MÀU
    // Nền màu Slate tối
    private static final Color BG_MAIN = new Color(30, 41, 59);        // Slate 800 (Nền chính)
    private static final Color BG_SIDEBAR = new Color(15, 23, 42);     // Slate 900 (Nền sidebar & panel)
    private static final Color BG_CARD = new Color(15, 23, 42);        // Slate 900 (Nền card/bảng)

    // Chữ Trắng và Viền
    private static final Color COLOR_TEXT = new Color(255, 255, 255);  // Trắng (Chữ chính)
    private static final Color COLOR_MUTED = new Color(148, 163, 184); // Slate 400 (Chữ phụ/nhạt)
    private static final Color COLOR_BORDER = new Color(51, 65, 85);   // Slate 700 (Đường viền)

    // Nút Xám
    private static final Color COLOR_BTN = new Color(71, 85, 105);     // Slate 600 (Màu nút xám)
    private static final Color COLOR_BTN_HOVER = new Color(100, 116, 139); // Slate 500 (Màu nút khi hover)

    // Trạng thái: Thu (Xanh), Chi (Đỏ), Dư (Tím)
    private static final Color COLOR_GREEN = new Color(34, 197, 94);   // Xanh lá (Thu)
    private static final Color COLOR_RED = new Color(239, 68, 68);     // Đỏ (Chi)
    private static final Color COLOR_PURPLE = new Color(168, 85, 247); // Tím (Dư)

    // Labels for Dashboard
    private JLabel lblIncomeVal;
    private JLabel lblExpenseVal;
    private JLabel lblBalanceVal;
    private JPanel pnlBreakdown;

    // Transaction Table & Combo
    private DefaultTableModel transactionTableModel;
    private JComboBox<String> cmbLeafCategories;

    // Search Result Table
    private DefaultTableModel searchTableModel;

    public MainGUI() {
        this.manager = new FinanceManager();
        this.manager.loadData();

        // Frame Setup
        setTitle("HE THONG QUAN LY TAI CHINH CA NHAN");
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_MAIN);

        // Auto Save
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("  [GUI] Dang tu dong luu du lieu...");
                manager.saveData();
            }
        });

        setLayout(new BorderLayout());

        // Sidebar Panel
        JPanel sidebar = createSidebarPanel();
        add(sidebar, BorderLayout.WEST);

        // Main Content Area
        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(BG_MAIN);

        // Register Panels
        mainContentPanel.add(createDashboardPanel(), "DASHBOARD");
        mainContentPanel.add(createCategoryManagerPanel(), "CATEGORIES");
        mainContentPanel.add(createTransactionPanel(), "TRANSACTIONS");
        mainContentPanel.add(createSearchPanel(), "SEARCH");
        mainContentPanel.add(createFileIOPanel(), "FILE_IO");
        mainContentPanel.add(createPythonGUIPanel(), "PYTHON_GUI");

        add(mainContentPanel, BorderLayout.CENTER);

        cardLayout.show(mainContentPanel, "DASHBOARD");
        updateDashboardData();
    }




    // =========================================================================
    /// SIDEBAR NAVIGATION
    // =========================================================================
    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(250, getHeight()));
        sidebar.setBackground(BG_SIDEBAR);
        sidebar.setLayout(new BorderLayout());
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, COLOR_BORDER));

        // Header Title
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(30, 20, 30, 20));

        JLabel titleLbl = new JLabel("<html>HỆ THỐNG QUẢN LÝ<br>TÀI CHÍNH CÁ NHÂN</html>");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLbl.setForeground(COLOR_TEXT);
        headerPanel.add(titleLbl);



        sidebar.add(headerPanel, BorderLayout.NORTH);

        // Nav Buttons Panel
        JPanel navPanel = new JPanel();
        navPanel.setOpaque(false);
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBorder(new EmptyBorder(15, 5, 10, 20));
        navPanel.setAlignmentX(SwingConstants.CENTER);

        JButton btnDash = createNavButton("Báo cáo Tổng quan", "DASHBOARD");
        JButton btnCats = createNavButton("Cấu trúc Danh mục", "CATEGORIES");
        JButton btnTxns = createNavButton("Nhập liệu Giao dịch", "TRANSACTIONS");
        JButton btnSrch = createNavButton("Tìm kiếm & Bộ lọc", "SEARCH");
        JButton btnFile = createNavButton("Lưu trữ & Tải file", "FILE_IO");
        JButton btnPyGUI = createNavButton("Kiểm thử Python GUI", "PYTHON_GUI");

        navPanel.add(btnDash); navPanel.add(Box.createVerticalStrut(10));
        navPanel.add(btnCats); navPanel.add(Box.createVerticalStrut(10));
        navPanel.add(btnTxns); navPanel.add(Box.createVerticalStrut(10));
        navPanel.add(btnSrch); navPanel.add(Box.createVerticalStrut(10));
        navPanel.add(btnFile); navPanel.add(Box.createVerticalStrut(100)); ///tách biệt ra tí để dễ hình dung
        navPanel.add(btnPyGUI);

        /// đống này chèn thêm kiểu button để nhấp nhô
        navButtons.clear();
        navButtons.add(btnDash); setActiveNavButton(btnDash);  //default ở tab đầu
        navButtons.add(btnCats);
        navButtons.add(btnTxns);
        navButtons.add(btnSrch);
        navButtons.add(btnFile);
        navButtons.add(btnPyGUI);
        ///

        sidebar.add(navPanel, BorderLayout.CENTER);



        return sidebar;
    }
///
/// fix lại hàm để apply motion của navigate btn
///
    private JButton createNavButton(String text, String cardName) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(COLOR_TEXT);
        btn.setBackground(BG_SIDEBAR);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);

        // gán lề mặc định
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(10, 15, 10, 15)
        ));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setMaximumSize(new Dimension(230, 42));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // hiệu ứng Hover (di chuột) chuyển màu nút
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {

                if (btn != currentActiveBtn) {
                    btn.setBackground(COLOR_BTN_HOVER);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {

                if (btn != currentActiveBtn) {
                    btn.setBackground(BG_SIDEBAR);
                }
            }
        });

        // sự kiện khi Click
        btn.addActionListener(e -> {

            setActiveNavButton(btn);   // gọi hàm tạo hiệu ứng

            // chuyển Card
            cardLayout.show(mainContentPanel, cardName);
            if (cardName.equals("DASHBOARD")) {
                updateDashboardData();
            } else if (cardName.equals("CATEGORIES")) {
                rebuildTree();
            } else if (cardName.equals("TRANSACTIONS")) {
                refreshTransactionForm();
            }
        });

        return btn;
    }
///============================
/// chuyển ộng của nút navigate (nhấp nhô)
///============================
    private void setActiveNavButton(JButton targetBtn) {
        currentActiveBtn = targetBtn;
        for (JButton btn : navButtons) {
            if (btn == targetBtn) {
                // TRẠNG THÁI ACTIVE: Nền sáng hơn, lề trái thụt vào 30px (nhô qua phải)
                btn.setBackground(COLOR_BTN);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(COLOR_BORDER, 1, true),
                        new EmptyBorder(10, 45, 10, 15)
                ));
            } else {
                // TRẠNG THÁI BÌNH THƯỜNG
                btn.setBackground(BG_SIDEBAR);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(COLOR_BORDER, 1, true),
                        new EmptyBorder(10, 15, 10, 15)
                ));
            }
        }
    }



    // =========================================================================
    /// PANEL 1: DASHBOARD
    // =========================================================================
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(30, 35, 30, 35));

        JLabel titleLbl = new JLabel("BÁO CÁO TỔNG QUAN TÀI CHÍNH");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLbl.setForeground(COLOR_TEXT);
        panel.add(titleLbl, BorderLayout.NORTH);

        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 24, 0));
        statsPanel.setOpaque(false);
        statsPanel.setBorder(new EmptyBorder(25, 0, 25, 0));

        JPanel cardInc = createStatCard("TỔNG THU NHẬP", COLOR_GREEN);
        lblIncomeVal = (JLabel) cardInc.getComponent(1);

        JPanel cardExp = createStatCard("TỔNG CHI TIÊU", COLOR_RED);
        lblExpenseVal = (JLabel) cardExp.getComponent(1);

        JPanel cardBal = createStatCard("SỐ DƯ HIỆN TẠI", COLOR_PURPLE);
        lblBalanceVal = (JLabel) cardBal.getComponent(1);

        statsPanel.add(cardInc);
        statsPanel.add(cardExp);
        statsPanel.add(cardBal);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);

        JLabel breakdownTitle = new JLabel("Chi tiết tiến độ theo các danh mục chính");
        breakdownTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        breakdownTitle.setForeground(COLOR_TEXT);
        breakdownTitle.setBorder(new EmptyBorder(10, 0, 12, 0));
        bottomPanel.add(breakdownTitle, BorderLayout.NORTH);

        pnlBreakdown = new JPanel();
        pnlBreakdown.setBackground(BG_CARD);
        pnlBreakdown.setLayout(new BoxLayout(pnlBreakdown, BoxLayout.Y_AXIS));
        pnlBreakdown.setBorder(new EmptyBorder(20, 24, 20, 24));

        JScrollPane scroll = new JScrollPane(pnlBreakdown);
        scroll.setBorder(new LineBorder(COLOR_BORDER, 1, true));
        scroll.getViewport().setBackground(BG_CARD);
        bottomPanel.add(scroll, BorderLayout.CENTER);

        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        container.add(statsPanel, BorderLayout.NORTH);
        container.add(bottomPanel, BorderLayout.CENTER);

        panel.add(container, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatCard(String title, Color accentColor) {
        JPanel card = new JPanel(new GridLayout(2, 1, 4, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(BG_CARD);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2d.setColor(accentColor);
                g2d.fillRoundRect(0, 0, 6, getHeight(), 8, 8);
                g2d.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(16, 24, 16, 20)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLbl.setForeground(COLOR_MUTED);
        card.add(titleLbl);

        JLabel valLbl = new JLabel("0 VND");
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valLbl.setForeground(COLOR_TEXT);
        card.add(valLbl);

        return card;
    }

    private void updateDashboardData() {
        double[] totals = manager.getTree().calculateIncomeAndExpense();
        double income = totals[0];
        double expense = totals[1];
        double balance = income - expense;

        lblIncomeVal.setText(String.format("%,.0f VND", income));
        lblIncomeVal.setForeground(COLOR_GREEN);

        lblExpenseVal.setText(String.format("%,.0f VND", expense));
        lblExpenseVal.setForeground(COLOR_RED);

        lblBalanceVal.setText(String.format("%,.0f VND", balance));
        lblBalanceVal.setForeground(COLOR_PURPLE);

        pnlBreakdown.removeAll();

        CategoryNode thuNode = manager.getTree().getNodeByPath("THU");
        if (thuNode != null && income > 0) {
            JLabel header = new JLabel("DANH MỤC THU NHẬP:");
            header.setFont(new Font("Segoe UI", Font.BOLD, 13));
            header.setForeground(COLOR_GREEN);
            header.setBorder(new EmptyBorder(5, 0, 8, 0));
            pnlBreakdown.add(header);

            for (CategoryNode child : thuNode.getChildren()) {
                double amount = manager.getTree().calculateTotalDfs(child);
                if (amount > 0) {
                    pnlBreakdown.add(createBreakdownRow(child.getName(), amount, (amount / income) * 100, COLOR_GREEN));
                }
            }
        }

        pnlBreakdown.add(Box.createVerticalStrut(15));

        CategoryNode chiNode = manager.getTree().getNodeByPath("CHI");
        if (chiNode != null && expense > 0) {
            JLabel header = new JLabel("DANH MỤC CHI TIÊU:");
            header.setFont(new Font("Segoe UI", Font.BOLD, 13));
            header.setForeground(COLOR_RED);
            header.setBorder(new EmptyBorder(5, 0, 8, 0));
            pnlBreakdown.add(header);

            for (CategoryNode child : chiNode.getChildren()) {
                double amount = manager.getTree().calculateTotalDfs(child);
                if (amount > 0) {
                    pnlBreakdown.add(createBreakdownRow(child.getName(), amount, (amount / expense) * 100, COLOR_RED));
                }
            }
        }

        if (income == 0 && expense == 0) {
            JLabel emptyLbl = new JLabel("Chưa có dữ liệu giao dịch. Hãy nhập dữ liệu ở menu Nhập liệu.", SwingConstants.CENTER);
            emptyLbl.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            emptyLbl.setForeground(COLOR_MUTED);
            emptyLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            pnlBreakdown.add(emptyLbl);
        }

        pnlBreakdown.revalidate();
        pnlBreakdown.repaint();
    }

    private JPanel createBreakdownRow(String name, double amount, double pct, Color barColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(850, 35));
        row.setBorder(new EmptyBorder(4, 5, 4, 5));

        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLbl.setForeground(COLOR_TEXT);
        nameLbl.setPreferredSize(new Dimension(180, 20));
        row.add(nameLbl, BorderLayout.WEST);

        JPanel barPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(BG_MAIN); // Thanh nền nằm dưới
                g2d.fillRoundRect(0, 5, getWidth(), 10, 8, 8);
                int fillWidth = (int) (getWidth() * (pct / 100.0));
                if (fillWidth > 0) {
                    g2d.setColor(barColor);
                    g2d.fillRoundRect(0, 5, fillWidth, 10, 8, 8);
                }
                g2d.dispose();
            }
        };
        barPanel.setOpaque(false);
        row.add(barPanel, BorderLayout.CENTER);

        JLabel valLbl = new JLabel(String.format("%,.0f VND (%.1f%%)", amount, pct), SwingConstants.RIGHT);
        valLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        valLbl.setForeground(COLOR_TEXT);
        valLbl.setPreferredSize(new Dimension(200, 20));
        row.add(valLbl, BorderLayout.EAST);

        return row;
    }

    // =========================================================================
    /// PANEL 2: CATEGORY TREE MANAGER
    // =========================================================================
    private JPanel createCategoryManagerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel titleLbl = new JLabel("CẤU TRÚC DANH MỤC PHÂN CẤP (TREE VIEW)");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLbl.setForeground(COLOR_TEXT);
        titleLbl.setBorder(new EmptyBorder(0, 0, 15, 0));
        panel.add(titleLbl, BorderLayout.NORTH);

        rootTreeNode = new DefaultMutableTreeNode("ROOT");
        treeModel = new DefaultTreeModel(rootTreeNode);
        categoryTree = new JTree(treeModel);
        categoryTree.setBackground(BG_CARD);
        categoryTree.setForeground(COLOR_TEXT);
        categoryTree.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        categoryTree.setBorder(new EmptyBorder(10, 10, 10, 10));

        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) categoryTree.getCellRenderer();
        renderer.setBackgroundNonSelectionColor(BG_CARD);
        renderer.setTextNonSelectionColor(COLOR_TEXT);
        renderer.setTextSelectionColor(COLOR_TEXT);
        renderer.setBackgroundSelectionColor(COLOR_BTN);
        renderer.setBorderSelectionColor(COLOR_BORDER);

        JScrollPane treeScroll = new JScrollPane(categoryTree);
        treeScroll.setBorder(new LineBorder(COLOR_BORDER, 1, true));

        JPanel controlPanel = new JPanel();
        controlPanel.setPreferredSize(new Dimension(280, getHeight()));
        controlPanel.setBackground(BG_CARD);
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(new EmptyBorder(24, 20, 24, 20));

        JLabel ctrlTitle = new JLabel("Thao tác danh mục");
        ctrlTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        ctrlTitle.setForeground(COLOR_TEXT);
        ctrlTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(ctrlTitle);
        controlPanel.add(Box.createVerticalStrut(25));

        /// chỉnh lại default render của tree (xấu hết cả code)
        // hiển thị số tiền và hạn mức
        categoryTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                          boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

                if (sel) {
                    setForeground(getTextSelectionColor());
                    setBackground(getBackgroundSelectionColor());
                } else {
                    setForeground(tree.getForeground()); // Lấy màu chữ gốc của cây
                    setBackground(tree.getBackground()); // Lấy màu nền gốc của cây
                }
                /// hiển thị cây ///
                if (value instanceof DefaultMutableTreeNode) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                    if (node.getUserObject() instanceof String) {
                        String path = (String) node.getUserObject();
                        CategoryNode catNode = manager.getTree().getNodeByPath(path);

                        if (catNode != null) {
                            // tên gốc của danh mục
                            // tạo tên danh mục bằng html để thêm màu cho thu chi
                            StringBuilder sb = new StringBuilder("<html>");
                            sb.append(catNode.getName());
                            sb.append((" ___"));
/*
                            //   hạn mức
                            if (catNode.getBudget() > 0) {
                                sb.append(String.format(" <font color='#FF7043'> [%,.0f]</font>", catNode.getBudget()));
                            }
*/
                            // số tiền đã thu / chi thực tế
                            double total = manager.getTree().calculateTotalDfs(catNode);
                            if (total > 0) {
                                if ("CHI".equals(catNode.getCategoryType()) || path.startsWith("CHI/")) {
                                    // đỏ nhạt
                                    ///chi
                                    sb.append(String.format(" <font color='#FF7043'> [%,.0f]</font>", total));
                                } else {
                                    // xanh lá cây
                                    ///thu
                                    sb.append(String.format(" <font color='#4CAF50'> [%,.0f]</font>", total));
                                }
                            }



                            sb.append("</html>");

                            //gán chuỗi html hoàn chỉnh vào Node
                            setText(sb.toString());
                        }
                    }
                }
                return this;
            }
            /// bỏ màu default của tree
            @Override
            public Color getBackgroundNonSelectionColor() {
                // Trả về null hoặc màu nền của cây để nó hoàn toàn trong suốt
                return (categoryTree != null) ? categoryTree.getBackground() : super.getBackgroundNonSelectionColor();
            }

            @Override
            public Color getTextNonSelectionColor() {
                // Đảm bảo chữ khi không được chọn luôn lấy đúng màu text của hệ thống
                return (categoryTree != null) ? categoryTree.getForeground() : super.getTextNonSelectionColor();
            }
        });

        /// nút thao tác ///

        JButton btnAdd = createCtrlButton("Thêm danh mục con");
        btnAdd.addActionListener(e -> addCategoryAction());
        controlPanel.add(btnAdd);
        controlPanel.add(Box.createVerticalStrut(12));

        JButton btnRename = createCtrlButton("Đổi tên danh mục");
        btnRename.addActionListener(e -> renameCategoryAction());
        controlPanel.add(btnRename);
        controlPanel.add(Box.createVerticalStrut(12));

        JButton btnDelete = createCtrlButton("Xóa danh mục");
        btnDelete.addActionListener(e -> deleteCategoryAction());
        controlPanel.add(btnDelete);

        controlPanel.add(Box.createVerticalStrut(12));
/*
        JButton btnBudget = createCtrlButton("Cài đặt hạn mức");
        btnBudget.addActionListener(e -> setBudgetAction());
        controlPanel.add(btnBudget);
 */
        controlPanel.add(Box.createVerticalStrut(20));

        JPanel textnotePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        textnotePanel.setOpaque(false); //trong suốt (để hòa vào màu nền của thanh bên trái)

        // mã html vẽ các ô vuông màu kèm chữ
        JLabel textnoteLabel = new JLabel("<html><div style='font-family: Segoe UI; font-size: 11px; color: #AAAAAA;'>"
                + "<font color='#4CAF50'>■</font> Tổng thu &nbsp;&nbsp;"
                + "<font color='#FF7043'>■</font> Tổng chi"
                + "</div></html>");
        textnotePanel.add(textnoteLabel);
        //ép chiều cao giới hạn của textnotePanel để ko lỗi chiều cao nút ở trên
        textnotePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, textnotePanel.getPreferredSize().height));
        controlPanel.add(textnotePanel);
        //

        controlPanel.add(Box.createVerticalStrut(12));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, controlPanel);
        splitPane.setDividerLocation(480);
        splitPane.setBorder(new LineBorder(COLOR_BORDER, 1, true));
        splitPane.setOpaque(false);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JButton createCtrlButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(COLOR_TEXT);
        btn.setBackground(COLOR_BTN);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setBorder(new LineBorder(COLOR_BORDER, 1, true));
        btn.setFocusPainted(false);
        btn.setMaximumSize(new Dimension(240, 42));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(COLOR_BTN_HOVER);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(COLOR_BTN);
            }
        });
        return btn;
    }

    private void rebuildTree() {
        rootTreeNode.removeAllChildren();
        CategoryNode coreRoot = manager.getTree().getRoot();
        buildTreeRecursive(rootTreeNode, coreRoot);
        treeModel.reload();

        for (int i = 0; i < categoryTree.getRowCount(); i++) {
            categoryTree.expandRow(i);
        }
    }

    private void buildTreeRecursive(DefaultMutableTreeNode uiNode, CategoryNode coreNode) {
        for (CategoryNode child : coreNode.getChildren()) {
            double total = manager.getTree().calculateTotalDfs(child);
            String label = child.getName();
            if (total > 0) {
                label += String.format(" (%,.0f đ)", total);
            }
            DefaultMutableTreeNode childUiNode = new DefaultMutableTreeNode(label);
            childUiNode.setUserObject(child.getPath());
            uiNode.add(childUiNode);
            buildTreeRecursive(childUiNode, child);
        }
    }

    private String getSelectedNodePath() {
        TreePath path = categoryTree.getSelectionPath();
        if (path == null) return null;
        DefaultMutableTreeNode selected = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (selected == rootTreeNode) return "ROOT";
        return (String) selected.getUserObject();
    }

    private void addCategoryAction() {
        String parentPath = getSelectedNodePath();
        if (parentPath == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một danh mục cha từ cây bên trái.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String name = JOptionPane.showInputDialog(this, "Nhập tên danh mục mới:", "Thêm danh mục", JOptionPane.QUESTION_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;

        String[] options = {"Kế thừa từ cha", "THU (Thu nhập)", "CHI (Chi tiêu)"};
        int typeChoice = JOptionPane.showOptionDialog(this, "Chọn loại danh mục:", "Loại danh mục",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        String catType = null;
        if (typeChoice == 1) catType = "THU";
        else if (typeChoice == 2) catType = "CHI";

        boolean success = manager.addCategory(parentPath, name.trim(), catType);
        if (success) {
            rebuildTree();
        } else {
            JOptionPane.showMessageDialog(this, "Không thể thêm danh mục. Vui lòng kiểm tra lại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renameCategoryAction() {
        String path = getSelectedNodePath();
        if (path == null || path.equals("ROOT") || path.equals("THU") || path.equals("CHI")) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn danh mục hợp lệ.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String newName = JOptionPane.showInputDialog(this, "Nhập tên mới:", "Đổi tên", JOptionPane.QUESTION_MESSAGE);
        if (newName == null || newName.trim().isEmpty()) return;

        boolean success = manager.getTree().renameNode(path, newName.trim());
        if (success) {
            rebuildTree();
        } else {
            JOptionPane.showMessageDialog(this, "Đổi tên thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteCategoryAction() {
        String path = getSelectedNodePath();
        if (path == null || path.equals("ROOT") || path.equals("THU") || path.equals("CHI")) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn danh mục hợp lệ.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] options = {"CASCADE (Xóa hết)", "REPARENT (Chuyển lên cha)", "Hủy bỏ"};
        int choice = JOptionPane.showOptionDialog(this, "Chọn chế độ xóa:", "Xóa danh mục",
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);

        if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) return;

        String mode = (choice == 0) ? "CASCADE" : "REPARENT";
        boolean success = manager.removeCategory(path, mode);
        if (success) {
            rebuildTree();
        } else {
            JOptionPane.showMessageDialog(this, "Xóa danh mục thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    /// PANEL 3: ADD TRANSACTION & TABLE
    // =========================================================================
    private JPanel createTransactionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel titleLbl = new JLabel("NHẬP LIỆU GIAO DỊCH MỚI");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLbl.setForeground(COLOR_TEXT);
        titleLbl.setBorder(new EmptyBorder(0, 0, 15, 0));
        panel.add(titleLbl, BorderLayout.NORTH);

        JPanel inputCard = new JPanel(new GridBagLayout());
        inputCard.setBackground(BG_CARD);
        inputCard.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(20, 24, 20, 24)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 8, 6, 8);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        JLabel lblCat = new JLabel("Danh mục lá:");
        lblCat.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblCat.setForeground(COLOR_TEXT);
        inputCard.add(lblCat, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        cmbLeafCategories = new JComboBox<>();
        cmbLeafCategories.setBackground(BG_MAIN);
        cmbLeafCategories.setForeground(COLOR_TEXT);
        cmbLeafCategories.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        inputCard.add(cmbLeafCategories, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        JLabel lblAmount = new JLabel("Số tiền (VND):");
        lblAmount.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblAmount.setForeground(COLOR_TEXT);
        inputCard.add(lblAmount, gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        JTextField txtAmount = createStyledTextField();
        inputCard.add(txtAmount, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        JLabel lblDate = new JLabel("Ngày (YYYY-MM-DD):");
        lblDate.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblDate.setForeground(COLOR_TEXT);
        inputCard.add(lblDate, gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        JTextField txtDate = createStyledTextField();
        txtDate.setText(LocalDate.now().toString());
        inputCard.add(txtDate, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0;
        JLabel lblNote = new JLabel("Ghi chú:");
        lblNote.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblNote.setForeground(COLOR_TEXT);
        inputCard.add(lblNote, gbc);

        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0;
        JTextField txtNote = createStyledTextField();
        inputCard.add(txtNote, gbc);

        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 1.0;
        JButton btnAddTxn = new JButton("Ghi nhận giao dịch");
        styleAccentButton(btnAddTxn);
        btnAddTxn.addActionListener(e -> {
            String path = (String) cmbLeafCategories.getSelectedItem();
            String amountStr = txtAmount.getText().trim();
            String dateStr = txtDate.getText().trim();
            String note = txtNote.getText().trim();

            if (path == null || path.isEmpty()) {
                JOptionPane.showMessageDialog(MainGUI.this, "Không có danh mục nào.", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    JOptionPane.showMessageDialog(MainGUI.this, "Số tiền phải lớn hơn 0.", "Thông báo", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(MainGUI.this, "Số tiền không hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            LocalDate date;
            try {
                date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(MainGUI.this, "Định dạng ngày sai (YYYY-MM-DD).", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean success = manager.addTransaction(amount, date, note, path);
            if (success) {
                txtAmount.setText("");
                txtNote.setText("");
                txtDate.setText(LocalDate.now().toString());
                refreshTransactionTable();
                JOptionPane.showMessageDialog(MainGUI.this, "Thêm giao dịch thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(MainGUI.this, "Thêm giao dịch thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        inputCard.add(btnAddTxn, gbc);

        String[] columns = {"ID", "Ngày", "Số tiền", "Danh mục", "Ghi chú"};
        transactionTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(transactionTableModel);
        styleTable(table);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(new LineBorder(COLOR_BORDER, 2, true));
        tableScroll.getViewport().setBackground(BG_CARD);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JLabel recentLbl = new JLabel("Danh sách các giao dịch gần đây");
        recentLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        recentLbl.setForeground(COLOR_TEXT);
        recentLbl.setBorder(new EmptyBorder(15, 0, 8, 0));
        bottom.add(recentLbl, BorderLayout.NORTH);
        bottom.add(tableScroll, BorderLayout.CENTER);

        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        container.add(inputCard, BorderLayout.NORTH);
        container.add(bottom, BorderLayout.CENTER);

        panel.add(container, BorderLayout.CENTER);


        ////////////////////////////////
        ///////// padding chữ trong bảng

        PaddedTableCellRenderer paddingRenderer = new PaddedTableCellRenderer(10);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(paddingRenderer);
        }


        /// CSS riêng của cột tiền có căn lề phải
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                // gọi hàm cha để lấy giao diện chuẩn
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                // căn lề phải
                setHorizontalAlignment(JLabel.RIGHT);

                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        };
        ///
        table.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);

        ///////////////////////////////
        ///////////////////////////////

        return panel;
    }

    private void refreshTransactionForm() {
        cmbLeafCategories.removeAllItems();
        List<String> leaves = manager.listLeafCategories();
        for (String leaf : leaves) {
            cmbLeafCategories.addItem(leaf);
        }
        refreshTransactionTable();
    }

    private void refreshTransactionTable() {
        transactionTableModel.setRowCount(0);
        List<Transaction> list = manager.getAllTransactions(30);
        for (Transaction txn : list) {
            transactionTableModel.addRow(new Object[]{
                    txn.getTransactionId(),
                    txn.getDate().toString(),
                    String.format("%,.0f đ", txn.getAmount()),
                    txn.getCategoryPath(),
                    txn.getNote()
            });
        }
    }

    // =========================================================================
    /// PANEL 4: SEARCH & FILTER
    // =========================================================================
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel titleLbl = new JLabel("TÌM KIẾM & BỘ LỌC GIAO DỊCH");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLbl.setForeground(COLOR_TEXT);
        titleLbl.setBorder(new EmptyBorder(0, 0, 15, 0));
        panel.add(titleLbl, BorderLayout.NORTH);

        JPanel ctrlPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        ctrlPanel.setBackground(BG_CARD);
        ctrlPanel.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row1.setOpaque(false);
        JLabel lblKeyword = new JLabel("Từ khóa:");
        lblKeyword.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblKeyword.setForeground(COLOR_TEXT);
        JTextField txtKeyword = createStyledTextField();
        txtKeyword.setColumns(18);

        JButton btnKeyword = new JButton("Tìm từ khóa");
        styleAccentButton(btnKeyword);

        row1.add(lblKeyword); row1.add(txtKeyword); row1.add(btnKeyword);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row2.setOpaque(false);
        JLabel lblFrom = new JLabel("Từ ngày:");
        lblFrom.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblFrom.setForeground(COLOR_TEXT);
        JTextField txtFrom = createStyledTextField();
        txtFrom.setText("2024-01-01");
        txtFrom.setColumns(10);

        JLabel lblTo = new JLabel("Đến ngày:");
        lblTo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTo.setForeground(COLOR_TEXT);
        JTextField txtTo = createStyledTextField();
        txtTo.setText(LocalDate.now().toString());
        txtTo.setColumns(10);

        JButton btnDate = new JButton("Lọc ngày");
        styleAccentButton(btnDate);

        row2.add(lblFrom); row2.add(txtFrom);
        row2.add(lblTo); row2.add(txtTo);
        row2.add(btnDate);

        ctrlPanel.add(row1);
        ctrlPanel.add(row2);

        String[] columns = {"ID", "Ngày", "Số tiền", "Danh mục", "Ghi chú"};
        searchTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(searchTableModel);
        styleTable(table);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(new LineBorder(COLOR_BORDER, 1, true));
        tableScroll.getViewport().setBackground(BG_CARD);

        btnKeyword.addActionListener(e -> {
            String keyword = txtKeyword.getText().trim();
            List<Transaction> results = manager.searchTransactions(keyword);
            displaySearchResults(results);
        });

        btnDate.addActionListener(e -> {
            try {
                LocalDate from = LocalDate.parse(txtFrom.getText().trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                LocalDate to = LocalDate.parse(txtTo.getText().trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                List<Transaction> results = manager.filterByDate(from, to);
                displaySearchResults(results);
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(MainGUI.this, "Định dạng ngày nhập vào sai (YYYY-MM-DD).", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        container.setBorder(new EmptyBorder(10, 0, 0, 0));
        container.add(ctrlPanel, BorderLayout.NORTH);
        container.add(tableScroll, BorderLayout.CENTER);

        panel.add(container, BorderLayout.CENTER);

        ////////////////////////////////
        ///////// padding chữ trong bảng (bê nguyên trên xuống)

        PaddedTableCellRenderer paddingRenderer = new PaddedTableCellRenderer(10);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(paddingRenderer);
        }


        /// CSS riêng của cột tiền có căn lề phải
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                // gọi hàm cha để lấy giao diện chuẩn
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                // căn lề phải
                setHorizontalAlignment(JLabel.RIGHT);

                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        };
        ///
        table.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);

        ///////////////////////////////
        ///////////////////////////////
        return panel;
    }

    private void displaySearchResults(List<Transaction> list) {
        searchTableModel.setRowCount(0);
        for (Transaction txn : list) {
            searchTableModel.addRow(new Object[]{
                    txn.getTransactionId(),
                    txn.getDate().toString(),
                    String.format("%,.0f đ", txn.getAmount()),
                    txn.getCategoryPath(),
                    txn.getNote()
            });
        }
    }

    // =========================================================================
    /// PANEL 5: SAVE/LOAD DATA
    // =========================================================================
    private JPanel createFileIOPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel titleLbl = new JLabel("LƯU TRỮ & TẢI FILE DỮ LIỆU");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLbl.setForeground(COLOR_TEXT);
        titleLbl.setBorder(new EmptyBorder(0, 0, 15, 0));
        panel.add(titleLbl, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(BG_CARD);
        content.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(40, 40, 40, 40)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(15, 15, 15, 15);

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblFile = new JLabel("Tên file CSV:");
        lblFile.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblFile.setForeground(COLOR_TEXT);
        content.add(lblFile, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        JTextField txtFilename = createStyledTextField();
        txtFilename.setText("data.csv");
        txtFilename.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        content.add(txtFilename, gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        btnPanel.setOpaque(false);

        JButton btnSave = new JButton("Lưu vào file");
        styleAccentButton(btnSave);
        btnSave.addActionListener(e -> {
            String name = txtFilename.getText().trim();
            boolean success = manager.saveData(name);
            if (success) {
                JOptionPane.showMessageDialog(this, "Đã lưu dữ liệu thành công vào file '" + name + "'!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Lưu dữ liệu thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnLoad = new JButton("Tải từ file");
        styleAccentButton(btnLoad);
        btnLoad.addActionListener(e -> {
            String name = txtFilename.getText().trim();
            boolean success = manager.loadData(name);
            if (success) {
                JOptionPane.showMessageDialog(this, "Đã tải lại dữ liệu thành công từ file '" + name + "'!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Tải dữ liệu thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(btnSave);
        btnPanel.add(btnLoad);
        content.add(btnPanel, gbc);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(content, BorderLayout.NORTH);
        panel.add(wrapper, BorderLayout.CENTER);

        return panel;
    }

    // =========================================================================
    // HELPERS FOR STYLE
    // =========================================================================
    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setBackground(BG_MAIN);
        field.setForeground(COLOR_TEXT);
        field.setCaretColor(COLOR_TEXT);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        return field;
    }

    private void styleAccentButton(JButton button) {
        button.setBackground(COLOR_BTN);
        button.setForeground(COLOR_TEXT);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(8, 16, 8, 16));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(COLOR_BTN_HOVER);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(COLOR_BTN);
            }
        });
    }

    private void styleTable(JTable table) {
        table.setBackground(BG_CARD);
        table.setForeground(COLOR_TEXT);
        table.setGridColor(COLOR_BORDER);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        table.getTableHeader().setBackground(BG_MAIN);
        table.getTableHeader().setForeground(COLOR_TEXT);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));

        // Căn lề phải cho cột Số tiền
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        table.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);

        // --- ĐIỀU CHỈNH ĐỘ RỘNG CÁC CỘT CHO BẢNG ---
        // 0: Cột ID (Rất ngắn)
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(60);

        // 1: Cột Ngày (Vừa đủ chứa ngày tháng YYYY-MM-DD)
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setMaxWidth(110);

        // 2: Cột Số tiền
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setMaxWidth(160);

        // 3: Cột Danh mục (Hơi rộng)
        table.getColumnModel().getColumn(3).setPreferredWidth(200);

        // 4: Cột Ghi chú (Sẽ tự động chiếm toàn bộ phần diện tích còn lại)
        table.getColumnModel().getColumn(4).setPreferredWidth(300);
    }

    private void launchPythonGUI() {
        try {
            // Xác định đường dẫn tương đối từ thư mục chạy ứng dụng
            String pythonScriptPath = "src/python/main.py";
            
            // Tạo tiến trình chạy Python
            ProcessBuilder pb = new ProcessBuilder("python", pythonScriptPath);
            pb.directory(new java.io.File("."));
            
            // Khởi chạy tiến trình
            pb.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Không thể chạy ứng dụng Python.\nHãy chắc chắn rằng Python đã được cài đặt và cấu hình PATH.", 
                "Lỗi Khởi Chạy", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createPythonGUIPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel titleLbl = new JLabel("KIỂM THỬ GIAO DIỆN PYTHON (DESKTOP)");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLbl.setForeground(COLOR_TEXT);
        titleLbl.setBorder(new EmptyBorder(0, 0, 15, 0));
        panel.add(titleLbl, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(BG_CARD);
        content.setBorder(new EmptyBorder(40, 40, 40, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(15, 15, 15, 15);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel lblDesc = new JLabel("<html><body style='width: 450px;'>"
                + "<h3 style='color: white; margin-bottom: 8px;'>Chức năng khởi chạy Giao diện Python</h3>"
                + "<p style='color: #94A3B8; line-height: 1.4;'>"
                + "Bạn có thể khởi chạy ứng dụng Quản lý tài chính cá nhân viết bằng Python (sử dụng giao diện đồ họa Tkinter Desktop). "
                + "Cả hai ứng dụng Java và Python sẽ cùng chia sẻ dữ liệu để đối chiếu tính năng."
                + "</p>"
                + "<p style='color: #94A3B8; line-height: 1.4; margin-top: 10px;'>"
                + "<i>Lưu ý:</i> Hệ thống cần cấu hình sẵn trình biên dịch Python trong biến môi trường PATH."
                + "</p></body></html>");
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        content.add(lblDesc, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        JButton btnLaunch = new JButton("KHỞI CHẠY PYTHON GUI");
        styleAccentButton(btnLaunch);
        btnLaunch.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLaunch.addActionListener(e -> launchPythonGUI());
        content.add(btnLaunch, gbc);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(content, BorderLayout.NORTH);
        panel.add(wrapper, BorderLayout.CENTER);

        return panel;
    }
}