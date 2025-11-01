package org.example;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DTOTypes {

    public static final int TITLE_MAX_LENGTH = 50;
    public static final int PASSWORD_MAX_LENGTH = 150;

    public record ArticlePartial(String title, String content) {
    }

    public record Article(
            Integer id,
            String title,
            String content,
            Integer author,
            @JsonProperty("deleted")
            boolean isDeleted
    ) {
    }

    public record Author(int id, String pw, @JsonProperty("sudoer") boolean isSudoer) {
    }
}
