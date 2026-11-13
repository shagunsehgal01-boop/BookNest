
package com.booknest.dao;

import com.booknest.database.DBConnection;
import com.booknest.model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {

    // Add a new book to the database
    public boolean addBook(
            String email,
            String title,
            String author,
            String category,
            String condition,
            String listingType,
            double price,
            String imagePath,
            String branch,
            Integer semester,
            String subject,
            String genre
    ) throws SQLException {

        String query = """
            INSERT INTO books
            (student_id, title, author, category, book_condition,
             listing_type, price, image_path, branch, semester,
             subject, genre)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection con = DBConnection.getConnection()) {

            int studentId;

            String studentQuery =
                    "SELECT student_id FROM students WHERE email = ?";

            try (PreparedStatement ps =
                         con.prepareStatement(studentQuery)) {

                ps.setString(1, email);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Student account not found.");
                    }

                    studentId = rs.getInt("student_id");
                }
            }

            try (PreparedStatement ps = con.prepareStatement(query)) {

                ps.setInt(1, studentId);
                ps.setString(2, title);
                ps.setString(3, author);
                ps.setString(4, category);
                ps.setString(5, condition);
                ps.setString(6, listingType);
                ps.setDouble(7, price);
                ps.setString(8, imagePath);
                ps.setString(9, branch);

                if (semester == null) {
                    ps.setNull(10, Types.INTEGER);
                } else {
                    ps.setInt(10, semester);
                }

                ps.setString(11, subject);
                ps.setString(12, genre);

                return ps.executeUpdate() > 0;
            }
        }
    }

    // Search available books
    public List<Book> searchBooks(String keyword, String category)
            throws SQLException {

        List<Book> books = new ArrayList<>();

        String query = """
            SELECT b.book_id, b.student_id AS owner_id,
                   b.title, b.author, b.category,
                   b.book_condition, b.listing_type,
                   b.price, b.image_path,
                   s.name AS owner_name
            FROM books b
            JOIN students s ON b.student_id = s.student_id
            WHERE b.status = 'AVAILABLE'
            AND (b.title LIKE ? OR b.author LIKE ?)
            """;

        if (category != null && !category.equals("ALL")) {
            query += " AND b.category = ?";
        }

        query += " ORDER BY b.created_at DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            String search = "%" + keyword.trim() + "%";

            ps.setString(1, search);
            ps.setString(2, search);

            if (category != null && !category.equals("ALL")) {
                ps.setString(3, category);
            }

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    books.add(mapBook(rs));
                }
            }
        }

        return books;
    }

    // Get books listed by the currently logged-in student
    public List<Book> getOwnAvailableBooks(String email)
            throws SQLException {

        List<Book> books = new ArrayList<>();

        String query = """
            SELECT b.book_id, b.student_id AS owner_id,
                   b.title, b.author, b.category,
                   b.book_condition, b.listing_type,
                   b.price, b.image_path,
                   s.name AS owner_name
            FROM books b
            JOIN students s ON b.student_id = s.student_id
            WHERE s.email = ?
            AND b.status = 'AVAILABLE'
            ORDER BY b.created_at DESC
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    books.add(mapBook(rs));
                }
            }
        }

        return books;
    }

    // Convert a database row into a Book object
    private Book mapBook(ResultSet rs) throws SQLException {

        return new Book(
                rs.getInt("book_id"),
                rs.getInt("owner_id"),
                rs.getString("title"),
                rs.getString("author"),
                rs.getString("category"),
                rs.getString("book_condition"),
                rs.getString("listing_type"),
                rs.getDouble("price"),
                rs.getString("image_path"),
                rs.getString("owner_name")
        );
    }
}
