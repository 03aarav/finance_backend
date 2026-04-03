package com.finance_backned.finance.Controller;

import com.finance_backned.finance.DTO.LoginCred;
import com.finance_backned.finance.ExceptionHandler.BadRequestException;
import com.finance_backned.finance.Model.User;
import com.finance_backned.finance.Repository.UserRepository;
import com.finance_backned.finance.Util.JwtUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/register")
    public String register(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists");
        }
        userRepository.save(user);
        return "User created";
    }

    @PostMapping("/login")
    public String login(@RequestBody LoginCred request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        return JwtUtil.generateToken(user.getEmail(), user.getRole().name());
    }
}
