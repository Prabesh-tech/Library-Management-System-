package view;

import model.Fine;
import model.User;
import service.FineService;
import service.AuthService;
import util.FileHandler;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * FinesView.java
 * Administrative fine management screen for librarians and admins.
 */
public class FinesView extends JFrame {

    private static final long serialVersionUID = 1L;
    private final FineService fineService;
    private final AuthService authService;
    private final User currentUser;
    private final DefaultTableModel finesTableModel;
    private final JComboBox<String> paymentMethodCombo;
    private final JLabel totalUnpaidLabel;
    private final JButton downloadButton;

    public FinesView(User currentUser) {
        this.currentUser = currentUser;
        this.fineService = new FineService();
        this.authService = new AuthService();

        boolean isStudent = currentUser != null && currentUser.getAccessLevel() < config.AppConfig.ACCESS_LIBRARIAN;
        setTitle(isStudent ? "My Fines" : "Fine Management");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 560);
        setLocationRelativeTo(null);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel header = new JLabel(isStudent ? "My Fines" : "Fine Management");
        header.setFont(new Font("Segoe UI", Font.BOLD, 20));
        content.add(header, BorderLayout.NORTH);

        finesTableModel = new DefaultTableModel(
                new Object[]{"Fine ID", "Loan ID", "User ID", "Amount", "Status", "Issued At", "Paid At", "Reason"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable finesTable = new JTable(finesTableModel);
        finesTable.setAutoCreateRowSorter(true);
        content.add(new JScrollPane(finesTable), BorderLayout.CENTER);

        totalUnpaidLabel = new JLabel("Total unpaid fines: 0.00");
        totalUnpaidLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        paymentMethodCombo = new JComboBox<>(new String[]{"CASH", "CARD", "ONLINE", "TRANSFER"});
        paymentMethodCombo.setSelectedIndex(0);

        JPanel topSummary = new JPanel(new BorderLayout(8, 8));
        topSummary.add(totalUnpaidLabel, BorderLayout.WEST);
        topSummary.add(paymentMethodCombo, BorderLayout.EAST);
        content.add(topSummary, BorderLayout.NORTH);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshFines());

        downloadButton = new JButton("Download Fines");
        downloadButton.setEnabled(authService.hasAccessLevel(currentUser, config.AppConfig.ACCESS_LIBRARIAN));
        downloadButton.addActionListener(e -> downloadFines());

        JButton markPaidButton = new JButton("Mark Selected Paid");
        markPaidButton.setEnabled(authService.hasPermission(currentUser, "manage_fines"));
        markPaidButton.addActionListener(e -> {
            int selectedRow = finesTable.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Select a fine to mark as paid.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String fineId = (String) finesTableModel.getValueAt(finesTable.convertRowIndexToModel(selectedRow), 0);
            String paymentMethod = (String) paymentMethodCombo.getSelectedItem();
            if (paymentMethod == null || paymentMethod.isEmpty()) {
                paymentMethod = "CASH";
            }

            if (fineService.markFinePaid(fineId, paymentMethod)) {
                JOptionPane.showMessageDialog(this, "Fine marked paid successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshFines();
            } else {
                JOptionPane.showMessageDialog(this, "Unable to mark fine as paid.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        actions.add(refreshButton);
        actions.add(downloadButton);
        actions.add(markPaidButton);
        actions.add(new JLabel("Payment method:"));
        actions.add(paymentMethodCombo);

        content.add(actions, BorderLayout.SOUTH);
        setContentPane(content);

        refreshFines();
    }

    private void refreshFines() {
        finesTableModel.setRowCount(0);
        List<Fine> fines = authService.hasAccessLevel(currentUser, config.AppConfig.ACCESS_LIBRARIAN)
                ? fineService.getAllFines()
                : fineService.getUserFines(currentUser.getUserId());

        double totalUnpaid = 0.0;
        for (Fine fine : fines) {
            finesTableModel.addRow(new Object[]{
                    fine.getFineId(),
                    fine.getLoanId(),
                    fine.getUserId(),
                    String.format("%.2f", fine.getAmount()),
                    fine.isPaid() ? "Paid" : "Unpaid",
                    fine.getIssuedAt(),
                    fine.getPaidAt() != null ? fine.getPaidAt() : "-",
                    fine.getReason()
            });
            if (!fine.isPaid()) {
                totalUnpaid += fine.getAmount();
            }
        }

        totalUnpaidLabel.setText(String.format("Total unpaid fines: %.2f", totalUnpaid));
        downloadButton.setEnabled(authService.hasAccessLevel(currentUser, config.AppConfig.ACCESS_LIBRARIAN) && !fines.isEmpty());
    }

    private void downloadFines() {
        List<Fine> fines = authService.hasAccessLevel(currentUser, config.AppConfig.ACCESS_LIBRARIAN)
                ? fineService.getAllFines()
                : fineService.getUserFines(currentUser.getUserId());

        if (fines == null || fines.isEmpty()) {
            JOptionPane.showMessageDialog(this, "There is no fine history to export.", "Export Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Fine ID,Loan ID,User ID,Amount,Status,Issued At,Paid At,Reason\n");
        for (Fine fine : fines) {
            csv.append(escapeCsv(fine.getFineId())).append(",")
               .append(escapeCsv(fine.getLoanId())).append(",")
               .append(escapeCsv(fine.getUserId())).append(",")
               .append(String.format("%.2f", fine.getAmount())).append(",")
               .append(fine.isPaid() ? "Paid" : "Unpaid").append(",")
               .append(escapeCsv(fine.getIssuedAt() != null ? fine.getIssuedAt().toString() : "")).append(",")
               .append(escapeCsv(fine.getPaidAt() != null ? fine.getPaidAt().toString() : "")).append(",")
               .append(escapeCsv(fine.getReason())).append("\n");
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Fine History");
        chooser.setSelectedFile(new File("fine-history.csv"));
        int option = chooser.showSaveDialog(this);
        if (option != JFileChooser.APPROVE_OPTION) {
            return;
        }

        String filePath = chooser.getSelectedFile().getAbsolutePath();
        if (FileHandler.writeToFile(filePath, csv.toString())) {
            JOptionPane.showMessageDialog(this, "Fine history exported to " + filePath, "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Unable to export fine history.", "Export Failed", JOptionPane.ERROR_MESSAGE);
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
