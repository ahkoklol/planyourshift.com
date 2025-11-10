package com.planyourshift.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    /**
     * Defines the PasswordEncoder bean to be used for hashing.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCryptPasswordEncoder handles salting automatically.
        // The strength (work factor) is set to 10 by default, which is usually sufficient.
        return new BCryptPasswordEncoder();
    }
}
