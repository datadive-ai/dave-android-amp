package ai.datadive.api;

import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
@Config(manifest = Config.NONE)
public class DatadiveClientTest extends BaseTest {

    private String generateStringWithLength(int length, char c) {
        if (length < 0) return "";
        char [] array = new char[length];
        Arrays.fill(array, c);
        return new String(array);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        datadive.initialize(context, apiKey);
        Shadows.shadowOf(datadive.logThread.getLooper()).runOneTask();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testConstructor() {
        // verify that the constructor lowercases the instance name
        DatadiveClient a = new DatadiveClient("APP1");
        DatadiveClient b = new DatadiveClient("New_App_2");

        assertEquals(a.instanceName, "app1");
        assertEquals(b.instanceName, "new_app_2");
    }

    @Test
    public void testSetUserId() {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        String userId = "user_id";
        datadive.setUserId(userId);
        looper.runToEndOfTasks();
        assertEquals(userId, dbHelper.getValue(DatadiveClient.USER_ID_KEY));
        assertEquals(userId, datadive.getUserId());

        // try setting to null
        datadive.setUserId(null);
        looper.runToEndOfTasks();
        assertNull(dbHelper.getValue(DatadiveClient.USER_ID_KEY));
        assertNull(datadive.getUserId());
    }

    @Test
    public void testSetUserIdTwice() {
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        String userId1 = "user_id1";
        String userId2 = "user_id2";

        datadive.setUserId(userId1);
        looper.runToEndOfTasks();
        assertEquals(datadive.getUserId(), userId1);
        datadive.logEvent("event1");
        looper.runToEndOfTasks();

        JSONObject event1 = getLastUnsentEvent();
        assertEquals(event1.optString("event_type"), "event1");
        assertEquals(event1.optString("user_id"), userId1);

        datadive.setUserId(userId2);
        looper.runToEndOfTasks();
        assertEquals(datadive.getUserId(), userId2);
        datadive.logEvent("event2");
        looper.runToEndOfTasks();

        JSONObject event2 = getLastUnsentEvent();
        assertEquals(event2.optString("event_type"), "event2");
        assertEquals(event2.optString("user_id"), userId2);
    }

    @Test
    public void testSetDeviceId() {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        SharedPreferences prefs = Utils.getAmplitudeSharedPreferences(context, datadive.instanceName);
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        looper.runToEndOfTasks();

        String deviceId = datadive.getDeviceId(); // Randomly generated device ID
        assertNotNull(deviceId);
        assertEquals(deviceId.length(), 36 + 1); // 36 for UUID, + 1 for appended R
        assertEquals(deviceId.charAt(36), 'R');
        assertEquals(dbHelper.getValue(datadive.DEVICE_ID_KEY), deviceId);


        // test setting invalid device ids
        datadive.setDeviceId(null);
        looper.runToEndOfTasks();
        assertEquals(datadive.getDeviceId(), deviceId);
        assertEquals(dbHelper.getValue(datadive.DEVICE_ID_KEY), deviceId);
        assertEquals(prefs.getString(datadive.DEVICE_ID_KEY, null), deviceId);

        datadive.setDeviceId("");
        looper.runToEndOfTasks();
        assertEquals(datadive.getDeviceId(), deviceId);
        assertEquals(dbHelper.getValue(datadive.DEVICE_ID_KEY), deviceId);
        assertEquals(prefs.getString(datadive.DEVICE_ID_KEY, null), deviceId);

        datadive.setDeviceId("9774d56d682e549c");
        looper.runToEndOfTasks();
        assertEquals(datadive.getDeviceId(), deviceId);
        assertEquals(dbHelper.getValue(datadive.DEVICE_ID_KEY), deviceId);
        assertEquals(prefs.getString(datadive.DEVICE_ID_KEY, null), deviceId);

        datadive.setDeviceId("unknown");
        looper.runToEndOfTasks();
        assertEquals(datadive.getDeviceId(), deviceId);
        assertEquals(dbHelper.getValue(datadive.DEVICE_ID_KEY), deviceId);
        assertEquals(prefs.getString(datadive.DEVICE_ID_KEY, null), deviceId);

        datadive.setDeviceId("000000000000000");
        looper.runToEndOfTasks();
        assertEquals(datadive.getDeviceId(), deviceId);
        assertEquals(dbHelper.getValue(datadive.DEVICE_ID_KEY), deviceId);
        assertEquals(prefs.getString(datadive.DEVICE_ID_KEY, null), deviceId);

        datadive.setDeviceId("Android");
        looper.runToEndOfTasks();
        assertEquals(datadive.getDeviceId(), deviceId);
        assertEquals(dbHelper.getValue(datadive.DEVICE_ID_KEY), deviceId);
        assertEquals(prefs.getString(datadive.DEVICE_ID_KEY, null), deviceId);

        datadive.setDeviceId("DEFACE");
        looper.runToEndOfTasks();
        assertEquals(datadive.getDeviceId(), deviceId);
        assertEquals(dbHelper.getValue(datadive.DEVICE_ID_KEY), deviceId);
        assertEquals(prefs.getString(datadive.DEVICE_ID_KEY, null), deviceId);

        datadive.setDeviceId("00000000-0000-0000-0000-000000000000");
        assertEquals(datadive.getDeviceId(), deviceId);
        assertEquals(dbHelper.getValue(datadive.DEVICE_ID_KEY), deviceId);
        assertEquals(prefs.getString(datadive.DEVICE_ID_KEY, null), deviceId);

        // set valid device id
        String newDeviceId = UUID.randomUUID().toString();
        datadive.setDeviceId(newDeviceId);
        looper.runToEndOfTasks();
        assertEquals(datadive.getDeviceId(), newDeviceId);
        assertEquals(dbHelper.getValue(datadive.DEVICE_ID_KEY), newDeviceId);
        assertEquals(prefs.getString(datadive.DEVICE_ID_KEY, null), newDeviceId);

        datadive.logEvent("test");
        looper.runToEndOfTasks();
        JSONObject event = getLastUnsentEvent();
        assertEquals(event.optString("event_type"), "test");
        assertEquals(event.optString("device_id"), newDeviceId);
        assertEquals(prefs.getString(datadive.DEVICE_ID_KEY, null), newDeviceId);
    }

    @Test
    public void testSetUserProperties() throws JSONException {
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());

        // setting null or empty user properties does nothing
        datadive.setUserProperties(null);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);
        datadive.setUserProperties(new JSONObject());
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);

        JSONObject userProperties = new JSONObject().put("key1", "value1").put("key2", "value2");
        datadive.setUserProperties(userProperties);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 1);
        JSONObject event = getLastUnsentIdentify();
        assertEquals(Constants.IDENTIFY_EVENT, event.optString("event_type"));
        assertTrue(Utils.compareJSONObjects(
            event.optJSONObject("event_properties"), new JSONObject()
        ));

        JSONObject userPropertiesOperations = event.optJSONObject("user_properties");
        assertEquals(userPropertiesOperations.length(), 1);
        assertTrue(userPropertiesOperations.has(Constants.AMP_OP_SET));

        JSONObject setOperations = userPropertiesOperations.optJSONObject(Constants.AMP_OP_SET);
        assertTrue(Utils.compareJSONObjects(userProperties, setOperations));
    }

    @Test
    public void testSetCustomLibrary() {
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        datadive.setLibraryName("datadive-unity");
        datadive.setLibraryVersion("1.0.0");
        datadive.logEvent("test");
        looper.runToEndOfTasks();

        JSONObject event = getLastEvent();
        assertNotNull(event);
        try {
            JSONObject library = event.getJSONObject("library");
            String libName = library.getString("name");
            String libVersion = library.getString("version");
            assertEquals(libName, "datadive-unity");
            assertEquals(libVersion, "1.0.0");
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testSetCustomLibraryWithNullValues() {
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        datadive.setLibraryName(null);
        datadive.setLibraryVersion(null);
        datadive.logEvent("test");
        looper.runToEndOfTasks();

        JSONObject event = getLastEvent();
        assertNotNull(event);
        try {
            JSONObject library = event.getJSONObject("library");
            String libName = library.getString("name");
            String libVersion = library.getString("version");
            assertEquals(libName, "unknown-library");
            assertEquals(libVersion, "unknown-version");
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testIdentifyMultipleOperations() throws JSONException {
        String property1 = "string value";
        String value1 = "testValue";

        String property2 = "double value";
        double value2 = 0.123;

        String property3 = "boolean value";
        boolean value3 = true;

        String property4 = "json value";

        Identify identify = new Identify().setOnce(property1, value1).add(property2, value2);
        identify.set(property3, value3).unset(property4);

        // identify should ignore this since duplicate key
        identify.set(property4, value3);

        datadive.identify(identify);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();
        assertEquals(getUnsentIdentifyCount(), 1);
        assertEquals(getUnsentEventCount(), 0);
        JSONObject event = getLastUnsentIdentify();
        assertEquals(Constants.IDENTIFY_EVENT, event.optString("event_type"));

        JSONObject userProperties = event.optJSONObject("user_properties");
        JSONObject expected = new JSONObject();
        expected.put(Constants.AMP_OP_SET_ONCE, new JSONObject().put(property1, value1));
        expected.put(Constants.AMP_OP_ADD, new JSONObject().put(property2, value2));
        expected.put(Constants.AMP_OP_SET, new JSONObject().put(property3, value3));
        expected.put(Constants.AMP_OP_UNSET, new JSONObject().put(property4, "-"));
        assertTrue(Utils.compareJSONObjects(userProperties, expected));
    }

    @Test
    public void testOptOut() {
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        ShadowLooper httplooper = Shadows.shadowOf(datadive.httpThread.getLooper());

        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        assertFalse(datadive.isOptedOut());
        assertEquals((long) dbHelper.getLongValue(DatadiveClient.OPT_OUT_KEY), 0L);

        datadive.setOptOut(true);
        looper.runToEndOfTasks();
        assertTrue(datadive.isOptedOut());
        assertEquals((long) dbHelper.getLongValue(DatadiveClient.OPT_OUT_KEY), 1L);
        RecordedRequest request = sendEvent(datadive, "test_opt_out", null);
        assertNull(request);

        // Event shouldn't be sent event once opt out is turned off.
        datadive.setOptOut(false);
        looper.runToEndOfTasks();
        assertFalse(datadive.isOptedOut());
        assertEquals((long) dbHelper.getLongValue(DatadiveClient.OPT_OUT_KEY), 0L);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        httplooper.runToEndOfTasks();
        assertNull(request);

        request = sendEvent(datadive, "test_opt_out", null);
        assertNotNull(request);
    }

    @Test
    public void testOffline() {
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        ShadowLooper httplooper = Shadows.shadowOf(datadive.httpThread.getLooper());

        datadive.setOffline(true);
        RecordedRequest request = sendEvent(datadive, "test_offline", null);
        assertNull(request);

        // Events should be sent after offline is turned off.
        datadive.setOffline(false);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        httplooper.runToEndOfTasks();

        try {
            request = server.takeRequest(1, SECONDS);
        } catch (InterruptedException e) {
        }
        assertNotNull(request);
    }

    @Test
    public void testLogEvent() {
        RecordedRequest request = sendEvent(datadive, "test_event", null);
        assertNotNull(request);
    }

    @Test
    public void testIdentify() throws JSONException {
        long [] timestamps = {1000, 1001};
        clock.setTimestamps(timestamps);

        RecordedRequest request = sendIdentify(datadive, new Identify().set("key", "value"));
        assertNotNull(request);
        JSONArray events = getEventsFromRequest(request);
        assertEquals(events.length(), 1);
        JSONObject identify = events.getJSONObject(0);
        assertEquals(identify.getString("event_type"), Constants.IDENTIFY_EVENT);
        assertEquals(identify.getLong("event_id"), 1);
        assertEquals(identify.getLong("timestamp"), timestamps[0]);
        assertEquals(identify.getLong("sequence_number"), 1);
        JSONObject userProperties = identify.getJSONObject("user_properties");
        assertEquals(userProperties.length(), 1);
        assertTrue(userProperties.has(Constants.AMP_OP_SET));

        JSONObject expected = new JSONObject();
        expected.put("key", "value");
        assertTrue(Utils.compareJSONObjects(userProperties.getJSONObject(Constants.AMP_OP_SET), expected));

        // verify db state
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        assertNull(dbHelper.getValue(DatadiveClient.USER_ID_KEY));
        assertEquals((long)dbHelper.getLongValue(DatadiveClient.LAST_IDENTIFY_ID_KEY), 1L);
        assertEquals((long)dbHelper.getLongValue(DatadiveClient.LAST_EVENT_ID_KEY), -1L);
        assertEquals((long) dbHelper.getLongValue(DatadiveClient.SEQUENCE_NUMBER_KEY), 1L);
        assertEquals((long)dbHelper.getLongValue(DatadiveClient.LAST_EVENT_TIME_KEY), timestamps[0]);
    }

    @Test
    public void testNullIdentify() {
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);

        datadive.identify(null);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);
    }

    @Test
    public void testLog3Events() throws InterruptedException {
        long [] timestamps = {1, 2, 3, 4, 5, 6, 7};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        looper.runToEndOfTasks();


        datadive.logEvent("test_event1");
        datadive.logEvent("test_event2");
        datadive.logEvent("test_event3");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        assertEquals(getUnsentEventCount(), 3);
        assertEquals(getUnsentIdentifyCount(), 0);
        JSONArray events = getUnsentEvents(3);
        for (int i = 0; i < 3; i++) {
            assertEquals(events.optJSONObject(i).optString("event_type"), "test_event" + (i+1));
            assertEquals(events.optJSONObject(i).optLong("timestamp"), timestamps[i]);
            assertEquals(events.optJSONObject(i).optLong("sequence_number"), i+1);
        }

        // send response and check that remove events works properly
        runRequest(datadive);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);
    }

    @Test
    public void testLog3Identifys() throws JSONException {
        long [] timestamps = {1, 2, 3, 4, 5, 6, 7};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        looper.runToEndOfTasks();

        Robolectric.getForegroundThreadScheduler().advanceTo(1);
        datadive.identify(new Identify().set("photo_count", 1));
        datadive.identify(new Identify().add("karma", 2));
        datadive.identify(new Identify().unset("gender"));
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 3);
        JSONArray events = getUnsentIdentifys(3);

        JSONObject expectedIdentify1 = new JSONObject();
        expectedIdentify1.put(Constants.AMP_OP_SET, new JSONObject().put("photo_count", 1));
        JSONObject expectedIdentify2 = new JSONObject();
        expectedIdentify2.put(Constants.AMP_OP_ADD, new JSONObject().put("karma", 2));
        JSONObject expectedIdentify3 = new JSONObject();
        expectedIdentify3.put(Constants.AMP_OP_UNSET, new JSONObject().put("gender", "-"));

        assertEquals(events.optJSONObject(0).optString("event_type"), Constants.IDENTIFY_EVENT);
        assertEquals(events.optJSONObject(0).optLong("timestamp"), timestamps[0]);
        assertEquals(events.optJSONObject(0).optLong("sequence_number"), 1);
        assertTrue(Utils.compareJSONObjects(
                events.optJSONObject(0).optJSONObject("user_properties"), expectedIdentify1
        ));
        assertEquals(events.optJSONObject(1).optString("event_type"), Constants.IDENTIFY_EVENT);
        assertEquals(events.optJSONObject(1).optLong("timestamp"), timestamps[1]);
        assertEquals(events.optJSONObject(1).optLong("sequence_number"), 2);
        assertTrue(Utils.compareJSONObjects(
                events.optJSONObject(1).optJSONObject("user_properties"), expectedIdentify2
        ));
        assertEquals(events.optJSONObject(2).optString("event_type"), Constants.IDENTIFY_EVENT);
        assertEquals(events.optJSONObject(2).optLong("timestamp"), timestamps[2]);
        assertEquals(events.optJSONObject(2).optLong("sequence_number"), 3);
        assertTrue(Utils.compareJSONObjects(
                events.optJSONObject(2).optJSONObject("user_properties"), expectedIdentify3
        ));

        // send response and check that remove events works properly
        runRequest(datadive);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);
    }

    @Test
    public void testLogEventAndIdentify() throws JSONException {
        long [] timestamps = {1, 1, 2};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        looper.runToEndOfTasks();
        datadive.logEvent("test_event");
        datadive.identify(new Identify().add("photo_count", 1));
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        // verify some internal counters
        assertEquals(getUnsentEventCount(), 1);
        assertEquals(datadive.lastEventId, 1);
        assertEquals(getUnsentIdentifyCount(), 1);
        assertEquals(datadive.lastIdentifyId, 1);

        JSONArray unsentEvents = getUnsentEvents(1);
        assertEquals(unsentEvents.optJSONObject(0).optString("event_type"), "test_event");
        assertEquals(unsentEvents.optJSONObject(0).optLong("sequence_number"), 1);

        JSONObject expectedIdentify = new JSONObject();
        expectedIdentify.put(Constants.AMP_OP_ADD, new JSONObject().put("photo_count", 1));

        JSONArray unsentIdentifys = getUnsentIdentifys(1);
        assertEquals(unsentIdentifys.optJSONObject(0).optString("event_type"), Constants.IDENTIFY_EVENT);
        assertEquals(unsentIdentifys.optJSONObject(0).optLong("sequence_number"), 2);
        assertTrue(Utils.compareJSONObjects(
            unsentIdentifys.optJSONObject(0).optJSONObject("user_properties"), expectedIdentify
        ));

        // send response and check that remove events works properly
        RecordedRequest request = runRequest(datadive);
        JSONArray events = getEventsFromRequest(request);
        assertEquals(events.length(), 2);
        assertEquals(events.optJSONObject(0).optString("event_type"), "test_event");
        assertEquals(events.optJSONObject(1).optString("event_type"), Constants.IDENTIFY_EVENT);
        assertTrue(Utils.compareJSONObjects(
            events.optJSONObject(1).optJSONObject("user_properties"), expectedIdentify
        ));
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);
    }

    @Test
    public void testMergeEventsAndIdentifys() throws JSONException {
        long [] timestamps = {1, 2, 3, 4, 5, 5, 6, 7, 8, 9, 10};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        looper.runToEndOfTasks();

        datadive.logEvent("test_event1");
        datadive.identify(new Identify().add("photo_count", 1));
        datadive.logEvent("test_event2");
        datadive.logEvent("test_event3");
        datadive.logEvent("test_event4");
        datadive.identify(new Identify().set("gender", "male"));
        datadive.identify(new Identify().unset("karma"));

        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        // verify some internal counters
        assertEquals(getUnsentEventCount(), 4);
        assertEquals(datadive.lastEventId, 4);
        assertEquals(getUnsentIdentifyCount(), 3);
        assertEquals(datadive.lastIdentifyId, 3);

        RecordedRequest request = runRequest(datadive);
        JSONArray events = getEventsFromRequest(request);
        assertEquals(events.length(), 7);

        JSONObject expectedIdentify1 = new JSONObject();
        expectedIdentify1.put(Constants.AMP_OP_ADD, new JSONObject().put("photo_count", 1));
        JSONObject expectedIdentify2 = new JSONObject();
        expectedIdentify2.put(Constants.AMP_OP_SET, new JSONObject().put("gender", "male"));
        JSONObject expectedIdentify3 = new JSONObject();
        expectedIdentify3.put(Constants.AMP_OP_UNSET, new JSONObject().put("karma", "-"));

        assertEquals(events.getJSONObject(0).getString("event_type"), "test_event1");
        assertEquals(events.getJSONObject(0).getLong("event_id"), 1);
        assertEquals(events.getJSONObject(0).getLong("timestamp"), timestamps[0]);
        assertEquals(events.getJSONObject(0).getLong("sequence_number"), 1);

        assertEquals(events.getJSONObject(1).getString("event_type"), Constants.IDENTIFY_EVENT);
        assertEquals(events.getJSONObject(1).getLong("event_id"), 1);
        assertEquals(events.getJSONObject(1).getLong("timestamp"), timestamps[1]);
        assertEquals(events.getJSONObject(1).getLong("sequence_number"), 2);
        assertTrue(Utils.compareJSONObjects(
                events.getJSONObject(1).getJSONObject("user_properties"), expectedIdentify1
        ));

        assertEquals(events.getJSONObject(2).getString("event_type"), "test_event2");
        assertEquals(events.getJSONObject(2).getLong("event_id"), 2);
        assertEquals(events.getJSONObject(2).getLong("timestamp"), timestamps[2]);
        assertEquals(events.getJSONObject(2).getLong("sequence_number"), 3);

        assertEquals(events.getJSONObject(3).getString("event_type"), "test_event3");
        assertEquals(events.getJSONObject(3).getLong("event_id"), 3);
        assertEquals(events.getJSONObject(3).getLong("timestamp"), timestamps[3]);
        assertEquals(events.getJSONObject(3).getLong("sequence_number"), 4);

        // sequence number guarantees strict ordering regardless of timestamp
        assertEquals(events.getJSONObject(4).getString("event_type"), "test_event4");
        assertEquals(events.getJSONObject(4).getLong("event_id"), 4);
        assertEquals(events.getJSONObject(4).getLong("timestamp"), timestamps[4]);
        assertEquals(events.getJSONObject(4).getLong("sequence_number"), 5);

        assertEquals(events.getJSONObject(5).getString("event_type"), Constants.IDENTIFY_EVENT);
        assertEquals(events.getJSONObject(5).getLong("event_id"), 2);
        assertEquals(events.getJSONObject(5).getLong("timestamp"), timestamps[5]);
        assertEquals(events.getJSONObject(5).getLong("sequence_number"), 6);
        assertTrue(Utils.compareJSONObjects(
                events.getJSONObject(5).getJSONObject("user_properties"), expectedIdentify2
        ));

        assertEquals(events.getJSONObject(6).getString("event_type"), Constants.IDENTIFY_EVENT);
        assertEquals(events.getJSONObject(6).getLong("event_id"), 3);
        assertEquals(events.getJSONObject(6).getLong("timestamp"), timestamps[6]);
        assertEquals(events.getJSONObject(6).getLong("sequence_number"), 7);
        assertTrue(Utils.compareJSONObjects(
                events.getJSONObject(6).getJSONObject("user_properties"), expectedIdentify3
        ));

        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);

        // verify db state
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        assertNull(dbHelper.getValue(DatadiveClient.USER_ID_KEY));
        assertEquals((long) dbHelper.getLongValue(DatadiveClient.LAST_IDENTIFY_ID_KEY), 3L);
        assertEquals((long) dbHelper.getLongValue(DatadiveClient.LAST_EVENT_ID_KEY), 4L);
        assertEquals((long) dbHelper.getLongValue(DatadiveClient.SEQUENCE_NUMBER_KEY), 7L);
        assertEquals((long)dbHelper.getLongValue(DatadiveClient.LAST_EVENT_TIME_KEY), timestamps[6]);
    }

    @Test
    public void testMergeEventBackwardsCompatible() throws JSONException {
        datadive.setEventUploadThreshold(4);
        // eventst logged before v2.1.0 won't have a sequence number, should get priority
        long [] timestamps = {1, 1, 2, 3};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        looper.runToEndOfTasks();

        datadive.uploadingCurrently.set(true);
        datadive.identify(new Identify().add("photo_count", 1));
        datadive.logEvent("test_event1");
        datadive.identify(new Identify().add("photo_count", 2));
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        // need to delete sequence number from test event
        JSONObject event = getUnsentEvents(1).getJSONObject(0);
        assertEquals(event.getLong("event_id"), 1);
        event.remove("sequence_number");
        event.remove("event_id");
        // delete event from db and reinsert modified event
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        dbHelper.removeEvent(1);
        dbHelper.addEvent(event.toString());
        datadive.uploadingCurrently.set(false);

        // log another event to trigger upload
        datadive.logEvent("test_event2");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        // verify some internal counters
        assertEquals(getUnsentEventCount(), 2);
        assertEquals(datadive.lastEventId, 3);
        assertEquals(getUnsentIdentifyCount(), 2);
        assertEquals(datadive.lastIdentifyId, 2);

        JSONObject expectedIdentify1 = new JSONObject();
        expectedIdentify1.put(Constants.AMP_OP_ADD, new JSONObject().put("photo_count", 1));
        JSONObject expectedIdentify2 = new JSONObject();
        expectedIdentify2.put(Constants.AMP_OP_ADD, new JSONObject().put("photo_count", 2));

        // send response and check that merging events correctly ordered events
        RecordedRequest request = runRequest(datadive);
        JSONArray events = getEventsFromRequest(request);
        assertEquals(events.length(), 4);
        assertEquals(events.optJSONObject(0).optString("event_type"), "test_event1");
        assertFalse(events.optJSONObject(0).has("sequence_number"));
        assertEquals(events.optJSONObject(1).optString("event_type"), Constants.IDENTIFY_EVENT);
        assertEquals(events.optJSONObject(1).optLong("sequence_number"), 1);
        assertTrue(Utils.compareJSONObjects(
                events.optJSONObject(1).optJSONObject("user_properties"), expectedIdentify1
        ));
        assertEquals(events.optJSONObject(2).optString("event_type"), Constants.IDENTIFY_EVENT);
        assertEquals(events.optJSONObject(2).optLong("sequence_number"), 3);
        assertTrue(Utils.compareJSONObjects(
                events.optJSONObject(2).optJSONObject("user_properties"), expectedIdentify2
        ));
        assertEquals(events.optJSONObject(3).optString("event_type"), "test_event2");
        assertEquals(events.optJSONObject(3).optLong("sequence_number"), 4);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);
    }

    @Test
    public void testRemoveAfterSuccessfulUpload() throws JSONException {
        long [] timestamps = new long[Constants.EVENT_UPLOAD_MAX_BATCH_SIZE + 4];
        for (int i = 0; i < timestamps.length; i++) timestamps[i] = i;
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        looper.runToEndOfTasks();

        for (int i = 0; i < Constants.EVENT_UPLOAD_THRESHOLD; i++) {
            datadive.logEvent("test_event" + i);
        }
        datadive.identify(new Identify().add("photo_count", 1));
        datadive.identify(new Identify().add("photo_count", 2));
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        assertEquals(getUnsentEventCount(), Constants.EVENT_UPLOAD_THRESHOLD);
        assertEquals(getUnsentIdentifyCount(), 2);

        RecordedRequest request = runRequest(datadive);
        JSONArray events = getEventsFromRequest(request);
        for (int i = 0; i < events.length(); i++) {
            assertEquals(events.optJSONObject(i).optString("event_type"), "test_event" + i);
        }

        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 2); // should have 2 identifys left
    }

    @Test
    public void testLogEventHasUUID() {
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        datadive.logEvent("test_event");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        JSONObject event = getLastUnsentEvent();
        assertTrue(event.has("uuid"));
        assertNotNull(event.optString("uuid"));
        assertTrue(event.optString("uuid").length() > 0);
    }

    @Test
    public void testLogRevenue() {
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();

        JSONObject event, apiProps;

        datadive.logRevenue(10.99);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();

        event = getLastUnsentEvent();
        apiProps = event.optJSONObject("api_properties");
        assertEquals(Constants.AMP_REVENUE_EVENT, event.optString("event_type"));
        assertEquals(Constants.AMP_REVENUE_EVENT, apiProps.optString("special"));
        assertEquals(1, apiProps.optInt("quantity"));
        assertNull(apiProps.optString("productId", null));
        assertEquals(10.99, apiProps.optDouble("price"), .01);
        assertNull(apiProps.optString("receipt", null));
        assertNull(apiProps.optString("receiptSig", null));

        datadive.logRevenue("ID1", 2, 9.99);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();

        event = getLastUnsentEvent();
        apiProps = event.optJSONObject("api_properties");;
        assertEquals(Constants.AMP_REVENUE_EVENT, event.optString("event_type"));
        assertEquals(Constants.AMP_REVENUE_EVENT, apiProps.optString("special"));
        assertEquals(2, apiProps.optInt("quantity"));
        assertEquals("ID1", apiProps.optString("productId"));
        assertEquals(9.99, apiProps.optDouble("price"), .01);
        assertNull(apiProps.optString("receipt", null));
        assertNull(apiProps.optString("receiptSig", null));

        datadive.logRevenue("ID2", 3, 8.99, "RECEIPT", "SIG");
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();

        event = getLastUnsentEvent();
        apiProps = event.optJSONObject("api_properties");
        assertEquals(Constants.AMP_REVENUE_EVENT, event.optString("event_type"));
        assertEquals(Constants.AMP_REVENUE_EVENT, apiProps.optString("special"));
        assertEquals(3, apiProps.optInt("quantity"));
        assertEquals("ID2", apiProps.optString("productId"));
        assertEquals(8.99, apiProps.optDouble("price"), .01);
        assertEquals("RECEIPT", apiProps.optString("receipt"));
        assertEquals("SIG", apiProps.optString("receiptSig"));

        assertNotNull(runRequest(datadive));
    }

    @Test
    public void testLogRevenueV2() throws JSONException {
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        looper.runToEndOfTasks();

        // ignore invalid revenue objects
        datadive.logRevenueV2(null);
        looper.runToEndOfTasks();
        datadive.logRevenueV2(new Revenue());
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);

        // log valid revenue object
        double price = 10.99;
        int quantity = 15;
        String productId = "testProductId";
        String receipt = "testReceipt";
        String receiptSig = "testReceiptSig";
        String revenueType = "testRevenueType";
        JSONObject props = new JSONObject().put("city", "Boston");

        Revenue revenue = new Revenue().setProductId(productId).setPrice(price);
        revenue.setQuantity(quantity).setReceipt(receipt, receiptSig);
        revenue.setRevenueType(revenueType).setRevenueProperties(props);

        datadive.logRevenueV2(revenue);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 1);

        JSONObject event = getLastUnsentEvent();
        assertEquals(event.optString("event_type"), "revenue_amount");

        JSONObject obj = event.optJSONObject("event_properties");
        assertEquals(obj.optDouble("$price"), price, 0);
        assertEquals(obj.optInt("$quantity"), 15);
        assertEquals(obj.optString("$productId"), productId);
        assertEquals(obj.optString("$receipt"), receipt);
        assertEquals(obj.optString("$receiptSig"), receiptSig);
        assertEquals(obj.optString("$revenueType"), revenueType);
        assertEquals(obj.optString("city"), "Boston");

        // user properties should be empty
        assertTrue(Utils.compareJSONObjects(
            event.optJSONObject("user_properties"), new JSONObject()
        ));

        // api properties should not have any revenue info
        JSONObject apiProps = event.optJSONObject("api_properties");
        assertTrue(apiProps.length() > 0);
        assertFalse(apiProps.has("special"));
        assertFalse(apiProps.has("productId"));
        assertFalse(apiProps.has("quantity"));
        assertFalse(apiProps.has("price"));
        assertFalse(apiProps.has("receipt"));
        assertFalse(apiProps.has("receiptSig"));
    }

    @Test
    public void testLogEventSync() {
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        looper.runToEndOfTasks();

        datadive.logEventSync("test_event_sync", null);

        // Event should be in the database synchronously.
        JSONObject event = getLastEvent();
        assertEquals("test_event_sync", event.optString("event_type"));

        looper.runToEndOfTasks();

        server.enqueue(new MockResponse().setBody("success"));
        ShadowLooper httplooper = Shadows.shadowOf(datadive.httpThread.getLooper());
        httplooper.runToEndOfTasks();

        try {
            assertNotNull(server.takeRequest(1, SECONDS));
        } catch (InterruptedException e) {
            fail(e.toString());
        }
    }

    /**
     * Test for not excepting on empty event properties.
     * See https://github.com/datadive/Amplitude-Android/issues/35
     */
    @Test
    public void testEmptyEventProps() {
        RecordedRequest request = sendEvent(datadive, "test_event", new JSONObject());
        assertNotNull(request);
    }

    /**
     * Test that resend failed events only occurs every 30 events.
     */
    @Test
    public void testSaveEventLogic() {
        datadive.trackSessionEvents(true);
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);

        for (int i = 0; i < Constants.EVENT_UPLOAD_THRESHOLD; i++) {
            datadive.logEvent("test");
        }
        looper.runToEndOfTasks();
        // unsent events will be threshold (+1 for start session)
        assertEquals(getUnsentEventCount(), Constants.EVENT_UPLOAD_THRESHOLD + 1);

        server.enqueue(new MockResponse().setBody("invalid_api_key"));
        server.enqueue(new MockResponse().setBody("bad_checksum"));
        ShadowLooper httpLooper = Shadows.shadowOf(datadive.httpThread.getLooper());
        httpLooper.runToEndOfTasks();

        // no events sent, queue should be same size
        assertEquals(getUnsentEventCount(), Constants.EVENT_UPLOAD_THRESHOLD + 1);

        for (int i = 0; i < Constants.EVENT_UPLOAD_THRESHOLD; i++) {
            datadive.logEvent("test");
        }
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), Constants.EVENT_UPLOAD_THRESHOLD * 2 + 1);
        httpLooper.runToEndOfTasks();

        // sent 61 events, should have only made 2 requests
        assertEquals(server.getRequestCount(), 2);
    }

    @Test
    public void testRequestTooLargeBackoffLogic() {
        datadive.trackSessionEvents(true);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        // verify event queue empty
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);

        // 413 error force backoff with 2 events --> new upload limit will be 1
        datadive.logEvent("test");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 2); // 2 events: start session + test
        server.enqueue(new MockResponse().setResponseCode(413));
        ShadowLooper httpLooper = Shadows.shadowOf(datadive.httpThread.getLooper());
        httpLooper.runToEndOfTasks();

        // 413 error with upload limit 1 will remove the top (start session) event
        datadive.logEvent("test");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 3);
        server.enqueue(new MockResponse().setResponseCode(413));
        httpLooper.runToEndOfTasks();

        // verify only start session event removed
        assertEquals(getUnsentEventCount(), 2);
        JSONArray events = getUnsentEvents(2);
        assertEquals(events.optJSONObject(0).optString("event_type"), "test");
        assertEquals(events.optJSONObject(1).optString("event_type"), "test");

        // upload limit persists until event count below threshold
        server.enqueue(new MockResponse().setBody("success"));
        looper.runToEndOfTasks(); // retry uploading after removing large event
        httpLooper.runToEndOfTasks(); // send success --> 1 event sent
        looper.runToEndOfTasks(); // event count below threshold --> disable backoff
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 1);

        // verify backoff disabled - queue 2 more events, see that all get uploaded
        datadive.logEvent("test");
        datadive.logEvent("test");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 3);
        server.enqueue(new MockResponse().setBody("success"));
        httpLooper.runToEndOfTasks();
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
    }

    @Test
    public void testUploadRemainingEvents() {
        long [] timestamps = {1, 2, 3, 4, 5, 6, 7};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);

        datadive.setEventUploadMaxBatchSize(2);
        datadive.setEventUploadThreshold(2);
        datadive.uploadingCurrently.set(true); // block uploading until we queue up enough events
        for (int i = 0; i < 6; i++) {
            datadive.logEvent(String.format("test%d", i));
            looper.runToEndOfTasks();
            looper.runToEndOfTasks();
            assertEquals(dbHelper.getTotalEventCount(), i+1);
        }
        datadive.uploadingCurrently.set(false);

        // allow event uploads
        // 7 events in queue, should upload 2, and then 2, and then 2, and then 2
        datadive.logEvent("test7");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(dbHelper.getEventCount(), 7);
        assertEquals(dbHelper.getIdentifyCount(), 0);
        assertEquals(dbHelper.getTotalEventCount(), 7);

        // server response
        server.enqueue(new MockResponse().setBody("success"));
        ShadowLooper httpLooper = Shadows.shadowOf(datadive.httpThread.getLooper());
        httpLooper.runToEndOfTasks();

        // when receive success response, continue uploading
        looper.runToEndOfTasks();
        looper.runToEndOfTasks(); // remove uploaded events
        assertEquals(dbHelper.getEventCount(), 5);
        assertEquals(dbHelper.getIdentifyCount(), 0);
        assertEquals(dbHelper.getTotalEventCount(), 5);

        // 2nd server response
        server.enqueue(new MockResponse().setBody("success"));
        httpLooper.runToEndOfTasks();
        looper.runToEndOfTasks(); // remove uploaded events
        assertEquals(dbHelper.getEventCount(), 3);
        assertEquals(dbHelper.getIdentifyCount(), 0);
        assertEquals(dbHelper.getTotalEventCount(), 3);

        // 3rd server response
        server.enqueue(new MockResponse().setBody("success"));
        httpLooper.runToEndOfTasks();
        looper.runToEndOfTasks(); // remove uploaded events
        looper.runToEndOfTasks();
        assertEquals(dbHelper.getEventCount(), 1);
        assertEquals(dbHelper.getIdentifyCount(), 0);
        assertEquals(dbHelper.getTotalEventCount(), 1);
    }

    @Test
    public void testBackoffRemoveIdentify() {
        long [] timestamps = {1, 1, 2, 3, 4, 5};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);

        // 413 error force backoff with 2 events --> new upload limit will be 1
        datadive.identify(new Identify().add("photo_count", 1));
        datadive.logEvent("test1");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        assertEquals(getUnsentIdentifyCount(), 1);
        assertEquals(getUnsentEventCount(), 1);

        server.enqueue(new MockResponse().setResponseCode(413));
        ShadowLooper httpLooper = Shadows.shadowOf(datadive.httpThread.getLooper());
        httpLooper.runToEndOfTasks();

        // 413 error with upload limit 1 will remove the top identify
        datadive.logEvent("test2");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 2);
        assertEquals(getUnsentIdentifyCount(), 1);
        server.enqueue(new MockResponse().setResponseCode(413));
        httpLooper.runToEndOfTasks();

        // verify only identify removed
        assertEquals(getUnsentEventCount(), 2);
        assertEquals(getUnsentIdentifyCount(), 0);
        JSONArray events = getUnsentEvents(2);
        assertEquals(events.optJSONObject(0).optString("event_type"), "test1");
        assertEquals(events.optJSONObject(1).optString("event_type"), "test2");
    }

    @Test
    public void testLimitTrackingEnabled() {
        datadive.logEvent("test");
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();
        JSONObject apiProperties = getLastUnsentEvent().optJSONObject("api_properties");
        assertTrue(apiProperties.has("limit_ad_tracking"));
        assertFalse(apiProperties.optBoolean("limit_ad_tracking"));
        assertFalse(apiProperties.has("androidADID"));
    }

    @Test
    public void testTruncateString() {
        String longString = generateStringWithLength(Constants.MAX_STRING_LENGTH * 2, 'c');
        assertEquals(longString.length(), Constants.MAX_STRING_LENGTH * 2);
        String truncatedString = datadive.truncate(longString);
        assertEquals(truncatedString.length(), Constants.MAX_STRING_LENGTH);
        assertEquals(truncatedString, generateStringWithLength(Constants.MAX_STRING_LENGTH, 'c'));
    }

    @Test
    public void testTruncateJSONObject() throws JSONException {
        String longString = generateStringWithLength(Constants.MAX_STRING_LENGTH * 2, 'c');
        String truncString = generateStringWithLength(Constants.MAX_STRING_LENGTH, 'c');
        JSONObject object = new JSONObject();
        object.put("int value", 10);
        object.put("bool value", false);
        object.put("long string", longString);
        object.put("array", new JSONArray().put(longString).put(10));
        object.put("jsonobject", new JSONObject().put("long string", longString));
        object.put(Constants.AMP_REVENUE_RECEIPT, longString);
        object.put(Constants.AMP_REVENUE_RECEIPT_SIG, longString);

        object = datadive.truncate(object);
        assertEquals(object.optInt("int value"), 10);
        assertEquals(object.optBoolean("bool value"), false);
        assertEquals(object.optString("long string"), truncString);
        assertEquals(object.optJSONArray("array").length(), 2);
        assertEquals(object.optJSONArray("array").getString(0), truncString);
        assertEquals(object.optJSONArray("array").getInt(1), 10);
        assertEquals(object.optJSONObject("jsonobject").length(), 1);
        assertEquals(object.optJSONObject("jsonobject").optString("long string"), truncString);

        // receipt and receipt sig should not be truncated
        assertEquals(object.optString(Constants.AMP_REVENUE_RECEIPT), longString);
        assertEquals(object.optString(Constants.AMP_REVENUE_RECEIPT_SIG), longString);
    }

    @Test
    public void testTruncateNullJSONObject() throws JSONException {
        assertTrue(Utils.compareJSONObjects(
            datadive.truncate((JSONObject) null), new JSONObject()
        ));
        assertEquals(datadive.truncate((JSONArray) null).length(), 0);
    }

    @Test
    public void testTruncateEventAndIdentify() throws JSONException {
        String longString = generateStringWithLength(Constants.MAX_STRING_LENGTH * 2, 'c');
        String truncString = generateStringWithLength(Constants.MAX_STRING_LENGTH, 'c');

        long [] timestamps = {1, 1, 2, 3};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        looper.runToEndOfTasks();
        datadive.logEvent("test", new JSONObject().put("long_string", longString));
        datadive.identify(new Identify().set("long_string", longString));

        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        RecordedRequest request = runRequest(datadive);
        JSONArray events = getEventsFromRequest(request);

        assertEquals(events.optJSONObject(0).optString("event_type"), "test");
        assertTrue(Utils.compareJSONObjects(
                events.optJSONObject(0).optJSONObject("event_properties"),
                new JSONObject().put("long_string", truncString)
        ));
        assertEquals(events.optJSONObject(1).optString("event_type"), Constants.IDENTIFY_EVENT);
        assertTrue(Utils.compareJSONObjects(
                events.optJSONObject(1).optJSONObject("user_properties"),
                new JSONObject().put(Constants.AMP_OP_SET, new JSONObject().put("long_string", truncString))
        ));
    }

    @Test
    public void testAutoIncrementSequenceNumber() {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        int limit = 10;
        for (int i = 0; i < limit; i++) {
            assertEquals(datadive.getNextSequenceNumber(), i+1);
            assertEquals(dbHelper.getLongValue(DatadiveClient.SEQUENCE_NUMBER_KEY), Long.valueOf(i+1));
        }
    }

    @Test
    public void testSetOffline() throws JSONException {
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        datadive.setOffline(true);

        datadive.logEvent("test1");
        datadive.logEvent("test2");
        datadive.identify(new Identify().unset("key1"));
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 2);
        assertEquals(getUnsentIdentifyCount(), 1);

        datadive.setOffline(false);
        looper.runToEndOfTasks();
        RecordedRequest request = runRequest(datadive);
        JSONArray events = getEventsFromRequest(request);
        looper.runToEndOfTasks();

        assertEquals(events.length(), 3);
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);
    }

    @Test
    public void testSetOfflineTruncate() throws JSONException {
        long [] timestamps = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        int eventMaxCount = 3;
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        datadive.setEventMaxCount(eventMaxCount).setOffline(true);

        datadive.logEvent("test1");
        datadive.logEvent("test2");
        datadive.logEvent("test3");
        datadive.identify(new Identify().unset("key1"));
        datadive.identify(new Identify().unset("key2"));
        datadive.identify(new Identify().unset("key3"));
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), eventMaxCount);
        assertEquals(getUnsentIdentifyCount(), eventMaxCount);

        datadive.logEvent("test4");
        datadive.identify(new Identify().unset("key4"));
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), eventMaxCount);
        assertEquals(getUnsentIdentifyCount(), eventMaxCount);

        List<JSONObject> events = dbHelper.getEvents(-1, -1);
        assertEquals(events.size(), eventMaxCount);
        assertEquals(events.get(0).optString("event_type"), "test2");
        assertEquals(events.get(1).optString("event_type"), "test3");
        assertEquals(events.get(2).optString("event_type"), "test4");

        List<JSONObject> identifys = dbHelper.getIdentifys(-1, -1);
        assertEquals(identifys.size(), eventMaxCount);
        assertEquals(identifys.get(0).optJSONObject("user_properties").optJSONObject("$unset").optString("key2"), "-");
        assertEquals(identifys.get(1).optJSONObject("user_properties").optJSONObject("$unset").optString("key3"), "-");
        assertEquals(identifys.get(2).optJSONObject("user_properties").optJSONObject("$unset").optString("key4"), "-");
    }

    @Test
    public void testTruncateEventsQueues() {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        int eventMaxCount = 50;
        assertTrue(eventMaxCount > Constants.EVENT_REMOVE_BATCH_SIZE);
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        datadive.setEventMaxCount(eventMaxCount).setOffline(true);

        for (int i = 0; i < eventMaxCount; i++) {
            datadive.logEvent("test");
        }
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), eventMaxCount);

        datadive.logEvent("test");
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), eventMaxCount - (eventMaxCount/10) + 1);
    }

    @Test
    public void testTruncateEventsQueuesWithOneEvent() {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        int eventMaxCount = 1;
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        datadive.setEventMaxCount(eventMaxCount).setOffline(true);

        datadive.logEvent("test1");
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), eventMaxCount);

        datadive.logEvent("test2");
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), eventMaxCount);

        JSONObject event = getLastUnsentEvent();
        assertEquals(event.optString("event_type"), "test2");
    }

    @Test
    public void testClearUserProperties() throws JSONException {
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());

        datadive.clearUserProperties();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 1);
        JSONObject event = getLastUnsentIdentify();
        assertEquals(Constants.IDENTIFY_EVENT, event.optString("event_type"));
        assertTrue(Utils.compareJSONObjects(
            event.optJSONObject("event_properties"), new JSONObject()
        ));

        JSONObject userPropertiesOperations = event.optJSONObject("user_properties");
        assertEquals(userPropertiesOperations.length(), 1);
        assertTrue(userPropertiesOperations.has(Constants.AMP_OP_CLEAR_ALL));

        assertEquals(
            "-", userPropertiesOperations.optString(Constants.AMP_OP_CLEAR_ALL)
        );
    }

    @Test
    public void testSetGroup() throws JSONException {
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());

        datadive.setGroup("orgId", new JSONArray().put(10).put(15));
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 1);
        JSONObject event = getLastUnsentIdentify();
        assertEquals(Constants.IDENTIFY_EVENT, event.optString("event_type"));
        assertTrue(Utils.compareJSONObjects(
            event.optJSONObject("event_properties"), new JSONObject()
        ));

        JSONObject userPropertiesOperations = event.optJSONObject("user_properties");
        assertEquals(userPropertiesOperations.length(), 1);
        assertTrue(userPropertiesOperations.has(Constants.AMP_OP_SET));

        JSONObject groups = event.optJSONObject("groups");
        assertEquals(groups.length(), 1);
        assertEquals(groups.optJSONArray("orgId"), new JSONArray().put(10).put(15));

        JSONObject setOperations = userPropertiesOperations.optJSONObject(Constants.AMP_OP_SET);
        assertEquals(setOperations.length(), 1);
        assertEquals(setOperations.optJSONArray("orgId"), new JSONArray().put(10).put(15));
    }

    @Test
    public void testLogEventWithGroups() throws JSONException {
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());

        JSONObject groups = new JSONObject().put("orgId", 10).put("sport", "tennis");
        datadive.logEvent("test", null, groups);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        assertEquals(getUnsentEventCount(), 1);
        assertEquals(getUnsentIdentifyCount(), 0);
        JSONObject event = getLastUnsentEvent();
        assertEquals(event.optString("event_type"), "test");
        assertTrue(Utils.compareJSONObjects(
            event.optJSONObject("event_properties"), new JSONObject()
        ));
        assertTrue(Utils.compareJSONObjects(
            event.optJSONObject("user_properties"), new JSONObject()
        ));

        JSONObject eventGroups = event.optJSONObject("groups");
        assertEquals(eventGroups.length(), 2);
        assertEquals(eventGroups.optInt("orgId"), 10);
        assertEquals(eventGroups.optString("sport"), "tennis");
    }

    @Test
    public void testMergeEventsArrayIndexOutOfBounds() throws JSONException {
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());

        datadive.setOffline(true);

        datadive.logEvent("testEvent1");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        // force failure case
        datadive.setLastEventId(0);

        datadive.setOffline(false);
        looper.runToEndOfTasks();

        // make sure next upload succeeds
        datadive.setLastEventId(1);
        datadive.logEvent("testEvent2");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        RecordedRequest request = runRequest(datadive);
        JSONArray events = getEventsFromRequest(request);
        assertEquals(events.length(), 2);

        assertEquals(events.getJSONObject(0).optString("event_type"), "testEvent1");
        assertEquals(events.getJSONObject(0).optLong("event_id"), 1);

        assertEquals(events.getJSONObject(1).optString("event_type"), "testEvent2");
        assertEquals(events.getJSONObject(1).optLong("event_id"), 2);
    }

    @Test
    public void testCursorWindowAllocationException() {
        Robolectric.getForegroundThreadScheduler().advanceTo(1);
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());

        // log an event successfully
        datadive.logEvent("testEvent1");
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 1);
        assertEquals(getUnsentIdentifyCount(), 0);

        // mock out database helper to force CursorWindowAllocationExceptions
        DatabaseHelper.instances.put(Constants.DEFAULT_INSTANCE, new MockDatabaseHelper(context));

        // force an upload and verify no request sent
        // make sure we catch it during sending of events and defer sending
        RecordedRequest request = runRequest(datadive);
        assertNull(request);
        assertEquals(getUnsentEventCount(), 1);
        assertEquals(getUnsentIdentifyCount(), 0);

        // make sure we catch it during initialization and treat as uninitialized
        datadive.initialized = false;
        datadive.initialize(context, apiKey);
        looper.runToEndOfTasks();
        assertNull(datadive.apiKey);

        // since event meta data is loaded during initialize, in theory we should
        // be able to log an event even if we can't query from it
        datadive.context = context;
        datadive.apiKey = apiKey;
        Identify identify = new Identify().set("car", "blue");
        datadive.identify(identify);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 1);
        assertEquals(getUnsentIdentifyCount(), 1);
    }

    @Test
    public void testBlockTooManyEventUserProperties() throws JSONException {
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());

        JSONObject eventProperties = new JSONObject();
        JSONObject userProperties = new JSONObject();
        Identify identify = new Identify();

        for (int i = 0; i < Constants.MAX_PROPERTY_KEYS + 1; i++) {
            eventProperties.put(String.valueOf(i), i);
            userProperties.put(String.valueOf(i*2), i*2);
            identify.setOnce(String.valueOf(i), i);
        }

        // verify user properties is filtered out
        datadive.setUserProperties(userProperties);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(getUnsentIdentifyCount(), 0);

        // verify scrubbed from events
        datadive.logEvent("test event", eventProperties);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 1);
        JSONObject event = getLastUnsentEvent();
        assertEquals(event.optString("event_type"), "test event");
        assertTrue(Utils.compareJSONObjects(
            event.optJSONObject("event_properties"), new JSONObject()
        ));

        // verify scrubbed from identifys - but leaves an empty JSONObject
        datadive.identify(identify);
        looper.runToEndOfTasks();
        assertEquals(getUnsentIdentifyCount(), 1);
        JSONObject identifyEvent = getLastUnsentIdentify();
        assertEquals(identifyEvent.optString("event_type"), "$identify");
        assertTrue(Utils.compareJSONObjects(
            identifyEvent.optJSONObject("user_properties"),
            new JSONObject().put("$setOnce", new JSONObject())
        ));
    }

    @Test
    public void testLogEventWithTimestamp() throws JSONException {
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());

        datadive.logEvent("test", null, null, 1000, false);
        looper.runToEndOfTasks();
        JSONObject event = getLastUnsentEvent();
        assertEquals(event.optLong("timestamp"), 1000);

        datadive.logEventSync("test", null, null, 2000, false);
        looper.runToEndOfTasks();
        event = getLastUnsentEvent();
        assertEquals(event.optLong("timestamp"), 2000);
    }

    @Test
    public void testRegenerateDeviceId() {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        String oldDeviceId = datadive.getDeviceId();
        assertEquals(oldDeviceId, dbHelper.getValue("device_id"));

        datadive.regenerateDeviceId();
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();
        String newDeviceId = datadive.getDeviceId();
        assertNotEquals(oldDeviceId, newDeviceId);
        assertEquals(newDeviceId, dbHelper.getValue("device_id"));
        assertTrue(newDeviceId.endsWith("R"));
    }

    @Test
    public void testSendNullEvents() throws JSONException {
        DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper(context);
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());

        dbHelper.addEvent(null);
        datadive.setLastEventId(1);
        datadive.getNextSequenceNumber();
        assertEquals(getUnsentEventCount(), 1);

        datadive.logEvent("test event");
        looper.runToEndOfTasks();

        datadive.updateServer();
        RecordedRequest request = runRequest(datadive);
        JSONArray events = getEventsFromRequest(request);
        assertEquals(events.length(), 1);
        assertEquals(events.optJSONObject(0).optString("event_type"), "test event");
    }

    @Test
    @PrepareForTest(OkHttpClient.class)
    public void testHandleUploadExceptions() throws Exception {
        ShadowLooper logLooper = Shadows.shadowOf(datadive.logThread.getLooper());
        ShadowLooper httpLooper = Shadows.shadowOf(datadive.httpThread.getLooper());
        IOException error = new IOException("test IO Exception");

        // mock out client
        OkHttpClient oldClient = datadive.httpClient;
        OkHttpClient mockClient = PowerMockito.mock(OkHttpClient.class);

        // need to have mock client return mock call that throws exception
        Call mockCall = PowerMockito.mock(Call.class);
        PowerMockito.when(mockCall.execute()).thenThrow(error);
        PowerMockito.when(mockClient.newCall(Matchers.any(Request.class))).thenReturn(mockCall);

        // attach mock client to datadive
        datadive.httpClient = mockClient;
        datadive.logEvent("test event");
        logLooper.runToEndOfTasks();
        logLooper.runToEndOfTasks();
        httpLooper.runToEndOfTasks();

        assertEquals(datadive.lastError, error);

        // restore old client
        datadive.httpClient = oldClient;
    }

    @Test
    public void testDefaultPlatform() throws InterruptedException {
        long [] timestamps = {1, 2, 3, 4, 5, 6, 7};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        looper.runToEndOfTasks();

        assertEquals(datadive.platform, Constants.PLATFORM);

        datadive.logEvent("test_event1");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        assertEquals(getUnsentEventCount(), 1);
        assertEquals(getUnsentIdentifyCount(), 0);
        JSONArray events = getUnsentEvents(1);
        for (int i = 0; i < 1; i++) {
            assertEquals(events.optJSONObject(i).optString("event_type"), "test_event" + (i+1));
            assertEquals(events.optJSONObject(i).optLong("timestamp"), timestamps[i]);
            assertEquals(events.optJSONObject(i).optString("platform"), Constants.PLATFORM);
        }
        runRequest(datadive);
    }

    @Test
    public void testOverridePlatform() throws InterruptedException {
        long [] timestamps = {1, 2, 3, 4, 5, 6, 7};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        looper.runToEndOfTasks();

        String customPlatform = "test_custom_platform";

        // force re-initialize to override platform
        datadive.initialized = false;
        datadive.initialize(context, apiKey, null, customPlatform, false);
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();
        assertEquals(datadive.platform, customPlatform);

        datadive.logEvent("test_event1");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        assertEquals(getUnsentEventCount(), 1);
        assertEquals(getUnsentIdentifyCount(), 0);
        JSONArray events = getUnsentEvents(1);
        for (int i = 0; i < 1; i++) {
            assertEquals(events.optJSONObject(i).optString("event_type"), "test_event" + (i+1));
            assertEquals(events.optJSONObject(i).optLong("timestamp"), timestamps[i]);
            assertEquals(events.optJSONObject(i).optString("platform"), customPlatform);
        }
        runRequest(datadive);
    }

    @Test
    public void testSetTrackingConfig() throws JSONException {
        long [] timestamps = {1, 2, 3, 4, 5, 6, 7};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        looper.runToEndOfTasks();

        TrackingOptions options = new TrackingOptions().disableCity().disableCountry().disableIpAddress().disableLanguage().disableLatLng();
        datadive.setTrackingOptions(options);

        assertEquals(datadive.appliedTrackingOptions, options);
        assertTrue(Utils.compareJSONObjects(datadive.apiPropertiesTrackingOptions, options.getApiPropertiesTrackingOptions()));
        assertFalse(datadive.appliedTrackingOptions.shouldTrackCity());
        assertFalse(datadive.appliedTrackingOptions.shouldTrackCountry());
        assertFalse(datadive.appliedTrackingOptions.shouldTrackIpAddress());
        assertFalse(datadive.appliedTrackingOptions.shouldTrackLanguage());
        assertFalse(datadive.appliedTrackingOptions.shouldTrackLatLng());

        datadive.logEvent("test event");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        JSONArray events = getUnsentEvents(1);
        assertEquals(events.length(), 1);
        JSONObject event = events.getJSONObject(0);

        // verify we do have platform and carrier since those were not filtered out
        assertTrue(event.has("carrier"));
        assertTrue(event.has("platform"));

        // verify we do not have any of the filtered out fields
        assertFalse(event.has("city"));
        assertFalse(event.has("country"));
        assertFalse(event.has("language"));

        // verify api properties contains tracking options for location filtering
        JSONObject apiProperties = event.getJSONObject("api_properties");
        assertFalse(apiProperties.getBoolean("limit_ad_tracking"));
        assertFalse(apiProperties.getBoolean("gps_enabled"));
        assertTrue(apiProperties.has("tracking_options"));

        JSONObject trackingOptions = apiProperties.getJSONObject("tracking_options");
        assertEquals(trackingOptions.length(), 4);
        assertFalse(trackingOptions.getBoolean("city"));
        assertFalse(trackingOptions.getBoolean("country"));
        assertFalse(trackingOptions.getBoolean("ip_address"));
        assertFalse(trackingOptions.getBoolean("lat_lng"));
    }

    @Test
    public void testEnableCoppaControl() throws JSONException {
        long [] timestamps = {1, 2, 3, 4, 5, 6, 7};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        looper.runToEndOfTasks();

        datadive.disableCoppaControl();  // this shouldn't do anything

        TrackingOptions options = new TrackingOptions();
        assertEquals(datadive.inputTrackingOptions, options);
        assertEquals(datadive.appliedTrackingOptions, options);
        assertTrue(Utils.compareJSONObjects(datadive.apiPropertiesTrackingOptions, options.getApiPropertiesTrackingOptions()));

        // haven't merged in the privacy guard settings yet
        assertTrue(datadive.appliedTrackingOptions.shouldTrackLanguage());
        assertTrue(datadive.appliedTrackingOptions.shouldTrackCarrier());
        assertTrue(datadive.appliedTrackingOptions.shouldTrackIpAddress());
        assertTrue(datadive.appliedTrackingOptions.shouldTrackAdid());
        assertTrue(datadive.appliedTrackingOptions.shouldTrackCity());
        assertTrue(datadive.appliedTrackingOptions.shouldTrackLatLng());

        datadive.logEvent("test event");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        JSONArray events = getUnsentEvents(1);
        assertEquals(events.length(), 1);
        JSONObject event = events.getJSONObject(0);

        // verify we do have platform and carrier since those were not filtered out
        assertTrue(event.has("country"));
        assertTrue(event.has("platform"));
        assertTrue(event.has("carrier"));
        assertTrue(event.has("language"));

        // verify api properties contains tracking options for location filtering
        JSONObject apiProperties = event.getJSONObject("api_properties");
        assertFalse(apiProperties.getBoolean("limit_ad_tracking"));
        assertFalse(apiProperties.getBoolean("gps_enabled"));
        assertFalse(apiProperties.has("tracking_options"));

        // test enabling privacy guard
        datadive.enableCoppaControl();
        assertEquals(datadive.inputTrackingOptions, options);
        assertNotEquals(datadive.appliedTrackingOptions, options);

        // make sure we merge in the privacy guard options
        assertFalse(datadive.appliedTrackingOptions.shouldTrackIpAddress());
        assertFalse(datadive.appliedTrackingOptions.shouldTrackAdid());
        assertFalse(datadive.appliedTrackingOptions.shouldTrackCity());
        assertFalse(datadive.appliedTrackingOptions.shouldTrackLatLng());

        datadive.logEvent("test event 1");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        events = getUnsentEvents(2);
        assertEquals(events.length(), 2);
        event = events.getJSONObject(1);

        // verify we do have platform and carrier since those were not filtered out
        assertTrue(event.has("country"));
        assertTrue(event.has("platform"));
        assertTrue(event.has("carrier"));
        assertTrue(event.has("language"));

        // verify api properties contains tracking options for location filtering
        apiProperties = event.getJSONObject("api_properties");
        assertFalse(apiProperties.getBoolean("limit_ad_tracking"));
        assertFalse(apiProperties.getBoolean("gps_enabled"));
        assertTrue(apiProperties.has("tracking_options"));

        JSONObject trackingOptions = apiProperties.getJSONObject("tracking_options");
        assertEquals(trackingOptions.length(), 3);
        assertFalse(trackingOptions.getBoolean("ip_address"));
        assertFalse(trackingOptions.getBoolean("city"));
        assertFalse(trackingOptions.getBoolean("lat_lng"));

        // test disabling privacy guard
        datadive.disableCoppaControl();

        assertEquals(datadive.inputTrackingOptions, options);
        assertEquals(datadive.appliedTrackingOptions, options);
        assertTrue(Utils.compareJSONObjects(datadive.apiPropertiesTrackingOptions, options.getApiPropertiesTrackingOptions()));
        assertTrue(datadive.appliedTrackingOptions.shouldTrackLanguage());
        assertTrue(datadive.appliedTrackingOptions.shouldTrackCarrier());
        assertTrue(datadive.appliedTrackingOptions.shouldTrackIpAddress());
        assertTrue(datadive.appliedTrackingOptions.shouldTrackAdid());
        assertTrue(datadive.appliedTrackingOptions.shouldTrackCity());
        assertTrue(datadive.appliedTrackingOptions.shouldTrackLatLng());

        datadive.logEvent("test event 2");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        events = getUnsentEvents(3);
        assertEquals(events.length(), 3);
        event = events.getJSONObject(2);

        // verify we do have platform and carrier since those were not filtered out
        assertTrue(event.has("country"));
        assertTrue(event.has("platform"));
        assertTrue(event.has("carrier"));
        assertTrue(event.has("language"));

        // verify api properties contains tracking options for location filtering
        apiProperties = event.getJSONObject("api_properties");
        assertFalse(apiProperties.getBoolean("limit_ad_tracking"));
        assertFalse(apiProperties.getBoolean("gps_enabled"));
        assertFalse(apiProperties.has("tracking_options"));
    }

    @Test
    public void testEnableCoppaControlWithOptions() throws JSONException {
        long [] timestamps = {1, 2, 3, 4, 5, 6, 7};
        clock.setTimestamps(timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        looper.runToEndOfTasks();

        TrackingOptions options = new TrackingOptions().disableLanguage().disableCarrier().disableIpAddress();
        datadive.setTrackingOptions(options);

        assertEquals(datadive.inputTrackingOptions, options);
        assertEquals(datadive.appliedTrackingOptions, options);
        assertTrue(Utils.compareJSONObjects(datadive.apiPropertiesTrackingOptions, options.getApiPropertiesTrackingOptions()));
        assertFalse(datadive.appliedTrackingOptions.shouldTrackLanguage());
        assertFalse(datadive.appliedTrackingOptions.shouldTrackCarrier());
        assertFalse(datadive.appliedTrackingOptions.shouldTrackIpAddress());

        // haven't merged in the privacy guard settings yet
        assertTrue(datadive.appliedTrackingOptions.shouldTrackAdid());
        assertTrue(datadive.appliedTrackingOptions.shouldTrackCity());
        assertTrue(datadive.appliedTrackingOptions.shouldTrackLatLng());

        datadive.logEvent("test event");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        JSONArray events = getUnsentEvents(1);
        assertEquals(events.length(), 1);
        JSONObject event = events.getJSONObject(0);

        // verify we do have platform and carrier since those were not filtered out
        assertTrue(event.has("country"));
        assertTrue(event.has("platform"));

        // verify we do not have any of the filtered out fields
        assertFalse(event.has("carrier"));
        assertFalse(event.has("language"));

        // verify api properties contains tracking options for location filtering
        JSONObject apiProperties = event.getJSONObject("api_properties");
        assertFalse(apiProperties.getBoolean("limit_ad_tracking"));
        assertFalse(apiProperties.getBoolean("gps_enabled"));
        assertTrue(apiProperties.has("tracking_options"));

        JSONObject trackingOptions = apiProperties.getJSONObject("tracking_options");
        assertEquals(trackingOptions.length(), 1);
        assertFalse(trackingOptions.getBoolean("ip_address"));

        // when we enable privacy guard, make sure we maintain original tracking options
        datadive.enableCoppaControl();
        assertEquals(datadive.inputTrackingOptions, options);
        assertNotEquals(datadive.appliedTrackingOptions, options);

        // also make sure we merge in the privacy guard options
        assertFalse(datadive.appliedTrackingOptions.shouldTrackLanguage());
        assertFalse(datadive.appliedTrackingOptions.shouldTrackCarrier());
        assertFalse(datadive.appliedTrackingOptions.shouldTrackIpAddress());
        assertFalse(datadive.appliedTrackingOptions.shouldTrackAdid());
        assertFalse(datadive.appliedTrackingOptions.shouldTrackCity());
        assertFalse(datadive.appliedTrackingOptions.shouldTrackLatLng());

        datadive.logEvent("test event 1");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        events = getUnsentEvents(2);
        assertEquals(events.length(), 2);
        event = events.getJSONObject(1);

        // verify we do have platform and carrier since those were not filtered out
        assertTrue(event.has("country"));
        assertTrue(event.has("platform"));

        // verify we do not have any of the filtered out fields
        assertFalse(event.has("carrier"));
        assertFalse(event.has("language"));

        // verify api properties contains tracking options for location filtering
        apiProperties = event.getJSONObject("api_properties");
        assertFalse(apiProperties.getBoolean("limit_ad_tracking"));
        assertFalse(apiProperties.getBoolean("gps_enabled"));
        assertTrue(apiProperties.has("tracking_options"));

        trackingOptions = apiProperties.getJSONObject("tracking_options");
        assertEquals(trackingOptions.length(), 3);
        assertFalse(trackingOptions.getBoolean("ip_address"));
        assertFalse(trackingOptions.getBoolean("city"));
        assertFalse(trackingOptions.getBoolean("lat_lng"));

        // disable privacy guard and make sure original user input is maintained
        datadive.disableCoppaControl();

        assertEquals(datadive.inputTrackingOptions, options);
        assertEquals(datadive.appliedTrackingOptions, options);
        assertTrue(Utils.compareJSONObjects(datadive.apiPropertiesTrackingOptions, options.getApiPropertiesTrackingOptions()));
        assertFalse(datadive.appliedTrackingOptions.shouldTrackLanguage());
        assertFalse(datadive.appliedTrackingOptions.shouldTrackCarrier());
        assertFalse(datadive.appliedTrackingOptions.shouldTrackIpAddress());

        // haven't merged in the privacy guard settings yet
        assertTrue(datadive.appliedTrackingOptions.shouldTrackAdid());
        assertTrue(datadive.appliedTrackingOptions.shouldTrackCity());
        assertTrue(datadive.appliedTrackingOptions.shouldTrackLatLng());

        datadive.logEvent("test event 2");
        looper.runToEndOfTasks();
        looper.runToEndOfTasks();

        events = getUnsentEvents(3);
        assertEquals(events.length(), 3);
        event = events.getJSONObject(2);

        // verify we do have platform and carrier since those were not filtered out
        assertTrue(event.has("country"));
        assertTrue(event.has("platform"));

        // verify we do not have any of the filtered out fields
        assertFalse(event.has("carrier"));
        assertFalse(event.has("language"));

        // verify api properties contains tracking options for location filtering
        apiProperties = event.getJSONObject("api_properties");
        assertFalse(apiProperties.getBoolean("limit_ad_tracking"));
        assertFalse(apiProperties.getBoolean("gps_enabled"));
        assertTrue(apiProperties.has("tracking_options"));

        trackingOptions = apiProperties.getJSONObject("tracking_options");
        assertEquals(trackingOptions.length(), 1);
        assertFalse(trackingOptions.getBoolean("ip_address"));
    }

    @Test
    public void testGroupIdentifyMultipleOperations() throws JSONException {
        String groupType = "test group type";
        String groupName = "test group name";

        String property1 = "string value";
        String value1 = "testValue";

        String property2 = "double value";
        double value2 = 0.123;

        String property3 = "boolean value";
        boolean value3 = true;

        String property4 = "json value";

        Identify identify = new Identify().setOnce(property1, value1).add(property2, value2);
        identify.set(property3, value3).unset(property4);

        // identify should ignore this since duplicate key
        identify.set(property4, value3);

        datadive.groupIdentify(groupType, groupName, identify);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();
        assertEquals(getUnsentIdentifyCount(), 1);
        assertEquals(getUnsentEventCount(), 0);
        JSONObject event = getLastUnsentIdentify();
        assertEquals(Constants.GROUP_IDENTIFY_EVENT, event.optString("event_type"));

        assertTrue(Utils.compareJSONObjects(event.optJSONObject("event_properties"), new JSONObject()));
        assertTrue(Utils.compareJSONObjects(event.optJSONObject("user_properties"), new JSONObject()));

        JSONObject groups = event.optJSONObject("groups");
        JSONObject expectedGroups = new JSONObject();
        expectedGroups.put(groupType, groupName);
        assertTrue(Utils.compareJSONObjects(groups, expectedGroups));

        JSONObject groupProperties = event.optJSONObject("group_properties");
        JSONObject expected = new JSONObject();
        expected.put(Constants.AMP_OP_SET_ONCE, new JSONObject().put(property1, value1));
        expected.put(Constants.AMP_OP_ADD, new JSONObject().put(property2, value2));
        expected.put(Constants.AMP_OP_SET, new JSONObject().put(property3, value3));
        expected.put(Constants.AMP_OP_UNSET, new JSONObject().put(property4, "-"));
        assertTrue(Utils.compareJSONObjects(groupProperties, expected));
    }
}
