package com.generativimt12.incomingtone;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int ROLE_REQUEST = 42;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        showUi();
    }

    private void showUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 64, 48, 48);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("Incoming Tone");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView info = new TextView(this);
        info.setText("האפליקציה משמיעה צליל צלצול עצמאי לשיחות נכנסות.\n\nכדי שאנדרואיד יאפשר לאפליקציה לשלוט בצלצול של שיחות סלולריות, יש להגדיר אותה כאפליקציית הטלפון המוגדרת כברירת מחדל. אין צורך בהרשאת נגישות.");
        info.setTextSize(17);
        info.setPadding(0, 40, 0, 40);
        root.addView(info, new LinearLayout.LayoutParams(-1, -2));

        Button role = new Button(this);
        role.setText("הגדר כאפליקציית הטלפון");
        role.setOnClickListener(v -> requestDialerRole());
        root.addView(role, new LinearLayout.LayoutParams(-1, -2));

        Button settings = new Button(this);
        settings.setText("פתח הגדרות אפליקציות טלפון");
        settings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)));
        root.addView(settings, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
    }

    private void requestDialerRole() {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            RoleManager rm = getSystemService(RoleManager.class);
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_DIALER)
                    && !rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
                startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER), ROLE_REQUEST);
            }
        } else {
            Intent i = new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);
            i.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, getPackageName());
            startActivity(i);
        }
    }
}
