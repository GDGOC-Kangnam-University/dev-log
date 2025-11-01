package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SuppressWarnings("UastIncorrectHttpHeaderInspection")
@RestController
@SpringBootApplication
public class MainApplication {

    private final Persistence db;

    @Autowired
    public MainApplication(Persistence db) {
        this.db = db;
        db.preloadExampleData();
    }

    @GetMapping("/article/{articleId}")
    public DTOTypes.Article listArticle(@PathVariable int articleId) {
        return db.readSingleArticle(articleId);
    }

    @GetMapping("/article")
    public ResponseEntity<List<DTOTypes.Article>> listAllArticles() {
        return ResponseEntity.ok(db.readArticleList());
    }

    @PostMapping("/article")
    public int createArticle(@RequestBody DTOTypes.Article article,
                             @RequestHeader("userid") int userId,
                             @RequestHeader("userpass") String userPass) {
        Permission perm = db.authorize(userId, userPass);
        if (perm == Permission.NONE) {
            throw new UnauthorizedError();
        }
        return db.createArticle(article, userId);
    }

    @PatchMapping("/article/{id}")
    public void updateArticle(@PathVariable int id,
                              @RequestBody DTOTypes.ArticlePartial article,
                              @RequestHeader("userid") int userId,
                              @RequestHeader("userpass") String userPass) {
        Permission perm = db.authorize(userId, userPass);
        if (perm == Permission.NONE) {
            throw new UnauthorizedError();
        }
        ActionResult result = db.updateArticle(id, userId, perm, article.title(), article.content());
        switch (result) {
            case LACK_OF_PERMISSION -> throw new ForbiddenError();
            case NOT_EXIST -> throw new NotFoundError();
        }
    }

    @DeleteMapping("/article/{id}")
    public void deleteArticle(@PathVariable int id,
                              @RequestHeader("userid") int userId,
                              @RequestHeader("userpass") String userPass) {
        Permission perm = db.authorize(userId, userPass);
        if (perm == Permission.NONE) {
            throw new UnauthorizedError();
        }
        ActionResult result = db.deleteArticle(id, userId, perm);
        switch (result) {
            case LACK_OF_PERMISSION -> throw new ForbiddenError();
            case NOT_EXIST -> throw new NotFoundError();
        }
    }
}
