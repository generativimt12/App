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
        int[] colors = {Color.rgb(91, 79, 255), Color.rgb(157, 92, 255), Color.rgb(45, 111, 255), Color.rgb(40, 197, 255), Color.rgb(209, 102, 255)};
        return colors[Math.max(0, Math.min(colors.length - 1, SettingsStore.accent(c)))];
    }

    public static int bg(Context c) { return SettingsStore.dark(c) ? Color.rgb(8,10,18) : Color.rgb(244,246,252); }
    public static int card(Context c) { return SettingsStore.dark(c) ? Color.rgb(28,30,43) : Color.rgb(255,255,255); }
    public static int ink(Context c) { return SettingsStore.dark(c) ? Color.rgb(244,246,250) : Color.rgb(27,30,37); }
    public static int muted(Context c) { return SettingsStore.dark(c) ? Color.rgb(172,176,195) : Color.rgb(94,99,116); }
    public static int soft(Context c) {
        int a = accent(c), r=Color.red(a), g=Color.green(a), b=Color.blue(a);
        return Color.rgb((r+255)/2,(g+255)/2,(b+255)/2);
    }

    public static GradientDrawable glass(Context c, float radius){
        GradientDrawable d=new GradientDrawable();
        int fill=SettingsStore.dark(c)?0x2AFFFFFF:0x70FFFFFF;
        d.setColor(fill);
        d.setCornerRadius(radius);
        d.setStroke(1,SettingsStore.dark(c)?0x35FFFFFF:0x9AFFFFFF);
        return d;
    }

    public static int dp(Context c,float v){return (int)(v*c.getResources().getDisplayMetrics().density+0.5f);}
    public static GradientDrawable rounded(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(radius); return d;
    }

    public static TextView text(Context c, String s, float size) {
        TextView t = new TextView(c); t.setText(s); t.setTextSize(size); t.setTextColor(ink(c)); return t;
    }

    public static Button button(Context c, String label) {
        Button b = new Button(c); b.setText(label); b.setAllCaps(false); b.setTextSize(14);
        b.setTextColor(ink(c)); b.setBackground(glass(c, 28)); b.setMinHeight(0); b.setMinWidth(0);
        return b;
    }

    public static Button filled(Context c, String label) {
        Button b = button(c, label); b.setTextColor(Color.WHITE); b.setBackground(rounded(accent(c), 28)); return b;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w,h); }
    public static LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0,-2,1); }
}
