
package com.booknest.ui;

import com.booknest.dao.BookDAO;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class AddBookFrame extends JFrame {

    private String email;
    private JTextField titleField, authorField, priceField;
    private JTextField branchField, subjectField, genreField;
    private JComboBox<String> categoryBox, conditionBox, typeBox, semesterBox;
    private JLabel imageLabel;
    private File selectedImage;

    public AddBookFrame(String email) {

        this.email = email;

        setTitle("BookNest - Add a Book");
        setSize(550, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        titleField = new JTextField();
        authorField = new JTextField();
        priceField = new JTextField("0");

        categoryBox = new JComboBox<>(
                new String[]{"EDUCATIONAL", "LITERATURE"}
        );

        conditionBox = new JComboBox<>(
                new String[]{"NEW", "LIKE_NEW", "GOOD", "ACCEPTABLE"}
        );

        typeBox = new JComboBox<>(
                new String[]{"SALE", "EXCHANGE", "BOTH", "FREE"}
        );

        branchField = new JTextField();
        subjectField = new JTextField();
        genreField = new JTextField();

        semesterBox = new JComboBox<>(new String[]{
                "1", "2", "3", "4", "5", "6", "7", "8"
        });

        imageLabel = new JLabel("No photo selected");

        JButton imageButton = new JButton("Choose Photo");
        JButton addButton = new JButton("Add Book");

        panel.add(new JLabel("Book Title:"));
        panel.add(titleField);

        panel.add(new JLabel("Author:"));
        panel.add(authorField);

        panel.add(new JLabel("Category:"));
        panel.add(categoryBox);

        panel.add(new JLabel("Condition:"));
        panel.add(conditionBox);

        panel.add(new JLabel("Listing Type:"));
        panel.add(typeBox);

        panel.add(new JLabel("Price (₹):"));
        panel.add(priceField);

        panel.add(new JLabel("Branch (Educational):"));
        panel.add(branchField);

        panel.add(new JLabel("Semester (Educational):"));
        panel.add(semesterBox);

        panel.add(new JLabel("Subject (Educational):"));
        panel.add(subjectField);

        panel.add(new JLabel("Genre (Literature):"));
        panel.add(genreField);

        panel.add(new JLabel("Book Photograph:"));
        panel.add(imageButton);

        panel.add(new JLabel("Selected Photo:"));
        panel.add(imageLabel);

        panel.add(new JLabel(""));
        panel.add(addButton);

        add(new JScrollPane(panel));

        imageButton.addActionListener(e -> chooseImage());

        categoryBox.addActionListener(e -> updateCategoryFields());

        addButton.addActionListener(e -> saveBook());

        updateCategoryFields();
    }

    private void updateCategoryFields() {

        boolean educational =
                categoryBox.getSelectedItem().equals("EDUCATIONAL");

        branchField.setEnabled(educational);
        semesterBox.setEnabled(educational);
        subjectField.setEnabled(educational);
        genreField.setEnabled(!educational);
    }

    private void chooseImage() {

        JFileChooser chooser = new JFileChooser();

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {

            selectedImage = chooser.getSelectedFile();

            imageLabel.setText(selectedImage.getName());
        }
    }

    private void saveBook() {

        try {

            String title = titleField.getText().trim();
            String author = authorField.getText().trim();

            if (title.isEmpty() || author.isEmpty()) {
                throw new IllegalArgumentException(
                        "Title and author are required."
                );
            }

            if (selectedImage == null) {
                throw new IllegalArgumentException(
                        "Please select a book photograph."
                );
            }

            String category = categoryBox.getSelectedItem().toString();
            String type = typeBox.getSelectedItem().toString();

            double price;

            try {
                price = Double.parseDouble(priceField.getText().trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Enter a valid price.");
            }

            if (price < 0) {
                throw new IllegalArgumentException("Price cannot be negative.");
            }

            if ((type.equals("SALE") || type.equals("BOTH")) && price <= 0) {
                throw new IllegalArgumentException(
                        "Enter a price greater than zero for SALE or BOTH."
                );
            }

            if (type.equals("FREE") || type.equals("EXCHANGE")) {
                price = 0;
            }

            String branch = null;
            String subject = null;
            String genre = null;
            Integer semester = null;

            if (category.equals("EDUCATIONAL")) {

                branch = branchField.getText().trim();
                subject = subjectField.getText().trim();
                semester = Integer.parseInt(
                        semesterBox.getSelectedItem().toString()
                );

                if (branch.isEmpty() || subject.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Enter branch and subject for educational books."
                    );
                }

            } else {

                genre = genreField.getText().trim();

                if (genre.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Enter a genre for literature books."
                    );
                }
            }

            Path uploadFolder = Paths.get("uploads");
            Files.createDirectories(uploadFolder);

            String originalName = selectedImage.getName();
            String extension = "";

            int dot = originalName.lastIndexOf(".");
            if (dot >= 0) {
                extension = originalName.substring(dot);
            }

            String fileName = UUID.randomUUID() + extension;

            Path destination = uploadFolder.resolve(fileName);

            Files.copy(
                    selectedImage.toPath(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            String imagePath = "uploads/" + fileName;

            BookDAO dao = new BookDAO();

            boolean saved = dao.addBook(
                    email,
                    title,
                    author,
                    category,
                    conditionBox.getSelectedItem().toString(),
                    type,
                    price,
                    imagePath,
                    branch,
                    semester,
                    subject,
                    genre
            );

            if (saved) {

                JOptionPane.showMessageDialog(
                        this,
                        "Book successfully listed on BookNest!"
                );

                dispose();
            }

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Invalid semester or price."
            );

        } catch (IllegalArgumentException e) {

            JOptionPane.showMessageDialog(
                    this,
                    e.getMessage()
            );

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Error: " + e.getMessage()
            );
        }
    }
}

