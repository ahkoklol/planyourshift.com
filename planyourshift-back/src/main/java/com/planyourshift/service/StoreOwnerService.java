package com.planyourshift.service;

import com.planyourshift.controller.StoreOwnerController;
import com.planyourshift.entity.StoreOwner;
import com.planyourshift.repository.StoreOwnerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StoreOwnerService {

    private static final Logger log = LoggerFactory.getLogger(StoreOwnerService.class);

    @Autowired
    private StoreOwnerRepository storeOwnerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Fetch a StoreOwner by id
     * @param storeOwnerId the id of a StoreOwner
     * @return a StoreOwner if found, null otherwise
     */
    public Optional<StoreOwner> getStoreOwner(String storeOwnerId){
        return storeOwnerRepository.findById(storeOwnerId);
    }

    /**
     * Creates a StoreOwner
     * @param storeOwner a StoreOwner object
     */
    public void register(StoreOwner storeOwner){
        Optional<StoreOwner> optionalStoreOwner = getStoreOwnerByEmail(storeOwner.getEmail());
        if(optionalStoreOwner.isEmpty()){
            log.info("Store owner with email {} already exists", storeOwner.getEmail());
            throw new IllegalArgumentException("Store owner with email " + storeOwner.getEmail() + " already exists");
        }
        storeOwner.setStoreOwnerID(UUID.randomUUID().toString());
        validatePasswordStrength(storeOwner.getPassword());
        storeOwner.setPassword(passwordEncoder.encode(storeOwner.getPassword()));
        storeOwnerRepository.save(storeOwner);
        log.info("Store owner with email {} registered", storeOwner.getEmail());
    }

    /**
     * Helper to fetch a StoreOwner by email
     * @param email an email
     * @return a StoreOwner if found, null otherwise
     */
    private Optional<StoreOwner> getStoreOwnerByEmail(String email){
        return storeOwnerRepository.findStoreOwnerByEmail(email);
    }

    /**
     * Validates that the password meets the minimum strength requirements.
     * @param password the plain text password
     * @throws IllegalArgumentException if the password is weak
     */
    private void validatePasswordStrength(String password) {
        // Regex for password strength:
        // (?=.*[0-9]): At least one digit
        // (?=.*[a-z]): At least one lowercase letter
        // (?=.*[A-Z]): At least one uppercase letter
        // (?=.*[!@#$%^&+=]): At least one special character
        // .{8,}: At least 8 characters long
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least " + 8 + " characters long.");
        }
        if (!Pattern.compile(".*[A-Z].*").matcher(password).matches()) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter.");
        }
        if (!Pattern.compile(".*[a-z].*").matcher(password).matches()) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter.");
        }
        if (!Pattern.compile(".*[0-9].*").matcher(password).matches()) {
            throw new IllegalArgumentException("Password must contain at least one number.");
        }
        if (!Pattern.compile(".*[!@#$%^&+=].*").matcher(password).matches()) {
            throw new IllegalArgumentException("Password must contain at least one special character: !@#$%^&+=.");
        }
    }

    /**
     * Authenticates a StoreOwner
     * @param storeOwner a StoreOwner
     * @return true if login was successfull, false otherwise
     */
    public boolean login(StoreOwner storeOwner){
        Optional<StoreOwner> optionalStoreOwner = getStoreOwnerByEmail(storeOwner.getEmail());
        if(optionalStoreOwner.isEmpty()){
            log.info("Store owner with email {} does not exists", storeOwner.getEmail());
            throw new IllegalArgumentException("Store owner with email " + storeOwner.getEmail() + " does not exists");
        }
        String storedHash = optionalStoreOwner.get().getPassword();
        if (passwordEncoder.matches(storeOwner.getPassword(), storedHash)) {
            log.info("Login successful for email {}", storeOwner.getEmail());
            return true;
        }
        log.info("Login failed due to bad credentials for email {}", storeOwner.getEmail());
        return false;
    }
}
