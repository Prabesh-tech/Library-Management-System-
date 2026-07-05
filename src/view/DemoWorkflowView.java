package view;

import javax.swing.*;
import java.awt.*;
import controller.BookController;
import controller.LoanController;
import model.Book;
import model.Loan;
import model.User;
import util.IDGenerator;

public class DemoWorkflowView extends JFrame {
    private final User currentUser;

    public DemoWorkflowView(User currentUser) {
        this.currentUser = currentUser;
        setTitle("Demo Workflow");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 560);
        setLocationRelativeTo(null);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Complete Library Workflow Demo");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        content.add(title, BorderLayout.NORTH);

        JTextArea steps = new JTextArea();
        steps.setEditable(false);
        steps.setText(buildDemoText());
        content.add(new JScrollPane(steps), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton runButton = new JButton("Run Demo Actions");
        runButton.addActionListener(e -> runDemoActions());
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        actions.add(runButton);
        actions.add(closeButton);
        content.add(actions, BorderLayout.SOUTH);

        add(content);
    }

    private String buildDemoText() {
        return String.join(System.lineSeparator(),
                "1. Login with a sample account.",
                "2. Add a new book to the catalog.",
                "3. Search for the title.",
                "4. Borrow the book using the issue window.",
                "5. Return the book and generate any fine.",
                "6. Review the reports and log out.");
    }

    private void runDemoActions() {
        try {
            BookController bookController = new BookController();
            LoanController loanController = new LoanController();

            Book demoBook = new Book(IDGenerator.generateBookId(), "Demo Workflow Book", "Demo Author", "978-9999999999", "Demo");
            demoBook.setCategory("Technology");
            demoBook.setImagePath("images/demo-book.png");
            bookController.addBook(demoBook);

            String borrowerId = currentUser != null ? currentUser.getLibraryId() : "STU-001";
            Loan loan = loanController.issueBook(IDGenerator.generateLoanId(), demoBook.getBookId(), borrowerId, currentUser != null ? currentUser.getUserId() : "SYSTEM", currentUser);
            loanController.returnBook(loan.getLoanId(), "Demo Workflow");

            JOptionPane.showMessageDialog(this,
                    "Demo completed. Added book '" + demoBook.getTitle() + "', issued it, and processed a return.",
                    "Workflow Demo",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Demo workflow failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
