package ai.datadive.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@Config(manifest = Config.NONE)
public class DatadiveTest extends BaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testGetInstance() {
        DatadiveClient a = Datadive.getInstance();
        DatadiveClient b = Datadive.getInstance("");
        DatadiveClient c = Datadive.getInstance(null);
        DatadiveClient d = Datadive.getInstance(Constants.DEFAULT_INSTANCE);
        DatadiveClient e = Datadive.getInstance("app1");
        DatadiveClient f = Datadive.getInstance("app2");

        assertSame(a, b);
        assertSame(b, c);
        assertSame(c, d);
        assertSame(d, Datadive.getInstance());
        assertNotSame(d, e);
        assertSame(e, Datadive.getInstance("app1"));
        assertNotSame(e, f);
        assertSame(f, Datadive.getInstance("app2"));

        // test for instance name case insensitivity
        assertSame(e, Datadive.getInstance("APP1"));
        assertSame(e, Datadive.getInstance("App1"));
        assertSame(e, Datadive.getInstance("aPP1"));
        assertSame(e, Datadive.getInstance("apP1"));

        assertTrue(Datadive.instances.size() == 3);
        assertTrue(Datadive.instances.containsKey(Constants.DEFAULT_INSTANCE));
        assertTrue(Datadive.instances.containsKey("app1"));
        assertTrue(Datadive.instances.containsKey("app2"));
    }

    @Test
    public void testSeparateInstancesLogEventsSeparately() {
        Datadive.instances.clear();
        DatabaseHelper.instances.clear();

        String newInstance1 = "newApp1";
        String newApiKey1 = "1234567890";
        String newInstance2 = "newApp2";
        String newApiKey2 = "0987654321";

        DatabaseHelper oldDbHelper = DatabaseHelper.getDatabaseHelper(context);
        DatabaseHelper newDbHelper1 = DatabaseHelper.getDatabaseHelper(context, newInstance1);
        DatabaseHelper newDbHelper2 = DatabaseHelper.getDatabaseHelper(context, newInstance2);

        // Setup existing Databasefile
        oldDbHelper.insertOrReplaceKeyValue("device_id", "oldDeviceId");
        oldDbHelper.insertOrReplaceKeyLongValue("sequence_number", 1000L);
        oldDbHelper.addEvent("oldEvent1");
        oldDbHelper.addIdentify("oldIdentify1");
        oldDbHelper.addIdentify("oldIdentify2");

        // Verify persistence of old database file in default instance
        Datadive.getInstance().initialize(context, apiKey);
        Shadows.shadowOf(Datadive.getInstance().logThread.getLooper()).runToEndOfTasks();
        assertEquals(Datadive.getInstance().getDeviceId(), "oldDeviceId");
        assertEquals(Datadive.getInstance().getNextSequenceNumber(), 1001L);
        assertTrue(oldDbHelper.dbFileExists());
        assertFalse(newDbHelper1.dbFileExists());
        assertFalse(newDbHelper2.dbFileExists());

        // init first new app and verify separate database file
        Datadive.getInstance(newInstance1).initialize(context, newApiKey1);
        Shadows.shadowOf(
            Datadive.getInstance(newInstance1).logThread.getLooper()
        ).runToEndOfTasks();
        assertTrue(newDbHelper1.dbFileExists()); // db file is created after deviceId initialization

        assertFalse(newDbHelper1.getValue("device_id").equals("oldDeviceId"));
        assertEquals(
            newDbHelper1.getValue("device_id"), Datadive.getInstance(newInstance1).getDeviceId()
        );
        assertEquals(Datadive.getInstance(newInstance1).getNextSequenceNumber(), 1L);
        assertEquals(newDbHelper1.getEventCount(), 0);
        assertEquals(newDbHelper1.getIdentifyCount(), 0);

        // init second new app and verify separate database file
        Datadive.getInstance(newInstance2).initialize(context, newApiKey2);
        Shadows.shadowOf(
            Datadive.getInstance(newInstance2).logThread.getLooper()
        ).runToEndOfTasks();
        assertTrue(newDbHelper2.dbFileExists()); // db file is created after deviceId initialization

        assertFalse(newDbHelper2.getValue("device_id").equals("oldDeviceId"));
        assertEquals(
            newDbHelper2.getValue("device_id"), Datadive.getInstance(newInstance2).getDeviceId()
        );
        assertEquals(Datadive.getInstance(newInstance2).getNextSequenceNumber(), 1L);
        assertEquals(newDbHelper2.getEventCount(), 0);
        assertEquals(newDbHelper2.getIdentifyCount(), 0);

        // verify existing database still intact
        assertTrue(oldDbHelper.dbFileExists());
        assertEquals(oldDbHelper.getValue("device_id"), "oldDeviceId");
        assertEquals(oldDbHelper.getLongValue("sequence_number").longValue(), 1001L);
        assertEquals(oldDbHelper.getEventCount(), 1);
        assertEquals(oldDbHelper.getIdentifyCount(), 2);

        // verify both apps can modify their database independently and not affect old database
        newDbHelper1.insertOrReplaceKeyValue("device_id", "fakeDeviceId");
        assertEquals(newDbHelper1.getValue("device_id"), "fakeDeviceId");
        assertFalse(newDbHelper2.getValue("device_id").equals("fakeDeviceId"));
        assertEquals(oldDbHelper.getValue("device_id"), "oldDeviceId");
        newDbHelper1.addIdentify("testIdentify3");
        assertEquals(newDbHelper1.getIdentifyCount(), 1);
        assertEquals(newDbHelper2.getIdentifyCount(), 0);
        assertEquals(oldDbHelper.getIdentifyCount(), 2);

        newDbHelper2.insertOrReplaceKeyValue("device_id", "brandNewDeviceId");
        assertEquals(newDbHelper1.getValue("device_id"), "fakeDeviceId");
        assertEquals(newDbHelper2.getValue("device_id"), "brandNewDeviceId");
        assertEquals(oldDbHelper.getValue("device_id"), "oldDeviceId");
        newDbHelper2.addEvent("testEvent2");
        newDbHelper2.addEvent("testEvent3");
        assertEquals(newDbHelper1.getEventCount(), 0);
        assertEquals(newDbHelper2.getEventCount(), 2);
        assertEquals(oldDbHelper.getEventCount(), 1);
    }

    @Test
    public void testSeparateInstancesSeparateSharedPreferences() {
        // set up existing preferences values for default instance
        long timestamp = System.currentTimeMillis();
        String prefName = Constants.SHARED_PREFERENCES_NAME_PREFIX + "." + context.getPackageName();
        SharedPreferences preferences = context.getSharedPreferences(
            prefName, Context.MODE_PRIVATE);
        preferences.edit().putLong(Constants.PREFKEY_LAST_EVENT_ID, 1000L).commit();
        preferences.edit().putLong(Constants.PREFKEY_LAST_EVENT_TIME, timestamp).commit();
        preferences.edit().putLong(Constants.PREFKEY_LAST_IDENTIFY_ID, 2000L).commit();
        preferences.edit().putLong(Constants.PREFKEY_PREVIOUS_SESSION_ID, timestamp).commit();

        // init default instance, which should load preferences values
        Datadive.getInstance().initialize(context, apiKey);
        Shadows.shadowOf(Datadive.getInstance().logThread.getLooper()).runToEndOfTasks();
        assertEquals(Datadive.getInstance().lastEventId, 1000L);
        assertEquals(Datadive.getInstance().lastEventTime, timestamp);
        assertEquals(Datadive.getInstance().lastIdentifyId, 2000L);
        assertEquals(Datadive.getInstance().previousSessionId, timestamp);

        // init new instance, should have blank slate
        Datadive.getInstance("new_app").initialize(context, "1234567890");
        Shadows.shadowOf(Datadive.getInstance("new_app").logThread.getLooper()).runToEndOfTasks();
        assertEquals(Datadive.getInstance("new_app").lastEventId, -1L);
        assertEquals(Datadive.getInstance("new_app").lastEventTime, -1L);
        assertEquals(Datadive.getInstance("new_app").lastIdentifyId, -1L);
        assertEquals(Datadive.getInstance("new_app").previousSessionId, -1L);

        // shared preferences should update independently
        Datadive.getInstance("new_app").logEvent("testEvent");
        Shadows.shadowOf(Datadive.getInstance("new_app").logThread.getLooper()).runToEndOfTasks();
        assertEquals(Datadive.getInstance("new_app").lastEventId, 1L);
        assertTrue(Datadive.getInstance("new_app").lastEventTime > timestamp);
        assertEquals(Datadive.getInstance("new_app").lastIdentifyId, -1L);
        assertTrue(Datadive.getInstance("new_app").previousSessionId > timestamp);

        assertEquals(Datadive.getInstance().lastEventId, 1000L);
        assertEquals(Datadive.getInstance().lastEventTime, timestamp);
        assertEquals(Datadive.getInstance().lastIdentifyId, 2000L);
        assertEquals(Datadive.getInstance().previousSessionId, timestamp);
    }
}
