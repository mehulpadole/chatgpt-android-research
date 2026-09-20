# Approach comparison

| Approach | What the evidence supports | Main cost/risk | Decision |
|---|---|---|---|
| Modify official APK | Resource-level modification and launch are feasible; deeper code path is obfuscated/Valdi-coupled | New signer breaks updates; backend/account assumptions remain; renderer/composer are not portable; legal/security/provenance risk | Reference-only |
| Wrap or intercept official client | Some Android/Valdi boundaries are visible | Requires undocumented lifecycle, UI, auth, and protocol coupling; difficult to test safely | Do not choose as foundation |
| Independent provider-neutral client | Core state, streaming, persistence, and lifecycle behavior are directly controllable; prototype passes tests/emulator smoke | Must implement product UI and later provider adapters; real backend contract still needs a separate decision | Recommended |

## Why the independent path wins

The user-visible behaviors with the highest product value can be implemented and tested independently: stable turn identity, incremental rendering, cancellation, partial failure, persistence, and restoration. The prototype already exercises these boundaries. The official artifact adds a compiled Valdi rendering dependency and an unknown authenticated backend/security contract without providing a reliable replacement seam.

## What should not be copied

Do not copy official credentials, production endpoints/headers, private user data, or proprietary Valdi assets into the prototype. Do not modify MoCHi until a separate design review approves an integration boundary and test data policy.
