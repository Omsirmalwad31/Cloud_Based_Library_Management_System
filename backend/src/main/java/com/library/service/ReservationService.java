package com.library.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.library.dto.response.LoanResponse;
import com.library.dto.response.ReservationResponse;
import com.library.entity.BookReservation;
import com.library.entity.IssueRecord;
import com.library.entity.ReservationStatus;
import com.library.repository.BookRepository;
import com.library.repository.BookReservationRepository;
import com.library.repository.IssueRecordRepository;
import com.library.repository.UserRepository;

@Service
public class ReservationService {
    private static final List<ReservationStatus> ACTIVE_STATUSES =
            List.of(ReservationStatus.WAITING, ReservationStatus.READY);

    private final BookRepository books;
    private final BookReservationRepository reservations;
    private final UserRepository users;
    private final IssueRecordRepository loans;
    private final int durationDays;

    public ReservationService(
            BookRepository books,
            BookReservationRepository reservations,
            UserRepository users,
            IssueRecordRepository loans,
            @Value("${app.loan.duration-days:14}") int durationDays) {
        this.books = books;
        this.reservations = reservations;
        this.users = users;
        this.loans = loans;
        this.durationDays = durationDays;
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> myReservations(String email) {
        return reservations.findByUserEmailOrderByCreatedAtDesc(email).stream()
                .map(ReservationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> allReservations() {
        return reservations.findAllByOrderByCreatedAtAsc().stream()
                .map(ReservationResponse::from).toList();
    }

    @Transactional
    public ReservationResponse reserve(Long bookId, String email) {
        var book = books.findByIdForUpdate(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
        if (book.getAvailableCopies() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This book is available to borrow now");
        }
        var user = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (reservations.existsByBook_IdAndUser_IdAndStatusIn(bookId, user.getId(), ACTIVE_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an active reservation");
        }
        return ReservationResponse.from(reservations.save(new BookReservation(book, user)));
    }

    @Transactional
    public LoanResponse claim(Long reservationId, String email) {
        BookReservation reservation = reservations.findWithBookAndUserById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
        if (!reservation.getUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only claim your own reservation");
        }
        if (reservation.getStatus() != ReservationStatus.READY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This reservation is not ready to claim");
        }
        var book = books.findByIdForUpdate(reservation.getBook().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book is no longer available"));
        if (book.getAvailableCopies() < 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No copies are currently available");
        }
        book.checkOutCopy();
        reservation.markClaimed();
        LocalDate today = LocalDate.now();
        IssueRecord loan = loans.save(new IssueRecord(
                reservation.getUser(), book, today, today.plusDays(durationDays)));
        return LoanResponse.from(loan, hasActiveReservation(book.getId()));
    }

    @Transactional
    public void cancel(Long reservationId, String email) {
        BookReservation reservation = reservations.findWithBookAndUserById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
        if (!reservation.getUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only cancel your own reservation");
        }
        if (!ACTIVE_STATUSES.contains(reservation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This reservation is no longer active");
        }
        var book = books.findByIdForUpdateIncludingDeleted(reservation.getBook().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
        boolean wasReady = reservation.getStatus() == ReservationStatus.READY;
        reservation.cancel();
        if (wasReady && !book.isDeleted() && book.getAvailableCopies() > 0) {
            activateNext(book.getId());
        }
    }

    @Transactional
    public void activateNext(Long bookId) {
        if (reservations.existsByBook_IdAndStatus(bookId, ReservationStatus.READY)) {
            return;
        }
        reservations.findFirstByBook_IdAndStatusOrderByCreatedAtAscIdAsc(
                bookId, ReservationStatus.WAITING).ifPresent(BookReservation::markReady);
    }

    @Transactional
    public void cancelForDeletedBook(Long bookId) {
        reservations.findAllByBook_IdAndStatusIn(bookId, ACTIVE_STATUSES)
                .forEach(BookReservation::cancel);
    }

    public boolean hasActiveReservation(Long bookId) {
        return reservations.existsByBook_IdAndStatusIn(bookId, ACTIVE_STATUSES);
    }
}
