package com.abaho.farecollector;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class FareRangeTest {
    private Context context;
    private final String[] ids={"sb_saver","sb_plus","sb_ev","sb_car","sb_comfort","sb_xl"};
    private final long[] lows={2800,3300,2800,12000,14000,15000},highs={3200,3700,3200,12300,14300,15300};
    @Before public void prepare(){
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue(context.getSharedPreferences("collector",Context.MODE_PRIVATE).edit().clear().putBoolean("consent",true).commit());
    }
    private JSONArray rows()throws JSONException {
        JSONArray rows=new JSONArray();
        for(int i=0;i<ids.length;i++){
            FareLogic.Product product=null;
            for(FareLogic.Product p:FareLogic.defaults())if(p.id.equals(ids[i]))product=p;
            assertNotNull(product);
            String raw="UGX "+String.format(java.util.Locale.UK,"%,d ~ %,d",lows[i],highs[i]);
            rows.put(new JSONObject().put("id",java.util.UUID.randomUUID().toString()).put("provider","SafeBoda")
                .put("product_id",ids[i]).put("product",product.label).put("mode",product.mode).put("route_id","TEST01")
                .put("pickup","Test pickup").put("dropoff","Test destination").put("low",lows[i]).put("high",highs[i])
                .put("raw",raw).put("evidence",product.label+" | "+raw+" | ").put("captured_at",System.currentTimeMillis())
                .put("uploaded",false));
        }
        return rows;
    }
    @Test public void uploadKeepsBothBoundsAndOriginalEvidenceWithoutChangingTheQueue()throws Exception {
        Store store=new Store(context);store.saveRecords(rows());
        JSONArray pending=store.pending();String before=pending.toString();
        JSONArray payload=Api.recordsForReceiver(pending);
        assertEquals(before,pending.toString());assertEquals(before,store.records().toString());
        assertEquals(6,payload.length());
        for(int i=0;i<payload.length();i++){
            JSONObject row=payload.getJSONObject(i);
            assertEquals(lows[i],row.getLong("low"));assertEquals(highs[i],row.getLong("high"));
            assertFalse(row.getString("raw").contains("~"));
            assertEquals(pending.getJSONObject(i).getString("evidence"),row.getString("evidence"));
            assertTrue(row.getString("evidence").contains("~"));
        }
    }
    private static String visibleText(View view){
        StringBuilder out=new StringBuilder();
        if(view instanceof TextView)out.append(((TextView)view).getText()).append('\n');
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)out.append(visibleText(group.getChildAt(i)));}
        return out.toString();
    }
    @Test public void reviewDisplaysTheSixFullRanges()throws Exception {
        new Store(context).set("review",new JSONObject().put("rows",rows()).toString());
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(activity->{
                String text=visibleText(activity.getWindow().getDecorView());
                for(int i=0;i<ids.length;i++)
                    assertTrue("Missing review range for "+ids[i],text.contains(String.format(java.util.Locale.UK,"UGX %,d – %,d",lows[i],highs[i])));
                assertTrue(text.contains("Electric Boda"));
            });
        }
    }
}
