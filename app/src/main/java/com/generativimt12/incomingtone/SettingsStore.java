package com.generativimt12.incomingtone;

import android.content.Context;
import android.content.SharedPreferences;

public final class SettingsStore {
    private static final String PREFS = "incomingtone_settings";
    private SettingsStore() {}
    private static SharedPreferences p(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}

    public static boolean dark(Context c){return p(c).getBoolean("dark",false);}
    public static void setDark(Context c,boolean v){p(c).edit().putBoolean("dark",v).apply();}
    public static int accent(Context c){return p(c).getInt("accent",0);}
    public static void setAccent(Context c,int v){p(c).edit().putInt("accent",v).apply();}
    public static boolean toneEnabled(Context c){return p(c).getBoolean("tone_enabled",true);}
    public static void setToneEnabled(Context c,boolean v){p(c).edit().putBoolean("tone_enabled",v).apply();}
    public static int tone(Context c){return p(c).getInt("tone",0);}
    public static void setTone(Context c,int v){p(c).edit().putInt("tone",v).apply();}
    public static int volume(Context c){return p(c).getInt("volume",75);}
    public static void setVolume(Context c,int v){p(c).edit().putInt("volume",Math.max(0,Math.min(100,v))).apply();}
    public static boolean vibrate(Context c){return p(c).getBoolean("vibrate",true);}
    public static void setVibrate(Context c,boolean v){p(c).edit().putBoolean("vibrate",v).apply();}
    public static boolean showFullScreen(Context c){return p(c).getBoolean("fullscreen",true);}
    public static void setShowFullScreen(Context c,boolean v){p(c).edit().putBoolean("fullscreen",v).apply();}
    public static boolean autoSpeaker(Context c){return p(c).getBoolean("auto_speaker",false);}
    public static void setAutoSpeaker(Context c,boolean v){p(c).edit().putBoolean("auto_speaker",v).apply();}

    public static String toneUri(Context c){return p(c).getString("tone_uri","");}
    public static void setToneUri(Context c,String uri){p(c).edit().putString("tone_uri",uri==null?"":uri).apply();}
    public static String toneLabel(Context c){return p(c).getString("tone_label","קלאסי");}
    public static void setToneLabel(Context c,String s){p(c).edit().putString("tone_label",s).apply();}
    public static int contactAccent(Context c,String contactId){
        if(contactId==null||contactId.isEmpty())return -1;
        return p(c).getInt("contact_accent_"+contactId,-1);
    }
    public static void setContactAccent(Context c,String contactId,int value){
        if(contactId!=null&&!contactId.isEmpty())p(c).edit().putInt("contact_accent_"+contactId,value).apply();
    }
    public static String toneName(Context c){
        String uri=toneUri(c);
        if(uri!=null&&!uri.isEmpty())return toneLabel(c);
        String[] n={"קלאסי","צליל כפול","פעמון","רך"};
        int i=Math.max(0,Math.min(n.length-1,tone(c)));
        return n[i];
    }
}
