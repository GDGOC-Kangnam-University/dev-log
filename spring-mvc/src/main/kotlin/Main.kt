package org.example

import kotlinx.serialization.json.Json
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
import org.springframework.web.bind.annotation.*


val db = Persistence()
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
	
	@RequestMapping("/")
	fun root() {
	}
	
	@GetMapping("/article/{articleId}", produces = ["application/json"])
	@ResponseBody
	suspend fun listArticle(@PathVariable(required = false) articleId: Int?): ResponseEntity<List<Article>> {
		return ResponseEntity.ok()
			.body(db.readArticle(articleId))
	}
	
	@GetMapping("/article", produces = ["application/json"])
	@ResponseBody
	suspend fun listAllArticle(): ResponseEntity<List<Article>> {
		return ResponseEntity.ok()
			.body(db.readArticle())
	}
	
	@PostMapping("/article", consumes = ["application/json"])
	@ResponseStatus(value = HttpStatus.NO_CONTENT)
	suspend fun createArticle(
		@RequestBody article: Article,
		@RequestHeader("userid", required = false) userid: Int? = null, // Just for testing
		@RequestHeader("userpass", required = false) userpass: String? = null
	): ResponseWithoutBody {
		if (userid == null || userpass == null)
			return Response.UNAUTHORIZED.body
		val perm = db.authorize(userid, userpass)
		return when (perm) {
			Permission.NONE -> Response.UNAUTHORIZED.body
			Permission.WRONG_LOGIN -> Response.FORBIDDEN.body
			Permission.NORMAL, Permission.SUPER -> if (db.createArticle(
					article,
					userid,
					perm
				)
			) ResponseEntity.ok(null) else ResponseEntity.internalServerError()
				.body(null)
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
			Permission.WRONG_LOGIN -> Response.FORBIDDEN
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

@Configuration
open class JsonConfig {
	@Bean
	open fun json(): Json = Json {
		prettyPrint = true
		isLenient = true
		ignoreUnknownKeys = true
	}
	
	@Bean
	open fun kotlinSerializationJsonHttpMessageConverter(json: Json): HttpMessageConverter<*> =
		KotlinSerializationJsonHttpMessageConverter(json)
	
}

fun main() {
	db.preloadExampleData()
	runApplication<MainApplication>()
}