package org.example.controller;

import org.example.ActionResult;
import org.example.DTOTypes;
import org.example.Permission;
import org.example.Persistence;
import org.example.exception.ForbiddenError;
import org.example.exception.NotFoundError;
import org.example.exception.UnauthorizedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @see <a href="https://www.youtube.com/watch?v=Q2CYvEOk0LM">link</a>
 * <br>
 * Article과 Articles 구분이 한눈에 안 보이네...<br>
 * 내 골반이 멈추지 않는 탓일가?<br>
 * T.T
 */
@SuppressWarnings("UastIncorrectHttpHeaderInspection")
@RestController
@RequestMapping("/article/{id}")
class SingleArticleController {
    private final Persistence db;

    @Autowired
    public SingleArticleController(Persistence db) {
        this.db = db;
    }

    @DeleteMapping
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

    @GetMapping
    public DTOTypes.Article listArticle(@PathVariable String id) {
        return db.readSingleArticle(Integer.parseInt(id));
    }

}
