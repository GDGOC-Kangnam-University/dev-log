package org.example.record;

import com.fasterxml.jackson.annotation.JsonProperty;


public record AuthorRecord(int id,
                           String pw,
                           @JsonProperty("sudoer")
                           boolean isSudoer
) {
}
