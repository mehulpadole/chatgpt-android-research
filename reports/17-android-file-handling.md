# Android File Handling

`AndroidAttachmentPicker` uses `ACTION_OPEN_DOCUMENT` with `CATEGORY_OPENABLE` and read/persistable URI flags. `AndroidAttachmentStore` copies the selected `content://` source into application-managed storage through `ContentResolver` before the attachment is persisted. No broad storage permission was introduced.

Validation rejects missing/unreadable, zero-byte, oversized, and unsupported content. The application does not execute selected files or render arbitrary active content.

Emulator picker UI and reboot/source-app deletion behavior are Not tested because no Android AVD or connected device was available.
