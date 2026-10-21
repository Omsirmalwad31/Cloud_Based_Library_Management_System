package com.library.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import com.library.entity.BookReservation;
import com.library.entity.ReservationStatus;

public interface BookReservationRepository extends JpaRepository<BookReservation, Long> {
    boolean existsByBook_IdAndStatus(Long bookId, ReservationStatus status);

    boolean existsByBook_IdAndStatusIn(Long bookId, Collection<ReservationStatus> statuses);

    boolean existsByBook_IdAndUser_IdAndStatusIn(
            Long bookId, Long userId, Collection<ReservationStatus> statuses);

    long countByBook_IdAndStatus(Long bookId, ReservationStatus status);

    @EntityGraph(attributePaths = {"book", "user"})
    List<BookReservation> findByUserEmailOrderByCreatedAtDesc(String email);

    @EntityGraph(attributePaths = {"book", "user"})
    List<BookReservation> findAllByOrderByCreatedAtAsc();

    List<BookReservation> findAllByBook_IdAndStatusIn(
            Long bookId, Collection<ReservationStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from BookReservation r join fetch r.book join fetch r.user where r.id = :id")
    Optional<BookReservation> findWithBookAndUserById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"book", "user"})
    Optional<BookReservation> findFirstByBook_IdAndStatusOrderByCreatedAtAscIdAsc(
            Long bookId, ReservationStatus status);
}
