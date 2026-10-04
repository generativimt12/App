package com.generativimt12.incomingtone;

import android.app.Activity;
import android.os.Bundle;
import android.telecom.Call;
import android.telecom.TelecomManager;

public class CallButtonActivity extends Activity {
    @Override public boolean onKeyDown(int keyCode, android.view.KeyEvent event){
        if(keyCode==android.view.KeyEvent.KEYCODE_CALL ||
           keyCode==android.view.KeyEvent.KEYCODE_ENTER ||
           keyCode==android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
           keyCode==android.view.KeyEvent.KEYCODE_HEADSETHOOK ||
           keyCode==android.view.KeyEvent.KEYCODE_FOCUS){
            IncomingCallService.answerIncoming();
            return true;
        }
        return super.onKeyDown(keyCode,event);
    }
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        String action=getIntent().getStringExtra("call_action");
        Call c=IncomingCallService.getCurrentCall();
        if("answer".equals(action)){IncomingCallService.answerIncoming();finish();return;}
        if("decline".equals(action)){if(c!=null&&c.getState()==Call.STATE_RINGING)try{c.disconnect();}catch(Exception ignored){}finish();return;}
        if("hangup".equals(action)){if(c!=null)try{c.disconnect();}catch(Exception ignored){}finish();return;}
        if(c!=null && c.getState()==Call.STATE_RINGING){
            IncomingCallService.answerIncoming();
            try { TelecomManager tm=getSystemService(TelecomManager.class); if(tm!=null) tm.acceptRingingCall(); } catch(Exception ignored) {}
            finish();
            return;
        }
        IncomingCallService.openRecentCalls(this);
        finish();
    }
}
