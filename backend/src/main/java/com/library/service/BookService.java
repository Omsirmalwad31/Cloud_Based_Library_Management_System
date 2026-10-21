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
import com.library.repository.BookRepository;

@Service
public class BookService {
    private final BookRepository books;

    public BookService(BookRepository books) {
        this.books = books;
    }

    @Transactional(readOnly = true)
    public List<BookResponse> search(String query) {
        String search = query == null ? "" : query.trim();
        List<Book> result = search.isEmpty()
                ? books.findAll(org.springframework.data.domain.Sort.by("title"))
                : books.search(search);
        return result.stream().map(BookResponse::from).toList();
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
}
