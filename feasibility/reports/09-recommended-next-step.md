# Recommended next step

## Recommendation

Proceed with the independent client architecture and keep the official APK as a static reference. Phase 5 has now established the provider-neutral contract and a local HTTP test boundary; do not integrate a commercial provider or modify MoCHi yet.

The next design milestone should be a review of the Phase 5 contract, conformance results, and security preparation. If the contract is accepted, choose between optional authenticated emulator observation and a separately designed, user-authorized real-provider/BYOK adapter.

## Optional evidence expansion

If authenticated evidence is worth the privacy and account-risk tradeoff, it can be done entirely in an available emulator. The user would need to sign in through the emulator’s browser/custom-tab flow, complete MFA/consent if requested, and personally choose non-sensitive test files or grant microphone permission. The resulting tests could cover one real conversation, history reload, attachment selection/upload, and voice permission/callbacks. No physical phone is required. Static analysis should continue independently either way.

## Do not do yet

- Do not put real provider credentials into the prototype or official APK until the Phase 5 security requirements are reviewed.
- Do not alter MoCHi, its database, Cloudflare, Vercel, or production configuration.
- Do not treat the logged-out welcome screen as evidence for authenticated features.
- Do not publish or distribute the re-signed official APK.
- Do not claim official signer authenticity without a trusted independent fingerprint reference.

## Decision checkpoint

Review `00-executive-summary.md`, `03-component-dependency-map.md`, `04-provider-boundary-feasibility.md`, `08-risks-and-unknowns.md`, and `10-phase-5-provider-neutral-contract-and-http-backend.md` together. Preserve the shared conformance suite before considering any real provider adapter.
