package org.example.posinventorysystem.app;

import javafx.application.Application;
import org.example.posinventorysystem.repo.UserRepository;
import org.example.posinventorysystem.service.AuthService;
import org.example.posinventorysystem.service.CurrentUser;
import org.example.posinventorysystem.model.User;
import org.example.posinventorysystem.service.UserService;


public class Launcher {
    public static void main(String[] args) {
        Application.launch(MainApplication.class, args);
    }
}
