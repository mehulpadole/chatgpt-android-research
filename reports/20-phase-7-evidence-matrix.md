# Phase 7 Evidence Matrix

| Requirement | Status | Evidence |
|---|---|---|
| Stable local attachment IDs | Confirmed | `AttachmentContractTest` |
| Provider-independent persistence | Confirmed | Conversation codec and file repository tests |
| Scoped Android picker | Confirmed for source code; UI Not tested | `AndroidAttachmentPicker` and no storage permission |
| Safe local managed copy | Confirmed in pure Java | `FileAttachmentRepository` |
| MIME/size/source validation | Confirmed | `AttachmentValidatorTest` |
| Composer add/remove path | Confirmed for code; UI Not tested | `ComposerDraft`, `MainActivity` attach path |
| Async prepare/cancel/retry | Confirmed locally | `AttachmentPreparationTest` |
| OpenRouter image wire shape | Confirmed by codec test | `OpenRouterAttachmentAdapterTest` |
| Live multimodal provider flow | Not tested | No credential/runtime |
| Restart/rendering on emulator | Not tested | No AVD/device |
| Cleanup ownership | Confirmed locally | shared-file repository test |
