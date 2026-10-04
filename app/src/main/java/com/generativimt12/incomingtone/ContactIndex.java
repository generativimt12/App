package com.generativimt12.incomingtone;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.text.TextUtils;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ContactIndex {
    public static final class ContactRow {
        public final String id,name,number,photo;
        public final boolean favorite;
        ContactRow(String id,String name,String number,String photo,boolean favorite){this.id=id;this.name=name;this.number=number;this.photo=photo;this.favorite=favorite;}
        public String contactId(){if(id==null)return"";int p=id.indexOf("|");return p<0?id:id.substring(0,p);}
    }
    public static final class CallRow {
        public final String name,number;
        public final long date,duration;
        public final int type;
        CallRow(String name,String number,long date,int type,long duration){this.name=name;this.number=number;this.date=date;this.type=type;this.duration=duration;}
    }

    private static final ExecutorService EXEC=Executors.newSingleThreadExecutor();
    private static final Object LOCK=new Object();
    private static volatile IndexDb db;
    private static volatile long lastRequest=0;

    private static IndexDb db(Context c){
        if(db==null)synchronized(LOCK){if(db==null)db=new IndexDb(c.getApplicationContext());}
        return db;
    }

    public static void refreshAsync(Context c){
        if(c.checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED &&
           c.checkSelfPermission(Manifest.permission.READ_CALL_LOG)!=PackageManager.PERMISSION_GRANTED)return;
        long now=System.currentTimeMillis();
        if(now-lastRequest<30000)return;
        lastRequest=now;
        EXEC.execute(()->refresh(c.getApplicationContext()));
    }

    public static void refreshAsync(Context c,Runnable done){
        if(c.checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED &&
           c.checkSelfPermission(Manifest.permission.READ_CALL_LOG)!=PackageManager.PERMISSION_GRANTED)return;
        long now=System.currentTimeMillis();
        if(now-lastRequest<30000){if(done!=null)new android.os.Handler(android.os.Looper.getMainLooper()).post(done);return;}
        lastRequest=now;
        EXEC.execute(()->{refresh(c.getApplicationContext());if(done!=null)new android.os.Handler(android.os.Looper.getMainLooper()).post(done);});
    }

    private static void refresh(Context c){
        SQLiteDatabase d=db(c).getWritableDatabase();
        d.beginTransaction();
        try{
            if(c.checkSelfPermission(Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED){
                d.delete("contacts",null,null);
                Cursor cur=null;
                try{
                    cur=c.getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{ContactsContract.CommonDataKinds.Phone.CONTACT_ID,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER,ContactsContract.CommonDataKinds.Phone.PHOTO_URI,ContactsContract.CommonDataKinds.Phone.STARRED},
                        null,null,null);
                    if(cur!=null)while(cur.moveToNext()){
                        String id=cur.getString(0);
                        String name=cur.getString(1);
                        String num=cur.getString(2);
                        String photo=cur.getString(3);
                        if(num==null)num="";
                        android.content.ContentValues v=new android.content.ContentValues();
                        v.put("id",id+"|"+num);
                        v.put("name",name==null?"":name);
                        v.put("name_norm",normalizeName(name));
                        v.put("number",num);
                        v.put("normalized",normalizeNumber(num));
                        v.put("photo",photo==null?"":photo);
                        v.put("favorite",cur.getInt(4));
                        d.insertWithOnConflict("contacts",null,v,SQLiteDatabase.CONFLICT_REPLACE);
                    }
                }finally{if(cur!=null)cur.close();}
            }

            if(c.checkSelfPermission(Manifest.permission.READ_CALL_LOG)==PackageManager.PERMISSION_GRANTED){
                d.delete("calls",null,null);
                Cursor cur=null;
                try{
                    cur=c.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                        new String[]{CallLog.Calls._ID,CallLog.Calls.NUMBER,CallLog.Calls.CACHED_NAME,CallLog.Calls.DATE,CallLog.Calls.TYPE,CallLog.Calls.DURATION},
                        null,null,CallLog.Calls.DATE+" DESC");
                    if(cur!=null)while(cur.moveToNext()){
                        android.content.ContentValues v=new android.content.ContentValues();
                        v.put("_id",cur.getLong(0));
                        v.put("number",cur.getString(1)==null?"":cur.getString(1));
                        v.put("name",cur.getString(2)==null?"":cur.getString(2));
                        v.put("date",cur.getLong(3));
                        v.put("type",cur.getInt(4));
                        v.put("duration",cur.getLong(5));
                        d.insertWithOnConflict("calls",null,v,SQLiteDatabase.CONFLICT_REPLACE);
                    }
                }finally{if(cur!=null)cur.close();}
            }
            android.content.ContentValues meta=new android.content.ContentValues();
            meta.put("key","last_sync");meta.put("value",System.currentTimeMillis());
            d.insertWithOnConflict("meta",null,meta,SQLiteDatabase.CONFLICT_REPLACE);
            d.setTransactionSuccessful();
        }finally{d.endTransaction();}
    }

    public static List<ContactRow> contacts(Context c,String q,int limit){
        if(TextUtils.isEmpty(q))return simpleContacts(c,limit);
        return rankedContacts(db(c).getReadableDatabase(),q,"",limit);
    }

    public static List<ContactRow> contacts(Context c,String q,String digits,int limit){
        if(TextUtils.isEmpty(q)&&TextUtils.isEmpty(digits))return simpleContacts(c,limit);
        return rankedContacts(db(c).getReadableDatabase(),q,digits,limit);
    }

    private static List<ContactRow> simpleContacts(Context c,int limit){
        SQLiteDatabase d=db(c).getReadableDatabase();
        ArrayList<ContactRow> out=new ArrayList<>();
        Cursor cur=d.query("contacts",new String[]{"id","name","number","photo","favorite"},null,null,null,null,"name COLLATE NOCASE ASC",""+Math.max(1,limit));
        try{while(cur.moveToNext())out.add(new ContactRow(cur.getString(0),cur.getString(1),cur.getString(2),cur.getString(3),cur.getInt(4)==1));}
        finally{cur.close();}
        return out;
    }

    public static List<ContactRow> favorites(Context c,int limit){
        SQLiteDatabase d=db(c).getReadableDatabase();
        ArrayList<ContactRow> out=new ArrayList<>();
        Cursor cur=d.query("contacts",new String[]{"id","name","number","photo","favorite"},"favorite=1",null,null,null,"name COLLATE NOCASE ASC",""+Math.max(1,limit));
        try{while(cur.moveToNext())out.add(new ContactRow(cur.getString(0),cur.getString(1),cur.getString(2),cur.getString(3),true));}
        finally{cur.close();}
        return out;
    }

    public static List<CallRow> calls(Context c,String q,int limit){
        SQLiteDatabase d=db(c).getReadableDatabase();
        ArrayList<CallRow> out=new ArrayList<>();
        String sel=null;String[] args=null;
        if(!TextUtils.isEmpty(q)){
            String n=normalizeNumber(q);
            if(!n.isEmpty()){
                sel="name LIKE ? OR number LIKE ? OR number LIKE ?";
                args=new String[]{"%"+q+"%","%"+q+"%","%"+n+"%"};
            }else{
                sel="name LIKE ?";
                args=new String[]{"%"+q+"%"};
            }
        }
        Cursor cur=d.query("calls",new String[]{"name","number","date","type","duration"},sel,args,null,null,"date DESC",""+Math.max(1,limit));
        try{while(cur.moveToNext())out.add(new CallRow(cur.getString(0),cur.getString(1),cur.getLong(2),cur.getInt(3),cur.getLong(4)));}
        finally{cur.close();}
        return out;
    }

    private static ArrayList<ContactRow> rankedContacts(SQLiteDatabase d,String q,String digits,int limit){
        String nq=normalizeName(q);
        String nd=normalizeNumber(digits);
        StringBuilder sel=new StringBuilder();
        ArrayList<String> argList=new ArrayList<>();
        if(!nq.isEmpty()){
            sel.append("name_norm LIKE ? OR name LIKE ?");
            argList.add("%"+nq+"%");argList.add("%"+q+"%");
        }
        if(!nd.isEmpty()){
            if(sel.length()>0)sel.append(" OR ");
            sel.append("number LIKE ? OR normalized LIKE ?");
            argList.add("%"+nd+"%");argList.add("%"+nd+"%");
        }

        ArrayList<Ranked> rows=new ArrayList<>();
        Cursor cur=null;
        try{
            cur=d.query("contacts",new String[]{"id","name","number","photo","favorite","name_norm"},
                sel.length()==0?null:sel.toString(),
                sel.length()==0?null:argList.toArray(new String[0]),
                null,null,"name COLLATE NOCASE ASC","1200");

            scoreRows(cur,rows,nq,nd);
        }finally{if(cur!=null)cur.close();}

        if(rows.isEmpty() && !nq.isEmpty()){
            cur=null;
            try{
                cur=d.query("contacts",new String[]{"id","name","number","photo","favorite","name_norm"},null,null,null,null,"name COLLATE NOCASE ASC","700");
                scoreRows(cur,rows,nq,nd);
            }finally{if(cur!=null)cur.close();}
        }

        Collections.sort(rows,(a,b)->{
            int s=Integer.compare(b.score,a.score);
            return s!=0?s:a.row.name.compareToIgnoreCase(b.row.name);
        });
        ArrayList<ContactRow> out=new ArrayList<>();
        int max=Math.max(1,limit);
        for(int i=0;i<rows.size()&&i<max;i++)out.add(rows.get(i).row);
        return out;
    }

    private static void scoreRows(Cursor cur,ArrayList<Ranked> rows,String nq,String nd){
        if(cur==null)return;
        while(cur.moveToNext()){
            String name=cur.getString(1)==null?"":cur.getString(1);
            String num=cur.getString(2)==null?"":cur.getString(2);
            String nn=cur.getString(5);if(nn==null||nn.isEmpty())nn=normalizeName(name);
            String pn=normalizeNumber(num);
            int score=0;
            if(!nq.isEmpty()){
                if(nn.equals(nq))score=Math.max(score,10000);
                if(nn.startsWith(nq))score=Math.max(score,9000);
                if((" "+nn).contains(" "+nq))score=Math.max(score,8300);
                if(nn.contains(nq))score=Math.max(score,6500);
                String[] parts=nq.split(" +");
                for(String part:parts)if(!part.isEmpty()&&nn.contains(part))score=Math.max(score,6000);
                int dist=fuzzy(nn,nq);
                if(dist<=2)score=Math.max(score,4400-dist*500);
                if(initials(nn).startsWith(nq))score=Math.max(score,5200);
            }
            if(!nd.isEmpty()){
                if(pn.equals(nd))score=Math.max(score,11000);
                else if(pn.endsWith(nd))score=Math.max(score,8800);
                else if(pn.contains(nd))score=Math.max(score,7200);
            }
            if(score>0)rows.add(new Ranked(score,new ContactRow(cur.getString(0),name,num,cur.getString(3),cur.getInt(4)==1)));
        }
    }

    private static final class Ranked{
        final int score;final ContactRow row;
        Ranked(int score,ContactRow row){this.score=score;this.row=row;}
    }

    private static String initials(String s){
        StringBuilder b=new StringBuilder();
        for(String p:s.split(" +"))if(!p.isEmpty())b.append(p.charAt(0));
        return b.toString();
    }

    private static int fuzzy(String a,String b){
        if(a.length()>80||b.length()>40)return 99;
        int[] prev=new int[b.length()+1];
        for(int j=0;j<=b.length();j++)prev[j]=j;
        for(int i=1;i<=a.length();i++){
            int[] cur=new int[b.length()+1];cur[0]=i;
            for(int j=1;j<=b.length();j++)
                cur[j]=Math.min(Math.min(cur[j-1]+1,prev[j]+1),prev[j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1));
            prev=cur;
        }
        return prev[b.length()];
    }

    private static String normalizeNumber(String s){return s==null?"":s.replaceAll("[^0-9]","");}

    private static String normalizeName(String s){
        if(s==null)return"";
        String x=Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}+","");
        x=x.toLowerCase(Locale.ROOT).replace('ך','כ').replace('ם','מ').replace('ן','נ').replace('ף','פ').replace('ץ','צ');
        return x.replaceAll("[^\\p{L}\\p{Nd}]+"," ").trim().replaceAll(" +"," ");
    }

    private static final class IndexDb extends SQLiteOpenHelper{
        IndexDb(Context c){super(c,"incomingtone_index.db",null,2);}
        @Override public void onCreate(SQLiteDatabase d){
            d.execSQL("CREATE TABLE contacts(id TEXT PRIMARY KEY,name TEXT,name_norm TEXT,number TEXT,normalized TEXT,photo TEXT,favorite INTEGER)");
            d.execSQL("CREATE INDEX idx_contacts_name ON contacts(name COLLATE NOCASE)");
            d.execSQL("CREATE INDEX idx_contacts_name_norm ON contacts(name_norm)");
            d.execSQL("CREATE INDEX idx_contacts_normalized ON contacts(normalized)");
            d.execSQL("CREATE INDEX idx_contacts_number ON contacts(number)");
            d.execSQL("CREATE TABLE calls(_id INTEGER PRIMARY KEY,name TEXT,number TEXT,date INTEGER,type INTEGER,duration INTEGER)");
            d.execSQL("CREATE INDEX idx_calls_date ON calls(date DESC)");
            d.execSQL("CREATE INDEX idx_calls_name ON calls(name COLLATE NOCASE)");
            d.execSQL("CREATE INDEX idx_calls_number ON calls(number)");
            d.execSQL("CREATE TABLE meta(key TEXT PRIMARY KEY,value INTEGER)");
        }
        @Override public void onUpgrade(SQLiteDatabase d,int oldVersion,int newVersion){
            if(oldVersion<2){
                try{d.execSQL("ALTER TABLE contacts ADD COLUMN name_norm TEXT DEFAULT ''");}catch(Exception ignored){}
                try{d.execSQL("CREATE INDEX idx_contacts_name_norm ON contacts(name_norm)");}catch(Exception ignored){}
            }
        }
    }
}
