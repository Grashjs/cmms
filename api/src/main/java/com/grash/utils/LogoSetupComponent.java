package com.grash.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
public class LogoSetupComponent implements ApplicationRunner {

    @Value("${white-labeling.logo-paths:}")
    private String customLogoPaths;

    // Path inside the container where the app can read static files at runtime
    private static final String STATIC_LOGO_PATH = "/app/static/images/custom-logo.png";
    private static final String STATIC_LOGO_WHITE_PATH = "/app/static/images/custom-logo-white.png";
    private static final String LOGO_PARENT = "/app/static/images/";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (customLogoPaths != null && !customLogoPaths.isEmpty()) {
            copyLogoToStaticResources();
        }
    }

    void copyLogoToStaticResources() {
        copyLogoToStaticResources(LOGO_PARENT, STATIC_LOGO_PATH, STATIC_LOGO_WHITE_PATH);
    }

    void copyLogoToStaticResources(String logoParent, String staticLogoPath, String staticLogoWhitePath) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(customLogoPaths);
            // Source path inside container (must be accessible, mounted from host or included in container)
            Path source = Paths.get(logoParent + jsonNode.get("dark").asText());
            Path target = Paths.get(staticLogoPath);

            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

            //white
            Path sourceWhite = Paths.get(logoParent + jsonNode.get("white").asText());
            Path targetWhite = Paths.get(staticLogoWhitePath);

            Files.createDirectories(targetWhite.getParent());
            Files.copy(sourceWhite, targetWhite, StandardCopyOption.REPLACE_EXISTING);


            System.out.println("Custom logo copied to static resources at " + target);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to copy custom logo: " + e.getMessage());
        }
    }
}
