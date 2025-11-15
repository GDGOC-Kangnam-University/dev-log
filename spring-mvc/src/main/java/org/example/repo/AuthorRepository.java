package org.example.repo;

import org.example.entity.AuthorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<AuthorEntity, Integer> {
    // save(), findById(), findAll(), deleteById(): auto-implemented
}