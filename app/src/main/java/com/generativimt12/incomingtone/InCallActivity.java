package com.generativimt12.incomingtone;

import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.view.*;
import android.view.WindowManager;
import android.widget.*;
import java.util.*;

public class InCallActivity extends Activity {
    private static InCallActivity open;
    private TextView status, name, number, timer;
    private ImageView avatar;
    private LinearLayout controls, keypad, bottomBar;
    private long activeAt=0;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable tick=()->{refresh();handler.postDelayed(this::tickSafe,1000);};
    private void tickSafe(){if(!isFinishing())tick.run();}

    public static void finishIfOpen(){if(open!=null)open.runOnUiThread(()->{open.finish();open=null;});}

    @Override protected void onCreate(Bundle b){
        setTheme(SettingsStore.dark(this) ? R.style.AppThemeDark : R.style.AppTheme);
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        open=this;
        getWindow().getDecorView().setFocusableInTouchMode(true);
        getWindow().getDecorView().requestFocus();
        applyBars();build();refresh();
    }
    @Override protected void onPostResume(){super.onPostResume();requestFocus();}
    @Override protected void onResume(){super.onResume();refresh();handler.removeCallbacks(tick);handler.postDelayed(tick,400);}
    @Override public boolean onKeyDown(int keyCode, KeyEvent event){
        if(isAnswerKey(keyCode) && current()!=null && current().getState()==Call.STATE_RINGING){
            IncomingCallService.answerIncoming();
            return true;
        }
        return super.onKeyDown(keyCode,event);
    }
    private boolean isAnswerKey(int k){
        return k==KeyEvent.KEYCODE_CALL || k==KeyEvent.KEYCODE_ENTER ||
                k==KeyEvent.KEYCODE_DPAD_CENTER || k==KeyEvent.KEYCODE_HEADSETHOOK ||
                k==KeyEvent.KEYCODE_DIAL || k==KeyEvent.KEYCODE_FOCUS;
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event){
        if(event.getAction()==KeyEvent.ACTION_DOWN &&
                (isAnswerKey(event.getKeyCode()) ||
                 event.getKeyCode()==KeyEvent.KEYCODE_ENTER ||
                 event.getKeyCode()==KeyEvent.KEYCODE_DPAD_CENTER ||
                 event.getKeyCode()==KeyEvent.KEYCODE_HEADSETHOOK)){
            Call c=current();
            if(c!=null&&c.getState()==Call.STATE_RINGING){
                IncomingCallService.answerIncoming();
                return true;
            }
            return true;
        }
        if(event.getAction()==KeyEvent.ACTION_DOWN){
            int code=event.getKeyCode();char d=0;
            if(code>=KeyEvent.KEYCODE_0&&code<=KeyEvent.KEYCODE_9)d=(char)('0'+code-KeyEvent.KEYCODE_0);
            else if(code==KeyEvent.KEYCODE_STAR)d='*'; else if(code==KeyEvent.KEYCODE_POUND)d='#';
            if(d!=0){Call c=current();if(c!=null&&c.getState()==Call.STATE_ACTIVE){c.playDtmfTone(d);handler.postDelayed(()->{try{c.stopDtmfTone();}catch(Exception ignored){}},160);return true;}}
        }
        return super.dispatchKeyEvent(event);
    }
    @Override protected void onPause(){handler.removeCallbacks(tick);super.onPause();}
    @Override protected void onDestroy(){handler.removeCallbacks(tick);if(open==this)open=null;super.onDestroy();}

    private void applyBars(){getWindow().setStatusBarColor(Ui.bg(this));getWindow().setNavigationBarColor(Ui.bg(this));getWindow().getDecorView().setSystemUiVisibility(SettingsStore.dark(this)?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);}

    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setPadding(24,28,24,20);root.setBackgroundColor(Ui.bg(this));

        avatar=new ImageView(this);avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);avatar.setBackground(Ui.rounded(Ui.accent(this),100));root.addView(avatar,Ui.lp(104,104));
        name=Ui.text(this,"",27);name.setGravity(Gravity.CENTER);name.setTypeface(null,1);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.setMargins(0,18,0,3);root.addView(name,np);
        number=Ui.text(this,"",15);number.setTextColor(Ui.muted(this));number.setGravity(Gravity.CENTER);root.addView(number);
        status=Ui.text(this,"",17);status.setGravity(Gravity.CENTER);status.setTextColor(Ui.accent(this));root.addView(status,new LinearLayout.LayoutParams(-1,40));
        timer=Ui.text(this,"",13);timer.setTextColor(Ui.muted(this));timer.setGravity(Gravity.CENTER);root.addView(timer);

        ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setGravity(Gravity.CENTER_HORIZONTAL);
        controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);controls.setPadding(0,22,0,10);body.addView(controls,new LinearLayout.LayoutParams(-1,-2));
        keypad=buildKeypad();keypad.setVisibility(View.GONE);body.addView(keypad);
        scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        setContentView(root);
    }

    private void refresh(){
        Call c=current();if(c==null){finish();return;}
        int state=c.getState();String n=getNumber(c);name.setText(findName(n));number.setText(n);status.setText(stateText(state));
        loadPhoto(n);
        if(state==Call.STATE_ACTIVE){if(activeAt==0)activeAt=System.currentTimeMillis();timer.setText(formatElapsed(System.currentTimeMillis()-activeAt));}
        else {activeAt=0;timer.setText("");}
        buildControls(state);
        if(SettingsStore.autoSpeaker(this)&&state==Call.STATE_ACTIVE) setSpeaker(true);
    }

    private void refreshBottomBar(){
        if(bottomBar==null)return;
        if(current()==null){bottomBar.setVisibility(View.GONE);return;}
        bottomBar.setVisibility(View.VISIBLE);
        TextView left=(TextView)bottomBar.getChildAt(0), center=(TextView)bottomBar.getChildAt(1), right=(TextView)bottomBar.getChildAt(2);
        left.setText(isMuted()?"🎙\nמושתק":"🎙\nהשתקה");
        left.setTextColor(isMuted()?Ui.accent(this):Ui.muted(this));
        IncomingCallService svc=IncomingCallService.getInstance();
        String route=svc==null?"טלפון":svc.currentRouteName();
        String icon="◉";
        if("Bluetooth".equals(route))icon="♢";
        else if("רמקול".equals(route))icon="⌁";
        right.setText(icon+"\n"+route);
        right.setTextColor("טלפון".equals(route)?Ui.muted(this):Ui.accent(this));
        center.setText(stateText(current().getState())+"  •  "+timer.getText());
    }

    private void buildControls(int state){
        controls.removeAllViews();
        if(state==Call.STATE_RINGING){
            Button decline=roundButton("דחה",Color.rgb(210,60,70));decline.setOnClickListener(v->end());
            Button answer=roundButton("ענה",Ui.accent(this));answer.setOnClickListener(v->{Call c=current();if(c!=null)c.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY);});
            controls.addView(decline,Ui.lp(125,58));controls.addView(answer,Ui.lp(125,58));
            return;
        }
        Button mute=roundButton(isMuted()?"מושתק":"השתק",isMuted()?Ui.accent(this):Ui.card(this));mute.setOnClickListener(v->{IncomingCallService.toggleMute();refresh();});
        Button speaker=roundButton(isSpeaker()?"רמקול פעיל":"רמקול",isSpeaker()?Ui.accent(this):Ui.card(this));speaker.setOnClickListener(v->{setSpeaker(!isSpeaker());refresh();});
        Button pad=roundButton(keypad.getVisibility()==View.VISIBLE?"הסתר לוח":"לוח מקשים",Ui.card(this));pad.setOnClickListener(v->{keypad.setVisibility(keypad.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);});
        controls.addView(mute,Ui.lp(112,56));controls.addView(speaker,Ui.lp(112,56));controls.addView(pad,Ui.lp(126,56));
        LinearLayout bottom=new LinearLayout(this);bottom.setGravity(Gravity.CENTER);Button end=roundButton("סיום שיחה",Color.rgb(210,60,70));end.setTextColor(Color.WHITE);end.setOnClickListener(v->end());bottom.addView(end,Ui.lp(170,58));controls.addView(bottom);
    }

    private Button roundButton(String s,int color){Button b=Ui.button(this,s);b.setTextColor(color==Ui.card(this)?Ui.ink(this):Color.WHITE);b.setBackground(Ui.rounded(color,30));b.setTextSize(14);return b;}

    private LinearLayout buildKeypad(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(0,8,0,8);
        String[][] keys={{"1",""},{"2","ABC"},{"3","DEF"},{"4","GHI"},{"5","JKL"},{"6","MNO"},{"7","PQRS"},{"8","TUV"},{"9","WXYZ"},{"*",""},{"0","+"},{"#",""}};
        for(int r=0;r<4;r++){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER);for(int col=0;col<3;col++){String[] k=keys[r*3+col];Button b=Ui.button(this,k[0]+"\n"+k[1]);b.setTextSize(18);b.setOnClickListener(v->{String d=((Button)v).getText().toString().substring(0,1);Call c=current();if(c!=null){c.playDtmfTone(d.charAt(0));handler.postDelayed(()->{try{c.stopDtmfTone();}catch(Exception ignored){}},160);}});LinearLayout.LayoutParams p=Ui.weight();p.setMargins(4,4,4,4);row.addView(b,p);}box.addView(row,new LinearLayout.LayoutParams(-1,62));}
        return box;
    }

    private boolean isMuted(){IncomingCallService s=IncomingCallService.getInstance();return s!=null&&s.isMutedNow();}
    private boolean isSpeaker(){IncomingCallService s=IncomingCallService.getInstance();return s!=null&&s.isSpeakerNow();}
    private void setSpeaker(boolean on){IncomingCallService s=IncomingCallService.getInstance();if(s!=null)s.setSpeakerNow(on);}
    private void end(){Call c=current();if(c!=null)c.disconnect();}
    private Call current(){return IncomingCallService.getCurrentCall();}

    private String getNumber(Call c){try{Uri u=c.getDetails().getHandle();return u==null?"":u.getSchemeSpecificPart();}catch(Exception e){return"";}}
    private String findName(String n){if(n==null||n.isEmpty())return"טלפון";if(checkSelfPermission(android.Manifest.permission.READ_CONTACTS)!=0)return n;Cursor c=null;try{c=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},ContactsContract.CommonDataKinds.Phone.NUMBER+" LIKE ?",new String[]{"%"+n.replace("-","")+"%"},null);if(c!=null&&c.moveToFirst()){String x=c.getString(0);if(x!=null&&!x.isEmpty())return x;}}finally{if(c!=null)c.close();}return n;}
    private void loadPhoto(String n){avatar.setImageDrawable(null);if(n==null||n.isEmpty())return;Cursor c=null;try{c=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,new String[]{ContactsContract.CommonDataKinds.Phone.PHOTO_URI},ContactsContract.CommonDataKinds.Phone.NUMBER+" LIKE ?",new String[]{"%"+n.replace("-","")+"%"},null);if(c!=null&&c.moveToFirst()){String p=c.getString(0);if(p!=null&&!p.isEmpty())avatar.setImageURI(Uri.parse(p));}}finally{if(c!=null)c.close();}if(avatar.getDrawable()==null){TextView tmp=new TextView(this);tmp.setText(name.getText().toString().substring(0,1));tmp.setTextColor(Color.WHITE);tmp.setTextSize(38);tmp.setGravity(Gravity.CENTER);tmp.setBackground(Ui.rounded(Ui.accent(this),100));android.graphics.Bitmap bm=android.graphics.Bitmap.createBitmap(104,104,android.graphics.Bitmap.Config.ARGB_8888);android.graphics.Canvas cv=new android.graphics.Canvas(bm);tmp.layout(0,0,104,104);tmp.draw(cv);avatar.setImageBitmap(bm);}}
    private String stateText(int s){switch(s){case Call.STATE_RINGING:return"שיחה נכנסת";case Call.STATE_DIALING:return"מחייג…";case Call.STATE_CONNECTING:return"מתחבר…";case Call.STATE_ACTIVE:return"בשיחה";case Call.STATE_HOLDING:return"בהמתנה";default:return"שיחה";}}
    private String formatElapsed(long ms){long sec=ms/1000;return String.format(Locale.US,"%02d:%02d",sec/60,sec%60);}
}
