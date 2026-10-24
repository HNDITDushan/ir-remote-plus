package com.universal.irremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.universal.irremote.ir.IrSender;

/** A remote made of user-defined buttons (fans, TVs, lights, anything). */
public class ButtonsActivity extends Activity {
    private Remote remote;
    private LinearLayout grid;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout page = Ui.page(this);
        remote = Remote.find(this, getIntent().getLongExtra("id", -1));
        if (remote == null) {
            finish();
            return;
        }
        page.addView(Ui.title(this, remote.name));
        grid = Ui.column(this);
        page.addView(grid, Ui.matchWrap());
        page.addView(Ui.primary(this, "＋  Add button", v -> edit(-1)), Ui.spaced(this));
        page.addView(Ui.key(this, "🔍  Find codes with Code finder",
                v -> startActivity(new Intent(this, FinderActivity.class))), Ui.spaced(this));
        page.addView(Ui.hint(this, "Long-press a button to edit or delete it."));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Remote fresh = Remote.find(this, remote.id); // the finder may have added buttons
        if (fresh != null) remote = fresh;
        render();
    }

    private void render() {
        grid.removeAllViews();
        if (remote.buttons.isEmpty()) {
            grid.addView(Ui.hint(this, "No buttons yet. Add one with a code, or use the Code finder "
                    + "to discover your device's codes and save them here."));
            return;
        }
        View[] views = new View[remote.buttons.size()];
        for (int i = 0; i < views.length; i++) {
            final int index = i;
            Remote.Button b = remote.buttons.get(i);
            Button btn = Ui.key(this, b.label, v -> IrSender.sendCode(this, b.code));
            btn.setOnLongClickListener(v -> {
                edit(index);
                return true;
            });
            views[i] = btn;
        }
        Ui.grid(this, grid, views, 3);
    }

    private void edit(int index) {
        Remote.Button existing = index >= 0 ? remote.buttons.get(index) : null;
        LinearLayout form = Ui.column(this);
        int p = Ui.dp(this, 20);
        form.setPadding(p, p / 2, p, 0);
        EditText label = new EditText(this);
        label.setHint("Label, e.g. Power, Speed, Timer");
        EditText code = new EditText(this);
        code.setHint("Code, e.g. NEC:00:45");
        code.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        if (existing != null) {
            label.setText(existing.label);
            code.setText(existing.code);
        }
        form.addView(label, Ui.matchWrap());
        form.addView(code, Ui.matchWrap());
        form.addView(Ui.hint(this, com.universal.irremote.ir.IrCodes.HELP));
        Button test = Ui.key(this, "Test send", v -> IrSender.sendCode(this, code.getText().toString()));
        form.addView(test, Ui.spaced(this));

        AlertDialog.Builder dlg = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "New button" : "Edit button")
                .setView(form)
                .setPositiveButton("Save", (d, w) -> {
                    String l = label.getText().toString().trim();
                    String c = code.getText().toString().trim();
                    if (c.isEmpty()) return;
                    if (existing == null) {
                        remote.buttons.add(new Remote.Button(l.isEmpty() ? "Button" : l, c));
                    } else {
                        existing.label = l;
                        existing.code = c;
                    }
                    remote.save(this);
                    render();
                })
                .setNegativeButton("Cancel", null);
        if (existing != null) {
            dlg.setNeutralButton("Delete", (d, w) -> {
                remote.buttons.remove(index);
                remote.save(this);
                render();
            });
        }
        dlg.show();
    }
}
