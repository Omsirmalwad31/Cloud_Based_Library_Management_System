package com.library.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.library.dto.response.LoanResponse;
import com.library.dto.response.ReservationResponse;
import com.library.service.ReservationService;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/me")
    public List<ReservationResponse> myReservations(Authentication authentication) {
        return reservationService.myReservations(authentication.getName());
    }

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public List<ReservationResponse> allReservations() {
        return reservationService.allReservations();
    }

    @PostMapping("/{bookId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserve(@PathVariable Long bookId, Authentication authentication) {
        return reservationService.reserve(bookId, authentication.getName());
    }

    @PostMapping("/{reservationId}/claim")
    public LoanResponse claim(@PathVariable Long reservationId, Authentication authentication) {
        return reservationService.claim(reservationId, authentication.getName());
    }

    @DeleteMapping("/{reservationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long reservationId, Authentication authentication) {
        reservationService.cancel(reservationId, authentication.getName());
    }
}
