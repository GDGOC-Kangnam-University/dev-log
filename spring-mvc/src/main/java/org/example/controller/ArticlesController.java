package org.example.controller;

import org.example.DTOTypes;
import org.example.Permission;
import org.example.Persistence;
import org.example.exception.UnauthorizedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SuppressWarnings("UastIncorrectHttpHeaderInspection") // No unknown headers warning

@RestController
@RequestMapping("/article")
class ArticlesController {
    private final Persistence db;

    @Autowired
    public ArticlesController(Persistence db) {
        this.db = db;
    }

    @GetMapping
    public ResponseEntity<List<DTOTypes.Article>> listAllArticles() {
        return ResponseEntity.ok(db.readArticleList());
    }

    @PostMapping
    public int createArticle(@RequestBody DTOTypes.Article article,
                             @RequestHeader("userid") int userId,
                             @RequestHeader("userpass") String userPass) {
        Permission perm = db.authorize(userId, userPass);
        if (perm == Permission.NONE) {
            throw new UnauthorizedError();
        }
        return db.createArticle(article, userId);
    }
}
