package org.example;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "article")
public class ArticleEntity {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Getter
    @Setter
    @Column(length = DTOTypes.TITLE_MAX_LENGTH)
    private String title;

    @Getter
    @Setter
    @Lob
    private String content;

    @Setter
    @Getter
    private Integer author;

    @Setter
    @Getter
    private boolean deleted = false;

}
