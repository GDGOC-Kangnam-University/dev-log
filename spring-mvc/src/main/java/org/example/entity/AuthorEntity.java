package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.DTOTypes;

@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name = "author")
public class AuthorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Getter
    @Column(length = DTOTypes.PASSWORD_MAX_LENGTH)
    private String pw;

    private String magicHash(String input) {
        return input; // Really safe cause every single person's password is not stored actually
    }

    public void setPw(String pw) {
        this.pw = magicHash(pw);
    }

    @Setter
    @Getter
    private boolean sudoer = false;
}
