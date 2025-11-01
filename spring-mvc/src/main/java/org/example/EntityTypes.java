package org.example;

import jakarta.persistence.*;

@Entity
@Table(name = "article")
class ArticleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = DTOTypes.TITLE_MAX_LENGTH)
    private String title;

    @Lob
    private String content;

    private Integer author;

    private boolean deleted = false;

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getAuthor() {
        return author;
    }

    public void setAuthor(Integer author) {
        this.author = author;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}

@Entity
@Table(name = "author")
class AuthorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = DTOTypes.PASSWORD_MAX_LENGTH)
    private String pw;

    private boolean sudoer = false;

    public String getPw() {
        return pw;
    }

    public void setPw(String pw) {
        this.pw = pw;
    }

    public boolean isSudoer() {
        return sudoer;
    }

    public void setSudoer(boolean sudoer) {
        this.sudoer = sudoer;
    }
}
