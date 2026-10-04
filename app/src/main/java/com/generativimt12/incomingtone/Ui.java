package com.generativimt12.incomingtone;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public final class Ui {
    private Ui() {}

    public static int accent(Context c) {
        int[] colors = {Color.rgb(55, 105, 245), Color.rgb(124, 78, 220), Color.rgb(12, 155, 112), Color.rgb(232, 95, 71), Color.rgb(214, 137, 25)};
        return colors[Math.max(0, Math.min(colors.length - 1, SettingsStore.accent(c)))];
    }

    public static int bg(Context c) { return SettingsStore.dark(c) ? Color.rgb(16,18,22) : Color.rgb(247,248,252); }
    public static int card(Context c) { return SettingsStore.dark(c) ? Color.rgb(28,31,37) : Color.WHITE; }
    public static int ink(Context c) { return SettingsStore.dark(c) ? Color.rgb(244,246,250) : Color.rgb(27,30,37); }
    public static int muted(Context c) { return SettingsStore.dark(c) ? Color.rgb(166,172,185) : Color.rgb(103,109,123); }
    public static int soft(Context c) {
        int a = accent(c), r=Color.red(a), g=Color.green(a), b=Color.blue(a);
        return Color.rgb((r+255)/2,(g+255)/2,(b+255)/2);
    }

    public static GradientDrawable rounded(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(radius); return d;
    }

    public static TextView text(Context c, String s, float size) {
        TextView t = new TextView(c); t.setText(s); t.setTextSize(size); t.setTextColor(ink(c)); return t;
    }

    public static Button button(Context c, String label) {
        Button b = new Button(c); b.setText(label); b.setAllCaps(false); b.setTextSize(14);
        b.setTextColor(ink(c)); b.setBackground(rounded(card(c), 28)); b.setMinHeight(0); b.setMinWidth(0);
        return b;
    }

    public static Button filled(Context c, String label) {
        Button b = button(c, label); b.setTextColor(Color.WHITE); b.setBackground(rounded(accent(c), 28)); return b;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w,h); }
    public static LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0,-2,1); }
}
