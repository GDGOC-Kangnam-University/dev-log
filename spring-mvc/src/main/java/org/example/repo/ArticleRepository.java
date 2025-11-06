package org.example.repo;

import org.example.ArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<ArticleEntity, Integer> {
    /**
     * Find all undeleted articles.
     */
    List<ArticleEntity> findByDeletedFalse();

    /**
     * Find undeleted article by id.
     */
    Optional<ArticleEntity> findByIdAndDeletedFalse(int id);
}