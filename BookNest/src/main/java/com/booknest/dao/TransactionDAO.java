
package com.booknest.dao;

import com.booknest.database.DBConnection;
import com.booknest.model.AcceptedRequest;
import com.booknest.model.CampusTransaction;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

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

    // Load accepted requests which have not been scheduled yet
    public List<AcceptedRequest> getAcceptedRequests(String email)
            throws SQLException {

        List<AcceptedRequest> requests = new ArrayList<>();

        String query = """
            SELECT br.request_id,
                   br.book_id,
                   b.title AS book_title,
                   owner.name AS owner_name,
                   owner.email AS owner_email,
                   requester.name AS requester_name,
                   requester.email AS requester_email,
                   br.request_type,
                   br.offered_book_id,
                   offered.title AS offered_book_title
            FROM book_requests br
            JOIN books b ON br.book_id = b.book_id
            JOIN students owner ON b.student_id = owner.student_id
            JOIN students requester
                ON br.requester_id = requester.student_id
            LEFT JOIN books offered
                ON br.offered_book_id = offered.book_id
            WHERE br.request_status = 'ACCEPTED'
            AND (owner.email = ? OR requester.email = ?)
            AND NOT EXISTS (
                SELECT 1
                FROM transactions t
                WHERE t.request_id = br.request_id
            )
            ORDER BY br.created_at DESC
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, email);
            ps.setString(2, email);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    int offeredId = rs.getInt("offered_book_id");

                    Integer offeredBookId =
                            rs.wasNull() ? null : offeredId;

                    requests.add(new AcceptedRequest(
                            rs.getInt("request_id"),
                            rs.getInt("book_id"),
                            rs.getString("book_title"),
                            rs.getString("owner_name"),
                            rs.getString("owner_email"),
                            rs.getString("requester_name"),
                            rs.getString("requester_email"),
                            rs.getString("request_type"),
                            offeredBookId,
                            rs.getString("offered_book_title")
                    ));
                }
            }
        }

        return requests;
    }

    // Schedule a campus handover
    public void scheduleTransaction(
            int requestId,
            String email,
            String meetingPlace,
            LocalDate meetingDate,
            LocalTime meetingTime
    ) throws SQLException {

        try (Connection con = DBConnection.getConnection()) {

            con.setAutoCommit(false);

            try {

                int studentId = getStudentId(con, email);

                String query = """
                    SELECT br.book_id,
                           br.requester_id,
                           br.offered_book_id,
                           br.request_type,
                           br.request_status,
                           b.student_id AS owner_id,
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
                int ownerId;
                String bookStatus;

                try (PreparedStatement ps =
                             con.prepareStatement(query)) {

                    ps.setInt(1, requestId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Accepted request not found."
                            );
                        }

                        bookId = rs.getInt("book_id");
                        requesterId = rs.getInt("requester_id");

                        int offeredId = rs.getInt("offered_book_id");

                        offeredBookId =
                                rs.wasNull() ? null : offeredId;

                        requestType = rs.getString("request_type");
                        requestStatus = rs.getString("request_status");
                        ownerId = rs.getInt("owner_id");
                        bookStatus = rs.getString("book_status");
                    }
                }

                if (studentId != ownerId
                        && studentId != requesterId) {

                    throw new IllegalArgumentException(
                            "You are not part of this transaction."
                    );
                }

                if (!"ACCEPTED".equals(requestStatus)) {
                    throw new IllegalArgumentException(
                            "Only accepted requests can be scheduled."
                    );
                }

                if (!"RESERVED".equals(bookStatus)) {
                    throw new IllegalArgumentException(
                            "The book is not reserved."
                    );
                }

                // Check offered book in case of exchange
                if ("EXCHANGE".equals(requestType)) {

                    if (offeredBookId == null) {
                        throw new IllegalArgumentException(
                                "Exchange book information is missing."
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
                                    || !"RESERVED".equals(rs.getString("status"))) {

                                throw new IllegalArgumentException(
                                        "The offered book is not reserved correctly."
                                );
                            }
                        }
                    }
                }

                // Prevent scheduling the same request twice
                String checkQuery = """
                    SELECT transaction_id
                    FROM transactions
                    WHERE request_id = ?
                    """;

                try (PreparedStatement ps =
                             con.prepareStatement(checkQuery)) {

                    ps.setInt(1, requestId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (rs.next()) {
                            throw new IllegalArgumentException(
                                    "This handover is already scheduled."
                            );
                        }
                    }
                }

                String insertQuery = """
                    INSERT INTO transactions
                    (request_id, meeting_place, meeting_date, meeting_time)
                    VALUES (?, ?, ?, ?)
                    """;

                try (PreparedStatement ps =
                             con.prepareStatement(insertQuery)) {

                    ps.setInt(1, requestId);
                    ps.setString(2, meetingPlace);
                    ps.setDate(3, Date.valueOf(meetingDate));
                    ps.setTime(4, Time.valueOf(meetingTime));

                    ps.executeUpdate();
                }

                con.commit();

            } catch (SQLException | RuntimeException e) {

                con.rollback();
                throw e;
            }
        }
    }

    // Get all scheduled and completed transactions of a student
    public List<CampusTransaction> getMyTransactions(String email)
            throws SQLException {

        List<CampusTransaction> transactions = new ArrayList<>();

        String query = """
            SELECT t.transaction_id,
                   br.request_id,
                   br.book_id,
                   b.title AS book_title,
                   br.request_type,
                   owner.name AS owner_name,
                   owner.email AS owner_email,
                   requester.name AS requester_name,
                   requester.email AS requester_email,
                   offered.title AS offered_book_title,
                   t.meeting_place,
                   t.meeting_date,
                   t.meeting_time,
                   t.owner_confirmed,
                   t.requester_confirmed,
                   t.transaction_status
            FROM transactions t
            JOIN book_requests br ON t.request_id = br.request_id
            JOIN books b ON br.book_id = b.book_id
            JOIN students owner ON b.student_id = owner.student_id
            JOIN students requester
                ON br.requester_id = requester.student_id
            LEFT JOIN books offered
                ON br.offered_book_id = offered.book_id
            WHERE owner.email = ? OR requester.email = ?
            ORDER BY t.created_at DESC
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, email);
            ps.setString(2, email);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    Date sqlDate = rs.getDate("meeting_date");
                    Time sqlTime = rs.getTime("meeting_time");

                    transactions.add(new CampusTransaction(
                            rs.getInt("transaction_id"),
                            rs.getInt("request_id"),
                            rs.getInt("book_id"),
                            rs.getString("book_title"),
                            rs.getString("request_type"),
                            rs.getString("owner_name"),
                            rs.getString("owner_email"),
                            rs.getString("requester_name"),
                            rs.getString("requester_email"),
                            rs.getString("offered_book_title"),
                            rs.getString("meeting_place"),
                            sqlDate == null ? null : sqlDate.toLocalDate(),
                            sqlTime == null ? null : sqlTime.toLocalTime(),
                            rs.getBoolean("owner_confirmed"),
                            rs.getBoolean("requester_confirmed"),
                            rs.getString("transaction_status")
                    ));
                }
            }
        }

        return transactions;
    }

    // Cancel an accepted request before it has been scheduled
    public void cancelAcceptedRequest(int requestId, String email)
            throws SQLException {

        try (Connection con = DBConnection.getConnection()) {

            con.setAutoCommit(false);

            try {

                int studentId = getStudentId(con, email);

                String query = """
                    SELECT br.book_id,
                           br.requester_id,
                           br.offered_book_id,
                           br.request_type,
                           br.request_status,
                           b.student_id AS owner_id
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
                int ownerId;

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

                        offeredBookId =
                                rs.wasNull() ? null : offeredId;

                        requestType = rs.getString("request_type");
                        requestStatus = rs.getString("request_status");
                        ownerId = rs.getInt("owner_id");
                    }
                }

                if (studentId != ownerId
                        && studentId != requesterId) {

                    throw new IllegalArgumentException(
                            "You are not part of this request."
                    );
                }

                if (!"ACCEPTED".equals(requestStatus)) {
                    throw new IllegalArgumentException(
                            "This request cannot be cancelled."
                    );
                }

                String checkTransaction = """
                    SELECT transaction_id
                    FROM transactions
                    WHERE request_id = ?
                    """;

                try (PreparedStatement ps =
                             con.prepareStatement(checkTransaction)) {

                    ps.setInt(1, requestId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (rs.next()) {
                            throw new IllegalArgumentException(
                                    "A scheduled transaction must be cancelled from My Transactions."
                            );
                        }
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE book_requests SET request_status = 'CANCELLED' WHERE request_id = ?")) {

                    ps.setInt(1, requestId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE books SET status = 'AVAILABLE' WHERE book_id = ? AND status = 'RESERVED'")) {

                    ps.setInt(1, bookId);
                    ps.executeUpdate();

                    if ("EXCHANGE".equals(requestType)
                            && offeredBookId != null) {

                        ps.setInt(1, offeredBookId);
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

    // Cancel a scheduled handover
    public void cancelTransaction(int transactionId, String email)
            throws SQLException {

        try (Connection con = DBConnection.getConnection()) {

            con.setAutoCommit(false);

            try {

                int studentId = getStudentId(con, email);

                String query = """
                    SELECT t.transaction_status,
                           br.book_id,
                           br.requester_id,
                           br.offered_book_id,
                           br.request_type,
                           b.student_id AS owner_id
                    FROM transactions t
                    JOIN book_requests br ON t.request_id = br.request_id
                    JOIN books b ON br.book_id = b.book_id
                    WHERE t.transaction_id = ?
                    FOR UPDATE
                    """;

                String status;
                int bookId;
                int requesterId;
                Integer offeredBookId;
                String requestType;
                int ownerId;
                int requestId;

                String requestIdQuery = """
                    SELECT request_id
                    FROM transactions
                    WHERE transaction_id = ?
                    """;

                try (PreparedStatement ps =
                             con.prepareStatement(requestIdQuery)) {

                    ps.setInt(1, transactionId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Transaction not found."
                            );
                        }

                        requestId = rs.getInt("request_id");
                    }
                }

                try (PreparedStatement ps =
                             con.prepareStatement(query)) {

                    ps.setInt(1, transactionId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Transaction not found."
                            );
                        }

                        status = rs.getString("transaction_status");
                        bookId = rs.getInt("book_id");
                        requesterId = rs.getInt("requester_id");

                        int offeredId = rs.getInt("offered_book_id");

                        offeredBookId =
                                rs.wasNull() ? null : offeredId;

                        requestType = rs.getString("request_type");
                        ownerId = rs.getInt("owner_id");
                    }
                }

                if (studentId != ownerId
                        && studentId != requesterId) {

                    throw new IllegalArgumentException(
                            "You are not part of this transaction."
                    );
                }

                if (!"SCHEDULED".equals(status)) {
                    throw new IllegalArgumentException(
                            "Only scheduled transactions can be cancelled."
                    );
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE transactions SET transaction_status = 'CANCELLED' WHERE transaction_id = ?")) {

                    ps.setInt(1, transactionId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE book_requests SET request_status = 'CANCELLED' WHERE request_id = ?")) {

                    ps.setInt(1, requestId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE books SET status = 'AVAILABLE' WHERE book_id = ? AND status = 'RESERVED'")) {

                    ps.setInt(1, bookId);
                    ps.executeUpdate();

                    if ("EXCHANGE".equals(requestType)
                            && offeredBookId != null) {

                        ps.setInt(1, offeredBookId);
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

    // Record one student's handover confirmation
    // Returns true if both students have now confirmed
    public boolean confirmHandover(int transactionId, String email)
            throws SQLException {

        try (Connection con = DBConnection.getConnection()) {

            con.setAutoCommit(false);

            try {

                int studentId = getStudentId(con, email);

                String query = """
                    SELECT t.transaction_status,
                           t.owner_confirmed,
                           t.requester_confirmed,
                           br.book_id,
                           br.requester_id,
                           br.offered_book_id,
                           br.request_type,
                           b.student_id AS owner_id
                    FROM transactions t
                    JOIN book_requests br ON t.request_id = br.request_id
                    JOIN books b ON br.book_id = b.book_id
                    WHERE t.transaction_id = ?
                    FOR UPDATE
                    """;

                String status;
                boolean ownerConfirmed;
                boolean requesterConfirmed;
                int bookId;
                int requesterId;
                Integer offeredBookId;
                String requestType;
                int ownerId;

                try (PreparedStatement ps =
                             con.prepareStatement(query)) {

                    ps.setInt(1, transactionId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Transaction not found."
                            );
                        }

                        status = rs.getString("transaction_status");
                        ownerConfirmed = rs.getBoolean("owner_confirmed");
                        requesterConfirmed = rs.getBoolean("requester_confirmed");

                        bookId = rs.getInt("book_id");
                        requesterId = rs.getInt("requester_id");

                        int offeredId = rs.getInt("offered_book_id");

                        offeredBookId =
                                rs.wasNull() ? null : offeredId;

                        requestType = rs.getString("request_type");
                        ownerId = rs.getInt("owner_id");
                    }
                }

                if (studentId != ownerId
                        && studentId != requesterId) {

                    throw new IllegalArgumentException(
                            "You are not part of this transaction."
                    );
                }

                if (!"SCHEDULED".equals(status)) {
                    throw new IllegalArgumentException(
                            "This transaction is not awaiting confirmation."
                    );
                }

                if (studentId == ownerId) {
                    ownerConfirmed = true;
                } else {
                    requesterConfirmed = true;
                }

                String updateQuery = """
                    UPDATE transactions
                    SET owner_confirmed = ?,
                        requester_confirmed = ?
                    WHERE transaction_id = ?
                    """;

                try (PreparedStatement ps =
                             con.prepareStatement(updateQuery)) {

                    ps.setBoolean(1, ownerConfirmed);
                    ps.setBoolean(2, requesterConfirmed);
                    ps.setInt(3, transactionId);

                    ps.executeUpdate();
                }

                boolean completed =
                        ownerConfirmed && requesterConfirmed;

                if (completed) {

                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE transactions SET transaction_status = 'COMPLETED' WHERE transaction_id = ?")) {

                        ps.setInt(1, transactionId);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE book_requests SET request_status = 'COMPLETED' WHERE request_id = (SELECT request_id FROM transactions WHERE transaction_id = ?)")) {

                        ps.setInt(1, transactionId);
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE books SET status = 'UNAVAILABLE' WHERE book_id = ?")) {

                        ps.setInt(1, bookId);
                        ps.executeUpdate();

                        if ("EXCHANGE".equals(requestType)
                                && offeredBookId != null) {

                            ps.setInt(1, offeredBookId);
                            ps.executeUpdate();
                        }
                    }
                }

                con.commit();

                return completed;

            } catch (SQLException | RuntimeException e) {

                con.rollback();
                throw e;
            }
        }
    }
}

