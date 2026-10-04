package com.generativimt12.incomingtone;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.text.DateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final int ROLE_REQUEST=42, PERM_REQUEST=43;
    private EditText number, globalSearch;
    private LinearLayout content, nav;
    private int page=0;
    private String query="";

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        applyBars();
        build();
        handleDialIntent(getIntent());
        requestDataPermissions();
    }

    @Override protected void onResume(){ super.onResume(); applyBars(); if(content!=null) showPage(); }
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handleDialIntent(i);}

    private void applyBars(){
        boolean dark=SettingsStore.dark(this);
        getWindow().setStatusBarColor(Ui.bg(this));
        getWindow().setNavigationBarColor(Ui.bg(this));
        getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    private void build(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(18,14,18,8); root.setBackgroundColor(Ui.bg(this));

        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout brand=new LinearLayout(this); brand.setOrientation(LinearLayout.VERTICAL);
        TextView title=Ui.text(this,"טלפון",28); title.setTypeface(null,1); brand.addView(title);
        TextView sub=Ui.text(this,"חייגן אישי • צלצול עצמאי • פשוט",12); sub.setTextColor(Ui.muted(this)); brand.addView(sub);
        top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        Button settings=Ui.button(this,"⚙"); settings.setTextSize(21); settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class))); top.addView(settings,Ui.lp(52,52));
        root.addView(top);

        globalSearch=new EditText(this); globalSearch.setHint("חיפוש בכל הטלפון"); globalSearch.setSingleLine(); globalSearch.setTextSize(16); globalSearch.setTextColor(Ui.ink(this)); globalSearch.setHintTextColor(Ui.muted(this)); globalSearch.setPadding(18,0,18,0); globalSearch.setBackground(Ui.rounded(Ui.card(this),26));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,54);sp.setMargins(0,14,0,10);root.addView(globalSearch,sp);
        globalSearch.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int d){} public void onTextChanged(CharSequence s,int a,int b,int c){query=s.toString();showPage();} public void afterTextChanged(Editable e){}});

        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.addView(content); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        nav=new LinearLayout(this); nav.setGravity(Gravity.CENTER); nav.setPadding(2,7,2,0);
        String[] labels={"⌨\nחייגן","★\nמועדפים","◷\nיומן","●\nאנשי קשר"};
        for(int i=0;i<4;i++){final int p=i;TextView b=Ui.text(this,labels[i],12);b.setGravity(Gravity.CENTER);b.setPadding(3,6,3,4);b.setOnClickListener(v->{page=p;query="";globalSearch.setText("");showPage();});nav.addView(b,Ui.weight());}
        root.addView(nav); setContentView(root); showPage();
    }

    private void showPage(){
        content.removeAllViews();
        if(query.trim().length()>0){showSearchResults(query.trim());updateNav();return;}
        if(page==0)showDialer(); else if(page==1)showFavorites(); else if(page==2)showCallLog(); else showContacts();
        updateNav();
    }

    private void updateNav(){for(int i=0;i<nav.getChildCount();i++){TextView t=(TextView)nav.getChildAt(i);t.setTextColor(i==page?Ui.accent(this):Ui.muted(this));}}

    private void showDialer(){
        TextView h=Ui.text(this,"חייגן",24);h.setTypeface(null,1);content.addView(h);
        TextView hint=Ui.text(this,"הקלד מספר והתקשר בלחיצה אחת",13);hint.setTextColor(Ui.muted(this));hint.setPadding(0,2,0,10);content.addView(hint);
        number=new EditText(this);number.setHint("מספר טלפון");number.setGravity(Gravity.CENTER);number.setTextSize(23);number.setTextColor(Ui.ink(this));number.setHintTextColor(Ui.muted(this));number.setSingleLine();number.setInputType(3);number.setBackground(Ui.rounded(Ui.card(this),26));
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,62);np.setMargins(0,0,0,7);content.addView(number,np);

        String[][] keys={{"1","2","3"},{"4","5","6"},{"7","8","9"},{"*","0","#"}};
        for(String[] row:keys){LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER);for(String k:row){Button b=Ui.button(this,k);b.setTextSize(22);b.setOnClickListener(v->number.append(((Button)v).getText()));LinearLayout.LayoutParams p=Ui.weight();p.setMargins(4,4,4,4);line.addView(b,p);}content.addView(line,new LinearLayout.LayoutParams(-1,58));}
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER);
        Button call=Ui.filled(this,"☎  התקשר");call.setOnClickListener(v->placeCall(number.getText().toString().trim()));
        Button del=Ui.button(this,"⌫  מחק");del.setOnClickListener(v->{String s=number.getText().toString();if(!s.isEmpty())number.setText(s.substring(0,s.length()-1));});
        Button add=Ui.button(this,"＋  איש קשר");add.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_INSERT,ContactsContract.Contacts.CONTENT_URI)));
        actions.addView(call,Ui.weight());actions.addView(del,Ui.weight());actions.addView(add,Ui.weight());content.addView(actions);
        TextView role=Ui.text(this,isDefaultDialer()?"✓ האפליקציה מוגדרת כחייגן ברירת המחדל":"הגדר אותי כחייגן ברירת המחדל",13);role.setTextColor(isDefaultDialer()?Ui.accent(this):Ui.muted(this));role.setGravity(Gravity.CENTER);role.setPadding(0,14,0,6);role.setOnClickListener(v->requestDialerRole());content.addView(role);
    }

    private void showFavorites(){
        TextView h=Ui.text(this,"מועדפים",24);h.setTypeface(null,1);content.addView(h);
        TextView hint=Ui.text(this,"אנשים שסימנת כמועדפים",13);hint.setTextColor(Ui.muted(this));hint.setPadding(0,2,0,12);content.addView(hint);
        if(checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED){content.addView(info("אשר הרשאת אנשי קשר כדי לראות מועדפים."));return;}
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);
        Cursor c=null;try{String sel=ContactsContract.CommonDataKinds.Phone.STARRED+"=1";c=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER,ContactsContract.CommonDataKinds.Phone.PHOTO_URI},sel,null,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" COLLATE NOCASE");if(c!=null)while(c.moveToNext())addPersonCard(list,c.getString(0),c.getString(1),c.getString(2),true);}finally{if(c!=null)c.close();}
        if(list.getChildCount()==0)list.addView(info("אין עדיין מועדפים. סמן אנשי קשר כמועדפים באפליקציית אנשי הקשר."));
    }

    private void showCallLog(){
        TextView h=Ui.text(this,"יומן שיחות",24);h.setTypeface(null,1);content.addView(h);
        TextView hint=Ui.text(this,"השיחות האחרונות שלך",13);hint.setTextColor(Ui.muted(this));hint.setPadding(0,2,0,12);content.addView(hint);
        if(checkSelfPermission(Manifest.permission.READ_CALL_LOG)!=PackageManager.PERMISSION_GRANTED){content.addView(info("אשר הרשאת יומן שיחות."));return;}
        Cursor c=null;try{c=getContentResolver().query(CallLog.Calls.CONTENT_URI,new String[]{CallLog.Calls.NUMBER,CallLog.Calls.CACHED_NAME,CallLog.Calls.DATE,CallLog.Calls.TYPE,CallLog.Calls.DURATION},null,null,CallLog.Calls.DATE+" DESC LIMIT 80");if(c!=null)while(c.moveToNext()){String n=c.getString(0),name=c.getString(1);String label=(name==null||name.isEmpty()?n:name);String detail=callType(c.getInt(3))+"  •  "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(c.getLong(2)))+"  •  "+c.getLong(4)+" שנ׳";addCallCard(label,detail,n);}}finally{if(c!=null)c.close();}
    }

    private void showContacts(){
        TextView h=Ui.text(this,"אנשי קשר",24);h.setTypeface(null,1);content.addView(h);
        Button add=Ui.filled(this,"＋  הוסף איש קשר");add.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_INSERT,ContactsContract.Contacts.CONTENT_URI)));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,54);ap.setMargins(0,8,0,12);content.addView(add,ap);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);
        if(checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED){list.addView(info("אשר הרשאת אנשי קשר."));return;}
        loadContacts(list,"");
    }

    private void showSearchResults(String q){
        TextView h=Ui.text(this,"תוצאות חיפוש",24);h.setTypeface(null,1);content.addView(h);
        TextView hint=Ui.text(this,"חיפוש עבור: "+q,13);hint.setTextColor(Ui.muted(this));hint.setPadding(0,2,0,12);content.addView(hint);
        if(checkSelfPermission(Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED){
            TextView sh=Ui.text(this,"אנשי קשר",16);sh.setTypeface(null,1);content.addView(sh);
            LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);loadContacts(list,q);
            if(list.getChildCount()==0)list.addView(info("לא נמצאו אנשי קשר."));
        }
        if(checkSelfPermission(Manifest.permission.READ_CALL_LOG)==PackageManager.PERMISSION_GRANTED){
            TextView lh=Ui.text(this,"שיחות",16);lh.setTypeface(null,1);lh.setPadding(0,14,0,5);content.addView(lh);
            Cursor c=null;int count=0;try{String sel=CallLog.Calls.NUMBER+" LIKE ? OR "+CallLog.Calls.CACHED_NAME+" LIKE ?";String[] a={"%"+q+"%","%"+q+"%"};c=getContentResolver().query(CallLog.Calls.CONTENT_URI,new String[]{CallLog.Calls.NUMBER,CallLog.Calls.CACHED_NAME,CallLog.Calls.DATE,CallLog.Calls.TYPE,CallLog.Calls.DURATION},sel,a,CallLog.Calls.DATE+" DESC");if(c!=null)while(c.moveToNext()&&count<30){count++;String n=c.getString(0),name=c.getString(1);addCallCard(name==null||name.isEmpty()?n:name,callType(c.getInt(3))+"  •  "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(c.getLong(2))),n);}}finally{if(c!=null)c.close();}
        }
    }

    private void loadContacts(LinearLayout list,String q){
        list.removeAllViews();Cursor c=null;try{String sel=null;String[] args=null;if(!q.trim().isEmpty()){sel=ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" LIKE ? OR "+ContactsContract.CommonDataKinds.Phone.NUMBER+" LIKE ?";args=new String[]{"%"+q+"%","%"+q+"%"}}c=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER,ContactsContract.CommonDataKinds.Phone.PHOTO_URI,ContactsContract.CommonDataKinds.Phone.STARRED},sel,args,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" COLLATE NOCASE");if(c!=null)while(c.moveToNext())addPersonCard(list,c.getString(0),c.getString(1),c.getString(2),c.getInt(3)==1);}finally{if(c!=null)c.close();}
    }

    private void addPersonCard(LinearLayout list,String name,String n,String photo,boolean favorite){
        if(name==null||name.isEmpty())name="ללא שם";
        LinearLayout box=new LinearLayout(this);box.setGravity(Gravity.CENTER_VERTICAL);box.setPadding(12,9,10,9);box.setBackground(Ui.rounded(Ui.card(this),22));
        ImageView avatar=new ImageView(this);avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);if(photo!=null&&!photo.isEmpty())avatar.setImageURI(Uri.parse(photo));if(avatar.getDrawable()==null){TextView fallback=new TextView(this);fallback.setText(name.substring(0,1).toUpperCase());fallback.setTextColor(Color.WHITE);fallback.setTextSize(18);fallback.setGravity(Gravity.CENTER);fallback.setBackground(Ui.rounded(Ui.accent(this),50));box.addView(fallback,Ui.lp(48,48));}else box.addView(avatar,Ui.lp(48,48));
        LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(13,0,0,0);TextView a=Ui.text(this,(favorite?"★ ":"")+name,17);a.setTypeface(null,1);TextView b=Ui.text(this,n==null?"":n,13);b.setTextColor(Ui.muted(this));tx.addView(a);tx.addView(b);box.addView(tx,new LinearLayout.LayoutParams(0,-2,1));
        Button call=Ui.button(this,"☎");call.setOnClickListener(v->placeCall(n));box.addView(call,Ui.lp(50,50));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,8);list.addView(box,p);
    }

    private void addCallCard(String name,String detail,String n){
        LinearLayout box=new LinearLayout(this);box.setGravity(Gravity.CENTER_VERTICAL);box.setPadding(15,10,10,10);box.setBackground(Ui.rounded(Ui.card(this),22));
        LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);TextView a=Ui.text(this,name,17);a.setTypeface(null,1);TextView d=Ui.text(this,detail,12);d.setTextColor(Ui.muted(this));tx.addView(a);tx.addView(d);box.addView(tx,new LinearLayout.LayoutParams(0,-2,1));
        Button call=Ui.button(this,"☎");call.setOnClickListener(v->placeCall(n));box.addView(call,Ui.lp(50,50));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,8);content.addView(box,p);
    }

    private void placeCall(String raw){if(raw==null||raw.trim().isEmpty()){Toast.makeText(this,"הזן מספר",Toast.LENGTH_SHORT).show();return;}if(!isDefaultDialer()){requestDialerRole();return;}try{TelecomManager tm=getSystemService(TelecomManager.class);if(tm!=null)tm.placeCall(Uri.parse("tel:"+Uri.encode(raw)),null);if(number!=null)number.setText("");}catch(Exception e){Toast.makeText(this,"לא ניתן להתחיל את השיחה",Toast.LENGTH_LONG).show();}}
    private boolean isDefaultDialer(){TelecomManager tm=getSystemService(TelecomManager.class);return tm!=null&&getPackageName().equals(tm.getDefaultDialerPackage());}
    private void requestDialerRole(){if(android.os.Build.VERSION.SDK_INT>=29){RoleManager rm=getSystemService(RoleManager.class);if(rm!=null&&rm.isRoleAvailable(RoleManager.ROLE_DIALER)&&!rm.isRoleHeld(RoleManager.ROLE_DIALER))startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER),ROLE_REQUEST);}else{Intent i=new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);i.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,getPackageName());startActivity(i);}}
    private String callType(int t){if(t==CallLog.Calls.INCOMING_TYPE)return"נכנסת";if(t==CallLog.Calls.OUTGOING_TYPE)return"יוצאת";if(t==CallLog.Calls.MISSED_TYPE)return"שיחה שלא נענתה";if(t==CallLog.Calls.REJECTED_TYPE)return"נדחתה";return"שיחה";}
    private TextView info(String s){TextView t=Ui.text(this,s,14);t.setTextColor(Ui.muted(this));t.setPadding(16,28,16,28);return t;}
    private void requestDataPermissions(){if(android.os.Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.READ_CONTACTS,Manifest.permission.WRITE_CONTACTS,Manifest.permission.READ_CALL_LOG,Manifest.permission.WRITE_CALL_LOG,Manifest.permission.CALL_PHONE,Manifest.permission.READ_PHONE_STATE},PERM_REQUEST);}
    private void handleDialIntent(Intent i){if(i!=null&&Intent.ACTION_DIAL.equals(i.getAction())&&i.getData()!=null&&number!=null){String s=i.getData().getSchemeSpecificPart();if(s!=null)number.setText(s);}}
}
