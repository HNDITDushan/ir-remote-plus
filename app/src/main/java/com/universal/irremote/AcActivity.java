package com.universal.irremote;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.universal.irremote.ir.IrPattern;
import com.universal.irremote.ir.IrSender;
import com.universal.irremote.ir.Protocols;

/** Air-conditioner remote. AC remotes send the complete state on every press. */
public class AcActivity extends Activity {
    private static final String[] MODES = {"Auto", "Cool", "Dry", "Fan", "Heat"};
    private static final String[] FANS = {"Auto", "Low", "Medium", "High"};
    private static final String[][] PROTOCOLS = {
            {Remote.AC_ELECTRA, "AUX / YKR"},
            {Remote.AC_GREE, "Gree"},
            {Remote.AC_COOLIX, "Midea / Coolix"},
    };

    private Remote remote;
    private TextView display;
    private TextView tempView;
    private Button[] protoButtons;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        remote = Remote.find(this, getIntent().getLongExtra("id", -1));
        if (remote == null) {
            finish();
            return;
        }
        LinearLayout page = Ui.page(this);
        page.addView(Ui.title(this, remote.name));

        page.addView(Ui.hint(this, "AC protocol – if your AC does not respond to POWER, try another type. "
                + "Remotes named YKR-…/…E (AUX, DB fan coils) use AUX / YKR:"));
        LinearLayout protoRow = Ui.row(this);
        protoButtons = new Button[PROTOCOLS.length];
        for (int i = 0; i < PROTOCOLS.length; i++) {
            String id = PROTOCOLS[i][0];
            protoButtons[i] = Ui.key(this, PROTOCOLS[i][1], v -> {
                remote.acProtocol = id;
                remote.save(this);
                render();
            });
            protoButtons[i].setTextSize(13);
            protoRow.addView(protoButtons[i], Ui.cell(this));
        }
        page.addView(protoRow, Ui.matchWrap());

        LinearLayout screen = Ui.column(this);
        screen.setGravity(Gravity.CENTER);
        int p = Ui.dp(this, 16);
        screen.setPadding(p, p, p, p);
        screen.setBackground(Ui.rounded(Color.parseColor("#0D2847"), Ui.dp(this, 18)));
        tempView = Ui.text(this, "", 56, Color.WHITE);
        tempView.setTypeface(Typeface.DEFAULT_BOLD);
        tempView.setGravity(Gravity.CENTER);
        display = Ui.text(this, "", 16, Color.parseColor("#B8D4FF"));
        display.setGravity(Gravity.CENTER);
        screen.addView(tempView);
        screen.addView(display);
        page.addView(screen, Ui.spaced(this));

        Button power = Ui.button(this, "⏻  POWER", Ui.DANGER, Color.WHITE, v -> {
            remote.power = !remote.power;
            send();
        });
        page.addView(power, Ui.spaced(this));

        View[] keys = {
                Ui.key(this, "−  Temp", v -> changeTemp(-1)),
                Ui.key(this, "Temp  ＋", v -> changeTemp(+1)),
                Ui.key(this, "Mode", v -> {
                    remote.mode = (remote.mode + 1) % MODES.length;
                    remote.power = true;
                    send();
                }),
                Ui.key(this, "Fan speed", v -> {
                    remote.fan = (remote.fan + 1) % FANS.length;
                    remote.power = true;
                    send();
                }),
                Ui.key(this, "Swing", v -> {
                    remote.swing = !remote.swing;
                    remote.power = true;
                    if (Remote.AC_COOLIX.equals(remote.acProtocol)) {
                        save();
                        IrSender.send(this, Protocols.coolix(Protocols.COOLIX_SWING));
                    } else {
                        send();
                    }
                }),
                Ui.key(this, "Resend", v -> send()),
        };
        Ui.grid(this, page, keys, 2);

        page.addView(Ui.hint(this, "Tip: the app remembers the AC's settings. If the AC was changed with "
                + "its own remote, press Resend to sync it to what is shown here."));
        render();
    }

    private void changeTemp(int delta) {
        remote.temp = Math.max(16, Math.min(30, remote.temp + delta));
        remote.power = true;
        send();
    }

    private void save() {
        remote.save(this);
        render();
    }

    private void send() {
        save();
        IrPattern p;
        if (Remote.AC_COOLIX.equals(remote.acProtocol)) {
            p = Protocols.coolix(remote.power
                    ? Protocols.coolixState(remote.mode, remote.temp, remote.fan)
                    : Protocols.COOLIX_OFF);
        } else if (Remote.AC_ELECTRA.equals(remote.acProtocol)) {
            p = Protocols.electra(Protocols.electraState(remote.power, remote.mode, remote.temp, remote.fan, remote.swing));
        } else {
            p = Protocols.gree(Protocols.greeState(remote.power, remote.mode, remote.temp, remote.fan, remote.swing));
        }
        IrSender.send(this, p);
    }

    private void render() {
        tempView.setText(remote.power ? remote.temp + "°C" : "OFF");
        display.setText(MODES[remote.mode] + "  •  Fan " + FANS[remote.fan]
                + "  •  Swing " + (remote.swing ? "on" : "off"));
        for (int i = 0; i < PROTOCOLS.length; i++) {
            boolean sel = PROTOCOLS[i][0].equals(remote.acProtocol);
            protoButtons[i].setBackground(Ui.rounded(sel ? Ui.PRIMARY : Ui.CARD, Ui.dp(this, 14)));
            protoButtons[i].setTextColor(sel ? Color.WHITE : Ui.TEXT);
        }
    }
}
