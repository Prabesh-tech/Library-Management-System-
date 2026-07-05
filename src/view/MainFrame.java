package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import config.AppConfig;
import model.User;
import service.AuthService;

/**
 * MainFrame.java
 * Application main window with left sidebar navigation and top bar.
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private User currentUser;
    private AuthService authService;

    public MainFrame(User user) {
        this.currentUser = user;
        this.authService = new AuthService();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Library Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setPreferredSize(new Dimension(0, 60));
        topBar.setBackground(new Color(37,99,235));

        JLabel title = new JLabel("  Library Management System");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        topBar.add(title, BorderLayout.WEST);

        JButton logoutTop = new JButton("Logout");
        logoutTop.addActionListener(e -> logout());
        topBar.add(logoutTop, BorderLayout.EAST);

        // Sidebar
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBackground(new Color(30,41,59));

        addSidebarButton(sidebar, "Dashboard", e -> showPanel("dashboard"));
        addSidebarButton(sidebar, "Books", e -> showPanel("books"));

        if (authService.hasAccessLevel(currentUser, AppConfig.ACCESS_LIBRARIAN)) {
            addSidebarButton(sidebar, "Categories", e -> {
                try {
                    openWindow(new CategoryView(currentUser));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error opening Categories: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            addSidebarButton(sidebar, "Authors", e -> openWindow(new AuthorView(currentUser)));
        }

        if (authService.hasAccessLevel(currentUser, AppConfig.ACCESS_SUPERADMIN)) {
            addSidebarButton(sidebar, "System Tools", e -> openWindow(new SystemToolsView(currentUser)));
        }

        if (authService.hasPermission(currentUser, "manage_users") || authService.hasAccessLevel(currentUser, AppConfig.ACCESS_ADMIN)) {
            addSidebarButton(sidebar, "Members", e -> openWindow(new UserView(currentUser)));
        }

        if (authService.hasAccessLevel(currentUser, AppConfig.ACCESS_STUDENT)) {
            addSidebarButton(sidebar, "Reports", e -> openWindow(new ReportsView(currentUser)));
        }

        if (authService.hasPermission(currentUser, "process_loans")) {
            addSidebarButton(sidebar, "Issue / Return", e -> openWindow(new IssueReturnView(currentUser)));
        }

        addSidebarButton(sidebar, "Fines", e -> openWindow(new FinesView(currentUser)));

        if (authService.hasAccessLevel(currentUser, AppConfig.ACCESS_STUDENT)) {
            addSidebarButton(sidebar, "Reservations", e -> openWindow(new ReservationView(currentUser)));
        }

        sidebar.add(Box.createVerticalGlue());

        // Content area
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.add(new DashboardPanel(currentUser), "dashboard");
        contentPanel.add(new BooksPanel(currentUser), "books");

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, contentPanel);
        split.setDividerLocation(220);
        split.setContinuousLayout(true);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(topBar, BorderLayout.NORTH);
        getContentPane().add(split, BorderLayout.CENTER);
    }

    private void addSidebarButton(JPanel sidebar, String title, ActionListener a) {
        JButton btn = new JButton(title);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.addActionListener(a);
        sidebar.add(Box.createRigidArea(new Dimension(0,8)));
        sidebar.add(btn);
    }

    private void showPanel(String name) {
        cardLayout.show(contentPanel, name);
    }

    private void openWindow(JFrame frame) {
        frame.setVisible(true);
    }

    private void logout() {
        dispose();
        new LoginView().setVisible(true);
    }

}
