package com.fluxlink.app.service;

import org.springframework.stereotype.Service;

@Service
public class Base62Encoder {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length();

    public String encode(long id) {
        if (id == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }

        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            sb.append(ALPHABET.charAt((int) (id % BASE)));
            id /= BASE;
        }

        return sb.reverse().toString();
    }

    public long decode(String shortCode) {
        long id = 0;
        for (int i = 0; i < shortCode.length(); i++) {
            id = id * BASE + ALPHABET.indexOf(shortCode.charAt(i));
        }
        return id;
    }
}
