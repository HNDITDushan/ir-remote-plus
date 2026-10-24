package com.universal.irremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.universal.irremote.ir.IrSender;

import java.util.List;

public class MainActivity extends Activity {
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout page = Ui.page(this);
        page.addView(Ui.title(this, "IR Remote Plus"));

        boolean hasIr = IrSender.hasEmitter(this);
        TextView status = Ui.card(this, hasIr
                ? "✅ IR blaster found. Point the top of the phone at the device."
                : "⚠️ No IR blaster detected on this phone. Buttons will not work.");
        page.addView(status, Ui.spaced(this));

        page.addView(Ui.primary(this, "＋  Add air conditioner remote", v -> addRemote(Remote.TYPE_AC)), Ui.spaced(this));
        page.addView(Ui.primary(this, "＋  Add fan / TV / custom remote", v -> addRemote(Remote.TYPE_BUTTONS)), Ui.spaced(this));
        page.addView(Ui.key(this, "🔍  Code finder (find codes for unknown brands)",
                v -> startActivity(new Intent(this, FinderActivity.class))), Ui.spaced(this));

        TextView header = Ui.title(this, "My remotes");
        header.setTextSize(18);
        page.addView(header);
        list = Ui.column(this);
        page.addView(list, Ui.matchWrap());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        list.removeAllViews();
        List<Remote> remotes = Remote.loadAll(this);
        if (remotes.isEmpty()) {
            list.addView(Ui.hint(this, "No remotes yet. Add one above."));
        }
        for (Remote r : remotes) {
            String icon = Remote.TYPE_AC.equals(r.type) ? "❄️  " : "🎛️  ";
            TextView item = Ui.card(this, icon + r.name);
            item.setOnClickListener(v -> open(r));
            item.setOnLongClickListener(v -> {
                manage(r);
                return true;
            });
            list.addView(item, Ui.spaced(this));
        }
        if (!remotes.isEmpty()) list.addView(Ui.hint(this, "Long-press a remote to rename or delete it."));
    }

    private void open(Remote r) {
        Class<?> cls = Remote.TYPE_AC.equals(r.type) ? AcActivity.class : ButtonsActivity.class;
        startActivity(new Intent(this, cls).putExtra("id", r.id));
    }

    private void addRemote(String type) {
        EditText name = new EditText(this);
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        name.setHint(Remote.TYPE_AC.equals(type) ? "e.g. DB AC – Bedroom" : "e.g. Inovax Fan");
        new AlertDialog.Builder(this)
                .setTitle("Remote name")
                .setView(pad(name))
                .setPositiveButton("Create", (d, w) -> {
                    String n = name.getText().toString().trim();
                    Remote r = Remote.create(n.isEmpty() ? "My remote" : n, type);
                    r.save(this);
                    open(r);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void manage(Remote r) {
        new AlertDialog.Builder(this)
                .setTitle(r.name)
                .setItems(new String[]{"Rename", "Delete"}, (d, which) -> {
                    if (which == 0) rename(r);
                    else confirmDelete(r);
                })
                .show();
    }

    private void rename(Remote r) {
        EditText name = new EditText(this);
        name.setText(r.name);
        new AlertDialog.Builder(this)
                .setTitle("Rename")
                .setView(pad(name))
                .setPositiveButton("Save", (d, w) -> {
                    r.name = name.getText().toString().trim();
                    r.save(this);
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete(Remote r) {
        new AlertDialog.Builder(this)
                .setMessage("Delete \"" + r.name + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    r.delete(this);
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    LinearLayout pad(android.view.View v) {
        LinearLayout l = Ui.column(this);
        int p = Ui.dp(this, 20);
        l.setPadding(p, p / 2, p, 0);
        l.addView(v, Ui.matchWrap());
        return l;
    }
}
