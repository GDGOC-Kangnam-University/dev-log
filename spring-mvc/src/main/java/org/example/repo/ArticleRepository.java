package org.example.repo;

import org.example.entity.ArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 아무튼 뭔가 지가 알아서 구현을 함...
 */
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