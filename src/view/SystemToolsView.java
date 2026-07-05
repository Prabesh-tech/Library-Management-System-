package view;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import model.User;
import util.DataManager;

public class SystemToolsView extends JFrame {
    private final User currentUser;

    public SystemToolsView(User currentUser) {
        this.currentUser = currentUser;
        setTitle("System Tools");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 500);
        setLocationRelativeTo(null);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("System Tools");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        content.add(title, BorderLayout.NORTH);

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setText(buildSystemSummary());
        content.add(new JScrollPane(area), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton refreshButton = new JButton("Refresh Summary");
        refreshButton.addActionListener(e -> area.setText(buildSystemSummary()));
        JButton resetButton = new JButton("Reset Demo Data");
        resetButton.addActionListener(e -> {
            DataManager.getInstance().initializeWithDemoData();
            area.setText(buildSystemSummary());
            JOptionPane.showMessageDialog(this, "Demo data reset successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        });
        actions.add(refreshButton);
        actions.add(resetButton);
        content.add(actions, BorderLayout.SOUTH);

        add(content);
    }

    private String buildSystemSummary() {
        StringBuilder builder = new StringBuilder();
        builder.append("System overview\n");
        builder.append("===============\n");
        builder.append("User: ").append(currentUser != null ? currentUser.getName() : "Unknown").append("\n");
        builder.append("Role: ").append(currentUser != null ? currentUser.getRole() : "N/A").append("\n");
        builder.append("Timestamp: ").append(LocalDateTime.now()).append("\n\n");
        builder.append("Registered users: ").append(DataManager.getInstance().getUserRepository().findAll().size()).append("\n");
        builder.append("Books in catalog: ").append(DataManager.getInstance().getBookRepository().findActive().size()).append("\n");
        builder.append("Active loans: ").append(DataManager.getInstance().getLoanRepository().findActive().size()).append("\n");
        builder.append("Reservations: ").append(DataManager.getInstance().getReservationRepository().findAll().size()).append("\n");
        return builder.toString();
    }
}
