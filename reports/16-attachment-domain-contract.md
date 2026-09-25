# Attachment Domain Contract

Attachments now have stable local IDs, conversation/message relationships, MIME metadata, byte size, application-managed local references, lifecycle state, and a provider-reference map kept separate from local identity. Message content is represented by extensible `ContentPart` values (`TextPart`, `ImagePart`, and `FilePart`) while the legacy `Message.content` field remains compatible.

Binary bytes are stored by `FileAttachmentRepository` in app-managed files. Conversation JSON stores only identity, metadata, relationships, and content-part references. Phase 5 JSON without `contentParts` or `attachments` remains readable.

Confirmed by `AttachmentContractTest` and `AttachmentValidatorTest`.
