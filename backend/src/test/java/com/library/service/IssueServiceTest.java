package com.library.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.library.entity.Book;
import com.library.entity.IssueRecord;
import com.library.entity.Role;
import com.library.entity.User;
import com.library.repository.BookRepository;
import com.library.repository.BookReservationRepository;
import com.library.repository.IssueRecordRepository;
import com.library.repository.UserRepository;

class IssueServiceTest {
    private final BookRepository books = mock(BookRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final IssueRecordRepository loans = mock(IssueRecordRepository.class);
    private final BookReservationRepository reservations = mock(BookReservationRepository.class);
    private final ReservationService reservationService = mock(ReservationService.class);
    private final IssueService service =
            new IssueService(books, users, loans, reservations, reservationService, 14);

    @Test
    void renewsEligibleLoanForOneMoreLoanPeriod() {
        LocalDate today = LocalDate.now();
        User user = new User("reader@library.local", "hash", Role.STUDENT);
        Book book = new Book("Title", "Author", "1234567890", "TEST", 1);
        IssueRecord loan = new IssueRecord(user, book, today.minusDays(1), today.plusDays(13));
        when(loans.findWithBookAndUserById(4L)).thenReturn(Optional.of(loan));
        when(reservationService.hasActiveReservation(null)).thenReturn(false);

        service.renew(4L, user.getEmail());

        assertEquals(today.plusDays(27), loan.getDueDate());
        assertEquals(1, loan.getRenewalsUsed());
    }

    @Test
    void preventsRenewalWhenAnotherMemberIsWaiting() {
        LocalDate today = LocalDate.now();
        User user = new User("reader@library.local", "hash", Role.STUDENT);
        Book book = new Book("Title", "Author", "1234567890", "TEST", 1);
        IssueRecord loan = new IssueRecord(user, book, today.minusDays(1), today.plusDays(13));
        when(loans.findWithBookAndUserById(4L)).thenReturn(Optional.of(loan));
        when(reservationService.hasActiveReservation(null)).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.renew(4L, user.getEmail()));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(0, loan.getRenewalsUsed());
    }
}
