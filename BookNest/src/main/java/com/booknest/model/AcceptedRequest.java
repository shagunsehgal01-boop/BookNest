
package com.booknest.model;

public class AcceptedRequest {

    private int requestId;
    private int bookId;
    private String bookTitle;
    private String ownerName;
    private String ownerEmail;
    private String requesterName;
    private String requesterEmail;
    private String requestType;
    private Integer offeredBookId;
    private String offeredBookTitle;

    public AcceptedRequest(
            int requestId,
            int bookId,
            String bookTitle,
            String ownerName,
            String ownerEmail,
            String requesterName,
            String requesterEmail,
            String requestType,
            Integer offeredBookId,
            String offeredBookTitle) {

        this.requestId = requestId;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
        this.requesterName = requesterName;
        this.requesterEmail = requesterEmail;
        this.requestType = requestType;
        this.offeredBookId = offeredBookId;
        this.offeredBookTitle = offeredBookTitle;
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

    public String getRequestType() {
        return requestType;
    }

    public Integer getOfferedBookId() {
        return offeredBookId;
    }

    public String getOfferedBookTitle() {
        return offeredBookTitle;
    }
}

