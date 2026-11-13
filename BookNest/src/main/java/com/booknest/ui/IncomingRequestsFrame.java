
package com.booknest.ui;

import com.booknest.dao.RequestDAO;
import com.booknest.model.BookRequest;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Queue;

public class IncomingRequestsFrame extends JFrame {

    private String ownerEmail;
    private JPanel requestsPanel;

    private RequestDAO requestDAO;

    public IncomingRequestsFrame(String ownerEmail) {

        this.ownerEmail = ownerEmail;
        this.requestDAO = new RequestDAO();

        setTitle("BookNest - Incoming Requests");
        setSize(850, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

        JLabel heading = new JLabel("My Incoming Requests");

        heading.setFont(new Font("Arial", Font.BOLD, 24));

        heading.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 5, 15)
        );

        JButton refreshButton = new JButton("Refresh");

        JPanel topPanel = new JPanel(new BorderLayout());

        topPanel.add(heading, BorderLayout.CENTER);
        topPanel.add(refreshButton, BorderLayout.EAST);

        requestsPanel = new JPanel();

        requestsPanel.setLayout(
                new BoxLayout(requestsPanel, BoxLayout.Y_AXIS)
        );

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(
                new JScrollPane(requestsPanel),
                BorderLayout.CENTER
        );

        add(mainPanel);

        refreshButton.addActionListener(e -> loadRequests());

        loadRequests();
    }

    // Load requests received by the logged-in owner
    private void loadRequests() {

        requestsPanel.removeAll();

        try {

            List<BookRequest> requests =
                    requestDAO.getIncomingRequests(ownerEmail);

            // Queue demonstrates FIFO: oldest pending request is displayed first.
            Queue<BookRequest> requestQueue = new ArrayDeque<>(requests);

            if (requestQueue.isEmpty()) {

                JLabel emptyLabel =
                        new JLabel("You have not received any requests yet.");

                emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

                requestsPanel.add(Box.createVerticalStrut(30));
                requestsPanel.add(emptyLabel);

            } else {

                while (!requestQueue.isEmpty()) {
                    BookRequest request = requestQueue.poll();
                    requestsPanel.add(createRequestCard(request));
                    requestsPanel.add(Box.createVerticalStrut(12));
                }
            }

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to load requests: " + e.getMessage()
            );
        }

        requestsPanel.revalidate();
        requestsPanel.repaint();
    }

    // Create one request card
    private JPanel createRequestCard(BookRequest request) {

        JPanel card = new JPanel(new BorderLayout(10, 10));

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        card.setMaximumSize(new Dimension(
                Integer.MAX_VALUE, 220
        ));

        JPanel details = new JPanel();

        details.setLayout(
                new BoxLayout(details, BoxLayout.Y_AXIS)
        );

        details.add(new JLabel(
                "Your Book: " + request.getBookTitle()
        ));

        details.add(new JLabel(
                "Requested By: " + request.getRequesterName()
        ));

        details.add(new JLabel(
                "Student Email: " + request.getRequesterEmail()
        ));

        details.add(new JLabel(
                "Request Type: " + request.getRequestType()
        ));

        if (request.getOfferedBookTitle() != null) {

            details.add(new JLabel(
                    "Offered Book: " + request.getOfferedBookTitle()
            ));
        }

        details.add(new JLabel(
                "Status: " + request.getRequestStatus()
        ));

        JPanel actionPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );

        if (request.getRequestStatus().equals("PENDING")) {

            JButton acceptButton = new JButton("Accept");
            JButton rejectButton = new JButton("Reject");

            acceptButton.addActionListener(e ->
                    acceptRequest(request.getRequestId())
            );

            rejectButton.addActionListener(e ->
                    rejectRequest(request.getRequestId())
            );

            actionPanel.add(acceptButton);
            actionPanel.add(rejectButton);

        } else {

            JLabel statusLabel = new JLabel(
                    "Request " + request.getRequestStatus()
            );

            actionPanel.add(statusLabel);
        }

        card.add(details, BorderLayout.CENTER);
        card.add(actionPanel, BorderLayout.SOUTH);

        return card;
    }

    // Accept request
    private void acceptRequest(int requestId) {

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to accept this request?\nThe book will be reserved.",
                "Confirm Acceptance",
                JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {

            requestDAO.acceptRequest(requestId, ownerEmail);

            JOptionPane.showMessageDialog(
                    this,
                    "Request accepted successfully!\nThe book is now RESERVED.\nYou can schedule the campus handover from My Transactions."
            );

            loadRequests();

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to accept request: " + e.getMessage()
            );

            loadRequests();
        }
    }

    // Reject request
    private void rejectRequest(int requestId) {

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to reject this request?",
                "Confirm Rejection",
                JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {

            requestDAO.rejectRequest(requestId, ownerEmail);

            JOptionPane.showMessageDialog(
                    this,
                    "Request rejected successfully."
            );

            loadRequests();

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to reject request: " + e.getMessage()
            );

            loadRequests();
        }
    }
}

