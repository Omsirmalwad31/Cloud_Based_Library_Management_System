package com.library.service;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.library.dto.request.BookRequest;
import com.library.dto.response.BookResponse;
import com.library.entity.Book;
import com.library.entity.ReservationStatus;
import com.library.repository.BookReservationRepository;
import com.library.repository.BookRepository;

@Service
public class BookService {
    private final BookRepository books;
    private final BookReservationRepository reservations;
    private final ReservationService reservationService;

    public BookService(
            BookRepository books,
            BookReservationRepository reservations,
            ReservationService reservationService) {
        this.books = books;
        this.reservations = reservations;
        this.reservationService = reservationService;
    }

    @Transactional(readOnly = true)
    public List<BookResponse> search(String query) {
        String search = query == null ? "" : query.trim();
        List<Book> result = search.isEmpty()
                ? books.findByDeletedFalseOrderByTitleAsc()
                : books.search(search);
        return result.stream().map(book -> BookResponse.from(book,
                book.getAvailableCopies() - (int) reservations.countByBook_IdAndStatus(
                        book.getId(), ReservationStatus.READY))).toList();
    }

    @Transactional
    public BookResponse add(BookRequest request) {
        String isbn = request.isbn().trim();
        if (books.existsByIsbn(isbn)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A book with this ISBN already exists");
        }
        Book book = new Book(request.title().trim(), request.author().trim(), isbn,
                request.category().trim().toUpperCase(Locale.ROOT), request.copies());
        return BookResponse.from(books.save(book));
    }

    @Transactional
    public BookResponse update(Long id, BookRequest request) {
        Book book = books.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
        String isbn = request.isbn().trim();
        if (books.existsByIsbnAndIdNot(isbn, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A book with this ISBN already exists");
        }
        try {
            book.updateDetails(request.title().trim(), request.author().trim(), isbn,
                    request.category().trim().toUpperCase(Locale.ROOT), request.copies());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        if (book.getAvailableCopies() > 0) {
            reservationService.activateNext(id);
        }
        return BookResponse.from(book, book.getAvailableCopies() - (int) reservations
                .countByBook_IdAndStatus(id, ReservationStatus.READY));
    }

    @Transactional
    public void delete(Long id) {
        Book book = books.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
        reservationService.cancelForDeletedBook(id);
        book.markDeleted();
    }
}
