package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/")
@SpringBootApplication
public class MainApplication {
    @Autowired
    public MainApplication(Persistence db) {
        db.preloadExampleData();
    }

    /**
     * Root endpoint saying hi for keepalive checks
     */
    @GetMapping
    public String root() {
        return "Oh hi";
    }

    /**
     * Super secure login endpoint that definitely does not give you up
     * But it might let you down
     */
    @GetMapping("/login")
    public ResponseEntity login() {

        //redirect
        var url = "https://youtube.com/watch?v=dQw4w9WgXcQ";
        return ResponseEntity.status(302).header("Location", url).build();
    }
}
