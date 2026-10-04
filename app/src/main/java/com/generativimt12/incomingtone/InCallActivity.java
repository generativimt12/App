package com.generativimt12.incomingtone;

import android.app.Activity;
import android.os.Bundle;
import android.telecom.Call;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class InCallActivity extends Activity {
    private static InCallActivity open;
    private TextView status, number;

    public static void finishIfOpen(){
        if(open!=null) open.runOnUiThread(()->{open.finish();open=null;});
    }

    @Override protected void onCreate(Bundle b){ super.onCreate(b); open=this; build(); refresh(); }
    @Override protected void onResume(){ super.onResume(); refresh(); }
    @Override protected void onDestroy(){ if(open==this)open=null; super.onDestroy(); }

    private void build(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setPadding(32,48,32,32);
        number=new TextView(this); number.setTextSize(30); number.setGravity(Gravity.CENTER);
        root.addView(number,new LinearLayout.LayoutParams(-1,-2));
        status=new TextView(this); status.setTextSize(18); status.setGravity(Gravity.CENTER); status.setPadding(0,18,0,40);
        root.addView(status);
        LinearLayout row=new LinearLayout(this);
        Button answer=new Button(this); answer.setText("מענה");
        answer.setOnClickListener(v->{Call c=current();if(c!=null)c.answer(0);});
        Button end=new Button(this); end.setText("סיים");
        end.setOnClickListener(v->{Call c=current();if(c!=null)c.disconnect();});
        row.addView(answer,new LinearLayout.LayoutParams(0,-2,1));
        row.addView(end,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(row);
        setContentView(root);
    }

    private Call current(){return IncomingCallService.getCurrentCall();}
    private void refresh(){
        Call c=current(); if(c==null){finish();return;}
        String n=c.getDetails().getHandle()==null?"":c.getDetails().getHandle().getSchemeSpecificPart();
        number.setText(n==null?"":n); status.setText(state(c.getState()));
    }
    private String state(int s){
        switch(s){
            case Call.STATE_RINGING:return"שיחה נכנסת";
            case Call.STATE_DIALING:return"מחייג…";
            case Call.STATE_CONNECTING:return"מתחבר…";
            case Call.STATE_ACTIVE:return"בשיחה";
            case Call.STATE_HOLDING:return"בהמתנה";
            default:return"סטטוס: "+s;
        }
    }
}
