package com.petmate.backend.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Charge le fichier {@code .env} du répertoire de travail comme source de
 * propriétés de plus basse priorité. Les variables réellement exportées dans
 * l'environnement (ex : Docker) restent prioritaires.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE_NAME = "dotenv";
    private static final String DOTENV_FILE = ".env";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String userDir = environment.getProperty("user.dir", ".");
        Path dotenvPath = Path.of(userDir).resolve(DOTENV_FILE);

        if (!Files.isRegularFile(dotenvPath)) {
            return;
        }

        try {
            String content = Files.readString(dotenvPath, StandardCharsets.UTF_8);
            Map<String, Object> variables = new LinkedHashMap<>(parse(content));
            environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, variables));
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire le fichier .env : " + dotenvPath, e);
        }
    }

    static Map<String, String> parse(String content) {
        Map<String, String> variables = new LinkedHashMap<>();
        int i = 0;
        int n = content.length();

        while (i < n) {
            char c = content.charAt(i);

            if (c == '\r' || c == '\n' || Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '#') {
                while (i < n && content.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }

            int eq = content.indexOf('=', i);
            if (eq < 0) {
                break;
            }
            String key = content.substring(i, eq).trim();
            if (key.startsWith("export ")) {
                key = key.substring("export ".length()).trim();
            }
            i = eq + 1;

            while (i < n && Character.isWhitespace(content.charAt(i))) {
                i++;
            }

            String value;
            if (i < n && content.charAt(i) == '"') {
                i++;
                StringBuilder sb = new StringBuilder();
                while (i < n) {
                    char vc = content.charAt(i);
                    if (vc == '\\' && i + 1 < n) {
                        char next = content.charAt(i + 1);
                        switch (next) {
                            case 'n' -> sb.append('\n');
                            case 't' -> sb.append('\t');
                            case 'r' -> sb.append('\r');
                            case '"' -> sb.append('"');
                            case '\\' -> sb.append('\\');
                            default -> sb.append(next);
                        }
                        i += 2;
                    } else if (vc == '"') {
                        i++;
                        break;
                    } else {
                        sb.append(vc);
                        i++;
                    }
                }
                value = sb.toString();
            } else if (i < n && content.charAt(i) == '\'') {
                i++;
                int end = content.indexOf('\'', i);
                if (end < 0) {
                    value = content.substring(i);
                    i = n;
                } else {
                    value = content.substring(i, end);
                    i = end + 1;
                }
            } else {
                int lineEnd = content.indexOf('\n', i);
                String raw = lineEnd < 0 ? content.substring(i) : content.substring(i, lineEnd);
                int hash = raw.indexOf('#');
                if (hash > 0 && Character.isWhitespace(raw.charAt(hash - 1))) {
                    raw = raw.substring(0, hash);
                }
                value = raw.trim();
                i = lineEnd < 0 ? n : lineEnd + 1;
            }

            variables.put(key, value);
        }
        return variables;
    }
}