package com.generativimt12.incomingtone;

import android.app.Activity;
import android.os.Bundle;
import android.telecom.Call;

public class CallButtonActivity extends Activity {
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        Call c=IncomingCallService.getCurrentCall();
        if(c!=null && c.getState()==Call.STATE_RINGING){
            IncomingCallService.answerIncoming();
            finish();
            return;
        }
        IncomingCallService.openRecentCalls(this);
        finish();
    }
}
