# Provider Attachment Adapter

`ProviderAttachmentAdapter` owns asynchronous provider preparation and cancellation. `OpenRouterAttachmentAdapter` supports image preparation for models advertising `VISION`; it produces an ephemeral data URL for the OpenRouter codec and leaves local `Attachment` state/provider references separate.

`OpenRouterCodec.encodeRequest` can encode a text content part plus one or more prepared `image_url` parts. Unsupported model capability, missing source, cancellation, and retry are handled without deleting the local attachment.

Live multimodal delivery to OpenRouter was not tested without a user-owned credential and connected runtime.
