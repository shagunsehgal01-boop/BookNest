package com.booknest.ui;

import com.booknest.dao.BookDAO;
import com.booknest.model.Book;
import com.booknest.service.BookDSA;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BookListFrame extends JFrame {
    private static final Color NAVY = new Color(35, 48, 68);
    private static final Color CREAM = new Color(248, 247, 243);
    private static final Color MUTED = new Color(105, 112, 122);

    private final JTextField searchField = new JTextField(19);
    private final JComboBox<String> categoryBox = new JComboBox<>(
            new String[]{"ALL", "EDUCATIONAL", "LITERATURE"});
    private final JComboBox<String> sortBox = new JComboBox<>(
            new String[]{"Newest first", "Price: Low to High", "Title: A to Z"});
    private final JPanel booksPanel = new JPanel();
    private final String email;
    private List<Book> currentBooks = new ArrayList<>();
    private Map<Integer, Book> bookIdIndex;

    public BookListFrame(String category, String email) {
        this.email = email;
        setTitle("BookNest | Marketplace");
        setSize(1020, 760);
        setMinimumSize(new Dimension(820, 620));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(CREAM);
        setLayout(new BorderLayout(0, 0));

        JPanel header = new JPanel(new BorderLayout(12, 8));
        header.setBackground(NAVY);
        header.setBorder(new EmptyBorder(20, 25, 18, 25));
        JLabel title = new JLabel("Book Marketplace");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        JLabel subtitle = new JLabel("Find your next book, or give a book a new home.");
        subtitle.setForeground(new Color(220, 225, 232));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(subtitle);
        header.add(titleBox, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 12));
        controls.setBackground(CREAM);
        controls.setBorder(new EmptyBorder(8, 18, 2, 18));
        searchField.setToolTipText("Search by title or author");
        styleCombo(categoryBox);
        styleCombo(sortBox);
        JButton searchButton = styledButton("Search", NAVY, Color.WHITE);
        JButton exactButton = styledButton("Exact title", new Color(68, 112, 94), Color.WHITE);
        controls.add(new JLabel("Find"));
        controls.add(searchField);
        controls.add(categoryBox);
        controls.add(searchButton);
        controls.add(new JLabel("Sort"));
        controls.add(sortBox);
        controls.add(exactButton);

        booksPanel.setBackground(CREAM);
        booksPanel.setBorder(new EmptyBorder(12, 20, 20, 20));
        booksPanel.setLayout(new BoxLayout(booksPanel, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(booksPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CREAM);

        add(header, BorderLayout.NORTH);
        add(controls, BorderLayout.CENTER);
        add(scroll, BorderLayout.SOUTH);
        // Keep the marketplace list as the main expanding area.
        remove(scroll);
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(CREAM);
        content.add(controls, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        categoryBox.setSelectedItem(category);
        searchButton.addActionListener(e -> loadBooks());
        searchField.addActionListener(e -> loadBooks());
        categoryBox.addActionListener(e -> loadBooks());
        sortBox.addActionListener(e -> applySortAndRender());
        exactButton.addActionListener(e -> exactTitleSearch());
        loadBooks();
    }

    private void styleCombo(JComboBox<String> combo) {
        combo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        combo.setBackground(Color.WHITE);
        combo.setPreferredSize(new Dimension(155, 34));
    }

    private JButton styledButton(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(9, 15, 9, 15));
        return button;
    }

    private void loadBooks() {
        try {
            currentBooks = new ArrayList<>(new BookDAO().searchBooks(
                    searchField.getText(), categoryBox.getSelectedItem().toString()));
            bookIdIndex = BookDSA.createBookIdIndex(currentBooks);
            applySortAndRender();
        } catch (Exception ex) {
            showError("Unable to load books: " + ex.getMessage());
        }
    }

    private void applySortAndRender() {
        if (currentBooks == null) return;
        String sort = (String) sortBox.getSelectedItem();
        if ("Price: Low to High".equals(sort)) {
            BookDSA.mergeSortByPrice(currentBooks);
        } else if ("Title: A to Z".equals(sort)) {
            BookDSA.mergeSortByTitle(currentBooks);
        }
        renderBooks();
    }

    // Exact-title search deliberately loads the category's full available list,
    // sorts it using Merge Sort, and then applies Binary Search.
    private void exactTitleSearch() {
        String title = searchField.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter the exact book title first.");
            return;
        }
        try {
            List<Book> sorted = new ArrayList<>(new BookDAO().searchBooks("", categoryBox.getSelectedItem().toString()));
            BookDSA.mergeSortByTitle(sorted);
            int index = BookDSA.binarySearchTitle(sorted, title);
            if (index < 0) {
                currentBooks = new ArrayList<>();
                bookIdIndex = BookDSA.createBookIdIndex(currentBooks);
                renderBooks();
                JOptionPane.showMessageDialog(this, "No exact title match found.");
                return;
            }
            // Preserve separate listings when multiple students list the same title.
            String matchedTitle = sorted.get(index).getTitle();
            int first = index;
            int last = index;
            while (first > 0 && sorted.get(first - 1).getTitle().equalsIgnoreCase(matchedTitle)) first--;
            while (last + 1 < sorted.size() && sorted.get(last + 1).getTitle().equalsIgnoreCase(matchedTitle)) last++;
            // Keep every separately listed copy of the same title (O(log n + k)).
            currentBooks = new ArrayList<>(sorted.subList(first, last + 1));
            bookIdIndex = BookDSA.createBookIdIndex(currentBooks);
            renderBooks();
        } catch (Exception ex) {
            showError("Exact-title search failed: " + ex.getMessage());
        }
    }

    private void renderBooks() {
        booksPanel.removeAll();
        if (currentBooks == null || currentBooks.isEmpty()) {
            JLabel empty = new JLabel("No available books found.");
            empty.setFont(new Font("SansSerif", Font.PLAIN, 15));
            empty.setForeground(MUTED);
            empty.setBorder(new EmptyBorder(35, 10, 35, 10));
            booksPanel.add(empty);
        } else {
            for (Book book : currentBooks) {
                booksPanel.add(createBookCard(book));
                booksPanel.add(Box.createVerticalStrut(12));
            }
        }
        booksPanel.revalidate();
        booksPanel.repaint();
    }

    private JPanel createBookCard(Book book) {
        JPanel card = new JPanel(new BorderLayout(16, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 229, 224)),
                new EmptyBorder(13, 14, 13, 14)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel imageLabel = new JLabel("No Image", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(112, 145));
        imageLabel.setOpaque(true);
        imageLabel.setBackground(new Color(242, 241, 236));
        if (book.getImagePath() != null) {
            File imageFile = new File(book.getImagePath());
            if (!imageFile.isAbsolute()) imageFile = new File(System.getProperty("user.dir"), book.getImagePath());
            if (imageFile.exists()) {
                ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
                Image scaled = icon.getImage().getScaledInstance(112, 145, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaled));
                imageLabel.setText("");
            }
        }

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JLabel bookTitle = new JLabel(book.getTitle());
        bookTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
        bookTitle.setForeground(NAVY);
        details.add(bookTitle);
        details.add(Box.createVerticalStrut(5));
        details.add(info("by " + book.getAuthor()));
        details.add(info(book.getCategory() + "  •  " + book.getBookCondition()));
        details.add(info("Listed by " + book.getOwnerName()));
        String price = book.getListingType().equals("FREE") ? "FREE" :
                book.getListingType().equals("EXCHANGE") ? "Exchange only" : "₹" + book.getPrice();
        JLabel priceLabel = new JLabel(price);
        priceLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        priceLabel.setForeground(new Color(55, 116, 87));
        details.add(Box.createVerticalStrut(4));
        details.add(priceLabel);
        details.add(Box.createVerticalStrut(8));
        JButton detailsButton = styledButton("View Details", NAVY, Color.WHITE);
        detailsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsButton.addActionListener(e -> {
            // HashMap lookup by bookId retrieves the selected listing object.
            Book selected = bookIdIndex.get(book.getBookId());
            if (selected != null) new BookDetailsFrame(selected, email).setVisible(true);
        });
        details.add(detailsButton);

        card.add(imageLabel, BorderLayout.WEST);
        card.add(details, BorderLayout.CENTER);
        return card;
    }

    private JLabel info(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(MUTED);
        return label;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "BookNest", JOptionPane.ERROR_MESSAGE);
    }
}
