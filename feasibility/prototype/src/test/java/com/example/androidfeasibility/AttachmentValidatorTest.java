package com.example.androidfeasibility;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public final class AttachmentValidatorTest {
    public static void main(String[] args) throws Exception {
        File directory = new File(System.getProperty("java.io.tmpdir"), "mochi-attachment-validator-" + System.nanoTime());
        check(directory.mkdirs(), "test directory must be created");
        File image = new File(directory, "photo.png");
        try (FileOutputStream output = new FileOutputStream(image)) {
            output.write("synthetic image bytes".getBytes(StandardCharsets.UTF_8));
        }
        AttachmentValidator.Result valid = AttachmentValidator.validate(image, "image/png", 1024);
        check(valid.valid, "supported readable image must validate");
        check(valid.detectedMimeType.equals("image/png"), "declared image MIME must be retained");

        File empty = new File(directory, "empty.txt");
        check(empty.createNewFile(), "empty fixture must be created");
        check(!AttachmentValidator.validate(empty, "text/plain", 1024).valid,
                "zero-byte files must fail");

        File unsupported = new File(directory, "script.exe");
        try (FileOutputStream output = new FileOutputStream(unsupported)) { output.write(1); }
        check(!AttachmentValidator.validate(unsupported, "application/x-msdownload", 1024).valid,
                "unsupported file categories must fail");

        check(!AttachmentValidator.validate(image, "image/png", 2).valid,
                "oversized files must fail");
        check(!AttachmentValidator.validate(new File(directory, "missing.png"), "image/png", 1024).valid,
                "missing sources must fail");
        delete(directory, image, empty, unsupported);
        System.out.println("ATTACHMENT VALIDATOR TESTS PASSED");
    }

    private static void delete(File directory, File... files) {
        for (File file : files) if (file.exists()) file.delete();
        directory.delete();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
