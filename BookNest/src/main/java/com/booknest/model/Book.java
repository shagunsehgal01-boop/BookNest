
package com.booknest.model;

public class Book {

    private int bookId;
    private int ownerId;
    private String title;
    private String author;
    private String category;
    private String bookCondition;
    private String listingType;
    private double price;
    private String imagePath;
    private String ownerName;

    public Book(int bookId, int ownerId, String title,
                String author, String category,
                String bookCondition, String listingType,
                double price, String imagePath, String ownerName) {

        this.bookId = bookId;
        this.ownerId = ownerId;
        this.title = title;
        this.author = author;
        this.category = category;
        this.bookCondition = bookCondition;
        this.listingType = listingType;
        this.price = price;
        this.imagePath = imagePath;
        this.ownerName = ownerName;
    }

    public int getBookId() {
        return bookId;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getCategory() {
        return category;
    }

    public String getBookCondition() {
        return bookCondition;
    }

    public String getListingType() {
        return listingType;
    }

    public double getPrice() {
        return price;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public String toString() {
        return title + " (ID: " + bookId + ")";
    }
}

