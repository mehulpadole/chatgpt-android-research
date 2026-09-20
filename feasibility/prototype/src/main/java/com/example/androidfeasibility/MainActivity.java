package com.example.androidfeasibility;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class MainActivity extends Activity implements ConversationCoordinator.Listener {
    private ConversationCoordinator coordinator;
    private MockProvider provider;
    private LinearLayout messages;
    private ScrollView scroll;
    private EditText composer;
    private TextView status;
    private Spinner scenario;
    private final Map<String, TextView> messageViews = new HashMap<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        provider = new MockProvider();
        coordinator = new ConversationCoordinator(new JsonConversationRepository(this), provider, this);
        buildUi();
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
        title.setText("Local Stream Lab");
        title.setTextSize(24);
        title.setTextColor(Color.rgb(30, 30, 30));
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(56)));

        status = new TextView(this);
        status.setText("Idle · local-mock / deterministic");
        status.setTextColor(Color.DKGRAY);
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(32)));

        scenario = new Spinner(this);
        String[] names = new String[]{"NORMAL", "SLOW", "FAIL_BEFORE_CONTENT", "FAIL_AFTER_PARTIAL", "EMPTY", "DUPLICATE_TERMINAL"};
        scenario.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        root.addView(scenario, new LinearLayout.LayoutParams(-1, dp(48)));

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
            coordinator.startTurn(prompt, MockScenario.valueOf(scenario.getSelectedItem().toString()));
            composer.setText("");
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(composer.getWindowToken(), 0);
        } catch (Exception error) {
            status.setText("Cannot start: " + error.getMessage());
        }
    }

    private void stopPrompt() {
        try {
            coordinator.cancelActive();
        } catch (Exception error) {
            status.setText("Cancel failed: " + error.getMessage());
        }
    }

    @Override public void onChanged(final Conversation conversation, final TurnState state, final String error) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                status.setText(state.name() + (error == null || error.isEmpty() ? "" : " · " + error));
                render(conversation);
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
            view.setText(prefix + " · " + message.status.name() + "\n" + message.content);
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
        provider.shutdown();
        super.onDestroy();
    }
}
