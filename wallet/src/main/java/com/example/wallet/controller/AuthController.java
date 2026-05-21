
package com.example.wallet.controller;

import com.example.wallet.dto.*;
import com.example.wallet.model.User;
import com.example.wallet.service.UserService;
import com.example.wallet.config.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        User user = userService.registerUser(request);
        String token = jwtUtils.generateToken(user);
        return ResponseEntity.ok(new JwtResponse(token, user.getUsername(), user.getFullName(), user.getRole(), user.getUpiId(), user.getPhoneNumber()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.authenticate(request.getUsername(), request.getPassword());
            String token = jwtUtils.generateToken(user);
            return ResponseEntity.ok(new JwtResponse(token, user.getUsername(), user.getFullName(), user.getRole(), user.getUpiId(), user.getPhoneNumber()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }
}
