package org.example.posinventorysystem.app;

import javafx.application.Application;
import org.example.posinventorysystem.service.PasswordHasher;

public class Launcher {
    public static void main(String[] args) {
       // Application.launch(MainApplication.class, args);
        System.out.println(PasswordHasher.hash("admin"));
    }
}
