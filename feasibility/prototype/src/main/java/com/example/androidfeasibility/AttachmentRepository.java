package com.example.androidfeasibility;

import java.io.File;
import java.util.List;

public interface AttachmentRepository {
    Attachment importFile(File source, String conversationId, String messageId,
                          String declaredMimeType, String displayName) throws Exception;

    void save(Attachment attachment) throws Exception;
    Attachment load(String attachmentId) throws Exception;
    List<Attachment> forMessage(String messageId) throws Exception;
    void delete(String attachmentId) throws Exception;
}
