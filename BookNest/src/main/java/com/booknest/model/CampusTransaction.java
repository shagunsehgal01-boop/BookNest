
package com.booknest.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class CampusTransaction {

    private int transactionId;
    private int requestId;
    private int bookId;

    private String bookTitle;
    private String requestType;

    private String ownerName;
    private String ownerEmail;

    private String requesterName;
    private String requesterEmail;

    private String offeredBookTitle;

    private String meetingPlace;
    private LocalDate meetingDate;
    private LocalTime meetingTime;

    private boolean ownerConfirmed;
    private boolean requesterConfirmed;

    private String transactionStatus;

    public CampusTransaction(
            int transactionId,
            int requestId,
            int bookId,
            String bookTitle,
            String requestType,
            String ownerName,
            String ownerEmail,
            String requesterName,
            String requesterEmail,
            String offeredBookTitle,
            String meetingPlace,
            LocalDate meetingDate,
            LocalTime meetingTime,
            boolean ownerConfirmed,
            boolean requesterConfirmed,
            String transactionStatus) {

        this.transactionId = transactionId;
        this.requestId = requestId;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.requestType = requestType;
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
        this.requesterName = requesterName;
        this.requesterEmail = requesterEmail;
        this.offeredBookTitle = offeredBookTitle;
        this.meetingPlace = meetingPlace;
        this.meetingDate = meetingDate;
        this.meetingTime = meetingTime;
        this.ownerConfirmed = ownerConfirmed;
        this.requesterConfirmed = requesterConfirmed;
        this.transactionStatus = transactionStatus;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public int getRequestId() {
        return requestId;
    }

    public int getBookId() {
        return bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public String getRequestType() {
        return requestType;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public String getOfferedBookTitle() {
        return offeredBookTitle;
    }

    public String getMeetingPlace() {
        return meetingPlace;
    }

    public LocalDate getMeetingDate() {
        return meetingDate;
    }

    public LocalTime getMeetingTime() {
        return meetingTime;
    }

    public boolean isOwnerConfirmed() {
        return ownerConfirmed;
    }

    public boolean isRequesterConfirmed() {
        return requesterConfirmed;
    }

    public String getTransactionStatus() {
        return transactionStatus;
    }
}

