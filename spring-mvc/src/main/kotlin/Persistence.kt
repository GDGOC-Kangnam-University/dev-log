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
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Repository

@Primary
@Repository
class Persistence {
	val db = R2dbcDatabase.connect("r2dbc:h2:mem:///test;DB_CLOSE_DELAY=-1")
	
	val articleTable = ArticleTable
	val authorTable = AuthorTable
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
			}[articleTable.id]
		}
	
	suspend fun readSingleArticle(articleId: Int): Article = suspendTransaction(db) {
		val article =
			articleTable.selectAll().where { not(articleTable.deleted) }.andWhere { articleTable.id eq articleId }
				.run { singleOrNull() ?: throw NotFoundError() }
		Article(
			id = article[articleTable.id],
			title = article[articleTable.title],
			content = article[articleTable.content],
			author = article[articleTable.author],
			deleted = article[articleTable.deleted]
		)
		
	}
	
	// If articleId is null, lists up all article.
	// If limit is non-null and articleId isn't, listing will be capped.
	suspend fun readArticleList(limit: Int? = 5) = suspendTransaction(db) {
		var articles = articleTable.selectAll().where { not(articleTable.deleted) }
		
		if (limit != null)
			articles = articles.limit(limit)
		
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
			checkArticleWritePermission(articleId, userId, userPermission)
			if (articleTable.deleteWhere { articleTable.id eq articleId } == 1) return@suspendTransaction ActionResult.OK
			else throw DatabaseOperationError()
		}
	
	suspend fun updateArticle(
		articleId: Int, userId: Int, userPermission: Permission, newTitle: String? = null, newContent: String? = null
	): ActionResult {
		if (newTitle == null && newContent == null) return ActionResult.OK // yay it can be no-op
		return suspendTransaction(db) {
			checkArticleWritePermission(articleId, userId, userPermission)
			
			val updateResult = articleTable.update({ articleTable.id eq articleId }) {
				if (newTitle != null) it[title] = newTitle
				if (newContent != null) it[content] = newContent
			}
			
			if (updateResult == 1) return@suspendTransaction ActionResult.OK
			throw DatabaseOperationError()
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
			if (user[authorTable.pw] != pw) throw ForbiddenError()
			if (user[authorTable.sudoer]) return@suspendTransaction Permission.SUPER
			return@suspendTransaction Permission.NORMAL
		}
	}
	
	// On non-null result, something is weird like unauthorized or something.
	private suspend fun checkArticleWritePermission(articleId: Int, userId: Int, userPermission: Permission) =
		suspendTransaction(db) {
			val targetArticle = articleTable.select(articleTable.author)
				.where { (articleTable.id eq articleId) and not(articleTable.deleted) }.limit(1).singleOrNull()
				?: throw NotFoundError()
			
			val targetArticleAuthor = targetArticle[articleTable.author]
			
			if (!((userPermission == Permission.NORMAL && targetArticleAuthor == userId) || userPermission == Permission.SUPER)) throw ForbiddenError()
		}
}

enum class ActionResult {
	OK, LACK_OF_PERMISSION, NOT_EXIST
}

enum class Permission {
	SUPER, NORMAL, NONE
}