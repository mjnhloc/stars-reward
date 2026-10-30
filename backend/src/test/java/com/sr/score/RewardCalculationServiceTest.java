package com.sr.score;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class RewardCalculationServiceTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

        System.out.println("password: " + encoder.encode("Thuylinh2903."));
    }
}
