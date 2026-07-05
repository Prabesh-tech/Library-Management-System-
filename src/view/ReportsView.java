package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import controller.BookController;
import controller.LoanController;
import model.Book;
import model.Loan;
import model.User;
import service.FineService;
import util.DataManager;

public class ReportsView extends JFrame {

    private final BookController bookController;
    private final LoanController loanController;
    private final FineService fineService;
    private final DataManager dataManager;
    private final User currentUser;
    private JLabel booksLabel;
    private JLabel activeLoansLabel;
    private JLabel overdueLabel;
    private JLabel unpaidFinesLabel;
    private DefaultTableModel activeLoansModel;
    private DefaultTableModel overdueLoansModel;

    public ReportsView(User user) {
        this.currentUser = user;
        this.bookController = new BookController();
        this.loanController = new LoanController();
        this.fineService = new FineService();
        this.dataManager = DataManager.getInstance();

        setTitle(currentUser != null && currentUser.getAccessLevel() >= config.AppConfig.ACCESS_ADMIN ? "Reports" : "My Reports");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Overview", createOverviewPanel());
        tabs.addTab("Loan Activity", createLoanPanel());
        add(tabs);

        refreshReports();
    }

    private JPanel createOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel stats = new JPanel(new GridLayout(1, 4, 12, 12));
        booksLabel = new JLabel("<html><b>Total Books</b><br/>0</html>");
        activeLoansLabel = new JLabel("<html><b>Active Loans</b><br/>0</html>");
        overdueLabel = new JLabel("<html><b>Overdue Loans</b><br/>0</html>");
        unpaidFinesLabel = new JLabel("<html><b>Unpaid Fines</b><br/>0</html>");
        stats.add(booksLabel);
        stats.add(activeLoansLabel);
        stats.add(overdueLabel);
        stats.add(unpaidFinesLabel);
        panel.add(stats, BorderLayout.NORTH);

        JTextArea reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setLineWrap(true);
        reportArea.setWrapStyleWord(true);
        reportArea.setText("Library report summary will appear here.");
        panel.add(new JScrollPane(reportArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createLoanPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        activeLoansModel = new DefaultTableModel(new Object[]{"Loan ID", "Book", "Member", "Status", "Due Date"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        overdueLoansModel = new DefaultTableModel(new Object[]{"Loan ID", "Book", "Member", "Status", "Due Date"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JPanel tablesPanel = new JPanel(new GridLayout(1, 2, 12, 12));
        tablesPanel.add(createTablePanel("Active loans", activeLoansModel));
        tablesPanel.add(createTablePanel("Overdue loans", overdueLoansModel));
        panel.add(tablesPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTablePanel(String title, DefaultTableModel model) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JLabel(title, SwingConstants.CENTER), BorderLayout.NORTH);
        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void refreshReports() {
        List<Book> books = bookController.getAllBooks();
        List<Loan> activeLoans;
        List<Loan> overdueLoans;
        int unpaidFinCount;

        if (currentUser != null && currentUser.getAccessLevel() < config.AppConfig.ACCESS_ADMIN) {
            activeLoans = loanController.getStudentLoans(currentUser.getUserId()).stream()
                    .filter(l -> Loan.STATUS_ACTIVE.equals(l.getStatus()) || Loan.STATUS_OVERDUE.equals(l.getStatus()))
                    .toList();
            overdueLoans = loanController.getStudentLoans(currentUser.getUserId()).stream()
                    .filter(Loan::isOverdue)
                    .toList();
            unpaidFinCount = fineService.getUserFines(currentUser.getUserId()).stream().filter(f -> !f.isPaid()).toList().size();
            booksLabel.setText("<html><b>Total Books</b><br/>" + books.size() + "</html>");
            activeLoansLabel.setText("<html><b>My Active Loans</b><br/>" + activeLoans.size() + "</html>");
            overdueLabel.setText("<html><b>My Overdue Loans</b><br/>" + overdueLoans.size() + "</html>");
            unpaidFinesLabel.setText("<html><b>My Unpaid Fines</b><br/>" + unpaidFinCount + "</html>");
        } else {
            activeLoans = dataManager.getLoanRepository().findActive();
            overdueLoans = loanController.getOverdueLoans();
            unpaidFinCount = fineService.getUnpaidFines().size();
            booksLabel.setText("<html><b>Total Books</b><br/>" + books.size() + "</html>");
            activeLoansLabel.setText("<html><b>Active Loans</b><br/>" + activeLoans.size() + "</html>");
            overdueLabel.setText("<html><b>Overdue Loans</b><br/>" + overdueLoans.size() + "</html>");
            unpaidFinesLabel.setText("<html><b>Unpaid Fines</b><br/>" + unpaidFinCount + "</html>");
        }

        activeLoansModel.setRowCount(0);
        for (Loan loan : activeLoans) {
            activeLoansModel.addRow(new Object[]{loan.getLoanId(), loan.getBookId(), loan.getUserId(), loan.getStatus(), loan.getDueDate()});
        }

        overdueLoansModel.setRowCount(0);
        for (Loan loan : overdueLoans) {
            overdueLoansModel.addRow(new Object[]{loan.getLoanId(), loan.getBookId(), loan.getUserId(), loan.getStatus(), loan.getDueDate()});
        }
    }
}
