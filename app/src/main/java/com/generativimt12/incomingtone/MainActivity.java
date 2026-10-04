package com.generativimt12.incomingtone;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.text.DateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final int ROLE_REQUEST = 42;
    private static final int PERM_REQUEST = 43;
    private EditText number;
    private LinearLayout content;
    private int page = 0;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        build();
        handleDialIntent(getIntent());
        requestDataPermissions();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDialIntent(intent);
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 18);

        TextView title = new TextView(this);
        title.setText("Incoming Tone — טלפון");
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        number = new EditText(this);
        number.setHint("מספר לחיוג");
        number.setTextSize(24);
        number.setInputType(2);
        root.addView(number, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout tabs = new LinearLayout(this);
        Button dialTab = tab("חייגן", 0), callsTab = tab("יומן", 1), contactsTab = tab("אנשי קשר", 2);
        tabs.addView(dialTab, weight());
        tabs.addView(callsTab, weight());
        tabs.addView(contactsTab, weight());
        root.addView(tabs);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        dialTab.setOnClickListener(v -> { page=0; showPage(); });
        callsTab.setOnClickListener(v -> { page=1; showPage(); });
        contactsTab.setOnClickListener(v -> { page=2; showPage(); });

        Button role = new Button(this);
        role.setText("הגדר כאפליקציית הטלפון");
        role.setOnClickListener(v -> requestDialerRole());
        root.addView(role);

        Button settings = new Button(this);
        settings.setText("הגדרות אפליקציות ברירת מחדל");
        settings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)));
        root.addView(settings);

        setContentView(root);
        showPage();
    }

    private LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0, -2, 1); }
    private Button tab(String s, int ignored) { Button b=new Button(this); b.setText(s); return b; }

    private void showPage() {
        content.removeAllViews();
        if (page == 0) showDialer();
        else if (page == 1) showCallLog();
        else showContacts();
    }

    private void showDialer() {
        String[][] keys={{"1","2","3"},{"4","5","6"},{"7","8","9"},{"*","0","#"}};
        for (String[] row : keys) {
            LinearLayout line=new LinearLayout(this);
            for (String k:row) {
                Button b=new Button(this); b.setText(k); b.setTextSize(22);
                b.setOnClickListener(v -> number.append(((Button)v).getText()));
                line.addView(b, weight());
            }
            content.addView(line);
        }
        LinearLayout actions=new LinearLayout(this);
        Button call=new Button(this); call.setText("📞 התקשר");
        call.setOnClickListener(v -> placeCall(number.getText().toString().trim()));
        Button clear=new Button(this); clear.setText("⌫ מחק");
        clear.setOnClickListener(v -> { String s=number.getText().toString(); if(!s.isEmpty()) number.setText(s.substring(0,s.length()-1)); });
        Button add=new Button(this); add.setText("👤 איש קשר חדש");
        add.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)));
        actions.addView(call,weight()); actions.addView(clear,weight()); actions.addView(add,weight());
        content.addView(actions);
    }

    private void placeCall(String raw) {
        if (raw.isEmpty()) return;
        if (!isDefaultDialer()) { requestDialerRole(); return; }
        try {
            TelecomManager tm=getSystemService(TelecomManager.class);
            if (tm != null) {
                tm.placeCall(Uri.parse("tel:" + Uri.encode(raw)), null);
                number.setText("");
            }
        } catch (SecurityException e) {
            Toast.makeText(this,"אין הרשאת חיוג",Toast.LENGTH_LONG).show();
        }
    }

    private boolean isDefaultDialer() {
        TelecomManager tm=getSystemService(TelecomManager.class);
        return tm != null && getPackageName().equals(tm.getDefaultDialerPackage());
    }

    private void showCallLog() {
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG)!=PackageManager.PERMISSION_GRANTED) {
            TextView t=info("יש לאשר הרשאת יומן שיחות.");
            content.addView(t); return;
        }
        Cursor c=null;
        try {
            c=getContentResolver().query(CallLog.Calls.CONTENT_URI,
                    new String[]{CallLog.Calls._ID,CallLog.Calls.NUMBER,CallLog.Calls.CACHED_NAME,
                            CallLog.Calls.DATE,CallLog.Calls.TYPE,CallLog.Calls.DURATION},
                    null,null,CallLog.Calls.DATE+" DESC");
            if(c!=null) while(c.moveToNext()) {
                final String n=c.getString(1);
                String name=c.getString(2);
                long date=c.getLong(3);
                int type=c.getInt(4);
                long duration=c.getLong(5);
                String label=(name==null||name.isEmpty()?n:name)+"\n"+callType(type)+" · "+DateFormat.getDateTimeInstance().format(new Date(date))+" · "+duration+" שנ׳";
                Button b=new Button(this); b.setText(label); b.setGravity(Gravity.START); b.setOnClickListener(v->placeCall(n));
                content.addView(b,new LinearLayout.LayoutParams(-1,-2));
            }
        } finally { if(c!=null)c.close(); }
    }

    private String callType(int t) {
        if(t==CallLog.Calls.INCOMING_TYPE)return"נכנסת";
        if(t==CallLog.Calls.OUTGOING_TYPE)return"יוצאת";
        if(t==CallLog.Calls.MISSED_TYPE)return"נענתה/לא נענתה";
        if(t==CallLog.Calls.REJECTED_TYPE)return"נדחתה";
        return"שיחה";
    }

    private void showContacts() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED) {
            content.addView(info("יש לאשר הרשאת אנשי קשר.")); return;
        }
        EditText search=new EditText(this); search.setHint("חיפוש שם או מספר");
        content.addView(search);
        Button newContact=new Button(this); newContact.setText("➕ איש קשר חדש");
        newContact.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)));
        content.addView(newContact);
        LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); content.addView(list);
        search.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){ loadContacts(list,s.toString()); }
            public void afterTextChanged(android.text.Editable e){}
        });
        loadContacts(list,"");
    }

    private void loadContacts(LinearLayout list,String q) {
        list.removeAllViews();
        String sel=null; String[] args=null;
        if(!q.trim().isEmpty()){ sel=ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" LIKE ? OR "+ContactsContract.CommonDataKinds.Phone.NUMBER+" LIKE ?"; args=new String[]{"%"+q+"%","%"+q+"%"}; }
        Cursor c=null;
        try {
            c=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER},
                    sel,args,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" COLLATE NOCASE");
            if(c!=null) while(c.moveToNext()){
                String name=c.getString(0), n=c.getString(1);
                Button b=new Button(this); b.setText(name+"\n"+n); b.setGravity(Gravity.START); b.setOnClickListener(v->placeCall(n));
                list.addView(b,new LinearLayout.LayoutParams(-1,-2));
            }
        } finally { if(c!=null)c.close(); }
    }

    private TextView info(String s){TextView t=new TextView(this);t.setText(s);t.setTextSize(18);t.setPadding(16,24,16,24);return t;}

    private void requestDataPermissions() {
        if(android.os.Build.VERSION.SDK_INT>=23) requestPermissions(new String[]{
                Manifest.permission.READ_CONTACTS,Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.READ_CALL_LOG,Manifest.permission.WRITE_CALL_LOG,
                Manifest.permission.CALL_PHONE,Manifest.permission.READ_PHONE_STATE},PERM_REQUEST);
    }

    private void requestDialerRole() {
        if(android.os.Build.VERSION.SDK_INT>=29){
            RoleManager rm=getSystemService(RoleManager.class);
            if(rm!=null && rm.isRoleAvailable(RoleManager.ROLE_DIALER) && !rm.isRoleHeld(RoleManager.ROLE_DIALER))
                startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER),ROLE_REQUEST);
        } else {
            Intent i=new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);
            i.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,getPackageName());
            startActivity(i);
        }
    }

    private void handleDialIntent(Intent i){
        if(i!=null && Intent.ACTION_DIAL.equals(i.getAction()) && i.getData()!=null){
            String s=i.getData().getSchemeSpecificPart();
            if(s!=null) number.setText(s);
        }
    }
}
