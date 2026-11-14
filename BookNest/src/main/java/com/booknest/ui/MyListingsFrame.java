
package com.booknest.ui;

import com.booknest.dao.BookDAO;
import com.booknest.model.Book;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.List;

public class MyListingsFrame extends JFrame {

    private final String email;
    private final BookDAO bookDAO;
    private JPanel listingsPanel;

    private static final Color NAVY = new Color(35, 48, 68);
    private static final Color CREAM = new Color(248, 247, 243);
    private static final Color GREEN = new Color(65, 117, 91);

    public MyListingsFrame(String email) {

        this.email = email;
        this.bookDAO = new BookDAO();

        setTitle("BookNest | My Listings");
        setSize(850, 650);
        setMinimumSize(new Dimension(700, 500));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(CREAM);
        mainPanel.setBorder(new EmptyBorder(22, 25, 22, 25));

        JLabel heading = new JLabel("My Listings");
        heading.setFont(new Font("SansSerif", Font.BOLD, 25));
        heading.setForeground(NAVY);

        JLabel subtitle = new JLabel(
                "Manage the books you have listed on BookNest."
        );
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(Color.GRAY);

        JPanel header = new JPanel();
        header.setBackground(CREAM);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        header.add(heading);
        header.add(Box.createVerticalStrut(6));
        header.add(subtitle);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadListings());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(CREAM);
        topPanel.add(header, BorderLayout.CENTER);
        topPanel.add(refreshButton, BorderLayout.EAST);

        listingsPanel = new JPanel();
        listingsPanel.setBackground(CREAM);
        listingsPanel.setLayout(
                new BoxLayout(listingsPanel, BoxLayout.Y_AXIS)
        );

        JScrollPane scrollPane = new JScrollPane(listingsPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(CREAM);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);

        loadListings();
    }

    // Load the logged-in owner's books
    private void loadListings() {

        listingsPanel.removeAll();

        try {

            List<Book> books = bookDAO.getOwnListings(email);

            if (books.isEmpty()) {

                JLabel emptyLabel = new JLabel(
                        "You haven't listed any books yet."
                );

                emptyLabel.setFont(
                        new Font("SansSerif", Font.PLAIN, 15)
                );

                listingsPanel.add(emptyLabel);

            } else {

                for (Book book : books) {

                    listingsPanel.add(createBookCard(book));
                    listingsPanel.add(
                            Box.createVerticalStrut(12)
                    );
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to load your listings: " + e.getMessage()
            );
        }

        listingsPanel.revalidate();
        listingsPanel.repaint();
    }

    // Create an individual book card
    private JPanel createBookCard(Book book) {

        JPanel card = new JPanel(new BorderLayout(15, 10));

        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new Color(225, 225, 220)
                ),
                new EmptyBorder(15, 15, 15, 15)
        ));

        card.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 190)
        );

        JLabel imageLabel = new JLabel("No Image");
        imageLabel.setPreferredSize(new Dimension(105, 145));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        if (book.getImagePath() != null) {

            File imageFile = new File(book.getImagePath());

            if (imageFile.exists()) {

                ImageIcon icon = new ImageIcon(book.getImagePath());

                Image scaled = icon.getImage().getScaledInstance(
                        105, 145, Image.SCALE_SMOOTH
                );

                imageLabel.setIcon(new ImageIcon(scaled));
                imageLabel.setText("");
            }
        }

        JPanel details = new JPanel();
        details.setBackground(Color.WHITE);
        details.setLayout(
                new BoxLayout(details, BoxLayout.Y_AXIS)
        );

        JLabel title = new JLabel(book.getTitle());
        title.setFont(new Font("SansSerif", Font.BOLD, 17));
        title.setForeground(NAVY);

        details.add(title);
        details.add(Box.createVerticalStrut(7));

        details.add(new JLabel("Author: " + book.getAuthor()));
        details.add(new JLabel("Category: " + book.getCategory()));
        details.add(new JLabel("Condition: " + book.getBookCondition()));
        details.add(new JLabel("Listing Type: " + book.getListingType()));

        if ("FREE".equals(book.getListingType())) {

            details.add(new JLabel("Price: FREE"));

        } else if ("EXCHANGE".equals(book.getListingType())) {

            details.add(new JLabel("Price: Exchange only"));

        } else {

            details.add(new JLabel("Price: ₹" + book.getPrice()));
        }

        boolean removable = false;

        try {
            removable = bookDAO.canRemoveListing(
                    book.getBookId(),
                    email
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        JButton removeButton = new JButton("Remove Listing");

        removeButton.setForeground(Color.WHITE);
        removeButton.setBackground(new Color(170, 65, 65));
        removeButton.setFocusPainted(false);
        removeButton.setEnabled(removable);

        if (removable) {

            removeButton.addActionListener(e ->
                    removeListing(book)
            );

        } else {

            removeButton.setToolTipText(
                    "This book cannot be removed because it is unavailable or has a scheduled transaction."
            );
        }

        JPanel actionPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );

        actionPanel.setBackground(Color.WHITE);
        actionPanel.add(removeButton);

        if (!removable) {

            JLabel status = new JLabel(
                    "Removal unavailable"
            );

            status.setForeground(Color.GRAY);
            actionPanel.add(status);
        }

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(Color.WHITE);
        rightPanel.add(details, BorderLayout.CENTER);
        rightPanel.add(actionPanel, BorderLayout.SOUTH);

        card.add(imageLabel, BorderLayout.WEST);
        card.add(rightPanel, BorderLayout.CENTER);

        return card;
    }

    // Confirm and remove a listing
    private void removeListing(Book book) {

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to remove \"" +
                        book.getTitle() +
                        "\" from your listings?\n\n" +
                        "It will no longer appear in Search Books.",
                "Confirm Removal",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {

            bookDAO.removeListing(
                    book.getBookId(),
                    email
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Your book listing has been removed successfully."
            );

            loadListings();

        } catch (Exception e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    "Unable to Remove Listing",
                    JOptionPane.WARNING_MESSAGE
            );

            loadListings();
        }
    }
}
