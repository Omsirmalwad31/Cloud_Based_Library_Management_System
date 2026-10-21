package com.library.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.library.entity.Book;
import com.library.dto.request.BookRequest;
import com.library.repository.BookReservationRepository;
import com.library.repository.BookRepository;

import org.mockito.Mockito;

class BookServiceTest {
    private BookRepository books;
    private BookReservationRepository reservations;
    private ReservationService reservationService;
    private BookService service;

    @BeforeEach
    void setUp() {
        books = Mockito.mock(BookRepository.class);
        reservations = Mockito.mock(BookReservationRepository.class);
        reservationService = Mockito.mock(ReservationService.class);
        service = new BookService(books, reservations, reservationService);
    }

    @Test
    void marksBookDeletedWithoutRemovingItsHistory() {
        Book book = new Book("Test title", "Test author", "1234567890", "TEST", 1);
        when(books.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(book));

        service.delete(1L);

        assertTrue(book.isDeleted());
    }

    @Test
    void reportsMissingBook() {
        when(books.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.delete(1L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(books).findByIdAndDeletedFalse(1L);
    }

    @Test
    void updatesBookWhilePreservingCheckedOutCopies() {
        Book book = new Book("Old title", "Author", "1234567890", "TEST", 3);
        book.checkOutCopy();
        when(books.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(book));
        when(books.existsByIsbnAndIdNot("1234567891", 1L)).thenReturn(false);

        service.update(1L, new BookRequest("New title", "Author", "1234567891", "reference", 5));

        assertEquals("New title", book.getTitle());
        assertEquals(5, book.getTotalCopies());
        assertEquals(4, book.getAvailableCopies());
    }

    @Test
    void rejectsCopyCountBelowCurrentLoans() {
        Book book = new Book("Title", "Author", "1234567890", "TEST", 2);
        book.checkOutCopy();
        book.checkOutCopy();
        when(books.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(book));
        when(books.existsByIsbnAndIdNot("1234567890", 1L)).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.update(1L, new BookRequest("Title", "Author", "1234567890", "TEST", 1)));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }
}
