package com.library.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import com.library.entity.IssueRecord;

public interface IssueRecordRepository extends JpaRepository<IssueRecord, Long> {
    @EntityGraph(attributePaths = {"book", "user"})
    List<IssueRecord> findByUserEmailOrderByIssueDateDesc(String email);

    @EntityGraph(attributePaths = {"book", "user"})
    List<IssueRecord> findAllByOrderByIssueDateDesc();

    @EntityGraph(attributePaths = {"book", "user"})
    Optional<IssueRecord> findWithBookAndUserById(Long id);
}
