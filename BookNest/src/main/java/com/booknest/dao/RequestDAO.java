
package com.booknest.dao;

import com.booknest.database.DBConnection;
import com.booknest.model.BookRequest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RequestDAO {

    // Find student ID using email
    private int getStudentId(Connection con, String email)
            throws SQLException {

        String query =
                "SELECT student_id FROM students WHERE email = ?";

        try (PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt("student_id");
                }
            }
        }

        throw new SQLException("Student account not found.");
    }

    // Send a buy, free or exchange request
    public void createRequest(
            int bookId,
            String requesterEmail,
            String requestType,
            Integer offeredBookId
    ) throws SQLException {

        try (Connection con = DBConnection.getConnection()) {

            con.setAutoCommit(false);

            try {

                int requesterId = getStudentId(con, requesterEmail);

                String bookQuery = """
                    SELECT student_id, listing_type, status
                    FROM books
                    WHERE book_id = ?
                    FOR UPDATE
                    """;

                int ownerId;
                String listingType;
                String bookStatus;

                try (PreparedStatement ps =
                             con.prepareStatement(bookQuery)) {

                    ps.setInt(1, bookId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Book does not exist."
                            );
                        }

                        ownerId = rs.getInt("student_id");
                        listingType = rs.getString("listing_type");
                        bookStatus = rs.getString("status");
                    }
                }

                if (ownerId == requesterId) {
                    throw new IllegalArgumentException(
                            "You cannot request your own book."
                    );
                }

                if (!"AVAILABLE".equals(bookStatus)) {
                    throw new IllegalArgumentException(
                            "This book is no longer available."
                    );
                }

                if (requestType.equals("BUY")
                        && !listingType.equals("SALE")
                        && !listingType.equals("BOTH")) {

                    throw new IllegalArgumentException(
                            "This book is not available for buying."
                    );
                }

                if (requestType.equals("FREE")
                        && !listingType.equals("FREE")) {

                    throw new IllegalArgumentException(
                            "This book is not listed as free."
                    );
                }

                if (requestType.equals("EXCHANGE")
                        && !listingType.equals("EXCHANGE")
                        && !listingType.equals("BOTH")) {

                    throw new IllegalArgumentException(
                            "This book is not available for exchange."
                    );
                }

                if (!requestType.equals("BUY")
                        && !requestType.equals("FREE")
                        && !requestType.equals("EXCHANGE")) {

                    throw new IllegalArgumentException(
                            "Invalid request type."
                    );
                }

                // Validate the book offered for exchange
                if (requestType.equals("EXCHANGE")) {

                    if (offeredBookId == null) {
                        throw new IllegalArgumentException(
                                "Select a book to offer."
                        );
                    }

                    String offeredQuery = """
                        SELECT student_id, status
                        FROM books
                        WHERE book_id = ?
                        FOR UPDATE
                        """;

                    try (PreparedStatement ps =
                                 con.prepareStatement(offeredQuery)) {

                        ps.setInt(1, offeredBookId);

                        try (ResultSet rs = ps.executeQuery()) {

                            if (!rs.next()) {
                                throw new IllegalArgumentException(
                                        "Offered book does not exist."
                                );
                            }

                            if (rs.getInt("student_id") != requesterId) {
                                throw new IllegalArgumentException(
                                        "You can only offer your own book."
                                );
                            }

                            if (!rs.getString("status").equals("AVAILABLE")) {
                                throw new IllegalArgumentException(
                                        "Your offered book is not available."
                                );
                            }
                        }
                    }

                    if (offeredBookId == bookId) {
                        throw new IllegalArgumentException(
                                "You cannot offer the same book."
                        );
                    }

                } else {
                    offeredBookId = null;
                }

                // Prevent duplicate pending requests
                String duplicateQuery = """
                    SELECT request_id
                    FROM book_requests
                    WHERE book_id = ?
                    AND requester_id = ?
                    AND request_status = 'PENDING'
                    """;

                try (PreparedStatement ps =
                             con.prepareStatement(duplicateQuery)) {

                    ps.setInt(1, bookId);
                    ps.setInt(2, requesterId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (rs.next()) {
                            throw new IllegalArgumentException(
                                    "You already have a pending request for this book."
                            );
                        }
                    }
                }

                String insertQuery = """
                    INSERT INTO book_requests
                    (book_id, requester_id, offered_book_id, request_type)
                    VALUES (?, ?, ?, ?)
                    """;

                try (PreparedStatement ps =
                             con.prepareStatement(insertQuery)) {

                    ps.setInt(1, bookId);
                    ps.setInt(2, requesterId);

                    if (offeredBookId == null) {
                        ps.setNull(3, Types.INTEGER);
                    } else {
                        ps.setInt(3, offeredBookId);
                    }

                    ps.setString(4, requestType);

                    ps.executeUpdate();
                }

                con.commit();

            } catch (SQLException | RuntimeException e) {

                con.rollback();
                throw e;
            }
        }
    }

    // Get all incoming requests for the logged-in owner
    public List<BookRequest> getIncomingRequests(String ownerEmail)
            throws SQLException {

        List<BookRequest> requests = new ArrayList<>();

        String query = """
            SELECT br.request_id,
                   br.book_id,
                   b.title AS book_title,
                   s.name AS requester_name,
                   s.email AS requester_email,
                   ob.title AS offered_book_title,
                   br.request_type,
                   br.request_status
            FROM book_requests br
            JOIN books b ON br.book_id = b.book_id
            JOIN students s ON br.requester_id = s.student_id
            LEFT JOIN books ob ON br.offered_book_id = ob.book_id
            WHERE b.student_id = (
                SELECT student_id
                FROM students
                WHERE email = ?
            )
            ORDER BY br.created_at ASC
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, ownerEmail);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    requests.add(new BookRequest(
                            rs.getInt("request_id"),
                            rs.getInt("book_id"),
                            rs.getString("book_title"),
                            rs.getString("requester_name"),
                            rs.getString("requester_email"),
                            rs.getString("offered_book_title"),
                            rs.getString("request_type"),
                            rs.getString("request_status")
                    ));
                }
            }
        }

        return requests;
    }

    // Accept an incoming request
    public void acceptRequest(int requestId, String ownerEmail)
            throws SQLException {

        try (Connection con = DBConnection.getConnection()) {

            con.setAutoCommit(false);

            try {

                int ownerId = getStudentId(con, ownerEmail);

                String query = """
                    SELECT br.book_id,
                           br.requester_id,
                           br.offered_book_id,
                           br.request_type,
                           br.request_status,
                           b.student_id AS book_owner,
                           b.status AS book_status
                    FROM book_requests br
                    JOIN books b ON br.book_id = b.book_id
                    WHERE br.request_id = ?
                    FOR UPDATE
                    """;

                int bookId;
                int requesterId;
                Integer offeredBookId;
                String requestType;
                String requestStatus;
                int bookOwner;
                String bookStatus;

                try (PreparedStatement ps =
                             con.prepareStatement(query)) {

                    ps.setInt(1, requestId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Request not found."
                            );
                        }

                        bookId = rs.getInt("book_id");
                        requesterId = rs.getInt("requester_id");

                        int offeredId = rs.getInt("offered_book_id");

                        offeredBookId = rs.wasNull() ? null : offeredId;

                        requestType = rs.getString("request_type");
                        requestStatus = rs.getString("request_status");
                        bookOwner = rs.getInt("book_owner");
                        bookStatus = rs.getString("book_status");
                    }
                }

                if (bookOwner != ownerId) {
                    throw new IllegalArgumentException(
                            "You cannot accept another owner's request."
                    );
                }

                if (!requestStatus.equals("PENDING")) {
                    throw new IllegalArgumentException(
                            "This request has already been processed."
                    );
                }

                if (!bookStatus.equals("AVAILABLE")) {
                    throw new IllegalArgumentException(
                            "This book is no longer available."
                    );
                }

                // For exchange, lock and validate the offered book too
                if (requestType.equals("EXCHANGE")) {

                    if (offeredBookId == null) {
                        throw new IllegalArgumentException(
                                "Exchange request has no offered book."
                        );
                    }

                    String offeredQuery = """
                        SELECT student_id, status
                        FROM books
                        WHERE book_id = ?
                        FOR UPDATE
                        """;

                    try (PreparedStatement ps =
                                 con.prepareStatement(offeredQuery)) {

                        ps.setInt(1, offeredBookId);

                        try (ResultSet rs = ps.executeQuery()) {

                            if (!rs.next()
                                    || rs.getInt("student_id") != requesterId
                                    || !rs.getString("status").equals("AVAILABLE")) {

                                throw new IllegalArgumentException(
                                        "The offered book is no longer available."
                                );
                            }
                        }
                    }
                }

                // Reserve the requested book
                String reserveQuery = """
                    UPDATE books
                    SET status = 'RESERVED'
                    WHERE book_id = ?
                    AND status = 'AVAILABLE'
                    """;

                try (PreparedStatement ps =
                             con.prepareStatement(reserveQuery)) {

                    ps.setInt(1, bookId);

                    if (ps.executeUpdate() != 1) {
                        throw new IllegalArgumentException(
                                "Unable to reserve this book."
                        );
                    }
                }

                // Reserve the offered book in an exchange
                if (requestType.equals("EXCHANGE")) {

                    try (PreparedStatement ps =
                                 con.prepareStatement(reserveQuery)) {

                        ps.setInt(1, offeredBookId);

                        if (ps.executeUpdate() != 1) {
                            throw new IllegalArgumentException(
                                    "Unable to reserve the offered book."
                            );
                        }
                    }
                }

                // Mark the selected request as accepted
                String acceptQuery = """
                    UPDATE book_requests
                    SET request_status = 'ACCEPTED'
                    WHERE request_id = ?
                    AND request_status = 'PENDING'
                    """;

                try (PreparedStatement ps =
                             con.prepareStatement(acceptQuery)) {

                    ps.setInt(1, requestId);

                    if (ps.executeUpdate() != 1) {
                        throw new IllegalArgumentException(
                                "Request could not be accepted."
                        );
                    }
                }

                // Reject competing requests involving reserved books
                String rejectQuery = """
                    UPDATE book_requests
                    SET request_status = 'REJECTED'
                    WHERE request_status = 'PENDING'
                    AND request_id <> ?
                    AND (
                        book_id = ?
                        OR offered_book_id = ?
                    )
                    """;

                try (PreparedStatement ps =
                             con.prepareStatement(rejectQuery)) {

                    ps.setInt(1, requestId);
                    ps.setInt(2, bookId);
                    ps.setInt(3, bookId);
                    ps.executeUpdate();
                }

                if (offeredBookId != null) {

                    try (PreparedStatement ps =
                                 con.prepareStatement(rejectQuery)) {

                        ps.setInt(1, requestId);
                        ps.setInt(2, offeredBookId);
                        ps.setInt(3, offeredBookId);
                        ps.executeUpdate();
                    }
                }

                con.commit();

            } catch (SQLException | RuntimeException e) {

                con.rollback();
                throw e;
            }
        }
    }

    // Reject an incoming request
    public void rejectRequest(int requestId, String ownerEmail)
            throws SQLException {

        try (Connection con = DBConnection.getConnection()) {

            int ownerId = getStudentId(con, ownerEmail);

            String query = """
                UPDATE book_requests br
                JOIN books b ON br.book_id = b.book_id
                SET br.request_status = 'REJECTED'
                WHERE br.request_id = ?
                AND b.student_id = ?
                AND br.request_status = 'PENDING'
                """;

            try (PreparedStatement ps = con.prepareStatement(query)) {

                ps.setInt(1, requestId);
                ps.setInt(2, ownerId);

                if (ps.executeUpdate() != 1) {
                    throw new IllegalArgumentException(
                            "Request could not be rejected. It may already be processed."
                    );
                }
            }
        }
    }
}
