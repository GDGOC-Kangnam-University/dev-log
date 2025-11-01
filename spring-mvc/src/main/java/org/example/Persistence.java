package org.example;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class Persistence {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void preloadExampleData() {
        AuthorEntity author1 = new AuthorEntity();
        author1.setPw("alicepw");
        author1.setSudoer(true);
        entityManager.persist(author1);

        AuthorEntity author2 = new AuthorEntity();
        author2.setPw("bobpw");
        author2.setSudoer(false);
        entityManager.persist(author2);

        ArticleEntity article1 = new ArticleEntity();
        article1.setTitle("Welcome to the blog");
        article1.setAuthor(1);
        article1.setContent("This is the first example article.");
        entityManager.persist(article1);

        ArticleEntity article2 = new ArticleEntity();
        article2.setTitle("Second post");
        article2.setAuthor(2);
        article2.setContent("Another example article content.");
        entityManager.persist(article2);
    }

    public DTOTypes.Article readSingleArticle(int articleId) {
        ArticleEntity article = entityManager.find(ArticleEntity.class, articleId);
        if (article == null || article.isDeleted()) {
            throw new NotFoundError();
        }
        return new DTOTypes.Article(article.getId(), article.getTitle(), article.getContent(), article.getAuthor(), article.isDeleted());
    }

    public List<DTOTypes.Article> readArticleList() {
        return entityManager.createQuery("SELECT a FROM ArticleEntity a WHERE a.deleted = false", ArticleEntity.class)
                .getResultList().stream()
                .map(a -> new DTOTypes.Article(a.getId(), a.getTitle(), a.getContent(), a.getAuthor(), a.isDeleted()))
                .toList();
    }

    @Transactional
    public int createArticle(DTOTypes.Article article, int userId) {
        ArticleEntity newArticle = new ArticleEntity();
        newArticle.setTitle(article.title());
        newArticle.setAuthor(userId);
        newArticle.setContent(article.content());
        entityManager.persist(newArticle);
        return newArticle.getId();
    }

    @Transactional
    public ActionResult updateArticle(int articleId, int userId, Permission userPermission, String newTitle, String newContent) {
        ArticleEntity article = entityManager.find(ArticleEntity.class, articleId);
        if (article == null || article.isDeleted()) {
            return ActionResult.NOT_EXIST;
        }
        if (!hasWritePermission(article, userId, userPermission)) {
            return ActionResult.LACK_OF_PERMISSION;
        }
        if (newTitle != null) {
            article.setTitle(newTitle);
        }
        if (newContent != null) {
            article.setContent(newContent);
        }
        entityManager.merge(article);
        return ActionResult.OK;
    }

    @Transactional
    public ActionResult deleteArticle(int articleId, int userId, Permission userPermission) {
        ArticleEntity article = entityManager.find(ArticleEntity.class, articleId);
        if (article == null || article.isDeleted()) {
            return ActionResult.NOT_EXIST;
        }
        if (!hasWritePermission(article, userId, userPermission)) {
            return ActionResult.LACK_OF_PERMISSION;
        }
        article.setDeleted(true);
        entityManager.merge(article);
        return ActionResult.OK;
    }

    public Permission authorize(int id, String pw) {
        AuthorEntity author = entityManager.find(AuthorEntity.class, id);
        if (author == null || !author.getPw().equals(pw)) {
            return Permission.NONE;
        }
        return author.isSudoer() ? Permission.SUPER : Permission.NORMAL;
    }

    private boolean hasWritePermission(ArticleEntity article, int userId, Permission userPermission) {
        return userPermission == Permission.SUPER || (userPermission == Permission.NORMAL && article.getAuthor() == userId);
    }
}
