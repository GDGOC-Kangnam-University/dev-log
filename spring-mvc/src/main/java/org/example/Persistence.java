package org.example;

import jakarta.transaction.Transactional;
import org.example.exception.NotFoundError;
import org.example.repo.ArticleRepository;
import org.example.repo.AuthorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public class Persistence {
    private final ArticleRepository articleRepository;
    private final AuthorRepository authorRepository;

    @Autowired
    public Persistence(ArticleRepository articleRepository, AuthorRepository authorRepository) {
        this.articleRepository = articleRepository;
        this.authorRepository = authorRepository;
    }


    @Transactional
    public void preloadExampleData() {
        AuthorEntity author1 = new AuthorEntity();
        author1.setPw("alicepw");
        author1.setSudoer(true);
        authorRepository.save(author1);

        AuthorEntity author2 = new AuthorEntity();
        author2.setPw("bobpw");
        author2.setSudoer(false);
        authorRepository.save(author2);

        ArticleEntity article1 = new ArticleEntity();
        article1.setTitle("Welcome to the blog");
        article1.setAuthor(1);
        article1.setContent("This is the first example article.");
        articleRepository.save(article1);

        ArticleEntity article2 = new ArticleEntity();
        article2.setTitle("Second post");
        article2.setAuthor(2);
        article2.setContent("Another example article content.");
        articleRepository.save(article2);
    }

    public DTOTypes.Article readSingleArticle(int articleId) {
        ArticleEntity article = articleRepository.findById(articleId).orElse(null);
        if (article == null || article.isDeleted()) {
            throw new NotFoundError();
        }
        return new DTOTypes.Article(article.getId(), article.getTitle(), article.getContent(), article.getAuthor(), article.isDeleted());
    }

    public List<DTOTypes.Article> readArticleList() {
        return articleRepository.findAll().stream()
                .filter(article -> !article.isDeleted())
                .map(article -> new DTOTypes.Article(article.getId(), article.getTitle(), article.getContent(), article.getAuthor(), article.isDeleted()))
                .toList();
    }

    @Transactional
    public int createArticle(DTOTypes.Article article, int userId) {
        ArticleEntity newArticle = new ArticleEntity();
        newArticle.setTitle(article.title());
        newArticle.setAuthor(userId);
        newArticle.setContent(article.content());

        return articleRepository.save(newArticle).getId();
    }

    @Transactional
    public ActionResult updateArticle(int articleId, int userId, Permission userPermission, String newTitle, String newContent) {
        ArticleEntity article = articleRepository.findById(articleId).orElse(null);
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
        articleRepository.save(article);
        return ActionResult.OK;
    }

    @Transactional
    public ActionResult deleteArticle(int articleId, int userId, Permission userPermission) {
        ArticleEntity article = articleRepository.findById(articleId).orElse(null);
        if (article == null || article.isDeleted()) {
            return ActionResult.NOT_EXIST;
        }
        if (!hasWritePermission(article, userId, userPermission)) {
            return ActionResult.LACK_OF_PERMISSION;
        }
        article.setDeleted(true);
        articleRepository.save(article);
        return ActionResult.OK;
    }

    public Permission authorize(int id, String pw) {
        AuthorEntity author = authorRepository.findById(id).orElse(null);
        if (author == null || !author.getPw().equals(pw)) {
            return Permission.NONE;
        }
        return author.isSudoer() ? Permission.SUPER : Permission.NORMAL;
    }

    private boolean hasWritePermission(ArticleEntity article, int userId, Permission userPermission) {
        return userPermission == Permission.SUPER || (userPermission == Permission.NORMAL && article.getAuthor() == userId);
    }
}
