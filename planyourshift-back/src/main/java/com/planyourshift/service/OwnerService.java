package com.planyourshift.service;

import com.planyourshift.entity.Owner;
import com.planyourshift.repository.OwnerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class OwnerService {

    private static final Logger log = LoggerFactory.getLogger(OwnerService.class);

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Fetch an Owner by id
     * @param ownerId the id of an Owner
     * @return an Owner if found, null otherwise
     */
    public Optional<Owner> getOwner(String ownerId){
        return ownerRepository.findById(ownerId);
    }

    /**
     * Creates an Owner
     * @param owner an Owner object
     * @return the created Owner object
     */
    public Owner register(Owner owner){
        Optional<Owner> optionalOwner = getOwnerByEmail(owner.getEmail());
        if(optionalOwner.isPresent()){
            log.error("Owner with email {} already exists", owner.getEmail());
            throw new IllegalArgumentException("Store owner with email " + owner.getEmail() + " already exists");
        }
        owner.setOwnerId(UUID.randomUUID().toString());
        validatePasswordStrength(owner.getPassword());
        owner.setPassword(passwordEncoder.encode(owner.getPassword()));
        return ownerRepository.save(owner);
    }

    /**
     * Helper to fetch an Owner by email
     * @param email an email
     * @return an Owner if found, null otherwise
     */
    private Optional<Owner> getOwnerByEmail(String email){
        return ownerRepository.findOwnerByEmail(email);
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
     * Authenticates an Owner
     * @param owner an Owner
     * @return true if login was successfull, false otherwise
     */
    public boolean login(Owner owner){
        Optional<Owner> optionalOwner = getOwnerByEmail(owner.getEmail());
        if(optionalOwner.isEmpty()){
            log.error("Store owner with email {} does not exists", owner.getEmail());
            throw new IllegalArgumentException("Store owner with email " + owner.getEmail() + " does not exists");
        }
        String storedHash = optionalOwner.get().getPassword();
        if (passwordEncoder.matches(owner.getPassword(), storedHash)) {
            log.error("Login successful for email {}", owner.getEmail());
            return true;
        }
        log.info("Login failed due to bad credentials for email {}", owner.getEmail());
        return false;
    }
}
