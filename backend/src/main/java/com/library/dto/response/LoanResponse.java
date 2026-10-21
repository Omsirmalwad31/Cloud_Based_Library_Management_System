package com.library.dto.response;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.library.entity.IssueRecord;

public record LoanResponse(
        Long id,
        Long bookId,
        String title,
        String author,
        String borrowerEmail,
        LocalDate issueDate,
        LocalDate dueDate,
        LocalDate returnDate,
        boolean returned,
        int renewalsUsed,
        boolean renewalAvailable,
        long daysLate,
        long fineRupees) {
    public static LoanResponse from(IssueRecord loan, boolean reservationWaiting) {
        LocalDate endDate = loan.getReturnDate() == null ? LocalDate.now() : loan.getReturnDate();
        long daysLate = Math.max(0, ChronoUnit.DAYS.between(loan.getDueDate(), endDate));
        return new LoanResponse(loan.getId(), loan.getBook().getId(), loan.getBook().getTitle(),
                loan.getBook().getAuthor(), loan.getUser().getEmail(), loan.getIssueDate(), loan.getDueDate(),
                loan.getReturnDate(), loan.getReturnDate() != null, loan.getRenewalsUsed(),
                loan.getReturnDate() == null && loan.getRenewalsUsed() == 0
                        && !loan.getDueDate().isBefore(LocalDate.now()) && !reservationWaiting,
                daysLate, daysLate * 5);
    }
}
