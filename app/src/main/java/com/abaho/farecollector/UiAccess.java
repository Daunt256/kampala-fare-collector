package com.abaho.farecollector;

import android.view.accessibility.*;
import org.json.*;
import java.util.*;

final class UiAccess {
    static String text(CharSequence s){return s==null?"":s.toString();}
    static String label(AccessibilityNodeInfo n){String s=text(n.getText());return s.isEmpty()?text(n.getContentDescription()):s;}
    static List<FareLogic.Node> snapshot(AccessibilityNodeInfo root){List<FareLogic.Node> out=new ArrayList<>();walk(root,"0",0,out);return out;}
    private static void walk(AccessibilityNodeInfo n,String path,int depth,List<FareLogic.Node> out){
        if(n==null||depth>20||out.size()>=700||n.isPassword()||!n.isVisibleToUser())return;
        out.add(new FareLogic.Node(path,label(n)));
        for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo child=n.getChild(i);if(child!=null){walk(child,path+"/"+i,depth+1,out);child.recycle();}}
    }
    static String allText(AccessibilityNodeInfo root){StringBuilder s=new StringBuilder();for(FareLogic.Node n:snapshot(root))s.append(n.text).append('\n');return s.toString();}
    static AccessibilityNodeInfo clickTarget(AccessibilityNodeInfo source){
        AccessibilityNodeInfo n=AccessibilityNodeInfo.obtain(source);
        for(int i=0;i<4&&n!=null;i++){
            if(n.isClickable())return n;
            AccessibilityNodeInfo parent=n.getParent();n.recycle();n=parent;
        }
        if(n!=null)n.recycle();return null;
    }
    static JSONObject selector(AccessibilityNodeInfo n,JSONObject route)throws JSONException{
        String t=text(n.getText()),d=text(n.getContentDescription());boolean dynamic=false;
        for(String key:new String[]{"pickup","dropoff"}){
            String q=route.optString(key);if(q.isEmpty())continue;
            if(FareLogic.norm(t).contains(FareLogic.norm(q))){t="${"+key+"}";dynamic=true;}
            if(FareLogic.norm(d).contains(FareLogic.norm(q))){d="${"+key+"}";dynamic=true;}
        }
        return new JSONObject().put("id",text(n.getViewIdResourceName())).put("class",text(n.getClassName()))
            .put("text",t).put("description",d).put("contains",dynamic).put("editable",n.isEditable());
    }
    static String expand(String s,JSONObject route){return s.replace("${pickup}",route.optString("pickup")).replace("${dropoff}",route.optString("dropoff"));}
    static AccessibilityNodeInfo find(AccessibilityNodeInfo root,JSONObject selector,JSONObject route){
        List<AccessibilityNodeInfo> candidates=new ArrayList<>();findIn(root,selector,route,candidates,0);
        AccessibilityNodeInfo result=candidates.size()==1?candidates.remove(0):null;
        for(AccessibilityNodeInfo n:candidates)n.recycle();return result;
    }
    private static boolean stringMatch(String actual,String expected,boolean contains){
        if(expected.isEmpty())return true;
        String a=FareLogic.norm(actual),e=FareLogic.norm(expected);
        return contains?a.contains(e):a.equals(e);
    }
    private static void findIn(AccessibilityNodeInfo n,JSONObject selector,JSONObject route,List<AccessibilityNodeInfo> out,int depth){
        if(n==null||depth>20||out.size()>1||n.isPassword()||!n.isVisibleToUser())return;
        String id=selector.optString("id"),cl=selector.optString("class");
        String t=expand(selector.optString("text"),route),d=expand(selector.optString("description"),route);
        boolean input=selector.optBoolean("editable");
        boolean usable=input||!t.isEmpty()||!d.isEmpty();
        boolean idMatches=id.isEmpty()||id.equals(text(n.getViewIdResourceName()));
        boolean classMatches=cl.isEmpty()||cl.equals(text(n.getClassName()));
        boolean labelsMatch=input||(stringMatch(text(n.getText()),t,selector.optBoolean("contains"))
            &&stringMatch(text(n.getContentDescription()),d,selector.optBoolean("contains")));
        if(usable&&idMatches&&classMatches&&(!input||n.isEditable())&&labelsMatch)out.add(AccessibilityNodeInfo.obtain(n));
        for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo child=n.getChild(i);if(child!=null){findIn(child,selector,route,out,depth+1);child.recycle();}}
    }
    static JSONObject diagnostic(AccessibilityNodeInfo root,String provider,String mode,JSONObject route)throws JSONException{
        JSONArray nodes=new JSONArray();dump(root,"0",0,nodes);
        return new JSONObject().put("version","0.1.0").put("provider",provider).put("mode",mode).put("route_id",route.optString("id"))
            .put("captured_at",System.currentTimeMillis()).put("android_api",android.os.Build.VERSION.SDK_INT)
            .put("device_model",android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL).put("nodes",nodes);
    }
    private static String redact(String s){return s.replaceAll("(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}","[email]").replaceAll("(?<!\\d)(?:\\+?256[\\s-]?[37](?:[\\s-]?\\d){8}|0[37](?:[\\s-]?\\d){8})(?!\\d)","[phone]");}
    private static void dump(AccessibilityNodeInfo n,String path,int depth,JSONArray out)throws JSONException{
        if(n==null||depth>20||out.length()>=700||n.isPassword()||!n.isVisibleToUser())return;
        out.put(new JSONObject().put("path",path).put("id",text(n.getViewIdResourceName())).put("class",text(n.getClassName()))
            .put("text",redact(text(n.getText()))).put("description",redact(text(n.getContentDescription()))).put("editable",n.isEditable()).put("clickable",n.isClickable()));
        for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo child=n.getChild(i);if(child!=null){dump(child,path+"/"+i,depth+1,out);child.recycle();}}
    }
}
