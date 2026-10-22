package com.example.keepalive;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

public final class WhitelistConfig {
    private static final String CONFIG_PATH = "/data/adb/modules/keepalive_whitelist/whitelist.txt";

    public static Set<String> readWhitelist() {
        Set<String> set = new HashSet<>();
        File f = new File(CONFIG_PATH);
        if (!f.exists()) return set;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                set.add(line);
            }
        } catch (Throwable ignored) {}
        return set;
    }
}