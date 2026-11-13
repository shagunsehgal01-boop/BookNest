
package com.booknest.model;

public class BookRequest {

    private int requestId;
    private int bookId;
    private String bookTitle;
    private String requesterName;
    private String requesterEmail;
    private String offeredBookTitle;
    private String requestType;
    private String requestStatus;

    public BookRequest(int requestId, int bookId,
                       String bookTitle,
                       String requesterName,
                       String requesterEmail,
                       String offeredBookTitle,
                       String requestType,
                       String requestStatus) {

        this.requestId = requestId;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.requesterName = requesterName;
        this.requesterEmail = requesterEmail;
        this.offeredBookTitle = offeredBookTitle;
        this.requestType = requestType;
        this.requestStatus = requestStatus;
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

    public String getRequesterName() {
        return requesterName;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public String getOfferedBookTitle() {
        return offeredBookTitle;
    }

    public String getRequestType() {
        return requestType;
    }

    public String getRequestStatus() {
        return requestStatus;
    }
}
