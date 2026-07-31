package com.grash.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogoSetupComponentTest {

    @TempDir
    Path tempDir;

    @Mock
    private ApplicationArguments args;

    private LogoSetupComponent newComponent(String customLogoPaths) {
        LogoSetupComponent component = new LogoSetupComponent();
        ReflectionTestUtils.setField(component, "customLogoPaths", customLogoPaths);
        return component;
    }

    @Test
    void run_withNullConfig_doesNothing() throws Exception {
        LogoSetupComponent spy = spy(newComponent(null));

        spy.run(args);

        verify(spy, never()).copyLogoToStaticResources();
    }

    @Test
    void run_withEmptyConfig_doesNothing() throws Exception {
        LogoSetupComponent spy = spy(newComponent(""));

        spy.run(args);

        verify(spy, never()).copyLogoToStaticResources();
    }

    @Test
    void run_withNonEmptyConfig_copiesLogos() throws Exception {
        LogoSetupComponent spy = spy(newComponent("{}"));
        doNothing().when(spy).copyLogoToStaticResources();

        spy.run(args);

        verify(spy).copyLogoToStaticResources();
    }

    @Test
    void copy_copiesDarkAndWhiteLogosToTargets() throws Exception {
        Path srcDir = tempDir.resolve("src");
        Files.createDirectories(srcDir);
        byte[] darkContent = {1, 2, 3, 4};
        byte[] whiteContent = {5, 6, 7, 8};
        Files.write(srcDir.resolve("dark-logo.png"), darkContent);
        Files.write(srcDir.resolve("white-logo.png"), whiteContent);

        LogoSetupComponent component = newComponent(
                "{\"dark\":\"dark-logo.png\",\"white\":\"white-logo.png\"}");

        Path targetDir = tempDir.resolve("target");
        component.copyLogoToStaticResources(
                srcDir.toString() + File.separator,
                targetDir.resolve("custom-logo.png").toString(),
                targetDir.resolve("custom-logo-white.png").toString());

        assertArrayEquals(darkContent, Files.readAllBytes(targetDir.resolve("custom-logo.png")));
        assertArrayEquals(whiteContent, Files.readAllBytes(targetDir.resolve("custom-logo-white.png")));
    }

    @Test
    void copy_replacesExistingTargets() throws Exception {
        Path srcDir = tempDir.resolve("src");
        Files.createDirectories(srcDir);
        Files.write(srcDir.resolve("dark-logo.png"), new byte[]{9});
        Files.write(srcDir.resolve("white-logo.png"), new byte[]{8});

        Path targetDir = tempDir.resolve("target");
        Files.createDirectories(targetDir);
        Files.write(targetDir.resolve("custom-logo.png"), new byte[]{0});
        Files.write(targetDir.resolve("custom-logo-white.png"), new byte[]{0});

        LogoSetupComponent component = newComponent(
                "{\"dark\":\"dark-logo.png\",\"white\":\"white-logo.png\"}");
        component.copyLogoToStaticResources(
                srcDir.toString() + File.separator,
                targetDir.resolve("custom-logo.png").toString(),
                targetDir.resolve("custom-logo-white.png").toString());

        assertArrayEquals(new byte[]{9}, Files.readAllBytes(targetDir.resolve("custom-logo.png")));
        assertArrayEquals(new byte[]{8}, Files.readAllBytes(targetDir.resolve("custom-logo-white.png")));
    }

    @Test
    void copy_withInvalidJson_doesNotThrow() {
        LogoSetupComponent component = newComponent("not valid json");

        assertDoesNotThrow(() -> component.copyLogoToStaticResources(
                tempDir.toString() + File.separator,
                tempDir.resolve("custom-logo.png").toString(),
                tempDir.resolve("custom-logo-white.png").toString()));
    }

    @Test
    void copy_withMissingSource_doesNotThrow() throws Exception {
        LogoSetupComponent component = newComponent(
                "{\"dark\":\"missing-dark.png\",\"white\":\"missing-white.png\"}");

        Path targetDir = tempDir.resolve("out");
        assertDoesNotThrow(() -> component.copyLogoToStaticResources(
                tempDir.toString() + File.separator,
                targetDir.resolve("custom-logo.png").toString(),
                targetDir.resolve("custom-logo-white.png").toString()));
    }
}
