package com.finance_backned.finance.ServiceImp;


import com.finance_backned.finance.ExceptionHandler.BadRequestException;
import com.finance_backned.finance.ExceptionHandler.ResourceNotFoundException;
import com.finance_backned.finance.Model.User;
import com.finance_backned.finance.Repository.UserRepository;
import com.finance_backned.finance.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImp implements UserService {

    private final UserRepository userRepository;

    @Override
    public User createUser(User user) {

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists");
        }

        user.setActive(true);

        return userRepository.save(user);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    public User updateUser(String id, User updatedUser) {

        User existing = getUserById(id);

        if (!existing.getEmail().equals(updatedUser.getEmail())) {
            if (userRepository.findByEmail(updatedUser.getEmail()).isPresent()) {
                throw new BadRequestException("Email already exists");
            }
        }

        existing.setName(updatedUser.getName());
        existing.setEmail(updatedUser.getEmail());
        existing.setPassword(updatedUser.getPassword());
        existing.setRole(updatedUser.getRole());
        existing.setActive(updatedUser.isActive());

        return userRepository.save(existing);
    }

    @Override
    public void deleteUser(String id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }
}