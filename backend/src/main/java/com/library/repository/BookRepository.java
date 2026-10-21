package com.library.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.library.entity.Book;

import jakarta.persistence.LockModeType;

public interface BookRepository extends JpaRepository<Book, Long> {
    @Query("""
            select b from Book b
            where b.deleted = false
              and (lower(b.title) like lower(concat('%', :search, '%'))
               or lower(b.author) like lower(concat('%', :search, '%'))
               or lower(b.category) like lower(concat('%', :search, '%'))
               or lower(b.isbn) like lower(concat('%', :search, '%')))
            order by b.title
            """)
    List<Book> search(@Param("search") String search);

    List<Book> findByDeletedFalseOrderByTitleAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Book> findByIdAndDeletedFalse(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.id = :id and b.deleted = false")
    Optional<Book> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.id = :id")
    Optional<Book> findByIdForUpdateIncludingDeleted(@Param("id") Long id);

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);
}
