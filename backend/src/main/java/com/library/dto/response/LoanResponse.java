package com.library.dto.response;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.library.entity.IssueRecord;

public record LoanResponse(
        Long id,
        Long bookId,
        String title,
        String author,
        LocalDate issueDate,
        LocalDate dueDate,
        LocalDate returnDate,
        boolean returned,
        long daysLate,
        long fineRupees) {
    public static LoanResponse from(IssueRecord loan) {
        LocalDate endDate = loan.getReturnDate() == null ? LocalDate.now() : loan.getReturnDate();
        long daysLate = Math.max(0, ChronoUnit.DAYS.between(loan.getDueDate(), endDate));
        return new LoanResponse(loan.getId(), loan.getBook().getId(), loan.getBook().getTitle(),
                loan.getBook().getAuthor(), loan.getIssueDate(), loan.getDueDate(), loan.getReturnDate(),
                loan.getReturnDate() != null, daysLate, daysLate * 5);
    }
}
