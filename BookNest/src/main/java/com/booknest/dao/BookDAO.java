
package com.booknest.dao;

import com.booknest.database.DBConnection;
import com.booknest.model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {

    // 1. ADD A NEW BOOK
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

        String studentQuery =
                "SELECT student_id FROM students WHERE email = ?";

        String insertQuery = """
            INSERT INTO books
            (student_id, title, author, category,
             book_condition, listing_type, price, image_path,
             branch, semester, subject, genre)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection con = DBConnection.getConnection()) {

            int studentId;

            try (PreparedStatement ps =
                         con.prepareStatement(studentQuery)) {

                ps.setString(1, email);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        return false;
                    }

                    studentId = rs.getInt("student_id");
                }
            }

            try (PreparedStatement ps =
                         con.prepareStatement(insertQuery)) {

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


    // 2. SEARCH AVAILABLE BOOKS
    public List<Book> searchBooks(
            String keyword,
            String category
    ) throws SQLException {

        List<Book> books = new ArrayList<>();

        String query = """
            SELECT b.book_id,
                   b.student_id AS owner_id,
                   b.title,
                   b.author,
                   b.category,
                   b.book_condition,
                   b.listing_type,
                   b.price,
                   b.image_path,
                   s.name AS owner_name
            FROM books b
            JOIN students s
                ON b.student_id = s.student_id
            WHERE b.status = 'AVAILABLE'
            AND (
                b.title LIKE ?
                OR b.author LIKE ?
            )
            AND (? = 'ALL' OR b.category = ?)
            ORDER BY b.created_at DESC
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            String searchKeyword = "%" + keyword + "%";

            ps.setString(1, searchKeyword);
            ps.setString(2, searchKeyword);
            ps.setString(3, category);
            ps.setString(4, category);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    books.add(mapBook(rs));
                }
            }
        }

        return books;
    }


    // 3. GET OWNER'S AVAILABLE BOOKS
    // Used for selecting a book during an exchange request.
    public List<Book> getOwnAvailableBooks(String email)
            throws SQLException {

        List<Book> books = new ArrayList<>();

        String query = """
            SELECT b.book_id,
                   b.student_id AS owner_id,
                   b.title,
                   b.author,
                   b.category,
                   b.book_condition,
                   b.listing_type,
                   b.price,
                   b.image_path,
                   s.name AS owner_name
            FROM books b
            JOIN students s
                ON b.student_id = s.student_id
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


    // 4. GET ALL LISTINGS OF THE LOGGED-IN STUDENT
    // Reserved and unavailable books remain visible.
    // Removed books are excluded.
    public List<Book> getOwnListings(String email)
            throws SQLException {

        List<Book> books = new ArrayList<>();

        String query = """
            SELECT b.book_id,
                   b.student_id AS owner_id,
                   b.title,
                   b.author,
                   b.category,
                   b.book_condition,
                   b.listing_type,
                   b.price,
                   b.image_path,
                   s.name AS owner_name
            FROM books b
            JOIN students s
                ON b.student_id = s.student_id
            WHERE s.email = ?
            AND b.status <> 'REMOVED'
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


    // 5. CHECK WHETHER A BOOK CAN BE REMOVED
    // A book with a scheduled transaction cannot be removed.
    public boolean canRemoveListing(int bookId, String email)
            throws SQLException {

        String query = """
            SELECT b.book_id
            FROM books b
            JOIN students s
                ON b.student_id = s.student_id
            WHERE b.book_id = ?
            AND s.email = ?
            AND b.status IN ('AVAILABLE', 'RESERVED')
            AND NOT EXISTS (
                SELECT 1
                FROM book_requests br
                JOIN transactions t
                    ON t.request_id = br.request_id
                WHERE br.book_id = b.book_id
                   OR br.offered_book_id = b.book_id
            )
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setInt(1, bookId);
            ps.setString(2, email);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }


    // 6. REMOVE A LISTING
    public void removeListing(int bookId, String email)
            throws SQLException {

        try (Connection con = DBConnection.getConnection()) {

            con.setAutoCommit(false);

            try {

                int ownerId;
                String bookStatus;

                // Verify ownership and lock the book.
                String bookQuery = """
                    SELECT b.student_id, b.status
                    FROM books b
                    JOIN students s
                        ON b.student_id = s.student_id
                    WHERE b.book_id = ?
                    AND s.email = ?
                    FOR UPDATE
                    """;

                try (PreparedStatement ps =
                             con.prepareStatement(bookQuery)) {

                    ps.setInt(1, bookId);
                    ps.setString(2, email);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Book not found or you are not its owner."
                            );
                        }

                        ownerId = rs.getInt("student_id");
                        bookStatus = rs.getString("status");
                    }
                }

                if (!"AVAILABLE".equals(bookStatus)
                        && !"RESERVED".equals(bookStatus)) {

                    throw new IllegalArgumentException(
                            "This listing cannot be removed."
                    );
                }


                // Check whether a handover was ever scheduled.
                String transactionQuery = """
                    SELECT t.transaction_id
                    FROM transactions t
                    JOIN book_requests br
                        ON t.request_id = br.request_id
                    WHERE br.book_id = ?
                       OR br.offered_book_id = ?
                    LIMIT 1
                    """;

                try (PreparedStatement ps =
                             con.prepareStatement(transactionQuery)) {

                    ps.setInt(1, bookId);
                    ps.setInt(2, bookId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (rs.next()) {
                            throw new IllegalArgumentException(
                                    "This book has already been scheduled for a handover and cannot be removed."
                            );
                        }
                    }
                }


                // Find accepted requests that have not been scheduled.
                List<int[]> acceptedRequests = new ArrayList<>();

                String acceptedQuery = """
                    SELECT br.request_id,
                           br.book_id,
                           br.offered_book_id,
                           br.request_type
                    FROM book_requests br
                    WHERE br.request_status = 'ACCEPTED'
                    AND (
                        br.book_id = ?
                        OR br.offered_book_id = ?
                    )
                    FOR UPDATE
                    """;

                try (PreparedStatement ps =
                             con.prepareStatement(acceptedQuery)) {

                    ps.setInt(1, bookId);
                    ps.setInt(2, bookId);

                    try (ResultSet rs = ps.executeQuery()) {

                        while (rs.next()) {

                            int offeredId =
                                    rs.getInt("offered_book_id");

                            if (rs.wasNull()) {
                                offeredId = -1;
                            }

                            acceptedRequests.add(new int[]{
                                    rs.getInt("request_id"),
                                    rs.getInt("book_id"),
                                    offeredId,
                                    "EXCHANGE".equals(
                                            rs.getString("request_type")
                                    ) ? 1 : 0
                            });
                        }
                    }
                }


                // Cancel accepted requests and release reserved books.
                for (int[] request : acceptedRequests) {

                    int requestId = request[0];
                    int targetBookId = request[1];
                    int offeredBookId = request[2];
                    boolean isExchange = request[3] == 1;

                    try (PreparedStatement ps =
                                 con.prepareStatement("""
                                     UPDATE book_requests
                                     SET request_status = 'CANCELLED'
                                     WHERE request_id = ?
                                     AND request_status = 'ACCEPTED'
                                     """)) {

                        ps.setInt(1, requestId);
                        ps.executeUpdate();
                    }

                    // Release requested book.
                    try (PreparedStatement ps =
                                 con.prepareStatement("""
                                     UPDATE books
                                     SET status = 'AVAILABLE'
                                     WHERE book_id = ?
                                     AND status = 'RESERVED'
                                     """)) {

                        ps.setInt(1, targetBookId);
                        ps.executeUpdate();
                    }

                    // Release offered book in an exchange.
                    if (isExchange && offeredBookId != -1) {

                        try (PreparedStatement ps =
                                     con.prepareStatement("""
                                         UPDATE books
                                         SET status = 'AVAILABLE'
                                         WHERE book_id = ?
                                         AND status = 'RESERVED'
                                         """)) {

                            ps.setInt(1, offeredBookId);
                            ps.executeUpdate();
                        }
                    }
                }


                // Cancel pending requests involving this book.
                try (PreparedStatement ps =
                             con.prepareStatement("""
                                 UPDATE book_requests
                                 SET request_status = 'CANCELLED'
                                 WHERE request_status = 'PENDING'
                                 AND (
                                     book_id = ?
                                     OR offered_book_id = ?
                                 )
                                 """)) {

                    ps.setInt(1, bookId);
                    ps.setInt(2, bookId);
                    ps.executeUpdate();
                }


                // Mark the listing as REMOVED.
                try (PreparedStatement ps =
                             con.prepareStatement("""
                                 UPDATE books
                                 SET status = 'REMOVED'
                                 WHERE book_id = ?
                                 AND student_id = ?
                                 """)) {

                    ps.setInt(1, bookId);
                    ps.setInt(2, ownerId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "Unable to remove the listing."
                        );
                    }
                }

                con.commit();

            } catch (SQLException | RuntimeException e) {

                con.rollback();
                throw e;
            }
        }
    }


    // 7. CONVERT DATABASE RESULT INTO BOOK OBJECT
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
