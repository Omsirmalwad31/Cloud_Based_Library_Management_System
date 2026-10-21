package com.library.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.library.dto.response.LoanResponse;
import com.library.service.IssueService;

@RestController
@RequestMapping("/api/loans")
public class IssueController {
    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @GetMapping("/me")
    public List<LoanResponse> myLoans(Authentication authentication) {
        return issueService.myLoans(authentication.getName());
    }

    @PostMapping("/{bookId}")
    public LoanResponse issue(@PathVariable Long bookId, Authentication authentication) {
        return issueService.issue(bookId, authentication.getName());
    }

    @PutMapping("/{loanId}/return")
    public LoanResponse returnBook(@PathVariable Long loanId, Authentication authentication) {
        boolean librarian = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_LIBRARIAN"));
        return issueService.returnBook(loanId, authentication.getName(), librarian);
    }
}
