package com.library.dto.response;

import java.time.LocalDateTime;

import com.library.entity.BookReservation;

public record ReservationResponse(
        Long id,
        Long bookId,
        String title,
        String author,
        String status,
        LocalDateTime createdAt,
        String borrowerEmail) {
    public static ReservationResponse from(BookReservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getBook().getId(),
                reservation.getBook().getTitle(),
                reservation.getBook().getAuthor(),
                reservation.getStatus().name(),
                reservation.getCreatedAt(),
                reservation.getUser().getEmail());
    }
}
