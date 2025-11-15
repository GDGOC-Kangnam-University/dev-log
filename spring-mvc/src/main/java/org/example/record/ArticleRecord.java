package org.example.record;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ArticleRecord(
        Integer id,
        String title,
        String content,
        Integer author,
        @JsonProperty("deleted")
        boolean isDeleted
) {
}
