package com.universal.irremote;

import android.content.Context;
import android.content.SharedPreferences;

import com.universal.irremote.ir.Protocols;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** A saved remote: either an air-conditioner remote or a set of custom buttons. */
public final class Remote {
    public static final String TYPE_AC = "ac";
    public static final String TYPE_BUTTONS = "buttons";
    public static final String AC_GREE = "GREE";
    public static final String AC_COOLIX = "COOLIX";
    public static final String AC_ELECTRA = "ELECTRA";

    public static final class Button {
        public String label;
        public String code;

        public Button(String label, String code) {
            this.label = label;
            this.code = code;
        }
    }

    public long id;
    public String name;
    public String type;

    // Air-conditioner state (the whole state is sent on every key press).
    public String acProtocol = AC_GREE;
    public boolean power = false;
    public int mode = Protocols.MODE_COOL;
    public int temp = 24;
    public int fan = Protocols.FAN_AUTO;
    public boolean swing = false;

    public final List<Button> buttons = new ArrayList<>();

    // ------------------------------------------------------------- storage

    private static final String PREFS = "remotes";
    private static final String KEY = "list";

    public static List<Remote> loadAll(Context ctx) {
        List<Remote> out = new ArrayList<>();
        String json = prefs(ctx).getString(KEY, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) out.add(fromJson(arr.getJSONObject(i)));
        } catch (JSONException ignored) {
            // Corrupt data: start fresh rather than crash.
        }
        return out;
    }

    public static Remote find(Context ctx, long id) {
        for (Remote r : loadAll(ctx)) if (r.id == id) return r;
        return null;
    }

    public void save(Context ctx) {
        List<Remote> all = loadAll(ctx);
        boolean replaced = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id == id) {
                all.set(i, this);
                replaced = true;
            }
        }
        if (!replaced) all.add(this);
        writeAll(ctx, all);
    }

    public void delete(Context ctx) {
        List<Remote> all = loadAll(ctx);
        all.removeIf(r -> r.id == id);
        writeAll(ctx, all);
    }

    public static Remote create(String name, String type) {
        Remote r = new Remote();
        r.id = System.currentTimeMillis();
        r.name = name;
        r.type = type;
        return r;
    }

    private static void writeAll(Context ctx, List<Remote> all) {
        JSONArray arr = new JSONArray();
        try {
            for (Remote r : all) arr.put(r.toJson());
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        prefs(ctx).edit().putString(KEY, arr.toString()).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id).put("name", name).put("type", type)
                .put("acProtocol", acProtocol).put("power", power).put("mode", mode)
                .put("temp", temp).put("fan", fan).put("swing", swing);
        JSONArray b = new JSONArray();
        for (Button btn : buttons) b.put(new JSONObject().put("label", btn.label).put("code", btn.code));
        o.put("buttons", b);
        return o;
    }

    private static Remote fromJson(JSONObject o) throws JSONException {
        Remote r = new Remote();
        r.id = o.getLong("id");
        r.name = o.optString("name", "Remote");
        r.type = o.optString("type", TYPE_BUTTONS);
        r.acProtocol = o.optString("acProtocol", AC_GREE);
        r.power = o.optBoolean("power");
        r.mode = o.optInt("mode", Protocols.MODE_COOL);
        r.temp = o.optInt("temp", 24);
        r.fan = o.optInt("fan", Protocols.FAN_AUTO);
        r.swing = o.optBoolean("swing");
        JSONArray b = o.optJSONArray("buttons");
        if (b != null) {
            for (int i = 0; i < b.length(); i++) {
                JSONObject bo = b.getJSONObject(i);
                r.buttons.add(new Button(bo.optString("label"), bo.optString("code")));
            }
        }
        return r;
    }
}
