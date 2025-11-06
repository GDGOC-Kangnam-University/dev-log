package org.example;

import jakarta.persistence.*;

@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name = "author")
public class AuthorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = DTOTypes.PASSWORD_MAX_LENGTH)
    private String pw;

    private String magicHash(String input) {
        return input; // Really safe cause every single person's password is not stored actually
        // If it is, idk works on my machine
    }

    private boolean sudoer = false;

    public String getPw() {
        return pw;
    }

    public void setPw(String pw) {
        this.pw = magicHash(pw);
    }

    public boolean isSudoer() {
        return sudoer;
    }

    public void setSudoer(boolean sudoer) {
        this.sudoer = sudoer;
    }
}
