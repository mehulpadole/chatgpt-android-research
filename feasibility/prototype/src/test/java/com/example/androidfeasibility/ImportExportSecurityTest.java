package com.example.androidfeasibility;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ImportExportSecurityTest {
    public static void main(String[] args) throws Exception {
        testJsonAndTextRoundTrip();
        testZipRoundTripAndUnsafePaths();
        testDuplicateIdsAndUnsupportedEntries();
        testPhase5MigrationAndSecretOmission();
        testEndpointAndRedirectValidation();
        System.out.println("IMPORT EXPORT SECURITY TESTS PASSED");
    }

    private static void testJsonAndTextRoundTrip() throws Exception {
        Conversation source = sample();
        Conversation json = ImportService.importJson(ExportService.toJson(source));
        check(json.id.equals(source.id) && json.messages.size() == 2,
                "JSON export/import must preserve conversation and messages");
        check(json.messages.get(0).contentParts.size() == 1,
                "JSON round trip must preserve text content parts");
        Conversation text = ImportService.importText(ExportService.toText(source));
        check(text.title.equals(source.title) && text.messages.size() == 2,
                "TXT export/import must preserve basic conversation data");
    }

    private static void testZipRoundTripAndUnsafePaths() throws Exception {
        byte[] archive = ExportService.toZip(sample());
        Conversation restored = ImportService.importZip(archive);
        check(restored.id.equals("conversation-export") && restored.messages.size() == 2,
                "ZIP export/import must preserve conversation data");
        boolean rejected = false;
        try { ImportService.importZip(zip("../escape.txt", "bad")); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "ZIP path traversal must be rejected");
    }

    private static void testDuplicateIdsAndUnsupportedEntries() throws Exception {
        String json = ExportService.toJson(sample());
        Set<String> conversations = new HashSet<>();
        Set<String> messages = new HashSet<>();
        ImportService.importJson(json, conversations, messages);
        boolean rejected = false;
        try { ImportService.importJson(json, conversations, messages); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "duplicate conversation/message IDs must be rejected");
        rejected = false;
        try { ImportService.importZip(zip("attachments/payload.exe", "bad")); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "unsupported ZIP file types must be rejected");
    }

    private static void testPhase5MigrationAndSecretOmission() throws Exception {
        String phase5 = "{\"id\":\"legacy\",\"title\":\"Legacy\",\"updatedAt\":1,"
                + "\"messages\":[],\"attachments\":[]}";
        String migrated = MigrationService.migrateJson(phase5);
        check(migrated.contains("\"schemaVersion\":" + SchemaVersion.CURRENT),
                "Phase 5 JSON must migrate to current schema");
        check(ImportService.importJson(migrated).id.equals("legacy"),
                "migrated Phase 5 JSON must remain importable");
        InMemoryCredentialStore credentials = new InMemoryCredentialStore();
        credentials.put("openrouter", "sk-super-secret-value");
        String exported = ExportService.toJson(sample(), credentials);
        check(!exported.contains("sk-super-secret-value") && !exported.contains("openrouter"),
                "provider credentials must never enter exports");
    }

    private static void testEndpointAndRedirectValidation() throws Exception {
        EndpointValidator.validate(new URL("https://example.test/"));
        EndpointValidator.validate(new URL("http://127.0.0.1:8787/"));
        boolean rejected = false;
        try { EndpointValidator.validate(new URL("http://example.test/")); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "remote cleartext endpoint must be rejected");
        rejected = false;
        try { EndpointValidator.validateRedirect(new URL("https://example.test/"), new URL("http://example.test/")); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "HTTPS to HTTP redirect must be rejected");
        EndpointValidator.validateRedirect(new URL("https://example.test/"), new URL("https://example.test/api"));
    }

    private static Conversation sample() {
        Conversation conversation = new Conversation("conversation-export", "Exported", 1L);
        Message user = new Message("message-user", conversation.id, "turn-1", Role.USER,
                "hello\nworld", MessageStatus.COMPLETED, "local", "model", 1L);
        user.addContentPart(new TextPart("hello\nworld"));
        Message assistant = new Message("message-assistant", conversation.id, "turn-1", Role.ASSISTANT,
                "reply", MessageStatus.COMPLETED, "local", "model", 2L);
        conversation.add(user);
        conversation.add(assistant);
        return conversation;
    }

    private static byte[] zip(String name, String content) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(bytes);
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zip.closeEntry();
        zip.finish();
        zip.close();
        return bytes.toByteArray();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
