package com.backendintranet.service;

import com.backendintranet.exception.BadRequestException;
import com.backendintranet.service.impl.AbsenceSupportStorage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class AbsenceSupportStorageTest {

    @TempDir
    Path tempDir;

    private AbsenceSupportStorage storage;

    @BeforeEach
    void setUp() {

        storage =
                new AbsenceSupportStorage();

        ReflectionTestUtils.setField(
                storage,
                "supportDir",
                tempDir.toString());
    }

    @Test
    void storesValidPngWithRandomInternalName()
            throws Exception {

        byte[] png =
                new byte[] {
                    (byte) 0x89,
                    0x50,
                    0x4E,
                    0x47,
                    0x0D,
                    0x0A,
                    0x1A,
                    0x0A,
                    0x00
                };

        var file =
                new MockMultipartFile(
                        "file",
                        "soporte.png",
                        "image/png",
                        png);

        var stored =
                storage.store(file);

        assertThat(stored)
                .isNotNull();

        assertThat(stored.storedName())
                .endsWith(".png")
                .doesNotContain("soporte");

        assertThat(stored.contentType())
                .isEqualTo("image/png");

        assertThat(
                        Files.exists(
                                tempDir.resolve(
                                        stored.storedName())))
                .isTrue();
    }

    @Test
    void rejectsFilesLargerThanFiveMegabytes() {

        byte[] oversized =
                new byte[
                        (5 * 1024 * 1024)
                                + 1];

        oversized[0] =
                (byte) 0x89;
        oversized[1] =
                0x50;
        oversized[2] =
                0x4E;
        oversized[3] =
                0x47;
        oversized[4] =
                0x0D;
        oversized[5] =
                0x0A;
        oversized[6] =
                0x1A;
        oversized[7] =
                0x0A;

        var file =
                new MockMultipartFile(
                        "file",
                        "grande.png",
                        "image/png",
                        oversized);

        assertThatThrownBy(
                        () ->
                                storage.store(
                                        file))
                .isInstanceOf(
                        BadRequestException.class)
                .hasMessageContaining(
                        "5 MB");
    }

    @Test
    void rejectsFakeImageRenamedAsJpg() {

        var file =
                new MockMultipartFile(
                        "file",
                        "archivo.jpg",
                        "image/jpeg",
                        "esto no es una imagen"
                                .getBytes());

        assertThatThrownBy(
                        () ->
                                storage.store(
                                        file))
                .isInstanceOf(
                        BadRequestException.class)
                .hasMessageContaining(
                        "imagen JPG o PNG válida");
    }

    @Test
    void rejectsExtensionThatDoesNotMatchRealContent() {

        byte[] png =
                new byte[] {
                    (byte) 0x89,
                    0x50,
                    0x4E,
                    0x47,
                    0x0D,
                    0x0A,
                    0x1A,
                    0x0A
                };

        var file =
                new MockMultipartFile(
                        "file",
                        "falso.jpg",
                        "image/jpeg",
                        png);

        assertThatThrownBy(
                        () ->
                                storage.store(
                                        file))
                .isInstanceOf(
                        BadRequestException.class)
                .hasMessageContaining(
                        "no coincide");
    }

    @Test
    void emptyOptionalSupportReturnsNull() {

        var file =
                new MockMultipartFile(
                        "file",
                        "",
                        "image/png",
                        new byte[0]);

        assertThat(
                        storage.store(
                                file))
                .isNull();
    }
}
