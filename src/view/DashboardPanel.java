package view;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import model.Book;
import model.Loan;
import model.User;
import repository.BookRepository;
import repository.LoanRepository;
import repository.UserRepository;
import util.DataManager;

public class DashboardPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final User currentUser;

    public DashboardPanel(User user) {
        this.currentUser = user;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Welcome back, " + user.getName());
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        actionPanel.add(createActionButton("View Books", "books"));
        actionPanel.add(createActionButton("Quick Demo", "demo"));
        if (user.getAccessLevel() >= config.AppConfig.ACCESS_LIBRARIAN) {
            actionPanel.add(createActionButton("Manage Categories", "categories"));
            actionPanel.add(createActionButton("Manage Authors", "authors"));
            actionPanel.add(createActionButton("Reports", "reports"));
        }
        if (user.getAccessLevel() >= config.AppConfig.ACCESS_STUDENT) {
            actionPanel.add(createActionButton("Reservations", "reservations"));
            actionPanel.add(createActionButton("My Loans", "loan_history"));
        }

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(title, BorderLayout.NORTH);
        topPanel.add(actionPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        JTextArea summaryArea = new JTextArea();
        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setText(buildSummaryText());
        add(new JScrollPane(summaryArea), BorderLayout.CENTER);
    }

    private JButton createActionButton(String label, String action) {
        JButton button = new JButton(label);
        button.addActionListener(e -> {
            switch (action) {
                case "books":
                    SwingUtilities.invokeLater(() -> new BookView(currentUser).setVisible(true));
                    break;
                case "demo":
                    SwingUtilities.invokeLater(() -> new DemoWorkflowView(currentUser).setVisible(true));
                    break;
                case "categories":
                    SwingUtilities.invokeLater(() -> new CategoryView(currentUser).setVisible(true));
                    break;
                case "authors":
                    SwingUtilities.invokeLater(() -> new AuthorView(currentUser).setVisible(true));
                    break;
                case "reports":
                    SwingUtilities.invokeLater(() -> new ReportsView(currentUser).setVisible(true));
                    break;
                case "reservations":
                    SwingUtilities.invokeLater(() -> new ReservationView(currentUser).setVisible(true));
                    break;
                case "loan_history":
                    SwingUtilities.invokeLater(() -> new LoanHistoryView(currentUser).setVisible(true));
                    break;
                default:
                    break;
            }
        });
        return button;
    }

    private String buildSummaryText() {
        DataManager dataManager = DataManager.getInstance();
        BookRepository bookRepository = dataManager.getBookRepository();
        LoanRepository loanRepository = dataManager.getLoanRepository();
        UserRepository userRepository = dataManager.getUserRepository();

        List<Book> books = bookRepository.findActive();
        List<Loan> loans = loanRepository.findActive();
        List<Loan> overdue = loanRepository.findOverdue();
        List<User> users = userRepository.findAll();

        StringBuilder builder = new StringBuilder();
        builder.append("Library overview\n");
        builder.append("=================\n");
        builder.append("Active books: ").append(books.size()).append("\n");
        builder.append("Active loans: ").append(loans.size()).append("\n");
        builder.append("Overdue loans: ").append(overdue.size()).append("\n");
        builder.append("Registered users: ").append(users.size()).append("\n\n");
        builder.append("Recent books:\n");
        for (Book book : books.subList(0, Math.min(5, books.size()))) {
            builder.append("- ").append(book.getTitle()).append(" by ").append(book.getAuthor())
                    .append(" (available: ").append(book.getAvailableCopies()).append(")\n");
        }
        return builder.toString();
    }
}
