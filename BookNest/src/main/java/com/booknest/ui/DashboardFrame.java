
package com.booknest.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class DashboardFrame extends JFrame {

    private static final Color NAVY = new Color(35, 48, 68);
    private static final Color CREAM = new Color(248, 247, 243);
    private static final Color GREEN = new Color(65, 117, 91);

    private final String email;

    public DashboardFrame(String studentName, String email) {

        this.email = email;

        setTitle("BookNest | Dashboard");
        setSize(850, 690);
        setMinimumSize(new Dimension(760, 620));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        getContentPane().setBackground(CREAM);
        setLayout(new BorderLayout());

        // HEADER
        JPanel header = new JPanel();
        header.setBackground(NAVY);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(28, 38, 27, 38));

        JLabel brand = new JLabel("BookNest");
        brand.setForeground(new Color(231, 215, 180));
        brand.setFont(new Font("SansSerif", Font.BOLD, 15));

        JLabel welcome = new JLabel("Welcome, " + studentName + "!");
        welcome.setForeground(Color.WHITE);
        welcome.setFont(new Font("SansSerif", Font.BOLD, 28));

        JLabel subtitle = new JLabel(
                "Discover, exchange and share university books."
        );
        subtitle.setForeground(new Color(220, 225, 232));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));

        header.add(brand);
        header.add(Box.createVerticalStrut(12));
        header.add(welcome);
        header.add(Box.createVerticalStrut(5));
        header.add(subtitle);

        // MAIN CONTENT
        JPanel content = new JPanel(new BorderLayout(15, 18));
        content.setBackground(CREAM);
        content.setBorder(new EmptyBorder(24, 35, 28, 35));

        JLabel section = new JLabel("Explore BookNest");
        section.setFont(new Font("SansSerif", Font.BOLD, 19));
        section.setForeground(NAVY);

        content.add(section, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 14, 14));
        grid.setBackground(CREAM);

        grid.add(createActionCard(
                "Educational Books",
                "Find semester-wise textbooks",
                () -> new BookListFrame("EDUCATIONAL", email).setVisible(true)
        ));

        grid.add(createActionCard(
                "Literature Books",
                "Explore novels and literature",
                () -> new BookListFrame("LITERATURE", email).setVisible(true)
        ));

        grid.add(createActionCard(
                "Explore All Books",
                "Browse every available listing",
                () -> new BookListFrame("ALL", email).setVisible(true)
        ));

        grid.add(createActionCard(
                "List Your Book",
                "Sell, exchange or share a book",
                () -> new AddBookFrame(email).setVisible(true)
        ));

        grid.add(createActionCard(
                "My Listings",
                "View or remove your listed books",
                () -> new MyListingsFrame(email).setVisible(true)
        ));

        grid.add(createActionCard(
                "Incoming Requests",
                "Review requests for your books",
                () -> new IncomingRequestsFrame(email).setVisible(true)
        ));

        grid.add(createActionCard(
                "My Transactions",
                "Manage campus handovers",
                () -> new MyTransactionsFrame(email).setVisible(true)
        ));

        content.add(grid, BorderLayout.CENTER);

        // FOOTER
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(CREAM);

        JButton logout = new JButton("Logout");
        logout.setFont(new Font("SansSerif", Font.BOLD, 12));
        logout.setForeground(Color.WHITE);
        logout.setBackground(NAVY);
        logout.setFocusPainted(false);
        logout.setBorder(new EmptyBorder(10, 22, 10, 22));

        logout.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });

        footer.add(logout);
        content.add(footer, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
    }

    // ACTION CARD
    private JPanel createActionCard(
            String title,
            String description,
            Runnable action
    ) {

        JPanel card = new JPanel(new BorderLayout(5, 9));
        card.setBackground(Color.WHITE);

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new Color(229, 229, 224)
                ),
                new EmptyBorder(16, 17, 15, 17)
        ));

        JLabel name = new JLabel(title);
        name.setFont(new Font("SansSerif", Font.BOLD, 15));
        name.setForeground(NAVY);

        JLabel desc = new JLabel(description);
        desc.setFont(new Font("SansSerif", Font.PLAIN, 12));
        desc.setForeground(new Color(105, 112, 122));

        JButton open = new JButton("Open  →");
        open.setFont(new Font("SansSerif", Font.BOLD, 12));
        open.setForeground(GREEN);
        open.setBackground(Color.WHITE);
        open.setFocusPainted(false);
        open.setBorder(BorderFactory.createEmptyBorder(7, 0, 0, 0));
        open.setHorizontalAlignment(SwingConstants.LEFT);

        // Show actual errors instead of silently failing.
        Runnable safeAction = () -> {
            try {
                action.run();
            } catch (Exception ex) {
                ex.printStackTrace();

                JOptionPane.showMessageDialog(
                        this,
                        "Unable to open " + title + ":\n"
                                + ex.getMessage(),
                        "BookNest Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        };

        open.addActionListener(e -> safeAction.run());

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        text.add(name);
        text.add(Box.createVerticalStrut(5));
        text.add(desc);
        text.add(open);

        card.add(text, BorderLayout.CENTER);

        // Make the card itself clickable too.
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getSource() == card) {
                    safeAction.run();
                }
            }
        });

        return card;
    }
}
