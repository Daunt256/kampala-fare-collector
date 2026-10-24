package com.abaho.farecollector;

import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;

final class Api {
    interface Callback {void done(JSONObject result,Exception error);}
    static boolean validEndpoint(String value){
        try{URI u=new URI(value);return "https".equals(u.getScheme())&&"script.google.com".equals(u.getHost())&&u.getPort()==-1&&u.getUserInfo()==null&&u.getQuery()==null&&u.getFragment()==null&&u.getPath().matches("/macros/s/[A-Za-z0-9_-]+/exec");}
        catch(Exception e){return false;}
    }
    static void call(Store store,String action,JSONArray records,Callback callback){
        new Thread(()->{
            try{
                String endpoint=store.get("endpoint","");if(!validEndpoint(endpoint))throw new IOException("Enter the deployed Apps Script /exec URL in Connection.");
                JSONObject body=new JSONObject().put("action",action).put("token",store.token()).put("device_id",store.deviceId());
                if(records!=null)body.put("records",recordsForReceiver(records));
                JSONObject result=request(endpoint,body.toString());
                if(!result.optBoolean("ok"))throw new IOException(result.optString("error","Receiver rejected the request."));
                if(action.equals("config")){JSONObject config=result.getJSONObject("config");validateConfig(config);store.config(config);}
                if(action.equals("submit")){
                    JSONArray ack=result.getJSONArray("accepted_ids");Set<String> expected=new HashSet<>(),ids=new HashSet<>();
                    for(int i=0;i<records.length();i++)expected.add(records.getJSONObject(i).getString("id"));
                    for(int i=0;i<ack.length();i++)ids.add(ack.getString(i));
                    if(!ids.equals(expected))throw new IOException("The receiver did not acknowledge every quote. They remain queued for retry.");
                    store.acknowledge(ids);
                }
                callback.done(result,null);
            }catch(Exception e){callback.done(null,e);}
        },"collector-upload").start();
    }
    static JSONArray recordsForReceiver(JSONArray records)throws JSONException {
        JSONArray copy=new JSONArray(records.toString());
        for(int i=0;i<copy.length();i++){
            JSONObject row=copy.getJSONObject(i);
            row.put("raw",FareLogic.receiverFare(row.getString("raw")));
        }
        return copy;
    }
    private static void validateConfig(JSONObject config)throws JSONException,IOException{
        JSONArray routes=config.getJSONArray("routes"),products=config.getJSONArray("products");
        if(routes.length()<1||routes.length()>200||products.length()<1||products.length()>50)throw new IOException("Unexpected route or product configuration.");
        Set<String> ids=new HashSet<>();
        for(int i=0;i<routes.length();i++){JSONObject r=routes.getJSONObject(i);if(!ids.add(r.getString("id"))||r.getString("pickup").trim().isEmpty()||r.getString("dropoff").trim().isEmpty())throw new IOException("Invalid route configuration.");}
        for(int i=0;i<products.length();i++)if(!Arrays.asList(FareLogic.PROVIDERS).contains(products.getJSONObject(i).getString("provider")))throw new IOException("An unsupported provider was returned.");
    }
    private static JSONObject request(String address,String payload)throws Exception{
        URL url=new URL(address);String method="POST";
        for(int hop=0;hop<4;hop++){
            if(!url.getProtocol().equals("https")||!Arrays.asList("script.google.com","script.googleusercontent.com").contains(url.getHost())||url.getUserInfo()!=null||(url.getPort()!=-1&&url.getPort()!=443))throw new IOException("Untrusted receiver redirect.");
            HttpsURLConnection c=(HttpsURLConnection)url.openConnection();
            c.setConnectTimeout(15000);c.setReadTimeout(25000);c.setInstanceFollowRedirects(false);c.setRequestMethod(method);
            try{
                if(method.equals("POST")){
                    c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");
                    byte[] data=payload.getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(data.length);
                    try(OutputStream out=c.getOutputStream()){out.write(data);}
                }
                int status=c.getResponseCode();
                if(status==301||status==302||status==303||status==307||status==308){
                    String location=c.getHeaderField("Location");if(location==null)throw new IOException("Missing redirect location.");
                    url=new URL(url,location);if(status==301||status==302||status==303)method="GET";
                    continue;
                }
                if(status!=200)throw new IOException("Receiver HTTP "+status+". Check deployment access and connection settings.");
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                try(InputStream in=c.getInputStream()){
                    byte[] buffer=new byte[4096];int n;
                    while((n=in.read(buffer))!=-1){if(bytes.size()+n>2000000)throw new IOException("Receiver response too large.");bytes.write(buffer,0,n);}
                }
                try{return new JSONObject(bytes.toString("UTF-8"));}
                catch(JSONException e){throw new IOException("The URL returned a web page. Deploy as a web app with the setup guide's access settings.");}
            }finally{c.disconnect();}
        }
        throw new IOException("Too many receiver redirects.");
    }
}
