package com.generativimt12.incomingtone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class SettingsActivity extends Activity {
    private LinearLayout root;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        build();
    }

    private void build() {
        boolean dark = SettingsStore.dark(this);
        getWindow().setStatusBarColor(Ui.bg(this));
        getWindow().setNavigationBarColor(Ui.bg(this));
        getWindow().getDecorView().setSystemUiVisibility(dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(20,18,20,18); root.setBackgroundColor(Ui.bg(this));

        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        Button back = Ui.button(this,"‹"); back.setTextSize(30); back.setOnClickListener(v->finish());
        top.addView(back, Ui.lp(52,52));
        TextView title = Ui.text(this,"הגדרות",28); title.setTypeface(null,1); title.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(title,new LinearLayout.LayoutParams(0,58,1));
        root.addView(top);

        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(0,16,0,20);
        scroll.addView(body); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        section(body,"מראה");
        Switch darkSwitch = new Switch(this); darkSwitch.setText("מצב כהה"); darkSwitch.setTextSize(16); darkSwitch.setTextColor(Ui.ink(this)); darkSwitch.setChecked(dark);
        darkSwitch.setOnCheckedChangeListener((v,checked)->{SettingsStore.setDark(this,checked);recreate();});
        addRow(body,darkSwitch,"ממשק כהה ונעים לעבודה בלילה");

        TextView colors = Ui.text(this,"צבע הדגשה",16); colors.setPadding(16,18,16,8); body.addView(colors);
        LinearLayout colorRow = new LinearLayout(this); colorRow.setGravity(Gravity.CENTER);
        int[] swatches={0,1,2,3,4}; for(int s:swatches){Button b=Ui.button(this,"");b.setBackground(Ui.rounded(new int[]{0}[0],50)); b.setBackground(Ui.rounded(colorFor(s),50)); final int pick=s; b.setOnClickListener(v->{SettingsStore.setAccent(this,pick);recreate();}); LinearLayout.LayoutParams p=Ui.lp(52,52);p.setMargins(6,6,6,6);colorRow.addView(b,p);}
        body.addView(colorRow);

        section(body,"צלצול נכנס — מנגנון עצמאי");
        Switch enabled=new Switch(this); enabled.setText("צלצול עצמאי פעיל"); enabled.setTextSize(16); enabled.setTextColor(Ui.ink(this)); enabled.setChecked(SettingsStore.toneEnabled(this));
        enabled.setOnCheckedChangeListener((v,c)->SettingsStore.setToneEnabled(this,c)); addRow(body,enabled,"זהו מנגנון תיקון ההשהיה של הצלצול");
        addChoice(body,"צליל",SettingsStore.toneName(this),v->chooseTone());
        Button fileTone=Ui.button(this,"🎵  בחר קובץ צליל מהטלפון"); fileTone.setOnClickListener(v->pickAudioFile()); body.addView(fileTone,new LinearLayout.LayoutParams(-1,58));
        Button systemTone=Ui.button(this,"🔔  בחר צליל מערכת"); systemTone.setOnClickListener(v->pickSystemTone()); body.addView(systemTone,new LinearLayout.LayoutParams(-1,58));
        addSlider(body,"עוצמת צלצול",SettingsStore.volume(this),v->SettingsStore.setVolume(this,v));
        Switch vib=new Switch(this); vib.setText("רטט"); vib.setTextSize(16); vib.setTextColor(Ui.ink(this)); vib.setChecked(SettingsStore.vibrate(this)); vib.setOnCheckedChangeListener((v,c)->SettingsStore.setVibrate(this,c)); addRow(body,vib,"רטט בזמן צלצול נכנס");
        Switch fs=new Switch(this); fs.setText("מסך שיחה מלא"); fs.setTextSize(16); fs.setTextColor(Ui.ink(this)); fs.setChecked(SettingsStore.showFullScreen(this)); fs.setOnCheckedChangeListener((v,c)->SettingsStore.setShowFullScreen(this,c)); addRow(body,fs,"פתיחת מסך השיחה מיד עם כניסה");
        Switch as=new Switch(this); as.setText("רמקול אוטומטי"); as.setTextSize(16); as.setTextColor(Ui.ink(this)); as.setChecked(SettingsStore.autoSpeaker(this)); as.setOnCheckedChangeListener((v,c)->SettingsStore.setAutoSpeaker(this,c)); addRow(body,as,"להתחיל שיחה פעילה על רמקול");

        section(body,"טלפון");
        Button role=Ui.button(this,"הגדר כאפליקציית הטלפון המוגדרת כברירת מחדל"); role.setOnClickListener(v->{
            Intent i=new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS); startActivity(i);
        }); body.addView(role,new LinearLayout.LayoutParams(-1,56));
        Button perms=Ui.button(this,"ניהול הרשאות"); perms.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName())))); body.addView(perms,new LinearLayout.LayoutParams(-1,56));
        section(body,"אודות");
        TextView about=Ui.text(this,"Incoming Tone\nחייגן אישי עם מנגנון צלצול עצמאי לטיפול בהשהיית הצלצול.\nגרסה 2.0",14); about.setTextColor(Ui.muted(this)); about.setPadding(16,12,16,12); body.addView(about);

        setContentView(root);
    }

    private int colorFor(int i){return new int[]{android.graphics.Color.rgb(55,105,245),android.graphics.Color.rgb(124,78,220),android.graphics.Color.rgb(12,155,112),android.graphics.Color.rgb(232,95,71),android.graphics.Color.rgb(214,137,25)}[i];}
    private void section(LinearLayout b,String s){TextView t=Ui.text(this,s,13);t.setTextColor(Ui.accent(this));t.setTypeface(null,1);t.setPadding(6,22,6,8);b.addView(t);}
    private void addRow(LinearLayout b,View v,String sub){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(16,10,16,10);box.setBackground(Ui.rounded(Ui.card(this),24));box.addView(v);TextView t=Ui.text(this,sub,12);t.setTextColor(Ui.muted(this));box.addView(t);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,8);b.addView(box,p);}
    private void addChoice(LinearLayout b,String title,String value,View.OnClickListener l){Button x=Ui.button(this,title+"    •    "+value);x.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);x.setOnClickListener(l);b.addView(x,new LinearLayout.LayoutParams(-1,58));}
    private void addSlider(LinearLayout b,String title,int value,java.util.function.IntConsumer save){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(16,10,16,8);box.setBackground(Ui.rounded(Ui.card(this),24));TextView t=Ui.text(this,title+"  "+value+"%",16);box.addView(t);SeekBar s=new SeekBar(this);s.setMax(100);s.setProgress(value);s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar x,int p,boolean f){t.setText(title+"  "+p+"%");save.accept(p);}public void onStartTrackingTouch(SeekBar x){}public void onStopTrackingTouch(SeekBar x){}});box.addView(s);b.addView(box,new LinearLayout.LayoutParams(-1,-2));}
    private void chooseTone(){String[] tones={"קלאסי","צליל כפול","פעמון","רך"};new AlertDialog.Builder(this).setTitle("צלילים מובנים").setSingleChoiceItems(tones,SettingsStore.tone(this),(d,w)->{SettingsStore.setTone(this,w);SettingsStore.setToneUri(this,"");SettingsStore.setToneLabel(this,tones[w]);d.dismiss();recreate();}).show();}
    private void pickAudioFile(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("audio/*");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,701);}
    private void pickSystemTone(){Intent i=new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_RINGTONE);i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT,true);i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,false);startActivityForResult(i,702);}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK||data==null)return;Uri u=data.getParcelableExtra(requestCode==701?Intent.EXTRA_STREAM:RingtoneManager.EXTRA_RINGTONE_PICKED_URI);if(u==null)return;if(requestCode==701){try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}SettingsStore.setToneUri(this,u.toString());SettingsStore.setToneLabel(this,resolveName(u));}else{SettingsStore.setToneUri(this,u.toString());SettingsStore.setToneLabel(this,"צליל מערכת");}recreate();}
    private String resolveName(Uri u){String s=u.getLastPathSegment();if(s==null||s.isEmpty())return"קובץ מהמכשיר";return s;}

}
