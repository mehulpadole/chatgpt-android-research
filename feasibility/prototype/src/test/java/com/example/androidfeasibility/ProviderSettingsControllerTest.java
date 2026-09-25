package com.example.androidfeasibility;

import java.net.URL;

public final class ProviderSettingsControllerTest {
    public static void main(String[] args) throws Exception {
        InMemoryCredentialStore store = new InMemoryCredentialStore();
        ProviderSettingsController controller = new ProviderSettingsController(store);
        check(controller.state() == ProviderSettingsController.State.NOT_CONFIGURED,
                "new settings must be unconfigured");
        controller.setEndpoint("https://openrouter.ai/");
        controller.setModel("openai/gpt-4");
        controller.saveCredential("sk-settings-secret");
        check(controller.state() == ProviderSettingsController.State.READY,
                "saved credential must make settings ready");
        check(controller.maskedCredential().equals("••••••••cret"),
                "settings must expose only a masked suffix");
        check(controller.configuration().providerId.equals("openrouter"),
                "settings must select OpenRouter");
        check(controller.configuration().modelId.equals("openai/gpt-4"),
                "settings must preserve model selection");
        final String[] seenSecret = new String[1];
        controller.testConnection(new ProviderSettingsController.ConnectionProbe() {
            @Override public ProviderModel test(URL endpoint, String modelId, String secret) {
                seenSecret[0] = secret;
                return new ProviderModel("openrouter", modelId, "GPT-4", null, 8192);
            }
        });
        check(controller.state() == ProviderSettingsController.State.CONNECTED,
                "successful probe must connect");
        check(seenSecret[0].equals("sk-settings-secret"),
                "only the transport probe may receive the full secret");
        controller.removeCredential();
        check(controller.state() == ProviderSettingsController.State.NOT_CONFIGURED,
                "remove must clear settings state");
        check(controller.maskedCredential().isEmpty(), "removed credential must be hidden");
        System.out.println("PROVIDER SETTINGS CONTROLLER TESTS PASSED");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
