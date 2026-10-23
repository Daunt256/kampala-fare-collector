package com.abaho.farecollector;

import android.accessibilityservice.AccessibilityService;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.*;
import android.view.*;
import android.view.accessibility.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public final class CollectorService extends AccessibilityService {
    static CollectorService instance;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Store store;private WindowManager manager;private LinearLayout panel;private TextView status;
    private boolean active=false,training=false,replaying=false,waiting=false;
    private String provider="",mode="Boda",field="",runId="",lastRecord="";
    private JSONObject route=new JSONObject();private JSONArray steps=new JSONArray();
    private int step=0,backCount=0;private long deadline=0,lastClickAt=0,startedAt=0;
    private String message="Ready";
    private List<String> batch=new ArrayList<>();private int batchIndex=0;private String batchRun="";

    protected void onServiceConnected(){super.onServiceConnected();instance=this;store=new Store(this);manager=(WindowManager)getSystemService(WINDOW_SERVICE);}
    public void onInterrupt(){stop("Accessibility was interrupted.");}
    public void onDestroy(){stop("Stopped");instance=null;super.onDestroy();}
    boolean isActive(){return active;}
    String message(){return message;}
    private void info(String text){message=text;if(status!=null)status.setText(text);store.set("last_status",text);}
    private void fail(String text){replaying=false;training=false;waiting=false;field="";info(text+"\nUse Read prices, return to the start screen, or Stop.");}
    private AccessibilityNodeInfo root(){
        String expected=provider.isEmpty()?"":FareLogic.packageFor(provider);
        AccessibilityNodeInfo n=getRootInActiveWindow();
        if(n!=null&&expected.equals(UiAccess.text(n.getPackageName())))return n;
        if(n!=null)n.recycle();
        for(AccessibilityWindowInfo window:getWindows()){
            if(!window.isActive()&&!window.isFocused())continue;
            AccessibilityNodeInfo r=window.getRoot();
            if(r!=null&&expected.equals(UiAccess.text(r.getPackageName())))return r;
            if(r!=null)r.recycle();
        }
        return null;
    }
    void start(String selected,String vertical,JSONObject selectedRoute,boolean teach,boolean run){
        stop("Starting");provider=selected;mode=vertical;route=selectedRoute;
        training=teach;replaying=run;active=true;runId=UUID.randomUUID().toString();startedAt=System.currentTimeMillis();
        steps=teach?new JSONArray():store.flow(provider,mode).optJSONArray("steps");if(steps==null)steps=new JSONArray();
        step=0;backCount=0;lastRecord="";field="";
        if(run&&(steps.length()==0||!store.flow(provider,mode).optBoolean("has_pickup")||!store.flow(provider,mode).optBoolean("has_dropoff"))){stop("Teach both route fields for this app and mode first.");return;}
        showPanel();
        info(teach?"Training "+provider+" "+mode+". Navigate to prices. Mark each route field before tapping it.":run?"Starting learned workflow…":"Open the fare screen, then Read prices.");
        Intent launch=getPackageManager().getLaunchIntentForPackage(FareLogic.packageFor(provider));
        if(launch==null){stop(provider+" is not installed.");return;}
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try{startActivity(launch);}catch(RuntimeException e){stop("Could not open "+provider+". Open it manually and retry.");return;}
        if(run){deadline=SystemClock.uptimeMillis()+12000;handler.postDelayed(this::tick,1200);}
    }
    void startBatch(String vertical,JSONObject selectedRoute){
        store.set("review","");start("SafeBoda",vertical,selectedRoute,false,true);
        if(active){batch=new ArrayList<>(Arrays.asList(FareLogic.PROVIDERS));batchIndex=0;batchRun=UUID.randomUUID().toString();}
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextSize(11);b.setAllCaps(false);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(6),0,dp(6),0);b.setOnClickListener(v->action.run());return b;}
    private void showPanel(){
        removePanel();panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(6),dp(5),dp(6),dp(5));panel.setBackgroundColor(Color.rgb(18,38,50));
        status=new TextView(this);status.setTextColor(Color.WHITE);status.setTextSize(11);status.setMaxLines(3);panel.addView(status);
        LinearLayout actions=new LinearLayout(this);panel.addView(actions);
        actions.addView(button("Read prices",this::capture),new LinearLayout.LayoutParams(0,dp(38),1));
        actions.addView(button("Inspect",this::inspect),new LinearLayout.LayoutParams(0,dp(38),1));
        actions.addView(button("Stop",()->stop("Stopped by you.")),new LinearLayout.LayoutParams(0,dp(38),1));
        if(training){
            LinearLayout teach=new LinearLayout(this);panel.addView(teach);
            teach.addView(button("Pickup field",()->armField("pickup")),new LinearLayout.LayoutParams(0,dp(38),1));
            teach.addView(button("Dropoff field",()->armField("dropoff")),new LinearLayout.LayoutParams(0,dp(38),1));
            teach.addView(button("Back",this::teachBack),new LinearLayout.LayoutParams(0,dp(38),1));
            teach.addView(button("Finish",this::finishTraining),new LinearLayout.LayoutParams(0,dp(38),1));
        }
        WindowManager.LayoutParams params=new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP;params.y=Math.max(dp(24),getResources().getDisplayMetrics().heightPixels-dp(training?220:165));
        final float[] drag=new float[2];
        status.setOnTouchListener((v,event)->{
            if(event.getAction()==MotionEvent.ACTION_DOWN){drag[0]=event.getRawY();drag[1]=params.y;return true;}
            if(event.getAction()==MotionEvent.ACTION_MOVE&&panel!=null){
                int max=Math.max(dp(24),getResources().getDisplayMetrics().heightPixels-panel.getHeight()-dp(24));
                params.y=Math.max(dp(24),Math.min(max,(int)(drag[1]+event.getRawY()-drag[0])));manager.updateViewLayout(panel,params);return true;
            }
            if(event.getAction()==MotionEvent.ACTION_UP){v.performClick();return true;}return false;
        });
        manager.addView(panel,params);
    }
    private void removePanel(){if(panel!=null&&manager!=null){try{manager.removeView(panel);}catch(RuntimeException ignored){}}panel=null;status=null;}
    void stop(String text){active=false;training=false;replaying=false;waiting=false;field="";batch.clear();batchRun="";handler.removeCallbacksAndMessages(null);removePanel();message=text;if(store!=null)store.set("last_status",text);}
    private void armField(String value){field=value;info("Tap the editable "+value+" search field. I will fill: "+route.optString(value));}
    private void teachBack(){if(!training)return;try{steps.put(new JSONObject().put("action","back"));performGlobalAction(GLOBAL_ACTION_BACK);info("Back recorded. Continue to the price screen.");}catch(JSONException e){fail("Could not record Back.");}}
    private void add(JSONObject value)throws JSONException{
        if(steps.length()>=60){fail("Training reached 60 actions. Finish and shorten the workflow.");training=false;return;}
        steps.put(value);info("Training "+provider+": "+steps.length()+" actions. Mark fields before tapping them.");
    }
    public void onAccessibilityEvent(AccessibilityEvent event){
        if(!active||!training||!FareLogic.packageFor(provider).equals(UiAccess.text(event.getPackageName())))return;
        if(event.getEventType()!=AccessibilityEvent.TYPE_VIEW_CLICKED&&event.getEventType()!=AccessibilityEvent.TYPE_VIEW_FOCUSED)return;
        AccessibilityNodeInfo source=event.getSource();if(source==null)return;
        try{
            AccessibilityNodeInfo current=root();
            if(current==null)return;
            try{if(FareLogic.activeTripOrLogin(UiAccess.allText(current))){fail("Sign-in or an active ride needs your attention.");return;}}
            finally{current.recycle();}
            if(source.isPassword()){fail("Password fields are not recorded.");return;}
            if(!field.isEmpty()&&source.isEditable()){
                String key=field;field="";JSONObject selector=UiAccess.selector(source,route);selector.put("text","").put("description","");
                add(new JSONObject().put("action","set_text").put("field",key).put("selector",selector));
                Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,route.optString(key));
                if(!source.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args))fail("This app did not accept text entry. Use Inspect.");
                return;
            }
            if(event.getEventType()!=AccessibilityEvent.TYPE_VIEW_CLICKED||!field.isEmpty())return;
            AccessibilityNodeInfo target=UiAccess.clickTarget(source);if(target==null)return;
            try{
                String label=UiAccess.label(target);if(label.isEmpty())label=UiAccess.label(source);
                if(!FareLogic.safeClick(label,UiAccess.text(target.getContentDescription()))){fail("That control cannot be recorded. Collection stops before booking, payment or confirmation.");training=false;return;}
                JSONObject selector=UiAccess.selector(target,route);
                if(selector.optString("text").isEmpty()&&selector.optString("description").isEmpty())selector=UiAccess.selector(source,route);
                String signature=selector.toString();long now=SystemClock.uptimeMillis();
                if(signature.equals(lastRecord)&&now-lastClickAt<500)return;
                lastRecord=signature;lastClickAt=now;
                add(new JSONObject().put("action","click").put("selector",selector));
            }finally{target.recycle();}
        }catch(JSONException e){fail("Could not record this screen. Use Inspect.");}
        finally{source.recycle();}
    }
    private void finishTraining(){
        boolean pickup=false,dropoff=false;
        for(int i=0;i<steps.length();i++){JSONObject s=steps.optJSONObject(i);if(s!=null&&s.optString("action").equals("set_text")){pickup|=s.optString("field").equals("pickup");dropoff|=s.optString("field").equals("dropoff");}}
        if(!pickup||!dropoff){info("Mark and fill both pickup and dropoff fields before Finish.");return;}
        try{
            store.flow(provider,mode,new JSONObject().put("steps",steps).put("has_pickup",pickup).put("has_dropoff",dropoff).put("version",1));
            training=false;showPanel();info("Workflow saved. Read prices now. Test replay from the app's start screen.");
        }catch(JSONException e){fail("Could not save training.");}
    }
    private void tick(){
        if(!active||!replaying)return;
        if(System.currentTimeMillis()-startedAt>180000){fail("Workflow timed out.");return;}
        AccessibilityNodeInfo root=root();
        if(root==null){if(SystemClock.uptimeMillis()>deadline)fail("The expected ride app is not visible.");else handler.postDelayed(this::tick,400);return;}
        try{
            String text=UiAccess.allText(root);
            if(FareLogic.activeTripOrLogin(text)){fail("Sign-in or an active ride needs your attention.");return;}
            if(step>=steps.length()){
                if(!waiting){waiting=true;deadline=SystemClock.uptimeMillis()+15000;info("Waiting for fresh fares…");}
                List<FareLogic.Quote> quotes=FareLogic.extract(UiAccess.snapshot(root),store.products(),provider,mode);
                if(!quotes.isEmpty()){replaying=false;capture();return;}
                if(SystemClock.uptimeMillis()>deadline){fail("No readable fares found. Use Inspect on the fare screen.");return;}
                handler.postDelayed(this::tick,700);return;
            }
            JSONObject action=steps.getJSONObject(step);String type=action.optString("action");boolean completed=false;
            if(type.equals("back")){completed=performGlobalAction(GLOBAL_ACTION_BACK);}
            else {
                AccessibilityNodeInfo node=UiAccess.find(root,action.getJSONObject("selector"),route);
                if(node!=null){
                    try{
                        if(type.equals("set_text")){
                            String key=action.optString("field");if(!key.equals("pickup")&&!key.equals("dropoff")){fail("Unknown route field.");return;}
                            Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,route.optString(key));
                            completed=node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args);
                        }else if(type.equals("click")){
                            if(!FareLogic.safeClick(UiAccess.label(node),UiAccess.text(node.getContentDescription()))){fail("Booking or an unidentified control was blocked.");return;}
                            AccessibilityNodeInfo target=UiAccess.clickTarget(node);
                            if(target!=null){try{
                                String label=UiAccess.label(target);if(!label.isEmpty()&&!FareLogic.safeClick(label,UiAccess.text(target.getContentDescription()))){fail("Booking control blocked.");return;}
                                completed=target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            }finally{target.recycle();}}
                        }else{fail("Unsupported workflow action.");return;}
                    }finally{node.recycle();}
                }
            }
            if(completed){step++;deadline=SystemClock.uptimeMillis()+10000;info("Running "+provider+": "+step+" / "+steps.length());handler.postDelayed(this::tick,900);}
            else if(SystemClock.uptimeMillis()>deadline){fail("Step "+(step+1)+" did not match a unique control. Reset the app or inspect its screen.");}
            else handler.postDelayed(this::tick,400);
        }catch(JSONException e){fail("Saved workflow is incomplete. Train this app again.");}
        finally{root.recycle();}
    }
    private void capture(){
        replaying=false;waiting=false;
        AccessibilityNodeInfo root=root();if(root==null){info("Open "+provider+" and its fare screen first.");return;}
        try{
            String text=UiAccess.allText(root);if(FareLogic.activeTripOrLogin(text)){info("Collection paused on a sign-in or active-ride screen.");return;}
            if(FareLogic.foreignCurrency(text)){info("A currency other than UGX is visible. Check the app's country and fare screen.");return;}
            List<FareLogic.Quote> quotes=FareLogic.extract(UiAccess.snapshot(root),store.products(),provider,mode);
            if(quotes.isEmpty()){info("No unambiguous price rows found. Use Inspect; no prices were saved.");return;}
            boolean routeVisible=FareLogic.routePresent(text,route.optString("pickup"))&&FareLogic.routePresent(text,route.optString("dropoff"));
            JSONArray rows=new JSONArray();long captured=System.currentTimeMillis();
            for(FareLogic.Quote q:quotes)rows.put(new JSONObject().put("id",UUID.randomUUID().toString()).put("run_id",batchRun.isEmpty()?runId:batchRun)
                .put("route_id",route.optString("id")).put("pickup",route.optString("pickup")).put("dropoff",route.optString("dropoff"))
                .put("provider",provider).put("mode",mode).put("product_id",q.product.id).put("product",q.product.label)
                .put("low",q.price.low).put("high",q.price.high).put("raw",q.price.raw).put("currency","UGX").put("captured_at",captured)
                .put("route_verification",routeVisible?"visible_labels":"unverified").put("payment_method","unspecified").put("promotion_status","unspecified")
                .put("evidence",q.evidence).put("uploaded",false));
            if(!batchRun.isEmpty()){
                JSONObject old=new JSONObject(store.get("review","{}"));JSONArray previous=old.optJSONArray("rows");
                if(previous!=null){for(int i=0;i<rows.length();i++)previous.put(rows.get(i));rows=previous;}
            }
            store.set("review",new JSONObject().put("rows",rows).put("route_visible",routeVisible).toString());
            if(!batch.isEmpty()&&batchIndex+1<batch.size()){
                List<String> savedBatch=new ArrayList<>(batch);int next=batchIndex+1;String savedRun=batchRun;
                start(savedBatch.get(next),mode,route,false,true);
                if(active){batch=savedBatch;batchIndex=next;batchRun=savedRun;}
                return;
            }
            info("Read "+rows.length()+" prices. Review the route and payment details before saving.");
            removePanel();active=false;training=false;
            Intent review=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("review",true);startActivity(review);
        }catch(JSONException e){info("Could not prepare review.");}
        finally{root.recycle();}
    }
    private void inspect(){
        AccessibilityNodeInfo root=root();if(root==null){info("Open the selected ride app first.");return;}
        try{
            if(FareLogic.activeTripOrLogin(UiAccess.allText(root))){info("Inspection is disabled on sign-in and active-ride screens.");return;}
            store.diagnostic(UiAccess.diagnostic(root,provider,mode,route));
            info("Screen details saved on this phone. Stop, then use Share screen details in the collector.");
        }catch(Exception e){info("Could not save screen details.");}
        finally{root.recycle();}
    }
}
