package com.library.dto.response;

import com.library.entity.Book;

public record BookResponse(
        Long id,
        String title,
        String author,
        String isbn,
        String category,
        int totalCopies,
        int availableCopies) {
    public static BookResponse from(Book book) {
        return from(book, book.getAvailableCopies());
    }

    public static BookResponse from(Book book, int availableCopies) {
        return new BookResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(),
                book.getCategory(), book.getTotalCopies(), availableCopies);
    }
}
