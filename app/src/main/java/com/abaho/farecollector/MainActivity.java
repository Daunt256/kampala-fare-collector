package com.abaho.farecollector;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.*;
import java.util.*;

public final class MainActivity extends Activity {
    private Store store;private LinearLayout body;private TextView notice;private String page="Collect";
    private static final int NAVY=Color.rgb(18,38,50),TEAL=Color.rgb(8,127,140);
    protected void onCreate(Bundle saved){super.onCreate(saved);store=new Store(this);getWindow().setNavigationBarColor(NAVY);getWindow().setStatusBarColor(NAVY);show();}
    protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(intent.getBooleanExtra("review",false))page="Review";show();}
    protected void onResume(){super.onResume();if(store!=null){if(!store.get("review","").isEmpty())page="Review";show();}}
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
    private TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(NAVY);t.setPadding(0,dp(5),0,dp(5));return t;}
    private void line(String value){body.addView(text(value,14));}
    private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(TEAL);b.setOnClickListener(v->action.run());body.addView(b,new LinearLayout.LayoutParams(-1,dp(48)));return b;}
    private EditText edit(String hint,String value,boolean secret){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setTextSize(14);e.setSingleLine(true);e.setInputType(secret?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);body.addView(e,new LinearLayout.LayoutParams(-1,dp(56)));return e;}
    private Spinner spinner(String[] options,int selected){Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,options));s.setSelection(Math.max(0,Math.min(options.length-1,selected)));body.addView(s,new LinearLayout.LayoutParams(-1,dp(50)));return s;}
    private void message(String value){if(notice!=null)notice.setText(value);}
    private void error(String value){new AlertDialog.Builder(this).setTitle("Needs attention").setMessage(value).setPositiveButton("OK",null).show();}
    private void show(){
        LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);shell.setFitsSystemWindows(true);shell.setBackgroundColor(Color.rgb(245,248,249));
        TextView header=text("Kampala Fare Collector",22);header.setTextColor(Color.WHITE);header.setBackgroundColor(NAVY);header.setPadding(dp(20),dp(20),dp(20),dp(16));shell.addView(header);
        LinearLayout nav=new LinearLayout(this);shell.addView(nav);
        for(String tab:new String[]{"Collect","Connection","Records"}){Button b=new Button(this);b.setText(tab);b.setAllCaps(false);b.setTextSize(12);b.setOnClickListener(v->{page=tab;show();});nav.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));}
        ScrollView scroll=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(20),dp(12),dp(20),dp(28));scroll.addView(body);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(shell);notice=text("",13);body.addView(notice);
        if(!store.consent()){disclosure();return;}
        if(page.equals("Connection"))connection();else if(page.equals("Records"))records();else if(page.equals("Review")&&!store.get("review","").isEmpty())review();else collect();
    }
    private void disclosure(){
        line("For your SafeBoda, Bolt and Faras price-collection test.");
        line("When you start a session, the accessibility service reads visible text and performs the navigation you teach inside these three apps. It can enter pickup and destination search text. It stops before booking or payment controls.");
        line("The first pilot uses accessible screen text. It does not include screenshot OCR. An app screen that does not expose readable text needs inspection and an adapter update.");
        line("Only quotes you review are sent to your configured receiver. Screen inspection details remain on your phone unless you explicitly share them. Password fields and other apps are excluded.");
        line("Your phone must stay unlocked during collection. You can stop a session from the floating controls or disable the service in Android Settings.");
        button("I understand — continue",()->{store.consent(true);show();});
    }
    private void collect(){
        line("Pilot 0.1 · one phone · three ride apps");
        line(CollectorService.instance==null?"Accessibility: not connected":"Accessibility: connected");
        button("Enable collection accessibility",()->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        line("Route");JSONArray routes=store.config().optJSONArray("routes");if(routes==null||routes.length()==0){line("No routes configured. Connect to your script first.");return;}
        String[] labels=new String[routes.length()];for(int i=0;i<labels.length;i++){JSONObject r=routes.optJSONObject(i);labels[i]=r.optString("id")+" · "+r.optString("pickup")+" → "+r.optString("dropoff");}
        int savedRoute=0;try{savedRoute=Integer.parseInt(store.get("route_index","0"));}catch(NumberFormatException ignored){}
        Spinner routeSpinner=spinner(labels,savedRoute);
        line("Provider");Spinner providerSpinner=spinner(FareLogic.PROVIDERS,Arrays.asList(FareLogic.PROVIDERS).indexOf(store.get("provider","SafeBoda")));
        line("Mode — teach Boda and Car separately");Spinner modeSpinner=spinner(new String[]{"Boda","Car"},store.get("mode","Boda").equals("Car")?1:0);
        line("Start from the ride app's home screen, signed in. For training, mark each route field before tapping it. All other navigation taps are recorded.");
        final JSONArray routeRows=routes;
        button("Teach this app's route workflow",()->begin(providerSpinner,modeSpinner,routeSpinner,routeRows,true,false));
        button("Run learned workflow",()->begin(providerSpinner,modeSpinner,routeSpinner,routeRows,false,true));
        button("Open app for inspection or price capture",()->begin(providerSpinner,modeSpinner,routeSpinner,routeRows,false,false));
        button("Run all three apps for this route",()->{
            if(!ready())return;
            JSONObject r=routeRows.optJSONObject(routeSpinner.getSelectedItemPosition());String mode=modeSpinner.getSelectedItem().toString();
            for(String p:FareLogic.PROVIDERS){if(!installed(p)){error(p+" is not installed.");return;}if(store.flow(p,mode).optJSONArray("steps")==null){error("Teach "+p+" in "+mode+" mode first.");return;}}
            saveSelection(providerSpinner,modeSpinner,routeSpinner);
            CollectorService.instance.startBatch(mode,r);
        });
        button("Stop collection",()->{if(CollectorService.instance!=null)CollectorService.instance.stop("Stopped by you.");show();});
        button("Share screen details",this::shareDiagnostic);
        message(store.get("last_status","Ready. Connect to your sheet or test capture locally."));
        line("One phone reads apps one after another. The sheet keeps each capture time and shows the time difference.");
    }
    private boolean installed(String provider){try{getPackageManager().getPackageInfo(FareLogic.packageFor(provider),0);return true;}catch(PackageManager.NameNotFoundException e){return false;}}
    private boolean ready(){
        if(CollectorService.instance==null){error("Enable Kampala Fare Collector in Accessibility settings first.");return false;}
        if(!store.get("review","").isEmpty()){page="Review";show();error("Save or discard the pending review before starting another session.");return false;}
        KeyguardManager keys=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);if(keys.isKeyguardLocked()){error("Unlock your phone first.");return false;}
        return true;
    }
    private void saveSelection(Spinner provider,Spinner mode,Spinner route){store.set("provider",provider.getSelectedItem().toString());store.set("mode",mode.getSelectedItem().toString());store.set("route_index",String.valueOf(route.getSelectedItemPosition()));}
    private void begin(Spinner provider,Spinner mode,Spinner route,JSONArray routes,boolean train,boolean replay){
        if(!ready())return;String selected=provider.getSelectedItem().toString();if(!installed(selected)){error(selected+" is not installed.");return;}
        saveSelection(provider,mode,route);CollectorService.instance.start(selected,mode.getSelectedItem().toString(),routes.optJSONObject(route.getSelectedItemPosition()),train,replay);
    }
    private void connection(){
        line("Connect to the Apps Script web app supplied with this project.");
        EditText endpoint=edit("Apps Script deployment URL ending /exec",store.get("endpoint",""),false);
        EditText token=edit("Pairing token from the sheet setup","",true);
        line("Leave the token blank to keep the saved token. Tokens are encrypted with Android Keystore.");
        button("Save and load routes",()->{
            String url=endpoint.getText().toString().trim(),key=token.getText().toString().trim();
            if(!Api.validEndpoint(url)){error("Use the deployed https://script.google.com/macros/s/…/exec URL.");return;}
            try{if(!key.isEmpty()){if(key.length()<32){error("Paste the full pairing token.");return;}store.token(key);}if(store.token().isEmpty()){error("A pairing token is required.");return;}store.set("endpoint",url);}
            catch(Exception e){error("Could not store the connection securely.");return;}
            message("Connecting…");Api.call(store,"config",null,(result,e)->runOnUiThread(()->{if(e!=null)error(e.getMessage());else{page="Collect";show();message("Connected. Routes and product aliases updated.");}}));
        });
        button("Upload queued quotes",this::upload);
        line("Device ID: "+store.deviceId());
        line("The receiver writes only to the spreadsheet configured during setup. This app does not need your Google password.");
    }
    private void review(){
        JSONObject draft;try{draft=new JSONObject(store.get("review","{}"));}catch(JSONException e){store.set("review","");page="Collect";show();return;}
        JSONArray rows=draft.optJSONArray("rows");if(rows==null||rows.length()==0){store.set("review","");page="Collect";show();return;}
        JSONObject first=rows.optJSONObject(0);body.addView(text("Review captured quotes",20));
        line(first.optString("pickup")+" → "+first.optString("dropoff"));
        line("Compare these with the fare screens before accepting. Missing categories are not treated as zero.");
        Map<String,Spinner> payment=new LinkedHashMap<>(),promo=new LinkedHashMap<>();
        Set<String> providers=new LinkedHashSet<>();for(int i=0;i<rows.length();i++)providers.add(rows.optJSONObject(i).optString("provider"));
        for(String p:providers){
            body.addView(text(p,18));
            for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(!p.equals(row.optString("provider")))continue;
                line(row.optString("product")+": UGX "+number(row.optLong("low"))+(row.optLong("high")!=row.optLong("low")?" – "+number(row.optLong("high")):"")+" · "+time(row.optLong("captured_at")));
            }
            line("Payment shown in "+p);payment.put(p,spinner(new String[]{"Unspecified","Cash","Wallet","Other"},0));
            line("Promotion shown in "+p);promo.put(p,spinner(new String[]{"Unspecified","No promotion shown","Promotion shown"},0));
        }
        CheckBox checked=new CheckBox(this);checked.setText("I checked the pickup, destination, mode and fares in every captured app.");body.addView(checked);
        button("Accept quotes and upload",()->{
            if(!checked.isChecked()){error("Check the route and fares before accepting this pilot capture.");return;}
            try{
                JSONArray saved=store.records();Set<String> ids=new HashSet<>();for(int i=0;i<saved.length();i++)ids.add(saved.optJSONObject(i).optString("id"));
                for(int i=0;i<rows.length();i++){
                    JSONObject row=rows.getJSONObject(i);String p=row.getString("provider");
                    String[] methods={"unspecified","cash","wallet","other"},promos={"unspecified","none_shown","shown"};
                    row.put("payment_method",methods[payment.get(p).getSelectedItemPosition()]).put("promotion_status",promos[promo.get(p).getSelectedItemPosition()]);
                    row.put("reviewed",true).put("route_verification","user_confirmed");
                    if(ids.add(row.getString("id")))saved.put(row);
                }
                store.saveRecords(saved);store.set("review","");page="Records";show();upload();
            }catch(Exception e){error("Could not save the capture. Keep this review and check available phone storage.");}
        });
        button("Discard this capture",()->new AlertDialog.Builder(this).setTitle("Discard captured quotes?").setMessage("These quotes have not been uploaded.").setPositiveButton("Discard",(d,w)->{store.set("review","");page="Collect";show();}).setNegativeButton("Keep",null).show());
    }
    private void upload(){
        JSONArray pending=store.pending();if(pending.length()==0){message("No queued quotes.");return;}
        if(store.get("endpoint","").isEmpty()){message("Saved on this phone. Set up Connection to upload "+pending.length()+" quotes.");return;}
        message("Uploading "+pending.length()+" quotes…");
        Api.call(store,"submit",pending,(result,e)->runOnUiThread(()->{
            if(e!=null){message("Quotes are still saved on this phone. "+e.getMessage());return;}
            if(store.pending().length()>0){upload();return;}
            page="Records";show();message("Upload complete.");
        }));
    }
    private void records(){
        JSONArray records=store.records();line(records.length()+" accepted quotes · "+store.pending().length()+" awaiting upload");
        button("Upload queued quotes",this::upload);
        if(!store.get("review","").isEmpty())button("Open pending review",()->{page="Review";show();});
        for(int i=records.length()-1;i>=Math.max(0,records.length()-30);i--){JSONObject r=records.optJSONObject(i);if(r==null)continue;
            line(r.optString("provider")+" · "+r.optString("product")+" · "+r.optString("route_id")+"\nUGX "+number(r.optLong("low"))+(r.optLong("high")!=r.optLong("low")?" – "+number(r.optLong("high")):"")+" · "+time(r.optLong("captured_at"))+" · "+(r.optBoolean("uploaded")?"Uploaded":"Queued"));
        }
        line(store.get("last_status",""));
    }
    private String time(long epoch){SimpleDateFormat fmt=new SimpleDateFormat("dd MMM HH:mm:ss",Locale.UK);fmt.setTimeZone(TimeZone.getTimeZone("Africa/Kampala"));return fmt.format(new Date(epoch));}
    private String number(long value){return NumberFormat.getIntegerInstance(Locale.UK).format(value);}
    private void shareDiagnostic(){
        if(!store.diagnosticFile().isFile()){error("Start inspection, open a fare screen, and tap Inspect in the floating controls first.");return;}
        new AlertDialog.Builder(this).setTitle("Share screen details?").setMessage("This file contains visible text and control identifiers from the last inspected fare screen. Phone numbers and email addresses are masked, but review the file before sharing it. It does not contain your pairing token.")
            .setPositiveButton("Choose where to share",(d,w)->{
                Uri uri=Uri.parse("content://com.abaho.farecollector.diagnostics/diagnostic.json");
                Intent send=new Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                send.setClipData(ClipData.newRawUri("Fare screen details",uri));startActivity(Intent.createChooser(send,"Share screen details"));
            }).setNegativeButton("Cancel",null).show();
    }
}
