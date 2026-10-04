package com.generativimt12.incomingtone;

import android.app.Activity;
import android.os.Bundle;
import android.telecom.Call;
import android.telecom.TelecomManager;

public class CallButtonActivity extends Activity {
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        Call c=IncomingCallService.getCurrentCall();
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
