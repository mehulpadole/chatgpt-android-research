package com.example.androidfeasibility;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MainActivity extends Activity implements ConversationCoordinator.Listener {
    private static final int PICK_ATTACHMENT = 7001;
    private ConversationCoordinator coordinator;
    private MockProvider mockProvider;
    private HttpStreamingProviderAdapter httpProvider;
    private OpenRouterProviderAdapter openRouterProvider;
    private AndroidCredentialStore credentialStore;
    private ProviderSettingsController providerSettings;
    private ProviderRouter router;
    private ProviderRegistry providerRegistry;
    private NavigationState navigation;
    private InMemoryProjectRepository projectRepository;
    private InMemoryMemoryRepository memoryRepository;
    private AndroidAttachmentStore attachmentStore;
    private ComposerDraft composerDraft;
    private SystemTextToSpeechAdapter textToSpeech;
    private AndroidAudioCaptureController audioCapture;
    private VoiceSessionCoordinator voiceCoordinator;
    private JsonSyncStore syncStore;
    private SyncWorker activeSyncWorker;
    private Conversation latestConversation;
    private LinearLayout messages;
    private ScrollView scroll;
    private EditText composer;
    private TextView status;
    private Spinner providerMode;
    private Spinner scenario;
    private EditText endpointInput;
    private EditText modelInput;
    private EditText apiKeyInput;
    private final Map<String, TextView> messageViews = new HashMap<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        mockProvider = new MockProvider();
        credentialStore = new AndroidCredentialStore(this);
        providerSettings = new ProviderSettingsController(credentialStore);
        attachmentStore = new AndroidAttachmentStore(this);
        composerDraft = new ComposerDraft();
        navigation = NavigationState.home();
        projectRepository = new InMemoryProjectRepository();
        memoryRepository = new InMemoryMemoryRepository();
        textToSpeech = new SystemTextToSpeechAdapter(this);
        audioCapture = new AndroidAudioCaptureController(this);
        voiceCoordinator = new VoiceSessionCoordinator(audioCapture,
                new DeterministicSpeechToTextAdapter(), textToSpeech, null,
                new VoiceSessionCoordinator.Listener() {
                    @Override public void onStateChanged(VoiceState state, String error) {
                        statusFromVoice(state, error);
                    }
                    @Override public void onTranscript(String transcript) {
                        if (composer != null) composer.setText(transcript);
                    }
                });
        try {
            syncStore = new JsonSyncStore(new File(getFilesDir(), "sync/outbox.properties"));
        } catch (Exception error) {
            syncStore = null;
        }
        providerRegistry = new ProviderRegistry();
        try {
            providerRegistry.register(new ProviderDefinition("local-mock", "Local deterministic",
                    new URL("https://local.mochi.invalid/"),
                    new HashSet<>(java.util.Arrays.asList(ProviderCapabilities.TEXT, ProviderCapabilities.STREAMING))),
                    mockProvider);
        } catch (Exception error) {
            statusSafe("Provider registry unavailable: " + error.getMessage());
        }
        try {
            if (BuildProfile.testControlsEnabled()) {
                httpProvider = new HttpStreamingProviderAdapter(new URL(httpBaseUrl()), "NORMAL");
                providerRegistry.register(new ProviderDefinition("test-http", "Local staging HTTP",
                        new URL(httpBaseUrl()),
                        new HashSet<>(java.util.Arrays.asList(ProviderCapabilities.TEXT, ProviderCapabilities.STREAMING))),
                        httpProvider);
            }
        } catch (MalformedURLException error) {
            httpProvider = null;
        } catch (IllegalArgumentException error) {
            httpProvider = null;
        }
        try {
            providerSettings.setEndpoint(openRouterBaseUrl());
            openRouterProvider = new OpenRouterProviderAdapter(providerSettings.endpoint(), credentialStore);
            providerRegistry.register(new ProviderDefinition("openrouter", "OpenRouter",
                    providerSettings.endpoint(),
                    new HashSet<>(java.util.Arrays.asList(ProviderCapabilities.TEXT,
                            ProviderCapabilities.VISION, ProviderCapabilities.STREAMING))),
                    openRouterProvider);
        } catch (Exception error) {
            openRouterProvider = null;
        }
        router = new ProviderRouter(providerRegistry);
        coordinator = new ConversationCoordinator(new JsonConversationRepository(this), router, this,
                new ProviderConfiguration("local-mock", "deterministic"));
        buildUi();
        if (httpProvider == null) status.setText("HTTP provider unavailable · invalid development URL");
        try {
            coordinator.restore();
        } catch (Exception error) {
            status.setText("Restore failed: " + error.getMessage());
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(12), dp(20), dp(12));
        root.setBackgroundColor(Color.rgb(248, 247, 244));

        TextView title = new TextView(this);
        title.setText("MoCHi Android");
        title.setTextSize(24);
        title.setTextColor(Color.rgb(30, 30, 30));
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(56)));

        status = new TextView(this);
        status.setText("Idle · local-mock / deterministic");
        status.setTextColor(Color.DKGRAY);
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(32)));

        LinearLayout navigationRow = new LinearLayout(this);
        navigationRow.setGravity(Gravity.CENTER_HORIZONTAL);
        addNavigationButton(navigationRow, "Home", NavigationState.Screen.HOME);
        addNavigationButton(navigationRow, "Projects", NavigationState.Screen.PROJECTS);
        addNavigationButton(navigationRow, "Memory", NavigationState.Screen.MEMORY);
        addNavigationButton(navigationRow, "Settings", NavigationState.Screen.SETTINGS);
        root.addView(navigationRow, new LinearLayout.LayoutParams(-1, dp(52)));

        TextView providerHeading = new TextView(this);
        providerHeading.setText("OpenRouter provider settings");
        providerHeading.setTextSize(16);
        providerHeading.setTextColor(Color.rgb(30, 30, 30));
        root.addView(providerHeading, new LinearLayout.LayoutParams(-1, dp(34)));

        endpointInput = new EditText(this);
        endpointInput.setSingleLine(true);
        endpointInput.setHint("Endpoint URL");
        endpointInput.setText(providerSettings.endpoint().toString());
        root.addView(endpointInput, new LinearLayout.LayoutParams(-1, dp(52)));

        modelInput = new EditText(this);
        modelInput.setSingleLine(true);
        modelInput.setHint("Model identifier");
        modelInput.setText(providerSettings.modelId());
        root.addView(modelInput, new LinearLayout.LayoutParams(-1, dp(52)));

        apiKeyInput = new EditText(this);
        apiKeyInput.setSingleLine(true);
        apiKeyInput.setHint("API key (never shown after save)");
        apiKeyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(apiKeyInput, new LinearLayout.LayoutParams(-1, dp(52)));

        LinearLayout providerActions = new LinearLayout(this);
        Button saveProvider = new Button(this);
        saveProvider.setText("Save key");
        saveProvider.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { saveProviderSettings(); }
        });
        providerActions.addView(saveProvider, new LinearLayout.LayoutParams(0, dp(50), 1));
        Button testProvider = new Button(this);
        testProvider.setText("Test");
        testProvider.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { testProviderSettings(); }
        });
        providerActions.addView(testProvider, new LinearLayout.LayoutParams(0, dp(50), 1));
        Button removeProvider = new Button(this);
        removeProvider.setText("Remove");
        removeProvider.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { removeProviderSettings(); }
        });
        providerActions.addView(removeProvider, new LinearLayout.LayoutParams(0, dp(50), 1));
        root.addView(providerActions, new LinearLayout.LayoutParams(-1, dp(56)));

        providerMode = new Spinner(this);
        String[] providerChoices = BuildProfile.testControlsEnabled()
                ? new String[]{"local-mock", "test-http", "openrouter"}
                : new String[]{"openrouter"};
        providerMode.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                providerChoices));
        root.addView(providerMode, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView syncHeading = new TextView(this);
        syncHeading.setText("Conversation sync mode");
        syncHeading.setTextSize(16);
        syncHeading.setTextColor(Color.rgb(30, 30, 30));
        root.addView(syncHeading, new LinearLayout.LayoutParams(-1, dp(34)));

        Spinner syncMode = new Spinner(this);
        syncMode.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"local-only", "cloud-sync (staging)"}));
        root.addView(syncMode, new LinearLayout.LayoutParams(-1, dp(48)));
        Button syncNow = new Button(this);
        syncNow.setText("Sync now");
        syncNow.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                runSync(syncMode.getSelectedItem().toString());
            }
        });
        root.addView(syncNow, new LinearLayout.LayoutParams(-1, dp(52)));

        scenario = new Spinner(this);
        String[] names = BuildProfile.testControlsEnabled()
                ? new String[]{"NORMAL", "SLOW", "FAIL_BEFORE_CONTENT", "FAIL_AFTER_PARTIAL", "EMPTY",
                "DUPLICATE_TERMINAL", "DELTA_AFTER_TERMINAL", "FAIL_AFTER_TERMINAL"}
                : new String[]{"NORMAL"};
        scenario.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        root.addView(scenario, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView privacy = new TextView(this);
        privacy.setText(PrivacyDisclosure.TEXT);
        privacy.setTextSize(12);
        privacy.setTextColor(Color.DKGRAY);
        root.addView(privacy, new LinearLayout.LayoutParams(-1, dp(54)));

        scroll = new ScrollView(this);
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(messages, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout composerRow = new LinearLayout(this);
        composerRow.setGravity(Gravity.CENTER_VERTICAL);
        composer = new EditText(this);
        composer.setHint("Type a synthetic prompt");
        composer.setSingleLine(false);
        composerRow.addView(composer, new LinearLayout.LayoutParams(0, dp(56), 1));
        Button attach = new Button(this);
        attach.setText("Attach");
        attach.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                startActivityForResult(AndroidAttachmentPicker.documentIntent(), PICK_ATTACHMENT);
            }
        });
        composerRow.addView(attach, new LinearLayout.LayoutParams(dp(94), dp(56)));
        Button read = new Button(this);
        read.setText("Read");
        read.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { readLastAssistant(); }
        });
        composerRow.addView(read, new LinearLayout.LayoutParams(dp(78), dp(56)));
        Button send = new Button(this);
        send.setText("Send");
        send.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { sendPrompt(); }
        });
        composerRow.addView(send, new LinearLayout.LayoutParams(dp(86), dp(56)));
        Button stop = new Button(this);
        stop.setText("Stop");
        stop.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { stopPrompt(); }
        });
        composerRow.addView(stop, new LinearLayout.LayoutParams(dp(82), dp(56)));
        root.addView(composerRow, new LinearLayout.LayoutParams(-1, dp(70)));
        setContentView(root);
    }

    private void sendPrompt() {
        String prompt = composer.getText().toString();
        try {
            String selectedProvider = providerMode.getSelectedItem().toString();
            String selectedScenario = scenario.getSelectedItem().toString();
            mockProvider.setScenario(MockScenario.valueOf(selectedScenario));
            if (httpProvider != null) httpProvider.setScenario(selectedScenario);
            String model = selectedProvider.equals("test-http") ? "ndjson-test" : "deterministic";
            if (selectedProvider.equals("openrouter")) {
                applyProviderFields();
                model = providerSettings.modelId();
                if (openRouterProvider == null) throw new IllegalStateException("OpenRouter endpoint is invalid");
            }
            coordinator.setProviderConfiguration(new ProviderConfiguration(selectedProvider, model));
            composerDraft.setText(prompt);
            List<Attachment> selectedAttachments = composerDraft.attachments();
            coordinator.startTurn(prompt, selectedAttachments);
            composerDraft.clear();
            composer.setText("");
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(composer.getWindowToken(), 0);
        } catch (Exception error) {
            status.setText("Cannot start: " + error.getMessage());
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_ATTACHMENT || resultCode != RESULT_OK || data == null
                || data.getData() == null || attachmentStore == null) return;
        try {
            Attachment attachment = attachmentStore.importUri(data.getData(),
                    coordinator.snapshot().id, "draft");
            composerDraft.addAttachment(attachment);
            status.setText("Attachment ready · " + attachment.displayName);
        } catch (Exception error) {
            status.setText("Attachment failed: " + error.getMessage());
        }
    }

    private void stopPrompt() {
        try {
            coordinator.cancelActive();
        } catch (Exception error) {
            status.setText("Cancel failed: " + error.getMessage());
        }
    }

    private void saveProviderSettings() {
        try {
            applyProviderFields();
            String key = apiKeyInput.getText().toString();
            if (!key.isEmpty()) providerSettings.saveCredential(key);
            apiKeyInput.setText("");
            status.setText("Provider saved · " + providerSettings.maskedCredential());
        } catch (Exception error) {
            status.setText("Provider settings invalid: " + error.getMessage());
        }
    }

    private void testProviderSettings() {
        try {
            applyProviderFields();
            String key = apiKeyInput.getText().toString();
            if (!key.isEmpty()) {
                providerSettings.saveCredential(key);
                apiKeyInput.setText("");
            }
            ProviderSettingsController.State result = providerSettings.testConnection();
            status.setText("OpenRouter test · " + result.name()
                    + (providerSettings.lastError().isEmpty() ? "" : " · " + providerSettings.lastError()));
        } catch (Exception error) {
            status.setText("Provider test failed: " + error.getMessage());
        }
    }

    private void removeProviderSettings() {
        providerSettings.removeCredential();
        apiKeyInput.setText("");
        status.setText("OpenRouter credential removed");
    }

    private void addNavigationButton(LinearLayout row, String label, final NavigationState.Screen screen) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                navigation = navigationForScreen(screen);
                status.setText("MoCHi · " + navigation.screen.name());
            }
        });
        row.addView(button, new LinearLayout.LayoutParams(0, dp(50), 1));
    }

    private NavigationState navigationForScreen(NavigationState.Screen screen) {
        if (screen == NavigationState.Screen.HOME) return navigation.toHome();
        if (screen == NavigationState.Screen.PROJECTS) return navigation.toProjects();
        if (screen == NavigationState.Screen.MEMORY) return navigation.toMemory();
        if (screen == NavigationState.Screen.SETTINGS) return navigation.toSettings();
        return navigation;
    }

    private void statusSafe(String message) {
        if (status != null) status.setText(message);
    }

    private void runSync(String selectedMode) {
        if (syncStore == null) {
            status.setText("Sync unavailable · local store could not open");
            return;
        }
        final boolean cloud = selectedMode != null && selectedMode.startsWith("cloud-sync");
        final SyncClient.Transport transport;
        final SyncClient client;
        try {
            if (cloud) {
                String baseUrl = syncBaseUrl();
                if (baseUrl.isEmpty()) {
                    status.setText("Cloud sync not configured · inject a staging URL");
                    return;
                }
                transport = new SyncHttpTransport(new URL(baseUrl), null);
                client = new SyncClient(Entitlement.CLOUD_SYNC);
            } else {
                transport = new SyncClient.Transport() {
                    @Override public SyncClient.PushResult push(List<SyncOperation> operations) {
                        return SyncClient.PushResult.accepted();
                    }
                    @Override public SyncClient.ChangeBatch changes(SyncCursor cursor) {
                        return new SyncClient.ChangeBatch(cursor, null);
                    }
                };
                client = new SyncClient(Entitlement.LOCAL_ONLY);
            }
        } catch (Exception error) {
            status.setText("Sync configuration invalid: " + error.getMessage());
            return;
        }
        final SyncWorker worker = new SyncWorker(client, syncStore, transport,
                new SyncWorker.BackoffPolicy(1_000L, 30_000L), null);
        activeSyncWorker = worker;
        status.setText(cloud ? "Cloud sync · connecting" : "Local-only · no cloud request");
        new Thread(new Runnable() {
            @Override public void run() {
                final SyncStatus result;
                try {
                    result = worker.runUntilTerminal(3);
                } catch (Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() { status.setText("Sync failed · local data preserved"); }
                    });
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override public void run() { status.setText("Sync · " + result.name()); }
                });
            }
        }, "mochi-sync-worker").start();
    }

    private void applyProviderFields() throws Exception {
        providerSettings.setEndpoint(endpointInput.getText().toString());
        providerSettings.setModel(modelInput.getText().toString());
        if (openRouterProvider != null) openRouterProvider.setBaseUrl(providerSettings.endpoint());
    }

    @Override public void onChanged(final Conversation conversation, final TurnState state, final String error) {
        latestConversation = conversation;
        runOnUiThread(new Runnable() {
            @Override public void run() {
                status.setText(state.name() + (error == null || error.isEmpty() ? "" : " · " + error));
                render(conversation);
            }
        });
    }

    private void readLastAssistant() {
        if (voiceCoordinator == null || latestConversation == null) {
            status.setText("No assistant response to read");
            return;
        }
        List<Message> messagesSnapshot = latestConversation.snapshotMessages();
        for (int i = messagesSnapshot.size() - 1; i >= 0; i--) {
            Message message = messagesSnapshot.get(i);
            if (message.role == Role.ASSISTANT && message.content != null && !message.content.isEmpty()) {
                voiceCoordinator.startReadAloud(message.content);
                return;
            }
        }
        status.setText("No assistant response to read");
    }

    private void statusFromVoice(final VoiceState state, final String error) {
        if (status == null) return;
        runOnUiThread(new Runnable() {
            @Override public void run() {
                status.setText("Voice · " + state.name()
                        + (error == null || error.isEmpty() ? "" : " · " + error));
            }
        });
    }

    private void render(Conversation conversation) {
        final boolean keepAtBottom = isNearBottom();
        Set<String> seen = new HashSet<>();
        for (Message message : conversation.snapshotMessages()) {
            seen.add(message.id);
            TextView view = messageViews.get(message.id);
            if (view == null) {
                view = new TextView(this);
                view.setTextSize(17);
                view.setPadding(dp(14), dp(12), dp(14), dp(12));
                messageViews.put(message.id, view);
                messages.addView(view, new LinearLayout.LayoutParams(-1, -2));
            }
            String prefix = message.role == Role.USER ? "You" : "Assistant";
            String failure = message.failureMessage == null || message.failureMessage.isEmpty()
                    ? "" : "\nError · " + message.failureMessage;
            view.setText(prefix + " · " + message.status.name() + failure + "\n" + message.content);
            view.setTextColor(message.role == Role.USER ? Color.rgb(35, 65, 95) : Color.rgb(35, 35, 35));
            view.setBackgroundColor(message.role == Role.USER ? Color.rgb(225, 236, 248) : Color.WHITE);
        }
        for (String id : new HashSet<>(messageViews.keySet())) {
            if (!seen.contains(id)) {
                TextView view = messageViews.remove(id);
                messages.removeView(view);
            }
        }
        if (keepAtBottom) scroll.post(new Runnable() {
            @Override public void run() { scroll.fullScroll(View.FOCUS_DOWN); }
        });
    }

    private boolean isNearBottom() {
        return scroll.getChildCount() == 0 || scroll.getScrollY() + scroll.getHeight() >= scroll.getChildAt(0).getHeight() - 80;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onDestroy() {
        if (activeSyncWorker != null) activeSyncWorker.cancel();
        mockProvider.shutdown();
        if (httpProvider != null) httpProvider.shutdown();
        if (openRouterProvider != null) openRouterProvider.shutdown();
        if (voiceCoordinator != null) voiceCoordinator.cancel();
        if (audioCapture != null) audioCapture.release();
        if (textToSpeech != null) textToSpeech.shutdown();
        super.onDestroy();
    }

    private String httpBaseUrl() {
        String extra = getIntent().getStringExtra("phase5_http_base_url");
        return extra == null || extra.isEmpty() ? "http://10.0.2.2:8765/" : extra;
    }

    private String openRouterBaseUrl() {
        String extra = getIntent().getStringExtra("phase6_openrouter_base_url");
        return extra == null || extra.isEmpty() ? "https://openrouter.ai/" : extra;
    }

    private String syncBaseUrl() {
        String extra = getIntent().getStringExtra("phase9_sync_base_url");
        return extra == null ? "" : extra;
    }
}
