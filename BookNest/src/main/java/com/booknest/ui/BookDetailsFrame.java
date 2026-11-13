
package com.booknest.ui;

import com.booknest.dao.BookDAO;
import com.booknest.dao.RequestDAO;
import com.booknest.model.Book;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

public class BookDetailsFrame extends JFrame {

    private Book book;
    private String email;
    private JComboBox<Book> offeredBookBox;
    private JButton exchangeButton;

    public BookDetailsFrame(Book book, String email) {

        this.book = book;
        this.email = email;

        setTitle("BookNest - Book Details");
        setSize(650, 520);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(20, 15));

        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(25, 25, 25, 25)
        );

        JLabel imageLabel = new JLabel("No Image");

        imageLabel.setPreferredSize(new Dimension(180, 230));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        if (book.getImagePath() != null) {

            File imageFile = new File(book.getImagePath());

            if (imageFile.exists()) {

                ImageIcon icon = new ImageIcon(book.getImagePath());

                Image scaled = icon.getImage().getScaledInstance(
                        180, 230, Image.SCALE_SMOOTH
                );

                imageLabel.setIcon(new ImageIcon(scaled));
                imageLabel.setText("");
            }
        }

        JPanel detailsPanel = new JPanel();

        detailsPanel.setLayout(
                new BoxLayout(detailsPanel, BoxLayout.Y_AXIS)
        );

        JLabel heading = new JLabel(book.getTitle());

        heading.setFont(new Font("Arial", Font.BOLD, 23));

        detailsPanel.add(heading);
        detailsPanel.add(Box.createVerticalStrut(15));

        detailsPanel.add(new JLabel("Author: " + book.getAuthor()));
        detailsPanel.add(new JLabel("Category: " + book.getCategory()));
        detailsPanel.add(new JLabel("Condition: " + book.getBookCondition()));
        detailsPanel.add(new JLabel("Listing Type: " + book.getListingType()));
        detailsPanel.add(new JLabel("Owner: " + book.getOwnerName()));

        if (book.getListingType().equals("FREE")) {
            detailsPanel.add(new JLabel("Price: FREE"));
        } else if (book.getListingType().equals("EXCHANGE")) {
            detailsPanel.add(new JLabel("Price: Exchange only"));
        } else {
            detailsPanel.add(new JLabel("Price: ₹" + book.getPrice()));
        }

        JPanel actionPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 12, 10)
        );

        String type = book.getListingType();

        if (type.equals("SALE") || type.equals("BOTH")) {

            JButton buyButton = new JButton("Send Buy Request");

            buyButton.addActionListener(e ->
                    sendRequest("BUY", null)
            );

            actionPanel.add(buyButton);
        }

        if (type.equals("FREE")) {

            JButton freeButton = new JButton("Request Free Book");

            freeButton.addActionListener(e ->
                    sendRequest("FREE", null)
            );

            actionPanel.add(freeButton);
        }

        if (type.equals("EXCHANGE") || type.equals("BOTH")) {

            offeredBookBox = new JComboBox<>();

            exchangeButton = new JButton("Send Exchange Request");

            loadOwnBooks();

            exchangeButton.addActionListener(e -> {

                Book offeredBook =
                        (Book) offeredBookBox.getSelectedItem();

                if (offeredBook == null) {
                    JOptionPane.showMessageDialog(
                            this,
                            "You need to list an available book before requesting an exchange."
                    );
                    return;
                }

                sendRequest(
                        "EXCHANGE",
                        offeredBook.getBookId()
                );
            });

            actionPanel.add(new JLabel("Your offered book:"));
            actionPanel.add(offeredBookBox);
            actionPanel.add(exchangeButton);
        }

        mainPanel.add(imageLabel, BorderLayout.WEST);
        mainPanel.add(detailsPanel, BorderLayout.CENTER);
        mainPanel.add(actionPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    // Load the logged-in student's own available books
    private void loadOwnBooks() {

        offeredBookBox.removeAllItems();

        try {

            BookDAO dao = new BookDAO();

            List<Book> ownBooks = dao.getOwnAvailableBooks(email);

            for (Book ownBook : ownBooks) {

                if (ownBook.getBookId() != book.getBookId()) {
                    offeredBookBox.addItem(ownBook);
                }
            }

            if (offeredBookBox.getItemCount() == 0) {

                exchangeButton.setEnabled(false);

                JOptionPane.showMessageDialog(
                        this,
                        "You have no other available books to offer. List a book first."
                );
            }

        } catch (Exception e) {

            exchangeButton.setEnabled(false);

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to load your books: " + e.getMessage()
            );
        }
    }

    // Send the selected request to MySQL
    private void sendRequest(String requestType, Integer offeredBookId) {

        try {

            RequestDAO dao = new RequestDAO();

            dao.createRequest(
                    book.getBookId(),
                    email,
                    requestType,
                    offeredBookId
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Your " + requestType + " request has been sent successfully!\nThe book owner can now review it."
            );

        } catch (IllegalArgumentException e) {

            JOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    "Request Error",
                    JOptionPane.WARNING_MESSAGE
            );

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to send request: " + e.getMessage()
            );
        }
    }
}

