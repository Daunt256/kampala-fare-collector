package com.abaho.farecollector;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class ConnectionPersistenceTest {
    private static final String URL="https://script.google.com/macros/s/test-published-deployment/exec";
    private static final String TOKEN="0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private Context context;
    @Before public void prepare(){
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue(context.getSharedPreferences("collector",Context.MODE_PRIVATE).edit().clear().putBoolean("consent",true).commit());
    }
    private static Button button(View view,String text){
        if(view instanceof Button&&text.contentEquals(((Button)view).getText()))return (Button)view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){Button b=button(group.getChildAt(i),text);if(b!=null)return b;}}
        return null;
    }
    private static void click(MainActivity activity,String label){Button b=button(activity.getWindow().getDecorView(),label);assertNotNull(label,b);b.performClick();}
    private static void enter(MainActivity activity){click(activity,"Connection");((EditText)activity.findViewById(R.id.connection_endpoint)).setText(URL);((EditText)activity.findViewById(R.id.connection_token)).setText(TOKEN);}
    private static void assertEntries(MainActivity activity){assertEquals(URL,((EditText)activity.findViewById(R.id.connection_endpoint)).getText().toString());assertEquals(TOKEN,((EditText)activity.findViewById(R.id.connection_token)).getText().toString());}
    @Test public void typedEntriesSurviveSwitchingAwayAndReturning(){
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(ConnectionPersistenceTest::enter);
            scenario.moveToState(Lifecycle.State.CREATED);scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(ConnectionPersistenceTest::assertEntries);
        }
    }
    @Test public void draftsSurviveActivityRecreation(){
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(ConnectionPersistenceTest::enter);scenario.recreate();scenario.onActivity(ConnectionPersistenceTest::assertEntries);
        }
    }
    @Test public void draftsSurviveClosingAndLaunchingAgain(){
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){scenario.onActivity(ConnectionPersistenceTest::enter);}
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){scenario.onActivity(ConnectionPersistenceTest::assertEntries);}
    }
    @Test public void savedConnectionReloadsWithAMaskedToken(){
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(activity->{enter(activity);click(activity,"Save connection only");});
            scenario.recreate();scenario.onActivity(activity->{assertEntries(activity);assertNotNull(((EditText)activity.findViewById(R.id.connection_token)).getTransformationMethod());});
        }
        try{assertEquals(URL,new Store(context).get("endpoint",""));assertEquals(TOKEN,new Store(context).token());}catch(Exception e){throw new AssertionError(e);}
    }
    @Test public void invalidUrlCannotOverwriteTheSavedConnection(){
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(activity->{enter(activity);click(activity,"Save connection only");((EditText)activity.findViewById(R.id.connection_endpoint)).setText(URL.replace("/exec","/dev"));click(activity,"Save connection only");});
            assertEquals(URL,new Store(context).get("endpoint",""));
        }
    }
    @Test public void draftAndSavedTokensAreEncryptedInPhoneStorage(){
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(ConnectionPersistenceTest::enter);
            String draft=context.getSharedPreferences("collector",Context.MODE_PRIVATE).getString("connection_token_draft","");
            assertFalse(draft.isEmpty());assertFalse(draft.contains(TOKEN));
            scenario.onActivity(activity->click(activity,"Save connection only"));
            String saved=context.getSharedPreferences("collector",Context.MODE_PRIVATE).getString("token","");
            assertFalse(saved.isEmpty());assertFalse(saved.contains(TOKEN));
        }
    }
}
