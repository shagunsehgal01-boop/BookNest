# BookNest – ADSA Viva Notes

BookNest uses DSA as part of its existing marketplace and request workflow. There is no separate DSA screen; the algorithms support normal application features.

## 1. ArrayList – Book, request and transaction collections
- **Where:** `BookDAO`, `RequestDAO`, `TransactionDAO`, and `BookListFrame`.
- **Use:** Store multiple Java objects retrieved from MySQL.
- **Why:** Dynamic size and convenient iteration.
- **Complexity:** `get(index)` is O(1); appending is amortized O(1); searching by value is O(n).
- **Viva note:** The database stores persistent records. ArrayList stores the records temporarily in Java after retrieval.

## 2. Merge Sort – Marketplace sorting
- **Where:** `service/BookDSA.java`, called from `ui/BookListFrame.java`.
- **Use:** Sort available books by price or alphabetically by title.
- **Idea:** Divide the list into halves, recursively sort each half, then merge the sorted halves.
- **Time:** Best, average and worst O(n log n).
- **Auxiliary space:** O(n).
- **Viva note:** This is our own Merge Sort implementation, not SQL `ORDER BY`.

## 3. Binary Search – Exact book-title search
- **Where:** `BookDSA.binarySearchTitle()` called by the Exact title button in the marketplace.
- **Use:** First sort the available books by title using Merge Sort, then search for an exact title.
- **Time:** O(log n) for finding a match; collecting k separately listed copies with the same title takes O(k) extra time.
- **Requirement:** The list must be sorted using the same title comparison before Binary Search.
- **Viva note:** Normal partial title/author search still uses a parameterized SQL LIKE query. Only the Exact title action demonstrates our Binary Search implementation.

## 4. HashMap – Book ID lookup
- **Where:** `BookDSA.createBookIdIndex()` and the View Details action in `BookListFrame`.
- **Use:** Map each `bookId` to its `Book` object; retrieve the selected listing by ID.
- **Average lookup:** O(1); worst case can be O(n) due to collisions.
- **Viva note:** Key is the unique book ID; value is the complete Book object.

## 5. Queue – Incoming request order
- **Where:** `IncomingRequestsFrame` uses `ArrayDeque` through the `Queue` interface; `RequestDAO` retrieves requests oldest-first.
- **Use:** Display incoming requests in FIFO order (First In, First Out). `poll()` removes the head from the in-memory display queue; it does not delete the database request.
- **Enqueue / dequeue:** O(1) amortized for ArrayDeque.
- **Viva note:** The database remains the permanent source of requests. The Queue is a temporary in-memory representation for FIFO display/processing order.

## Demonstration sequence
1. Add a few books with different prices and titles.
2. In Explore All Books, select **Price: Low to High** to demonstrate Merge Sort.
3. Enter a complete title and choose **Exact title** to demonstrate Merge Sort + Binary Search. If multiple students listed the same title, all matching copies appear.
4. Open View Details to demonstrate HashMap lookup by `bookId`.
5. Create requests from another student account. Incoming Requests displays the oldest request first using Queue FIFO order.
6. Explain ArrayList by showing the `ArrayList` collections populated from database ResultSets in the DAO classes.

## Important distinction
Do not say every database search is Binary Search. MySQL executes the regular SQL search. The custom Binary Search is used only by the marketplace's Exact title action after sorting the Java list.
