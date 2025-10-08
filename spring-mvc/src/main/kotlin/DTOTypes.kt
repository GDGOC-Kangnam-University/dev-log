package org.example

import kotlinx.serialization.Serializable

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
)

@Serializable
//Plaintext pw for test
data class Author(val id: Int, val pw: String, val sudoer: Boolean = false)
