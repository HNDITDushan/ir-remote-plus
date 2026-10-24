package com.universal.irremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.universal.irremote.ir.IrCodes;
import com.universal.irremote.ir.IrSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Steps through every possible code of a protocol so you can find the codes of
 * an unknown device (the phone can only transmit, it cannot learn from a remote).
 */
public class FinderActivity extends Activity {
    private static final int MODE_SYMPHONY = 0, MODE_NEC_COMMON = 1, MODE_NEC_ADDRESS = 2;
    private static final String[] MODE_NAMES = {"Fan (Symphony)", "NEC common", "NEC address"};
    private static final String[] MODE_HELP = {
            "12-bit codes used by many Chinese pedestal, wall and tower fans. 4096 codes.",
            "NEC codes on the 10 addresses most used by fans, lights and cheap devices. 2560 codes.",
            "All 256 NEC commands on one address you enter (hex, e.g. 00 or 807F).",
    };
    private static final int[] NEC_COMMON = {0x00, 0x80, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0xFF};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int mode = MODE_SYMPHONY;
    private int index = 0;
    private boolean running = false;

    private Button[] modeButtons;
    private TextView modeHelp, codeView, progressView;
    private EditText address, interval;
    private Button startButton;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            sendCurrent();
            if (index < count() - 1) {
                index++;
                handler.postDelayed(this, intervalMs());
            } else {
                setRunning(false);
                Toast.makeText(FinderActivity.this, "Finished all codes", Toast.LENGTH_SHORT).show();
            }
            render();
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout page = Ui.page(this);
        page.addView(Ui.title(this, "Code finder"));
        page.addView(Ui.hint(this, "1. Point the phone at the device (fan ON or OFF).\n"
                + "2. Press Start. The app sends codes one after another.\n"
                + "3. When the device beeps or reacts, press Pause.\n"
                + "4. Use ◀ Back and \"Send this\" to find the exact code, then Save it.\n"
                + "Try each type below until something reacts."));

        LinearLayout modes = Ui.row(this);
        modeButtons = new Button[MODE_NAMES.length];
        for (int i = 0; i < MODE_NAMES.length; i++) {
            final int m = i;
            modeButtons[i] = Ui.key(this, MODE_NAMES[i], v -> {
                setRunning(false);
                mode = m;
                index = 0;
                render();
            });
            modeButtons[i].setTextSize(13);
            modes.addView(modeButtons[i], Ui.cell(this));
        }
        page.addView(modes, Ui.matchWrap());
        modeHelp = Ui.hint(this, "");
        page.addView(modeHelp);

        address = new EditText(this);
        address.setHint("NEC address (hex), e.g. 00");
        address.setText("00");
        address.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        page.addView(address, Ui.matchWrap());

        interval = new EditText(this);
        interval.setHint("Delay between codes (ms)");
        interval.setText("300");
        interval.setInputType(InputType.TYPE_CLASS_NUMBER);
        LinearLayout ir = Ui.row(this);
        ir.setGravity(Gravity.CENTER_VERTICAL);
        ir.addView(Ui.text(this, "Delay (ms): ", 15, Ui.MUTED));
        ir.addView(interval, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        page.addView(ir, Ui.matchWrap());

        LinearLayout screen = Ui.column(this);
        screen.setGravity(Gravity.CENTER);
        int p = Ui.dp(this, 16);
        screen.setPadding(p, p, p, p);
        screen.setBackground(Ui.rounded(Color.parseColor("#0D2847"), Ui.dp(this, 18)));
        codeView = Ui.text(this, "", 30, Color.WHITE);
        codeView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        codeView.setGravity(Gravity.CENTER);
        progressView = Ui.text(this, "", 14, Color.parseColor("#B8D4FF"));
        progressView.setGravity(Gravity.CENTER);
        screen.addView(codeView);
        screen.addView(progressView);
        page.addView(screen, Ui.spaced(this));

        startButton = Ui.primary(this, "", v -> setRunning(!running));
        page.addView(startButton, Ui.spaced(this));

        Ui.grid(this, page, new android.view.View[]{
                Ui.key(this, "◀ Back", v -> step(-1)),
                Ui.key(this, "Send this", v -> sendCurrent()),
                Ui.key(this, "Next ▶", v -> step(+1)),
        }, 3);
        page.addView(Ui.button(this, "💾  Save this code to a remote", Ui.CARD, Ui.PRIMARY, v -> save()),
                Ui.spaced(this));
        render();
    }

    @Override
    protected void onPause() {
        super.onPause();
        setRunning(false);
    }

    private int count() {
        switch (mode) {
            case MODE_NEC_COMMON: return NEC_COMMON.length * 256;
            case MODE_NEC_ADDRESS: return 256;
            default: return 4096;
        }
    }

    private String currentCode() {
        switch (mode) {
            case MODE_NEC_COMMON:
                return IrCodes.nec(NEC_COMMON[index / 256], index % 256);
            case MODE_NEC_ADDRESS:
                return IrCodes.nec(parseAddress(), index);
            default:
                return IrCodes.symphony(index);
        }
    }

    private int parseAddress() {
        try {
            return Integer.parseInt(address.getText().toString().trim(), 16) & 0xFFFF;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int intervalMs() {
        try {
            return Math.max(150, Integer.parseInt(interval.getText().toString().trim()));
        } catch (NumberFormatException e) {
            return 300;
        }
    }

    private void sendCurrent() {
        IrSender.sendCode(this, currentCode());
    }

    private void step(int delta) {
        setRunning(false);
        index = Math.max(0, Math.min(count() - 1, index + delta));
        sendCurrent();
        render();
    }

    private void setRunning(boolean run) {
        running = run;
        handler.removeCallbacks(tick);
        if (run) handler.post(tick);
        if (startButton != null) render();
    }

    private void render() {
        for (int i = 0; i < modeButtons.length; i++) {
            boolean sel = i == mode;
            modeButtons[i].setBackground(Ui.rounded(sel ? Ui.PRIMARY : Ui.CARD, Ui.dp(this, 14)));
            modeButtons[i].setTextColor(sel ? Color.WHITE : Ui.TEXT);
        }
        modeHelp.setText(MODE_HELP[mode]);
        address.setVisibility(mode == MODE_NEC_ADDRESS ? android.view.View.VISIBLE : android.view.View.GONE);
        codeView.setText(currentCode());
        progressView.setText((index + 1) + " / " + count());
        startButton.setText(running ? "⏸  Pause" : "▶  Start sending");
    }

    private void save() {
        setRunning(false);
        String code = currentCode();
        List<Remote> targets = new ArrayList<>();
        for (Remote r : Remote.loadAll(this)) if (Remote.TYPE_BUTTONS.equals(r.type)) targets.add(r);
        String[] names = new String[targets.size() + 1];
        for (int i = 0; i < targets.size(); i++) names[i] = targets.get(i).name;
        names[targets.size()] = "＋ New remote…";
        new AlertDialog.Builder(this)
                .setTitle("Save " + code + " to")
                .setItems(names, (d, which) -> {
                    if (which < targets.size()) askLabel(targets.get(which), code);
                    else newRemoteThenSave(code);
                })
                .show();
    }

    private void newRemoteThenSave(String code) {
        EditText name = new EditText(this);
        name.setHint("e.g. Inovax Fan");
        new AlertDialog.Builder(this)
                .setTitle("Remote name")
                .setView(padded(name))
                .setPositiveButton("Next", (d, w) -> {
                    String n = name.getText().toString().trim();
                    Remote r = Remote.create(n.isEmpty() ? "My remote" : n, Remote.TYPE_BUTTONS);
                    r.save(this);
                    askLabel(r, code);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void askLabel(Remote r, String code) {
        EditText label = new EditText(this);
        label.setHint("Button label, e.g. Power");
        new AlertDialog.Builder(this)
                .setTitle("Button name")
                .setView(padded(label))
                .setPositiveButton("Save", (d, w) -> {
                    String l = label.getText().toString().trim();
                    r.buttons.add(new Remote.Button(l.isEmpty() ? code : l, code));
                    r.save(this);
                    Toast.makeText(this, "Saved to " + r.name, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private LinearLayout padded(android.view.View v) {
        LinearLayout l = Ui.column(this);
        int p = Ui.dp(this, 20);
        l.setPadding(p, p / 2, p, 0);
        l.addView(v, Ui.matchWrap());
        return l;
    }
}
