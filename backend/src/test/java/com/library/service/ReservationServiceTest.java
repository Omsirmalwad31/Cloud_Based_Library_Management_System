package com.library.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.library.entity.Book;
import com.library.entity.BookReservation;
import com.library.entity.ReservationStatus;
import com.library.repository.BookRepository;
import com.library.repository.BookReservationRepository;
import com.library.repository.IssueRecordRepository;
import com.library.repository.UserRepository;

class ReservationServiceTest {
    @Test
    void activatesOldestWaitingReservationWhenCopyReturns() {
        Book book = new Book("Title", "Author", "1234567890", "TEST", 1);
        BookReservation next = new BookReservation(book,
                new com.library.entity.User("reader@library.local", "hash", com.library.entity.Role.STUDENT));
        BookRepository books = Mockito.mock(BookRepository.class);
        BookReservationRepository reservations = Mockito.mock(BookReservationRepository.class);
        ReservationService service = new ReservationService(
                books, reservations, Mockito.mock(UserRepository.class),
                Mockito.mock(IssueRecordRepository.class), 14);
        when(reservations.existsByBook_IdAndStatus(1L, ReservationStatus.READY)).thenReturn(false);
        when(reservations.findFirstByBook_IdAndStatusOrderByCreatedAtAscIdAsc(
                1L, ReservationStatus.WAITING)).thenReturn(Optional.of(next));

        service.activateNext(1L);

        assertEquals(ReservationStatus.READY, next.getStatus());
        verify(reservations).findFirstByBook_IdAndStatusOrderByCreatedAtAscIdAsc(
                1L, ReservationStatus.WAITING);
    }
}
