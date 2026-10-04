package com.generativimt12.incomingtone;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class ContactDetailActivity extends Activity {
    private static final int TONE_PICK=801;
    private String contactId="";
    private String contactName="";
    private String number="";
    private TextView toneValue;

    @Override protected void onCreate(Bundle b){
        setTheme(SettingsStore.dark(this)?R.style.AppThemeDark:R.style.AppTheme);
        super.onCreate(b);
        contactId=getIntent().getStringExtra("contact_id");
        contactName=getIntent().getStringExtra("contact_name");
        number=getIntent().getStringExtra("number");
        build();
    }

    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(18,14,18,18);root.setBackgroundColor(Ui.bg(this));

        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        Button back=Ui.button(this,"‹");back.setTextSize(30);back.setOnClickListener(v->finish());top.addView(back,Ui.lp(52,52));
        TextView title=Ui.text(this,"איש קשר",24);title.setTypeface(null,1);top.addView(title,new LinearLayout.LayoutParams(0,56,1));
        Button more=Ui.button(this,"⋮");more.setTextSize(27);more.setOnClickListener(v->openEditor());top.addView(more,Ui.lp(52,52));root.addView(top);

        ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setGravity(Gravity.CENTER_HORIZONTAL);body.setPadding(0,16,0,24);

        TextView initials=Ui.text(this,initial(contactName),40);initials.setGravity(Gravity.CENTER);initials.setTextColor(Color.WHITE);initials.setBackground(Ui.rounded(Ui.accent(this),100));body.addView(initials,Ui.lp(110,110));
        TextView name=Ui.text(this,contactName==null||contactName.isEmpty()?number:contactName,28);name.setTypeface(null,1);name.setGravity(Gravity.CENTER);name.setPadding(0,16,0,2);body.addView(name,new LinearLayout.LayoutParams(-1,-2));
        TextView num=Ui.text(this,number==null?"":number,15);num.setTextColor(Ui.muted(this));num.setGravity(Gravity.CENTER);body.addView(num);

        Button call=Ui.filled(this,"☎  התקשר");call.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+Uri.encode(number==null?"":number)));startActivity(i);});LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,56);cp.setMargins(0,20,0,12);body.addView(call,cp);

        section(body,"עיצוב איש הקשר");
        TextView sub=Ui.text(this,"צבע אישי ייחודי לכרטיס ולמסך השיחה",12);sub.setTextColor(Ui.muted(this));sub.setGravity(Gravity.RIGHT);body.addView(sub,new LinearLayout.LayoutParams(-1,32));
        LinearLayout colors=new LinearLayout(this);colors.setGravity(Gravity.CENTER);
        int[] swatches={Color.rgb(91,79,255),Color.rgb(157,92,255),Color.rgb(45,111,255),Color.rgb(40,197,255),Color.rgb(209,102,255)};
        int selected=SettingsStore.contactAccent(this,contactId);
        for(int i=0;i<swatches.length;i++){final int idx=i;Button s=Ui.button(this,"");s.setBackground(Ui.rounded(swatches[i],100));if(idx==selected)s.setText("✓");s.setTextColor(Color.WHITE);s.setOnClickListener(v->{SettingsStore.setContactAccent(this,contactId,idx);Toast.makeText(this,"נשמר",Toast.LENGTH_SHORT).show();build();});LinearLayout.LayoutParams sp=Ui.lp(52,52);sp.setMargins(6,6,6,12);colors.addView(s,sp);}
        body.addView(colors);

        section(body,"צלצול אישי");
        toneValue=Ui.text(this,currentToneLabel(),16);toneValue.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);toneValue.setPadding(16,0,16,0);toneValue.setBackground(Ui.glass(this,24));toneValue.setOnClickListener(v->chooseTone());body.addView(toneValue,new LinearLayout.LayoutParams(-1,58));
        Button clear=Ui.button(this,"נקה צלצול אישי");clear.setOnClickListener(v->{setCustomRingtone(null);build();});body.addView(clear,new LinearLayout.LayoutParams(-1,52));

        section(body,"ניהול");
        Button edit=Ui.button(this,"✎  ערוך את איש הקשר");edit.setOnClickListener(v->openEditor());body.addView(edit,new LinearLayout.LayoutParams(-1,54));
        Button fav=Ui.button(this,isFavorite()?"★  הסר ממועדפים":"☆  הוסף למועדפים");fav.setOnClickListener(v->{toggleFavorite();build();});body.addView(fav,new LinearLayout.LayoutParams(-1,54));

        scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }

    private void section(LinearLayout b,String s){TextView t=Ui.text(this,s,13);t.setTextColor(Ui.accent(this));t.setTypeface(null,1);t.setGravity(Gravity.RIGHT);t.setPadding(4,20,4,7);b.addView(t);}
    private String initial(String s){return s==null||s.isEmpty()?"?":s.substring(0,1);}
    private void openEditor(){if(contactId==null||contactId.isEmpty()){finish();return;}try{Intent i=new Intent(Intent.ACTION_EDIT,Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI,contactId));startActivity(i);}catch(Exception e){Toast.makeText(this,"לא ניתן לפתוח עריכה",Toast.LENGTH_SHORT).show();}}
    private boolean isFavorite(){Cursor c=null;try{c=getContentResolver().query(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI,contactId),new String[]{ContactsContract.Contacts.STARRED},null,null,null);return c!=null&&c.moveToFirst()&&c.getInt(0)==1;}catch(Exception e){return false;}finally{if(c!=null)c.close();}}
    private void toggleFavorite(){try{ContentValues v=new ContentValues();v.put(ContactsContract.Contacts.STARRED,isFavorite()?0:1);getContentResolver().update(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI,contactId),v,null,null);}catch(Exception ignored){}}
    private String currentTone(){Cursor c=null;try{c=getContentResolver().query(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI,contactId),new String[]{ContactsContract.Contacts.CUSTOM_RINGTONE},null,null,null);if(c!=null&&c.moveToFirst()){String u=c.getString(0);if(u!=null&&!u.isEmpty())return"צלצול אישי מוגדר";}}catch(Exception ignored){}finally{if(c!=null)c.close();}return"ברירת המחדל";}
    private String currentToneLabel(){return currentTone();}
    private void chooseTone(){Intent i=new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_RINGTONE);i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT,true);i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,true);startActivityForResult(i,TONE_PICK);}
    private void setCustomRingtone(Uri u){try{ContentValues v=new ContentValues();v.put(ContactsContract.Contacts.CUSTOM_RINGTONE,u==null?null:u.toString());getContentResolver().update(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI,contactId),v,null,null);}catch(Exception ignored){}}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode==TONE_PICK&&resultCode==RESULT_OK){Uri u=data==null?null:data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);setCustomRingtone(u);build();}}
}
