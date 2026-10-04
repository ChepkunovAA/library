package com.library.repo;

import com.library.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndBookId(Long userId, Long bookId);

    long countByBookId(Long bookId);

    List<Favorite> findByUserId(Long userId);

    void deleteByBookId(Long bookId);
}
