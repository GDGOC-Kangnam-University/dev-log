package org.example

import org.jetbrains.exposed.v1.core.Table

object ArticleTable : Table("article") {
	val id = integer("id").autoIncrement()
	val title = varchar("title", TITLE_MAX_LENGTH)
	val content = text("content")
	val author = integer("author")
	val deleted = bool("deleted").default(false)
}

object AuthorTable : Table("author") {
	val id = integer("id").autoIncrement()
	val pw = varchar("pw", PASSWORD_MAX_LENGTH)
	val sudoer = bool("sudoer").default(false)
}