package org.example

import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter

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