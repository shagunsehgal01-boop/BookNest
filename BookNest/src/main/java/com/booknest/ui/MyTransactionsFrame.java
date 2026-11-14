package com.booknest.ui;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.booknest.dao.TransactionDAO;
import com.booknest.model.AcceptedRequest;
import com.booknest.model.CampusTransaction;

public class MyTransactionsFrame extends JFrame {

    private String email;
    private TransactionDAO transactionDAO;
    private JPanel contentPanel;

    private final String[] locations = {
            "Nescafe Cafe near Fountain",
            "Outside B1 Library"
    };

    private final String[] timeSlots = {
            "11:00 AM",
            "2:00 PM",
            "4:00 PM",
            "6:00 PM"
    };

    public MyTransactionsFrame(String email) {

        this.email = email;
        this.transactionDAO = new TransactionDAO();

        setTitle("BookNest - My Transactions");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

        JLabel heading = new JLabel("My Transactions");
        heading.setFont(new Font("Arial", Font.BOLD, 25));
        heading.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 10, 15)
        );

        JButton refreshButton = new JButton("Refresh");

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(heading, BorderLayout.CENTER);
        topPanel.add(refreshButton, BorderLayout.EAST);

        contentPanel = new JPanel();
        contentPanel.setLayout(
                new BoxLayout(contentPanel, BoxLayout.Y_AXIS)
        );

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(
                new JScrollPane(contentPanel),
                BorderLayout.CENTER
        );

        add(mainPanel);

        refreshButton.addActionListener(e -> loadTransactions());

        loadTransactions();
    }

    // Load accepted requests and transaction history
    private void loadTransactions() {

        contentPanel.removeAll();

        try {

            List<AcceptedRequest> acceptedRequests =
                    transactionDAO.getAcceptedRequests(email);

            List<CampusTransaction> transactions =
                    transactionDAO.getMyTransactions(email);

            addSectionHeading("Accepted Requests - Schedule Handover");

            if (acceptedRequests.isEmpty()) {

                contentPanel.add(
                        new JLabel("No accepted requests waiting for scheduling.")
                );

                contentPanel.add(Box.createVerticalStrut(12));

            } else {

                for (AcceptedRequest request : acceptedRequests) {

                    contentPanel.add(createAcceptedRequestCard(request));
                    contentPanel.add(Box.createVerticalStrut(12));
                }
            }

            addSectionHeading("Scheduled and Previous Transactions");

            if (transactions.isEmpty()) {

                contentPanel.add(
                        new JLabel("No scheduled transactions yet.")
                );

            } else {

                for (CampusTransaction transaction : transactions) {

                    contentPanel.add(createTransactionCard(transaction));
                    contentPanel.add(Box.createVerticalStrut(12));
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to load transactions: " + e.getMessage()
            );
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void addSectionHeading(String headingText) {

        JLabel heading = new JLabel(headingText);
        heading.setFont(new Font("Arial", Font.BOLD, 18));

        heading.setBorder(
                BorderFactory.createEmptyBorder(15, 5, 10, 5)
        );

        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        contentPanel.add(heading);
    }

    // Card for an accepted request which has not been scheduled
    private JPanel createAcceptedRequestCard(AcceptedRequest request) {

        JPanel card = createCardPanel();

        JPanel details = new JPanel();
        details.setLayout(
                new BoxLayout(details, BoxLayout.Y_AXIS)
        );

        details.add(new JLabel(
                "Book: " + request.getBookTitle()
        ));

        details.add(new JLabel(
                "Owner: " + request.getOwnerName()
        ));

        details.add(new JLabel(
                "Requester: " + request.getRequesterName()
        ));

        details.add(new JLabel(
                "Request Type: " + request.getRequestType()
        ));

        if (request.getOfferedBookTitle() != null) {

            details.add(new JLabel(
                    "Offered Book: " + request.getOfferedBookTitle()
            ));
        }

        details.add(new JLabel("Status: ACCEPTED - Not Scheduled"));

        JPanel actions = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );

        JButton scheduleButton = new JButton("Schedule Handover");
        JButton cancelButton = new JButton("Cancel Request");

        scheduleButton.addActionListener(e ->
                scheduleRequest(request)
        );

        cancelButton.addActionListener(e ->
                cancelAcceptedRequest(request.getRequestId())
        );

        actions.add(scheduleButton);
        actions.add(cancelButton);

        card.add(details, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);

        return card;
    }

    // Card for a scheduled, completed or cancelled transaction
    private JPanel createTransactionCard(CampusTransaction transaction) {

        JPanel card = createCardPanel();

        JPanel details = new JPanel();
        details.setLayout(
                new BoxLayout(details, BoxLayout.Y_AXIS)
        );

        details.add(new JLabel(
                "Book: " + transaction.getBookTitle()
        ));

        details.add(new JLabel(
                "Request Type: " + transaction.getRequestType()
        ));

        details.add(new JLabel(
                "Owner: " + transaction.getOwnerName()
        ));

        details.add(new JLabel(
                "Requester: " + transaction.getRequesterName()
        ));

        if (transaction.getOfferedBookTitle() != null) {

            details.add(new JLabel(
                    "Offered Book: " + transaction.getOfferedBookTitle()
            ));
        }

        details.add(new JLabel(
                "Meeting Place: " + transaction.getMeetingPlace()
        ));

        details.add(new JLabel(
                "Meeting Date: " + transaction.getMeetingDate()
        ));

        details.add(new JLabel(
                "Meeting Time: " + formatTime(transaction.getMeetingTime())
        ));

        details.add(new JLabel(
                "Owner Confirmed: " +
                        (transaction.isOwnerConfirmed() ? "YES" : "NO")
        ));

        details.add(new JLabel(
                "Requester Confirmed: " +
                        (transaction.isRequesterConfirmed() ? "YES" : "NO")
        ));

        details.add(new JLabel(
                "Transaction Status: " +
                        transaction.getTransactionStatus()
        ));

        JPanel actions = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );

        if ("SCHEDULED".equals(transaction.getTransactionStatus())) {

            boolean isOwner =
                    email.equalsIgnoreCase(transaction.getOwnerEmail());

            boolean alreadyConfirmed = isOwner
                    ? transaction.isOwnerConfirmed()
                    : transaction.isRequesterConfirmed();

            if (!alreadyConfirmed) {

                JButton confirmButton =
                        new JButton("Confirm Handover");

                confirmButton.addActionListener(e ->
                        confirmHandover(transaction.getTransactionId())
                );

                actions.add(confirmButton);

            } else {

                actions.add(
                        new JLabel("You have confirmed the handover.")
                );
            }

            JButton cancelButton =
                    new JButton("Cancel Transaction");

            cancelButton.addActionListener(e ->
                    cancelTransaction(transaction.getTransactionId())
            );

            actions.add(cancelButton);
        }

        card.add(details, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createCardPanel() {

        JPanel card = new JPanel(new BorderLayout(10, 10));

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        card.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 300)
        );

        return card;
    }

    // Schedule a meeting
    private void scheduleRequest(AcceptedRequest request) {

        JPanel form = new JPanel(new GridLayout(3, 2, 10, 12));

        JComboBox<String> locationBox =
                new JComboBox<>(locations);

        JComboBox<String> timeBox =
                new JComboBox<>(timeSlots);

        JTextField dateField =
                new JTextField(LocalDate.now().plusDays(1).toString());

        form.add(new JLabel("Meeting Location:"));
        form.add(locationBox);

        form.add(new JLabel("Meeting Date (YYYY-MM-DD):"));
        form.add(dateField);

        form.add(new JLabel("Meeting Time:"));
        form.add(timeBox);

        int choice = JOptionPane.showConfirmDialog(
                this,
                form,
                "Schedule Campus Handover",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        try {

            // Read and validate meeting date
            LocalDate meetingDate =
                    LocalDate.parse(dateField.getText().trim());

            // Get selected time from dropdown
            String selectedTime =
                    timeBox.getSelectedItem().toString();

            // Clean and normalize the selected time
            String cleanTime = selectedTime.trim()
                    .replaceFirst("(?i)^at\\s+", "")
                    .replaceAll("\\s+", " ")
                    .toUpperCase(Locale.ENGLISH);

            // Convert "4 PM" into "4:00 PM"
            if (cleanTime.matches("\\d{1,2}\\s+[AP]M")) {

                cleanTime = cleanTime.replaceFirst(
                        "^(\\d{1,2})\\s+([AP]M)$",
                        "$1:00 $2"
                );
            }

            // Parse the time into LocalTime
            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(
                            "h:mm a",
                            Locale.ENGLISH
                    );

            LocalTime meetingTime =
                    LocalTime.parse(cleanTime, formatter);

            // Validate meeting date
            if (meetingDate.isBefore(LocalDate.now())) {

                throw new IllegalArgumentException(
                        "Meeting date cannot be in the past."
                );
            }

            // Validate meeting time if date is today
            if (meetingDate.equals(LocalDate.now())
                    && meetingTime.isBefore(LocalTime.now())) {

                throw new IllegalArgumentException(
                        "Please select a future time."
                );
            }

            // Save meeting details in database
            transactionDAO.scheduleTransaction(
                    request.getRequestId(),
                    email,
                    locationBox.getSelectedItem().toString(),
                    meetingDate,
                    meetingTime
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Campus handover scheduled successfully!\n" +
                    "Both students can now view the meeting details in My Transactions."
            );

            loadTransactions();

        } catch (Exception e) {

            // Print complete error details in terminal
            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to schedule handover: " + e.getMessage()
            );
        }
    }

    // Cancel an accepted request which has not been scheduled
    private void cancelAcceptedRequest(int requestId) {

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Cancel this accepted request?\n" +
                "The reserved book will become available again.",
                "Confirm Cancellation",
                JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {

            transactionDAO.cancelAcceptedRequest(requestId, email);

            JOptionPane.showMessageDialog(
                    this,
                    "Request cancelled. The reserved book is available again."
            );

            loadTransactions();

        } catch (Exception e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to cancel request: " + e.getMessage()
            );

            loadTransactions();
        }
    }

    // Confirm handover
    private void confirmHandover(int transactionId) {

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Confirm that the book handover has actually taken place?",
                "Confirm Handover",
                JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {

            boolean completed =
                    transactionDAO.confirmHandover(
                            transactionId,
                            email
                    );

            if (completed) {

                JOptionPane.showMessageDialog(
                        this,
                        "Both students have confirmed!\n" +
                        "Transaction completed successfully."
                );

            } else {

                JOptionPane.showMessageDialog(
                        this,
                        "Your confirmation has been saved.\n" +
                        "Waiting for the other student to confirm."
                );
            }

            loadTransactions();

        } catch (Exception e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to confirm handover: " + e.getMessage()
            );

            loadTransactions();
        }
    }

    // Cancel a scheduled transaction
    private void cancelTransaction(int transactionId) {

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to cancel this transaction?\n" +
                "The reserved book(s) will become available again.",
                "Confirm Cancellation",
                JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {

            transactionDAO.cancelTransaction(
                    transactionId,
                    email
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Transaction cancelled. The book(s) are available again."
            );

            loadTransactions();

        } catch (Exception e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to cancel transaction: " + e.getMessage()
            );

            loadTransactions();
        }
    }

    // Format meeting time for display
    private String formatTime(LocalTime time) {

        if (time == null) {
            return "Not selected";
        }

        return time.format(
                DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
        );
    }
}
