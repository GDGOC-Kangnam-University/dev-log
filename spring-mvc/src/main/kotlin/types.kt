package org.example

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.Table

const val TITLE_MAX_LENGTH = 50
const val PASSWORD_MAX_LENGTH = 150


@Serializable
// null means it's not published. Client only send nulls.
data class ArticlePartial(
	val title: String? = null,
	val content: String? = null
)

@Serializable
// null means it's not published. Client only send nulls.
data class Article(
	val id: Int? = null,
	val title: String,
	val content: String,
	val author: Int? = null,
	val deleted: Boolean = false
) {
	
	companion object {
		object DatabaseTable : Table("article") {
			val id = integer("id").autoIncrement()
			val title = varchar("title", TITLE_MAX_LENGTH)
			val content = text("content")
			val author = integer("author")
			val deleted = bool("deleted").default(false)
		}
		
	}
}

@Serializable
//Plaintext pw for test
data class Author(val id: Int, val pw: String, val sudoer: Boolean = false) {
	companion object {
		object DatabaseTable : Table("author") {
			val id = integer("id").autoIncrement()
			val pw = varchar("pw", PASSWORD_MAX_LENGTH)
			val sudoer = bool("sudoer").default(false)
		}
	}
}



