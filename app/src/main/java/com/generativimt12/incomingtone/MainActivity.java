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
    private String searchDigits="";
    private char t9Last=0; private long t9At=0; private int t9Tap=0;


    @Override protected void onCreate(Bundle b){
        setTheme(SettingsStore.dark(this) ? R.style.AppThemeDark : R.style.AppTheme);
        super.onCreate(b);
        applyBars();
        build();
        handleDialIntent(getIntent());
        requestDataPermissions();
        requestNotificationPermission();
        ContactIndex.refreshAsync(this,()->{if(content!=null)showPage();});
    }

    @Override protected void onResume(){ super.onResume(); applyBars(); if(content!=null){if(getIntent().getBooleanExtra("open_recent",false)){page=2;getIntent().removeExtra("open_recent");}ContactIndex.refreshAsync(this);showPage();} }
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handleDialIntent(i);if(content!=null)showPage();}
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        if(event.getAction()==KeyEvent.ACTION_DOWN){
            int code=event.getKeyCode();
            if(code==KeyEvent.KEYCODE_CALL){
                android.telecom.Call current=IncomingCallService.getCurrentCall();
                if(current!=null&&current.getState()==android.telecom.Call.STATE_RINGING){IncomingCallService.answerIncoming();return true;}
                page=2;query="";searchDigits="";t9Last=0;t9Tap=0;if(globalSearch!=null)globalSearch.setText("");if(content!=null)showPage();return true;
            }
            String digit=null;
            if(code>=KeyEvent.KEYCODE_0&&code<=KeyEvent.KEYCODE_9) digit=String.valueOf(code-KeyEvent.KEYCODE_0);
            else if(code==KeyEvent.KEYCODE_STAR) digit="*";
            else if(code==KeyEvent.KEYCODE_POUND) digit="#";
            if(digit!=null&&number!=null&&number.hasFocus()){number.append(digit);return true;}
            if(digit!=null&&globalSearch!=null&&globalSearch.hasFocus()){handleT9(digit.charAt(0));return true;}
        }
        return super.dispatchKeyEvent(event);
    }

    private void applyBars(){
        boolean dark=SettingsStore.dark(this);
        getWindow().setStatusBarColor(Ui.bg(this));
        getWindow().setNavigationBarColor(Ui.bg(this));
        getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    private void build(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(18,14,18,8); root.setBackgroundColor(Ui.bg(this));

        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=Ui.text(this,"PHONE",25); title.setTypeface(null,1); title.setLetterSpacing(.08f);
        top.addView(title,new LinearLayout.LayoutParams(0,52,1));
        Button settings=Ui.button(this,"⋮"); settings.setTextSize(27); settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class))); top.addView(settings,Ui.lp(48,48));
        root.addView(top);

        globalSearch=new EditText(this); globalSearch.setHint("חיפוש חכם — שם, מספר או T9"); globalSearch.setSingleLine(); globalSearch.setTextSize(16); globalSearch.setTextColor(Ui.ink(this)); globalSearch.setHintTextColor(Ui.muted(this)); globalSearch.setPadding(18,0,18,0); globalSearch.setShowSoftInputOnFocus(false); globalSearch.setBackground(Ui.glass(this,24));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,52);sp.setMargins(0,6,0,8);root.addView(globalSearch,sp);
        globalSearch.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int d){} public void onTextChanged(CharSequence s,int a,int b,int c){query=s.toString();showPage();} public void afterTextChanged(Editable e){}});

        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.addView(content); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        nav=new LinearLayout(this); nav.setGravity(Gravity.CENTER); nav.setPadding(2,7,2,0);
        String[] labels={"⌨\nחייגן","★\nמועדפים","◷\nיומן","●\nאנשי קשר"};
        for(int i=0;i<4;i++){final int p=i;TextView b=Ui.text(this,labels[i],12);b.setGravity(Gravity.CENTER);b.setPadding(3,6,3,4);b.setOnClickListener(v->{page=p;query="";searchDigits="";t9Last=0;t9Tap=0;globalSearch.setText("");showPage();});nav.addView(b,Ui.weight());}
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
        TextView h=Ui.text(this,"חייג",22);h.setTypeface(null,1);content.addView(h);
        TextView hint=Ui.text(this,"מקשי הטלפון הפיזיים פועלים ישירות. אין צורך בלוח מקשים על המסך.",13);hint.setTextColor(Ui.muted(this));hint.setPadding(0,2,0,12);content.addView(hint);
        number=new EditText(this);number.setHint("מספר טלפון");number.setGravity(Gravity.CENTER);number.setTextSize(25);number.setTextColor(Ui.ink(this));number.setHintTextColor(Ui.muted(this));number.setSingleLine();number.setInputType(3);number.setShowSoftInputOnFocus(false);number.setBackground(Ui.rounded(Ui.card(this),26));
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,68);np.setMargins(0,4,0,12);content.addView(number,np);
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER);
        Button call=Ui.filled(this,"☎  התקשר");call.setOnClickListener(v->placeCall(number.getText().toString().trim()));
        Button del=Ui.button(this,"⌫  מחק");del.setOnClickListener(v->{String s=number.getText().toString();if(!s.isEmpty())number.setText(s.substring(0,s.length()-1));});
        Button add=Ui.button(this,"＋  איש קשר");add.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_INSERT,ContactsContract.Contacts.CONTENT_URI)));
        actions.addView(call,Ui.weight());actions.addView(del,Ui.weight());actions.addView(add,Ui.weight());content.addView(actions);
        TextView physical=Ui.text(this,"🟢 מקש ירוק: שיחה נכנסת = מענה  •  ללא שיחה = יומן אחרונות",14);physical.setTextColor(Ui.muted(this));physical.setGravity(Gravity.CENTER);physical.setPadding(0,24,0,10);content.addView(physical);
        if(!isDefaultDialer()){ TextView role=Ui.text(this,"הגדר אותי כאפליקציית הטלפון",14);role.setTextColor(Ui.accent(this));role.setGravity(Gravity.CENTER);role.setPadding(0,18,0,8);role.setOnClickListener(v->requestDialerRole());content.addView(role); }
    }

    private void showFavorites(){
        TextView h=Ui.text(this,"מועדפים",24);h.setTypeface(null,1);content.addView(h);
        TextView hint=Ui.text(this,"אנשים שסימנת כמועדפים",13);hint.setTextColor(Ui.muted(this));hint.setPadding(0,2,0,12);content.addView(hint);
        if(checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED){content.addView(info("אשר הרשאת אנשי קשר כדי לראות מועדפים."));return;}
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);
        for(ContactIndex.ContactRow r:ContactIndex.favorites(this,500))addPersonCard(list,r.name,r.number,r.photo,r.favorite,r.id);
        if(list.getChildCount()==0)list.addView(info("אין עדיין מועדפים. סמן אנשי קשר כמועדפים באפליקציית אנשי הקשר."));
    }

    private void showCallLog(){
        TextView h=Ui.text(this,"יומן שיחות",24);h.setTypeface(null,1);content.addView(h);
        TextView hint=Ui.text(this,"השיחות האחרונות שלך",13);hint.setTextColor(Ui.muted(this));hint.setPadding(0,2,0,12);content.addView(hint);
        if(checkSelfPermission(Manifest.permission.READ_CALL_LOG)!=PackageManager.PERMISSION_GRANTED){content.addView(info("אשר הרשאת יומן שיחות."));return;}
        for(ContactIndex.CallRow r:ContactIndex.calls(this,"",80)){
            String label=(r.name==null||r.name.isEmpty()?r.number:r.name);
            String detail=callType(r.type)+"  •  "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(r.date))+"  •  "+r.duration+" שנ׳";
            addCallCard(label,detail,r.number);
        }
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
            int count=0;
            for(ContactIndex.CallRow r:ContactIndex.calls(this,q,30)){
                count++;String label=(r.name==null||r.name.isEmpty()?r.number:r.name);
                String detail=callType(r.type)+"  •  "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(r.date));
                addCallCard(label,detail,r.number);
            }
            if(count==0)content.addView(info("לא נמצאו שיחות."));
        }
    }

    private void loadContacts(LinearLayout list,String q){
        list.removeAllViews();
        if(checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)return;
        for(ContactIndex.ContactRow r:ContactIndex.contacts(this,q,searchDigits,500))addPersonCard(list,r.name,r.number,r.photo,r.favorite,r.id);
    }

    private void addPersonCard(LinearLayout list,String name,String n,String photo,boolean favorite,String contactId){
        if(name==null||name.isEmpty())name="ללא שם";
        String displayName=name; LinearLayout box=new LinearLayout(this);box.setGravity(Gravity.CENTER_VERTICAL);box.setPadding(12,9,10,9);
        int ca=SettingsStore.contactAccent(this,contactId);int[] cc={Color.rgb(91,79,255),Color.rgb(157,92,255),Color.rgb(45,111,255),Color.rgb(40,197,255),Color.rgb(209,102,255)};box.setBackground(ca>=0?Ui.rounded(cc[Math.min(4,ca)],22):Ui.glass(this,22));
        box.setOnClickListener(v->{Intent i=new Intent(this,ContactDetailActivity.class);i.putExtra("contact_id",contactId);i.putExtra("contact_name",displayName);i.putExtra("number",n);startActivity(i);});
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

    private void handleT9(char digit){
        long now=System.currentTimeMillis();
        if(digit=='*'){if(searchDigits.length()>0)searchDigits=searchDigits.substring(0,searchDigits.length()-1);String s=globalSearch.getText().toString();if(!s.isEmpty())globalSearch.setText(s.substring(0,s.length()-1));t9Last=0;t9Tap=0;return;}
        if(digit=='#'){query="";searchDigits="";t9Last=0;t9Tap=0;globalSearch.setText("");return;}
        searchDigits+=digit;
        String[] map={" ","אבג","דהו","זחט","יכל","מנס","עפצ","קרש","תצ"};
        int idx=digit-'0';if(idx<0||idx>9||map[idx].isEmpty())return;String letters=map[idx];
        String s=globalSearch.getText().toString();
        if(t9Last==digit&&(now-t9At)<900&&!s.isEmpty()){t9Tap=(t9Tap+1)%letters.length();globalSearch.setText(s.substring(0,s.length()-1)+letters.charAt(t9Tap));}
        else{t9Tap=0;globalSearch.append(String.valueOf(letters.charAt(0)));}
        globalSearch.setSelection(globalSearch.length());query=globalSearch.getText().toString();t9Last=digit;t9At=now;showPage();
    }
    private void requestNotificationPermission(){
        if(android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},64);
    }
    private void addAlphabetScroller(LinearLayout body){
        HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);
        LinearLayout letters=new LinearLayout(this);letters.setGravity(Gravity.CENTER_VERTICAL);
        String alphabet="אבגדהוזחטיכלמנסעפצקרשת";
        for(int i=0;i<alphabet.length();i++){final String letter=String.valueOf(alphabet.charAt(i));TextView l=Ui.text(this,letter,15);l.setGravity(Gravity.CENTER);l.setPadding(10,8,10,8);l.setBackground(Ui.glass(this,18));l.setOnClickListener(v->{query=letter;searchDigits="";t9Last=0;t9Tap=0;globalSearch.setText(letter);});LinearLayout.LayoutParams lp=Ui.lp(38,40);lp.setMargins(3,2,3,6);letters.addView(l,lp);}
        hs.addView(letters);body.addView(hs,new LinearLayout.LayoutParams(-1,48));
    }

    private void placeCall(String raw){if(raw==null||raw.trim().isEmpty()){Toast.makeText(this,"הזן מספר",Toast.LENGTH_SHORT).show();return;}if(!isDefaultDialer()){requestDialerRole();return;}try{TelecomManager tm=getSystemService(TelecomManager.class);if(tm!=null)tm.placeCall(Uri.parse("tel:"+Uri.encode(raw)),null);if(number!=null)number.setText("");}catch(Exception e){Toast.makeText(this,"לא ניתן להתחיל את השיחה",Toast.LENGTH_LONG).show();}}
    private boolean isDefaultDialer(){TelecomManager tm=getSystemService(TelecomManager.class);return tm!=null&&getPackageName().equals(tm.getDefaultDialerPackage());}
    private void requestDialerRole(){if(android.os.Build.VERSION.SDK_INT>=29){RoleManager rm=getSystemService(RoleManager.class);if(rm!=null&&rm.isRoleAvailable(RoleManager.ROLE_DIALER)&&!rm.isRoleHeld(RoleManager.ROLE_DIALER))startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER),ROLE_REQUEST);}else{Intent i=new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);i.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,getPackageName());startActivity(i);}}
    private String callType(int t){if(t==CallLog.Calls.INCOMING_TYPE)return"נכנסת";if(t==CallLog.Calls.OUTGOING_TYPE)return"יוצאת";if(t==CallLog.Calls.MISSED_TYPE)return"שיחה שלא נענתה";if(t==CallLog.Calls.REJECTED_TYPE)return"נדחתה";return"שיחה";}
    private TextView info(String s){TextView t=Ui.text(this,s,14);t.setTextColor(Ui.muted(this));t.setPadding(16,28,16,28);return t;}
    private void requestDataPermissions(){
        if(android.os.Build.VERSION.SDK_INT<23)return;
        java.util.ArrayList<String> missing=new java.util.ArrayList<>();
        String[] wanted={Manifest.permission.READ_CONTACTS,Manifest.permission.WRITE_CONTACTS,Manifest.permission.READ_CALL_LOG,Manifest.permission.WRITE_CALL_LOG,Manifest.permission.CALL_PHONE,Manifest.permission.ANSWER_PHONE_CALLS,Manifest.permission.READ_PHONE_STATE};
        for(String p:wanted)if(checkSelfPermission(p)!=PackageManager.PERMISSION_GRANTED)missing.add(p);
        if(!missing.isEmpty())requestPermissions(missing.toArray(new String[0]),PERM_REQUEST);
    }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==PERM_REQUEST){
            ContactIndex.refreshAsync(this);
            new android.os.Handler().postDelayed(()->{ContactIndex.refreshAsync(this);showPage();},1200);
            new android.os.Handler().postDelayed(()->{ContactIndex.refreshAsync(this);showPage();},3200);
        }
    }
    private void handleDialIntent(Intent i){if(i!=null&&Intent.ACTION_DIAL.equals(i.getAction())&&i.getData()!=null&&number!=null){String s=i.getData().getSchemeSpecificPart();if(s!=null)number.setText(s);}}

}
