package com.library.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.library.dto.response.LoanResponse;
import com.library.entity.IssueRecord;
import com.library.repository.BookReservationRepository;
import com.library.repository.BookRepository;
import com.library.repository.IssueRecordRepository;
import com.library.repository.UserRepository;

@Service
public class IssueService {
    private final BookRepository books;
    private final UserRepository users;
    private final IssueRecordRepository loans;
    private final BookReservationRepository reservations;
    private final ReservationService reservationService;
    private final int durationDays;

    public IssueService(BookRepository books, UserRepository users, IssueRecordRepository loans,
            BookReservationRepository reservations, ReservationService reservationService,
            @Value("${app.loan.duration-days:14}") int durationDays) {
        this.books = books;
        this.users = users;
        this.loans = loans;
        this.reservations = reservations;
        this.reservationService = reservationService;
        this.durationDays = durationDays;
    }

    @Transactional
    public LoanResponse issue(Long bookId, String email) {
        var book = books.findByIdForUpdate(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
        if (book.getAvailableCopies() == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No copies are currently available");
        }
        if (reservationService.hasActiveReservation(bookId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This copy is held for a reservation");
        }
        var user = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        book.checkOutCopy();
        LocalDate today = LocalDate.now();
        return LoanResponse.from(
                loans.save(new IssueRecord(user, book, today, today.plusDays(durationDays))),
                reservationService.hasActiveReservation(bookId));
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> myLoans(String email) {
        return loans.findByUserEmailOrderByIssueDateDesc(email).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> allLoans() {
        return loans.findAllByOrderByIssueDateDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public LoanResponse returnBook(Long loanId, String email, boolean librarian) {
        IssueRecord loan = loans.findWithBookAndUserById(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
        if (!librarian && !loan.getUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only return your own loans");
        }
        if (loan.getReturnDate() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This book has already been returned");
        }
        var book = books.findByIdForUpdateIncludingDeleted(loan.getBook().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
        loan.markReturned(LocalDate.now());
        book.returnCopy();
        if (!book.isDeleted()) {
            reservationService.activateNext(book.getId());
        }
        return toResponse(loan);
    }

    @Transactional
    public LoanResponse renew(Long loanId, String email) {
        IssueRecord loan = loans.findWithBookAndUserById(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
        if (!loan.getUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only renew your own loans");
        }
        if (loan.getReturnDate() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A returned loan cannot be renewed");
        }
        if (loan.getRenewalsUsed() >= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This loan has already been renewed");
        }
        if (loan.getDueDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Overdue loans cannot be renewed");
        }
        if (reservationService.hasActiveReservation(loan.getBook().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This loan cannot be renewed while another member is waiting for the book");
        }
        loan.renew(durationDays);
        return toResponse(loan);
    }

    private LoanResponse toResponse(IssueRecord loan) {
        return LoanResponse.from(loan, reservationService.hasActiveReservation(loan.getBook().getId()));
    }
}
