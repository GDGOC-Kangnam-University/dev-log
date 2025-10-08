package org.example

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

typealias ResponseWithoutBody = ResponseEntity<Nothing?>

enum class Response(val body: ResponseWithoutBody) {
	NO_CONTENT(ResponseEntity.status(HttpStatus.NO_CONTENT).body(null)),
	UNAUTHORIZED(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null)),
	FORBIDDEN(ResponseEntity.status(HttpStatus.FORBIDDEN).body(null)),
	NOT_FOUND(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null))
}


@Suppress("unused")
@RestController
@SpringBootApplication
open class MainApplication {
	val db: Persistence
	
	@Autowired
	@Suppress("SpringJavaInjectionPointsAutowiringInspection") // False positive, it works
	constructor(db: Persistence) {
		this.db = db
		db.preloadExampleData()
	}
	
	@GetMapping("/article/{articleId}", produces = ["application/json"])
	suspend fun listArticle(@PathVariable(required = true) articleId: Int): Article {
		return db.readSingleArticle(articleId)
	}
	
	@GetMapping("/article", produces = ["application/json"])
	suspend fun listAllArticle(): ResponseEntity<List<Article>> {
		return ResponseEntity.ok()
			.body(db.readArticleList())
	}
	
	@PostMapping("/article", consumes = ["application/json"])
	@ResponseStatus(value = HttpStatus.NO_CONTENT)
	suspend fun createArticle(
		@RequestBody article: Article,
		@RequestHeader("userid", required = false) userid: Int? = null, // Just for testing
		@RequestHeader("userpass", required = false) userpass: String? = null // Just for testing 2
	): Int {
		if (userid == null || userpass == null)
			throw UnauthorizedError()
		val perm = db.authorize(userid, userpass)
		return when (perm) {
			Permission.NONE -> throw UnauthorizedError()
			Permission.NORMAL, Permission.SUPER -> db.createArticle(
				article,
				userid,
				perm
			)
		}
	}
	
	@PatchMapping("/article/{id}", consumes = ["application/json"])
	@ResponseStatus(value = HttpStatus.NO_CONTENT)
	suspend fun updateArticle(
		@PathVariable id: Int,
		@RequestBody article: ArticlePartial,
		@RequestHeader("userid", required = false) userId: Int? = null, // Just for testing
		@RequestHeader("userpass", required = false) userPass: String? = null
	): ResponseWithoutBody {
		if (userId == null || userPass == null)
			return Response.UNAUTHORIZED.body
		val perm = db.authorize(userId, userPass)
		return when (perm) {
			Permission.NONE -> Response.UNAUTHORIZED
			Permission.NORMAL, Permission.SUPER -> {
				
				val dbMutation = db.updateArticle(
					id,
					userId,
					perm,
					article.title,
					article.content
				)
				when (dbMutation) {
					ActionResult.LACK_OF_PERMISSION -> Response.FORBIDDEN
					ActionResult.NOT_EXIST -> Response.NOT_FOUND
					ActionResult.OK -> Response.NO_CONTENT
				}
			}
		}.body
	}
	
	@DeleteMapping("/article/{id}", consumes = ["application/json"])
	@ResponseStatus(value = HttpStatus.NO_CONTENT)
	suspend fun deleteArticle(
		@PathVariable id: Int,
		@RequestHeader("userid", required = false) userId: Int? = null, // Just for testing
		@RequestHeader("userpass", required = false) userPass: String? = null
	): ResponseWithoutBody {
		if (userId == null || userPass == null)
			return Response.UNAUTHORIZED.body
		val perm = db.authorize(userId, userPass)
		val dbAction = db.deleteArticle(id, userId, perm)
		return when (dbAction) {
			ActionResult.OK -> Response.NO_CONTENT
			ActionResult.NOT_EXIST -> Response.NOT_FOUND
			ActionResult.LACK_OF_PERMISSION -> Response.FORBIDDEN
		}.body
	}
}