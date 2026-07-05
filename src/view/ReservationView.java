package view;

import controller.ReservationController;
import model.Reservation;
import model.User;
import util.IDGenerator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReservationView extends JFrame {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final ReservationController reservationController;
    private final DefaultTableModel tableModel;
    private final User currentUser;

    public ReservationView(User currentUser) {
        this.currentUser = currentUser;
        this.reservationController = new ReservationController();

        setTitle("Reservations");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 520);
        setLocationRelativeTo(null);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Book Reservations");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        content.add(title, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new Object[]{"Reservation ID", "Book ID", "Member ID", "Reserved At", "Queue", "Expires At", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(tableModel);
        content.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel formPanel = new JPanel(new GridLayout(0, 2, 8, 8));
        JTextField bookIdField = new JTextField();
        JTextField memberIdField = new JTextField(currentUser != null ? currentUser.getLibraryId() : "");

        formPanel.add(new JLabel("Book ID:"));
        formPanel.add(bookIdField);
        formPanel.add(new JLabel("Member ID:"));
        formPanel.add(memberIdField);

        JButton reserveButton = new JButton("Reserve Book");
        reserveButton.addActionListener(e -> {
            String bookId = bookIdField.getText().trim();
            String memberId = memberIdField.getText().trim();
            if (bookId.isEmpty() || memberId.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Book ID and Member ID are required.",
                        "Invalid input", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                Reservation reservation = reservationController.reserveBook(IDGenerator.generateReservationId(), bookId, memberId);
                JOptionPane.showMessageDialog(this,
                        "Reservation created: " + reservation.getReservationId(),
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                bookIdField.setText("");
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to create reservation: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshTable());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        buttons.add(reserveButton);
        buttons.add(refreshButton);

        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
        bottomPanel.add(formPanel, BorderLayout.CENTER);
        bottomPanel.add(buttons, BorderLayout.SOUTH);
        content.add(bottomPanel, BorderLayout.SOUTH);

        add(content);
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        List<Reservation> reservations = currentUser != null
                ? reservationController.getReservationsForUser(currentUser.getUserId())
                : reservationController.getActiveReservations();
        for (Reservation reservation : reservations) {
            tableModel.addRow(new Object[]{
                    reservation.getReservationId(),
                    reservation.getBookId(),
                    reservation.getUserId(),
                    reservation.getReservedAt().format(FORMATTER),
                    reservation.getQueuePosition(),
                    reservation.getExpiresAt() != null ? reservation.getExpiresAt().format(FORMATTER) : "-",
                    reservation.getStatus()
            });
        }
    }
}
