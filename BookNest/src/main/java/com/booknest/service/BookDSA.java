package com.booknest.service;

import com.booknest.model.Book;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** DSA utilities used by the actual BookNest marketplace. */
public final class BookDSA {
    private BookDSA() { }

    // Merge Sort: sorts Book objects by price in ascending order.
    public static void mergeSortByPrice(List<Book> books) {
        if (books == null || books.size() < 2) return;
        List<Book> sorted = mergeSortPrice(new ArrayList<>(books));
        books.clear();
        books.addAll(sorted);
    }

    private static List<Book> mergeSortPrice(List<Book> items) {
        if (items.size() <= 1) return items;
        int mid = items.size() / 2;
        List<Book> left = mergeSortPrice(new ArrayList<>(items.subList(0, mid)));
        List<Book> right = mergeSortPrice(new ArrayList<>(items.subList(mid, items.size())));
        List<Book> merged = new ArrayList<>(items.size());
        int i = 0, j = 0;
        while (i < left.size() && j < right.size()) {
            if (left.get(i).getPrice() <= right.get(j).getPrice()) merged.add(left.get(i++));
            else merged.add(right.get(j++));
        }
        while (i < left.size()) merged.add(left.get(i++));
        while (j < right.size()) merged.add(right.get(j++));
        return merged;
    }

    // Merge Sort: sorts titles alphabetically, preparing data for Binary Search.
    public static void mergeSortByTitle(List<Book> books) {
        if (books == null || books.size() < 2) return;
        List<Book> sorted = mergeSortTitle(new ArrayList<>(books));
        books.clear();
        books.addAll(sorted);
    }

    private static List<Book> mergeSortTitle(List<Book> items) {
        if (items.size() <= 1) return items;
        int mid = items.size() / 2;
        List<Book> left = mergeSortTitle(new ArrayList<>(items.subList(0, mid)));
        List<Book> right = mergeSortTitle(new ArrayList<>(items.subList(mid, items.size())));
        List<Book> merged = new ArrayList<>(items.size());
        int i = 0, j = 0;
        while (i < left.size() && j < right.size()) {
            if (left.get(i).getTitle().compareToIgnoreCase(right.get(j).getTitle()) <= 0)
                merged.add(left.get(i++));
            else merged.add(right.get(j++));
        }
        while (i < left.size()) merged.add(left.get(i++));
        while (j < right.size()) merged.add(right.get(j++));
        return merged;
    }

    // Binary Search on a title-sorted list. Returns one matching index, or -1.
    public static int binarySearchTitle(List<Book> sortedBooks, String title) {
        int low = 0, high = sortedBooks.size() - 1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            int comparison = sortedBooks.get(mid).getTitle().compareToIgnoreCase(title.trim());
            if (comparison == 0) return mid;
            if (comparison < 0) low = mid + 1;
            else high = mid - 1;
        }
        return -1;
    }

    // HashMap index: book ID is the key and the full Book object is the value.
    public static Map<Integer, Book> createBookIdIndex(List<Book> books) {
        Map<Integer, Book> index = new HashMap<>();
        for (Book book : books) index.put(book.getBookId(), book);
        return index;
    }
}
