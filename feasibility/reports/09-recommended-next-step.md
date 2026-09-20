# Recommended next step

## Recommendation

Proceed with the independent client architecture and keep the official APK as a static reference. Do not implement a provider or modify MoCHi yet.

The next design milestone should be a provider-neutral contract review based on the prototype’s `ProviderAdapter`, `ConversationCoordinator`, and `ConversationRepository`. The contract should explicitly define stable conversation/turn/message IDs, delta ordering, terminal idempotence, cancellation, partial failure, persistence checkpoints, and lifecycle behavior.

## Optional evidence expansion

If authenticated evidence is worth the privacy and account-risk tradeoff, it can be done entirely in the existing emulator. The user would need to sign in through the emulator’s browser/custom-tab flow, complete MFA/consent if requested, and personally choose non-sensitive test files or grant microphone permission. The resulting tests could cover one real conversation, history reload, attachment selection/upload, and voice permission/callbacks. No physical phone is required. Static analysis should continue independently either way.

## Do not do yet

- Do not put real provider credentials into the prototype or official APK.
- Do not alter MoCHi, its database, Cloudflare, Vercel, or production configuration.
- Do not treat the logged-out welcome screen as evidence for authenticated features.
- Do not publish or distribute the re-signed official APK.
- Do not claim official signer authenticity without a trusted independent fingerprint reference.

## Decision checkpoint

Review `00-executive-summary.md`, `03-component-dependency-map.md`, `04-provider-boundary-feasibility.md`, and `08-risks-and-unknowns.md` together. If the architecture is accepted, the next implementation can be a new provider adapter against a deliberately chosen test backend or local service, while preserving the same mock-driven tests and emulator-only workflow.
