package org.example

import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.not
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

class Persistence {
	val db = R2dbcDatabase.connect("r2dbc:h2:mem:///test;DB_CLOSE_DELAY=-1")
	
	val articleTable = Article.Companion.DatabaseTable
	val authorTable = Author.Companion.DatabaseTable
	
	fun preloadExampleData() = runBlocking {
		suspendTransaction(db) {
			SchemaUtils.create(authorTable)
			SchemaUtils.create(articleTable)
			// 예제 authors
			authorTable.insert {
				it[authorTable.pw] = "alicepw"
				it[authorTable.sudoer] = true
			}
			authorTable.insert {
				it[authorTable.pw] = "bobpw"
				it[authorTable.sudoer] = false
			}
			
			// 예제 articles
			articleTable.insert {
				it[articleTable.title] = "Welcome to the blog"
				it[articleTable.author] = 1
				it[articleTable.content] = "This is the first example article."
				it[articleTable.deleted] = false
			}
			articleTable.insert {
				it[articleTable.title] = "Second post"
				it[articleTable.author] = 2
				it[articleTable.content] = "Another example article content."
				it[articleTable.deleted] = false
			}
		}
	}
	
	suspend fun createArticle(article: Article, userId: Int, userPermission: Permission = Permission.NONE) =
		suspendTransaction(db) {
			articleTable.insert {
				it[title] = article.title
				it[author] = userId
				it[content] = article.content
			}.insertedCount == 1
		}
	
	// If articleId is null, lists up all article.
	// If limit is non-null and articleId isn't, listing will be capped.
	suspend fun readArticle(articleId: Int? = null, limit: Int? = 5) = suspendTransaction(db) {
		var articles = articleTable.selectAll().where { not(articleTable.deleted) }
		if (articleId != null)
			articles = articles.andWhere { articleTable.id eq articleId }.limit(1)
		if (articleId == null && limit != null)
			articles.limit(limit)
		articles.mapNotNull {
			Article(
				id = it[articleTable.id],
				title = it[articleTable.title],
				content = it[articleTable.content],
				author = it[articleTable.author]
			)
		}.toList()
	}
	
	suspend fun deleteArticle(articleId: Int, userId: Int, userPermission: Permission): ActionResult =
		suspendTransaction(db) {
			val ability = checkArticleWritePermission(articleId, userId, userPermission)
			if (ability != null) return@suspendTransaction ability
			if (articleTable.deleteWhere { articleTable.id eq articleId } == 1) return@suspendTransaction ActionResult.OK
			else throw Exception() // ?
		}
	
	suspend fun updateArticle(
		articleId: Int, userId: Int, userPermission: Permission, newTitle: String? = null, newContent: String? = null
	): ActionResult {
		if (newTitle == null && newContent == null) return ActionResult.OK // yay it can be no-op
		return suspendTransaction(db) {
			val ability = checkArticleWritePermission(articleId, userId, userPermission)
			if (ability != null) return@suspendTransaction ability
			
			val updateResult = articleTable.update({ articleTable.id eq articleId }) {
				if (newTitle != null) it[title] = newTitle
				if (newContent != null) it[content] = newContent
			}
			
			if (updateResult == 1) return@suspendTransaction ActionResult.OK
			throw Exception("WHAT???")
		}
	}
	
	
	suspend fun authorize(id: Int?, pw: String?): Permission {
		if (id == null || pw == null)
			return Permission.NONE
		return suspendTransaction(db) {
			val user = authorTable
				.select(authorTable.sudoer, authorTable.pw)
				.where { authorTable.id eq id }.limit(1)
				.singleOrNull()
				?: return@suspendTransaction Permission.NONE
			if (user[authorTable.pw] != pw) return@suspendTransaction Permission.WRONG_LOGIN
			if (user[authorTable.sudoer]) return@suspendTransaction Permission.SUPER
			return@suspendTransaction Permission.NORMAL
		}
	}
	
	// On non-null result, something is weird like unauthorized or something.
	private suspend fun checkArticleWritePermission(articleId: Int, userId: Int, userPermission: Permission) =
		suspendTransaction(db) {
			val targetArticle = articleTable.select(articleTable.author)
				.where { (articleTable.id eq articleId) and not(articleTable.deleted) }.limit(1).singleOrNull()
				?: return@suspendTransaction ActionResult.NOT_EXIST
			
			val targetArticleAuthor = targetArticle[articleTable.author]
			
			if (!((userPermission == Permission.NORMAL && targetArticleAuthor == userId) || userPermission == Permission.SUPER)) return@suspendTransaction ActionResult.LACK_OF_PERMISSION
			return@suspendTransaction null
		}
}

enum class ActionResult {
	OK, LACK_OF_PERMISSION, NOT_EXIST
}

enum class Permission {
	SUPER, NORMAL, WRONG_LOGIN, NONE
}