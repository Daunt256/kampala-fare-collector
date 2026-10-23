package com.abaho.farecollector;

import android.content.*;
import android.security.keystore.*;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

final class Store {
    private final Context context;
    private final SharedPreferences prefs;
    Store(Context c) { context=c.getApplicationContext();prefs=context.getSharedPreferences("collector",Context.MODE_PRIVATE); }
    String get(String key,String fallback){return prefs.getString(key,fallback);}
    void set(String key,String value){prefs.edit().putString(key,value).apply();}
    boolean consent(){return prefs.getBoolean("consent",false);}
    void consent(boolean b){prefs.edit().putBoolean("consent",b).apply();}
    String deviceId(){String id=get("device_id","");if(id.isEmpty()){id=UUID.randomUUID().toString();set("device_id",id);}return id;}
    private SecretKey key() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(!ks.containsAlias("collector_token")){
            KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            gen.init(new KeyGenParameterSpec.Builder("collector_token",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            gen.generateKey();
        }
        return (SecretKey)ks.getKey("collector_token",null);
    }
    void token(String token) throws Exception {
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        String value=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(cipher.doFinal(token.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);
        set("token",value);
    }
    String token() throws Exception {
        String value=get("token","");if(value.isEmpty())return "";
        String[] bits=value.split(":",2);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(bits[0],Base64.NO_WRAP)));
        return new String(cipher.doFinal(Base64.decode(bits[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);
    }
    JSONObject config(){try{return new JSONObject(get("config",defaults().toString()));}catch(JSONException e){return defaults();}}
    void config(JSONObject config){set("config",config.toString());}
    static JSONObject defaults(){
        try {
            JSONArray routes=new JSONArray();
            JSONArray products=new JSONArray();
            for(FareLogic.Product p:FareLogic.defaults())products.put(new JSONObject().put("id",p.id).put("label",p.label).put("provider",p.provider).put("mode",p.mode).put("aliases",new JSONArray(p.aliases)));
            return new JSONObject().put("routes",routes).put("products",products).put("timezone","Africa/Kampala");
        }catch(JSONException e){throw new IllegalStateException(e);}
    }
    List<FareLogic.Product> products(){
        List<FareLogic.Product> out=new ArrayList<>();JSONArray rows=config().optJSONArray("products");
        if(rows!=null)for(int i=0;i<rows.length();i++){
            JSONObject r=rows.optJSONObject(i);if(r==null)continue;
            JSONArray a=r.optJSONArray("aliases");String[] aliases=new String[a==null?0:a.length()];
            for(int j=0;j<aliases.length;j++)aliases[j]=a.optString(j);
            out.add(new FareLogic.Product(r.optString("id"),r.optString("label"),r.optString("provider"),r.optString("mode"),aliases));
        }
        return out;
    }
    JSONObject flow(String provider,String mode){try{return new JSONObject(get("flow_"+provider+"_"+mode,"{}"));}catch(JSONException e){return new JSONObject();}}
    void flow(String provider,String mode,JSONObject flow){set("flow_"+provider+"_"+mode,flow.toString());}
    synchronized JSONArray records(){try{return new JSONArray(get("records","[]"));}catch(JSONException e){return new JSONArray();}}
    synchronized void add(JSONObject record){JSONArray all=records();all.put(record);prefs.edit().putString("records",all.toString()).commit();}
    synchronized void saveRecords(JSONArray rows){if(!prefs.edit().putString("records",rows.toString()).commit())throw new IllegalStateException("Phone storage is full or unavailable. Free some space and retry.");}
    synchronized void acknowledge(Set<String> ids){
        JSONArray all=records();for(int i=0;i<all.length();i++){
            JSONObject r=all.optJSONObject(i);if(r!=null&&ids.contains(r.optString("id")))try{r.put("uploaded",true);}catch(JSONException ignored){}
        }
        saveRecords(all);
    }
    synchronized JSONArray pending(){JSONArray out=new JSONArray(),all=records();for(int i=0;i<all.length();i++){JSONObject r=all.optJSONObject(i);if(r!=null&&!r.optBoolean("uploaded")&&out.length()<100)out.put(r);}return out;}
    void diagnostic(JSONObject value) throws IOException {
        File file=new File(context.getFilesDir(),"diagnostic.json");
        try(FileOutputStream out=new FileOutputStream(file)){out.write(value.toString(2).getBytes(StandardCharsets.UTF_8));}
        catch(JSONException e){throw new IOException(e);}
    }
    File diagnosticFile(){return new File(context.getFilesDir(),"diagnostic.json");}
}
