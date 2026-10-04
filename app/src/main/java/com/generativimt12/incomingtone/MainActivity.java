package com.generativimt12.incomingtone;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
    private static final int ROLE_REQUEST=42, PERM_REQUEST=43;
    private EditText number, search;
    private LinearLayout content, nav;
    private int page=0;
    private int bg=Color.rgb(248,249,252), card=Color.WHITE, ink=Color.rgb(28,31,38), muted=Color.rgb(105,111,125), accent=Color.rgb(52,92,240), soft=Color.rgb(232,237,255);

    @Override protected void onCreate(Bundle s){super.onCreate(s); getWindow().setStatusBarColor(bg); getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); build(); handleDialIntent(getIntent()); requestDataPermissions();}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handleDialIntent(i);}

    private GradientDrawable shape(int color,float r){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(r);return d;}
    private TextView text(String s,float size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    private LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,-2,1);}

    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(bg);root.setPadding(20,18,20,10);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout brand=new LinearLayout(this);brand.setOrientation(LinearLayout.VERTICAL);
        TextView title=text("טלפון",28,ink);title.setTypeface(null,1);brand.addView(title);
        TextView sub=text("מהיר • פשוט • בשליטתך",13,muted);brand.addView(sub);
        top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        Button settings=iconButton("⚙");settings.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)));top.addView(settings,lp(54,54));root.addView(top);

        number=new EditText(this);number.setHint("הזן מספר טלפון");number.setTextSize(22);number.setTextColor(ink);number.setHintTextColor(muted);number.setSingleLine();number.setGravity(Gravity.CENTER);number.setPadding(20,0,20,0);number.setInputType(3);number.setBackground(shape(card,28));LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,64);np.setMargins(0,18,0,12);root.addView(number,np);

        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(4,8,4,2);
        String[] labels={"⌨\nחייגן","◷\nיומן","●\nאנשי קשר"};
        for(int i=0;i<3;i++){final int p=i;TextView b=text(labels[i],13,muted);b.setGravity(Gravity.CENTER);b.setPadding(4,7,4,5);b.setOnClickListener(v->{page=p;showPage();});nav.addView(b,weight());}
        root.addView(nav);setContentView(root);showPage();
    }

    private Button iconButton(String s){Button b=new Button(this);b.setText(s);b.setTextSize(20);b.setTextColor(ink);b.setBackground(shape(card,30));return b;}

    private void showPage(){content.removeAllViews(); if(page==0)showDialer(); else if(page==1)showCallLog(); else showContacts(); updateNav();}
    private void updateNav(){for(int i=0;i<nav.getChildCount();i++){TextView t=(TextView)nav.getChildAt(i);t.setTextColor(i==page?accent:muted);}}

    private void showDialer(){
        TextView h=text("חייגן",24,ink);h.setTypeface(null,1);content.addView(h);
        TextView hint=text("התקשר למספר במהירות",14,muted);hint.setPadding(0,2,0,12);content.addView(hint);
        String[][] keys={{"1","2","3"},{"4","5","6"},{"7","8","9"},{"*","0","#"}};
        for(String[] row:keys){LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER);for(String k:row){Button b=new Button(this);b.setText(k);b.setTextSize(23);b.setTextColor(ink);b.setBackground(shape(card,22));b.setOnClickListener(v->number.append(((Button)v).getText()));LinearLayout.LayoutParams p=weight();p.setMargins(5,5,5,5);line.addView(b,p);}content.addView(line,new LinearLayout.LayoutParams(-1,62));}
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER);
        Button call=action("●","התקשר",accent,Color.WHITE);call.setOnClickListener(v->placeCall(number.getText().toString().trim()));
        Button del=action("⌫","מחק",card,ink);del.setOnClickListener(v->{String s=number.getText().toString();if(!s.isEmpty())number.setText(s.substring(0,s.length()-1));});
        Button add=action("+","איש קשר",card,ink);add.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_INSERT,ContactsContract.Contacts.CONTENT_URI)));
        actions.addView(call,weight());actions.addView(del,weight());actions.addView(add,weight());content.addView(actions);
    }

    private Button action(String icon,String label,int color,int tc){Button b=new Button(this);b.setText(icon+"  "+label);b.setTextSize(14);b.setTextColor(tc);b.setAllCaps(false);b.setBackground(shape(color,24));return b;}

    private void showCallLog(){
        TextView h=text("יומן שיחות",24,ink);h.setTypeface(null,1);content.addView(h);
        TextView hint=text("השיחות האחרונות שלך",14,muted);hint.setPadding(0,2,0,12);content.addView(hint);
        if(checkSelfPermission(Manifest.permission.READ_CALL_LOG)!=PackageManager.PERMISSION_GRANTED){content.addView(info("כדי להציג את היומן, אשר הרשאת שיחות."));return;}
        Cursor c=null;try{c=getContentResolver().query(CallLog.Calls.CONTENT_URI,new String[]{CallLog.Calls.NUMBER,CallLog.Calls.CACHED_NAME,CallLog.Calls.DATE,CallLog.Calls.TYPE,CallLog.Calls.DURATION},null,null,CallLog.Calls.DATE+" DESC");
            if(c!=null)while(c.moveToNext()){String n=c.getString(0),name=c.getString(1);long date=c.getLong(2),dur=c.getLong(4);String label=(name==null||name.isEmpty()?n:name);String detail=callType(c.getInt(3))+"  •  "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(date))+"  •  "+dur+" שנ׳";addCallCard(label,detail,n);}
        }finally{if(c!=null)c.close();}
    }
    private void addCallCard(String name,String detail,String n){LinearLayout box=new LinearLayout(this);box.setGravity(Gravity.CENTER_VERTICAL);box.setPadding(18,10,12,10);box.setBackground(shape(card,22));LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);TextView a=text(name,17,ink);a.setTypeface(null,1);TextView d=text(detail,12,muted);tx.addView(a);tx.addView(d);box.addView(tx,new LinearLayout.LayoutParams(0,-2,1));Button call=iconButton("☎");call.setOnClickListener(v->placeCall(n));box.addView(call,lp(52,52));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,9);content.addView(box,p);}

    private void showContacts(){
        TextView h=text("אנשי קשר",24,ink);h.setTypeface(null,1);content.addView(h);
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
        search=new EditText(this);search.setHint("חיפוש שם או מספר");search.setSingleLine();search.setTextSize(16);search.setBackground(shape(card,24));search.setPadding(18,0,18,0);head.addView(search,new LinearLayout.LayoutParams(0,58,1));
        Button add=iconButton("+");add.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_INSERT,ContactsContract.Contacts.CONTENT_URI)));head.addView(add,lp(58,58));content.addView(head);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);
        if(checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED){list.addView(info("כדי להציג אנשי קשר, אשר הרשאת אנשי קשר."));return;}
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){loadContacts(list,s.toString());}public void afterTextChanged(android.text.Editable e){}});loadContacts(list,"");
    }
    private void loadContacts(LinearLayout list,String q){list.removeAllViews();String sel=null;String[] args=null;if(!q.trim().isEmpty()){sel=ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" LIKE ? OR "+ContactsContract.CommonDataKinds.Phone.NUMBER+" LIKE ?";args=new String[]{"%"+q+"%","%"+q+"%"};}Cursor c=null;try{c=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER},sel,args,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" COLLATE NOCASE");if(c!=null)while(c.moveToNext()){String name=c.getString(0),n=c.getString(1);addContactCard(list,name==null?"ללא שם":name,n);}}finally{if(c!=null)c.close();}}
    private void addContactCard(LinearLayout list,String name,String n){LinearLayout box=new LinearLayout(this);box.setGravity(Gravity.CENTER_VERTICAL);box.setPadding(15,9,10,9);box.setBackground(shape(card,22));TextView avatar=text(name.substring(0,1).toUpperCase(),19,Color.WHITE);avatar.setGravity(Gravity.CENTER);avatar.setBackground(shape(accent,50));box.addView(avatar,lp(48,48));LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(14,0,0,0);TextView a=text(name,17,ink);a.setTypeface(null,1);TextView b=text(n,13,muted);tx.addView(a);tx.addView(b);box.addView(tx,new LinearLayout.LayoutParams(0,-2,1));Button call=iconButton("☎");call.setOnClickListener(v->placeCall(n));box.addView(call,lp(52,52));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,8);list.addView(box,p);}

    private void placeCall(String raw){if(raw.isEmpty()){Toast.makeText(this,"הזן מספר",Toast.LENGTH_SHORT).show();return;}if(!isDefaultDialer()){requestDialerRole();return;}try{TelecomManager tm=getSystemService(TelecomManager.class);if(tm!=null)tm.placeCall(Uri.parse("tel:"+Uri.encode(raw)),null);number.setText("");}catch(SecurityException e){Toast.makeText(this,"אין הרשאת חיוג",Toast.LENGTH_LONG).show();}}
    private boolean isDefaultDialer(){TelecomManager tm=getSystemService(TelecomManager.class);return tm!=null&&getPackageName().equals(tm.getDefaultDialerPackage());}
    private String callType(int t){if(t==CallLog.Calls.INCOMING_TYPE)return"נכנסת";if(t==CallLog.Calls.OUTGOING_TYPE)return"יוצאת";if(t==CallLog.Calls.MISSED_TYPE)return"שיחה שלא נענתה";if(t==CallLog.Calls.REJECTED_TYPE)return"נדחתה";return"שיחה";}
    private TextView info(String s){TextView t=text(s,15,muted);t.setPadding(16,28,16,28);return t;}
    private void requestDataPermissions(){if(android.os.Build.VERSION.SDK_INT>=23)requestPermissions(new String[]{Manifest.permission.READ_CONTACTS,Manifest.permission.WRITE_CONTACTS,Manifest.permission.READ_CALL_LOG,Manifest.permission.WRITE_CALL_LOG,Manifest.permission.CALL_PHONE,Manifest.permission.READ_PHONE_STATE},PERM_REQUEST);}
    private void requestDialerRole(){if(android.os.Build.VERSION.SDK_INT>=29){RoleManager rm=getSystemService(RoleManager.class);if(rm!=null&&rm.isRoleAvailable(RoleManager.ROLE_DIALER)&&!rm.isRoleHeld(RoleManager.ROLE_DIALER))startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER),ROLE_REQUEST);}else{Intent i=new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);i.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,getPackageName());startActivity(i);}}
    private void handleDialIntent(Intent i){if(i!=null&&Intent.ACTION_DIAL.equals(i.getAction())&&i.getData()!=null){String s=i.getData().getSchemeSpecificPart();if(s!=null)number.setText(s);}}
}
