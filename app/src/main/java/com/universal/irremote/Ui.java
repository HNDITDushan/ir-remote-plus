package com.universal.irremote;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Small helpers for building the UI in code. */
final class Ui {
    static final int BG = Color.parseColor("#F3F5F9");
    static final int PRIMARY = Color.parseColor("#1F6FEB");
    static final int DANGER = Color.parseColor("#D73A49");
    static final int CARD = Color.WHITE;
    static final int TEXT = Color.parseColor("#1B1F24");
    static final int MUTED = Color.parseColor("#5B6470");

    private Ui() {}

    static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** A scrolling, padded vertical page. Returns the inner column. */
    static LinearLayout page(android.app.Activity a) {
        ScrollView scroll = new ScrollView(a);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);
        LinearLayout col = column(a);
        int p = dp(a, 16);
        col.setPadding(p, p, p, p);
        scroll.addView(col);
        a.setContentView(scroll);
        return col;
    }

    static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    static TextView title(Context c, String s) {
        TextView t = text(c, s, 22, TEXT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(c, 8), 0, dp(c, 8));
        return t;
    }

    static TextView text(Context c, String s, int sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    static TextView hint(Context c, String s) {
        TextView t = text(c, s, 14, MUTED);
        t.setPadding(0, dp(c, 4), 0, dp(c, 8));
        return t;
    }

    static GradientDrawable rounded(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    static Button button(Context c, String label, int bg, int fg, View.OnClickListener click) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(fg);
        b.setStateListAnimator(null);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33000000),
                rounded(bg, dp(c, 14)), null));
        b.setOnClickListener(click);
        b.setMinHeight(dp(c, 56));
        return b;
    }

    static Button primary(Context c, String label, View.OnClickListener click) {
        return button(c, label, PRIMARY, Color.WHITE, click);
    }

    static Button key(Context c, String label, View.OnClickListener click) {
        return button(c, label, CARD, TEXT, click);
    }

    /** Lays out views in rows of {@code perRow} equal-width cells. */
    static void grid(Context c, LinearLayout parent, View[] views, int perRow) {
        LinearLayout r = null;
        for (int i = 0; i < views.length; i++) {
            if (i % perRow == 0) {
                r = row(c);
                parent.addView(r, matchWrap());
            }
            r.addView(views[i], cell(c));
        }
        if (r != null) {
            for (int i = views.length % perRow; i != 0 && i < perRow; i++) {
                r.addView(new View(c), cell(c));
            }
        }
    }

    static LinearLayout.LayoutParams cell(Context c) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(c, 64), 1f);
        int m = dp(c, 5);
        lp.setMargins(m, m, m, m);
        return lp;
    }

    static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    static LinearLayout.LayoutParams spaced(Context c) {
        LinearLayout.LayoutParams lp = matchWrap();
        lp.setMargins(0, dp(c, 6), 0, dp(c, 6));
        return lp;
    }

    static TextView card(Context c, String s) {
        TextView t = text(c, s, 16, TEXT);
        int p = dp(c, 16);
        t.setPadding(p, p, p, p);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setBackground(rounded(CARD, dp(c, 14)));
        return t;
    }
}
