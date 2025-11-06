package org.example;

import jakarta.persistence.*;

@Entity
@Table(name = "author")
public class AuthorEntity {

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
