package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import controller.LoanController;
import config.AppConfig;
import model.Loan;
import model.User;
import util.FileHandler;
import java.awt.*;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * LoanHistoryView.java
 * Displays the loan history for the current student or member.
 */
public class LoanHistoryView extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final LoanController loanController;
    private final User currentUser;
    private final DefaultTableModel tableModel;
    private final JComboBox<String> viewModeCombo;
    private final JButton downloadButton;
    private List<Loan> currentLoans;

    public LoanHistoryView(User currentUser) {
        this.currentUser = currentUser;
        this.loanController = new LoanController();
        this.currentLoans = new ArrayList<>();

        setTitle("Loan History");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(980, 560);
        setLocationRelativeTo(null);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("My Loan History");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        content.add(title, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new Object[]{"Loan ID", "Book ID", "Issue Date", "Due Date", "Return Date", "Status", "Fine", "Fine Paid"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(tableModel);
        content.add(new JScrollPane(table), BorderLayout.CENTER);

        viewModeCombo = new JComboBox<>(new String[]{"My Records"});
        if (currentUser != null && currentUser.getAccessLevel() >= AppConfig.ACCESS_LIBRARIAN) {
            viewModeCombo.addItem("All Records");
        }
        viewModeCombo.addActionListener(e -> refreshLoanHistory());

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshLoanHistory());

        downloadButton = new JButton("Download History");
        downloadButton.setEnabled(false);
        downloadButton.addActionListener(e -> downloadLoanHistory());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(new JLabel("View:"));
        bottomPanel.add(viewModeCombo);
        bottomPanel.add(downloadButton);
        bottomPanel.add(refreshButton);
        content.add(bottomPanel, BorderLayout.SOUTH);

        add(content);
        refreshLoanHistory();
    }

    private void refreshLoanHistory() {
        tableModel.setRowCount(0);
        currentLoans.clear();
        if (currentUser == null) {
            downloadButton.setEnabled(false);
            return;
        }

        boolean showAll = currentUser.getAccessLevel() >= AppConfig.ACCESS_LIBRARIAN
                && "All Records".equals(viewModeCombo.getSelectedItem());
        List<Loan> loans = showAll
                ? loanController.getAllLoans()
                : loanController.getStudentLoans(currentUser.getUserId());
        if (loans == null) {
            downloadButton.setEnabled(false);
            return;
        }

        currentLoans.addAll(loans);
        for (Loan loan : currentLoans) {
            tableModel.addRow(new Object[]{
                    loan.getLoanId(),
                    loan.getBookId(),
                    loan.getUserId(),
                    loan.getIssueDate() != null ? loan.getIssueDate().format(DATE_FORMATTER) : "-",
                    loan.getDueDate() != null ? loan.getDueDate().format(DATE_FORMATTER) : "-",
                    loan.getReturnDate() != null ? loan.getReturnDate().format(DATE_FORMATTER) : "-",
                    loan.getStatus(),
                    String.format("%.2f", loan.getFine()),
                    loan.isFinePaid() ? "Yes" : "No"
            });
        }

        downloadButton.setEnabled(!currentLoans.isEmpty() && currentUser.getAccessLevel() >= AppConfig.ACCESS_LIBRARIAN);
    }

    private void downloadLoanHistory() {
        if (currentLoans == null || currentLoans.isEmpty()) {
            JOptionPane.showMessageDialog(this, "There is no loan history to export.", "Export Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Loan ID,Book ID,User ID,Issue Date,Due Date,Return Date,Status,Fine,Fine Paid,Remarks\n");
        for (Loan loan : currentLoans) {
            csv.append(escapeCsv(loan.getLoanId())).append(",")
               .append(escapeCsv(loan.getBookId())).append(",")
               .append(escapeCsv(loan.getUserId())).append(",")
               .append(escapeCsv(loan.getIssueDate() != null ? loan.getIssueDate().format(DATE_FORMATTER) : "")).append(",")
               .append(escapeCsv(loan.getDueDate() != null ? loan.getDueDate().format(DATE_FORMATTER) : "")).append(",")
               .append(escapeCsv(loan.getReturnDate() != null ? loan.getReturnDate().format(DATE_FORMATTER) : "")).append(",")
               .append(escapeCsv(loan.getStatus())).append(",")
               .append(String.format("%.2f", loan.getFine())).append(",")
               .append(loan.isFinePaid() ? "Yes" : "No").append(",")
               .append(escapeCsv(loan.getRemarks())).append("\n");
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Loan History");
        chooser.setSelectedFile(new File("loan-history.csv"));
        int option = chooser.showSaveDialog(this);
        if (option != JFileChooser.APPROVE_OPTION) {
            return;
        }

        String filePath = chooser.getSelectedFile().getAbsolutePath();
        if (FileHandler.writeToFile(filePath, csv.toString())) {
            JOptionPane.showMessageDialog(this, "Loan history exported to " + filePath, "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Unable to export loan history.", "Export Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
