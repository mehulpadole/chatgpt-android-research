# Phase 3 — Runtime Observations

## Launch and onboarding

The installed package launched with `am start -n com.openai.chatgpt/.MainActivity`. The initial activity remained `com.openai.chatgpt/.MainActivity`; the process stayed alive. After approximately 28 seconds of first-run initialization, the UI hierarchy changed from a progress indicator to an unauthenticated welcome screen.

Observed welcome-screen text:

- `Welcome to ChatGPT`
- `Bring your files, your ideas, and your next big question. Log in for the full experience.`
- `Continue with Google`
- `Log in or sign up`
- `Skip`

Evidence: `07-observations/emulator-chatgpt-initial.png`, `emulator-chatgpt-initial-window.xml`, `emulator-chatgpt-after-28s.png`, and `emulator-chatgpt-after-28s-window.xml`.

## Logged-out main composer

Selecting `Skip` reached the main screen without an account. The screen exposed:

- Header title: `ChatGPT`
- `Log in`
- `What can I help with?`
- Composer placeholder: `Ask ChatGPT`
- Content descriptions: `Menu`, `Ask ChatGPT`, `Attachment`, `Dictation`, `Send Message`

Evidence: `07-observations/emulator-chatgpt-after-skip.png` and `emulator-chatgpt-after-skip-window.xml`.

This confirms that the application can render its logged-out shell and composer on the emulator. It does not confirm that anonymous message submission is enabled or that backend requests succeed.

## Runtime boundaries not crossed

The following were deliberately not performed:

- Google account login or OAuth credential entry
- OpenAI account login
- Chat prompt submission
- File/photo selection or upload
- Microphone permission grant or voice recording
- Accessibility-service enablement or screen sharing
- Subscription/billing flow
- History synchronization

These boundaries avoid transmitting user credentials, private files, audio, screen contents, or test prompts to third parties. They also explain why network streaming, persistence synchronization, and server responses are marked unknown rather than confirmed.

## Runtime stability

The app process remained alive after launch and after the onboarding transition. No `AndroidRuntime` fatal exception was observed in the filtered logcat capture. The emulator was shut down cleanly with `adb emu kill` to release resources during static decoding, then restarted after analysis; it is currently left running with ADB online.
