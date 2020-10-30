package ai.datadive.api;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@Config(manifest = Config.NONE)
public class SessionTest extends BaseTest {

    // allows for control of System.currentTimeMillis
    private class DatadiveCallbacksWithTime extends DatadiveCallbacks {

        private int index;
        private long [] timestamps = null;

        public DatadiveCallbacksWithTime(DatadiveClient client, long [] timestamps) {
            super(client);
            this.index = 0;
            this.timestamps = timestamps;
        }

        @Override
        protected long getCurrentTimeMillis() {
            return timestamps[index++ % timestamps.length];
        }
    }

    @Before
    public void setUp() throws Exception {
        super.setUp(true);
        datadive.initialize(context, apiKey);
        Shadows.shadowOf(datadive.logThread.getLooper()).runOneTask();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testDefaultStartSession() {
        long timestamp = System.currentTimeMillis();
        datadive.logEventAsync("test", null, null, null, null, null, timestamp, false);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();

        // trackSessionEvents is false, no start_session event added
        assertEquals(getUnsentEventCount(), 1);
        JSONObject event = getLastUnsentEvent();
        assertEquals(event.optString("event_type"), "test");
        assertEquals(event.optString("session_id"), String.valueOf(timestamp));
    }

    @Test
    public void testDefaultTriggerNewSession() {
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        long sessionTimeoutMillis = 5 * 1000; //5s
        datadive.setSessionTimeoutMillis(sessionTimeoutMillis);

        // log 1st event, initialize first session
        long timestamp1 = System.currentTimeMillis();
        datadive.logEventAsync("test1", null, null, null, null, null, timestamp1, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 1);

        // log 2nd event past timeout, verify new session started
        long timestamp2 = timestamp1 + sessionTimeoutMillis;
        datadive.logEventAsync("test2", null, null, null, null, null, timestamp2, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 2);

        JSONArray events = getUnsentEvents(2);
        JSONObject event1 = events.optJSONObject(0);
        JSONObject event2 = events.optJSONObject(1);

        assertEquals(event1.optString("event_type"), "test1");
        assertEquals(event1.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event2.optString("event_type"), "test2");
        assertEquals(event2.optString("session_id"), String.valueOf(timestamp2));

        // also test getSessionId
        assertEquals(datadive.getSessionId(), timestamp2);
    }

    @Test
    public void testDefaultExtendSession() {
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        long sessionTimeoutMillis = 5 * 1000; //5s
        datadive.setSessionTimeoutMillis(sessionTimeoutMillis);

        // log 3 events all just within session expiration window, verify all in same session
        long timestamp1 = System.currentTimeMillis();
        datadive.logEventAsync("test1", null, null, null, null, null, timestamp1, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 1);

        long timestamp2 = timestamp1 + sessionTimeoutMillis - 1;
        datadive.logEventAsync("test2", null, null, null, null, null, timestamp2, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 2);

        long timestamp3 = timestamp2 + sessionTimeoutMillis - 1;
        datadive.logEventAsync("test3", null, null, null, null, null, timestamp3, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 3);

        JSONArray events = getUnsentEvents(3);
        JSONObject event1 = events.optJSONObject(0);
        JSONObject event2 = events.optJSONObject(1);
        JSONObject event3 = events.optJSONObject(2);

        assertEquals(event1.optString("event_type"), "test1");
        assertEquals(event1.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event1.optString("timestamp"), String.valueOf(timestamp1));

        assertEquals(event2.optString("event_type"), "test2");
        assertEquals(event2.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event2.optString("timestamp"), String.valueOf(timestamp2));

        assertEquals(event3.optString("event_type"), "test3");
        assertEquals(event3.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event3.optString("timestamp"), String.valueOf(timestamp3));
    }

    @Test
    public void testDefaultStartSessionWithTracking() {
        datadive.trackSessionEvents(true);

        long timestamp = System.currentTimeMillis();
        datadive.logEventAsync("test", null, null, null, null, null, timestamp, false);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();

        // trackSessions is true, start_session event is added
        assertEquals(getUnsentEventCount(), 2);
        JSONArray events = getUnsentEvents(2);
        JSONObject session_event = events.optJSONObject(0);
        JSONObject test_event = events.optJSONObject(1);

        assertEquals(session_event.optString("event_type"), DatadiveClient.START_SESSION_EVENT);
        assertEquals(
                session_event.optJSONObject("api_properties").optString("special"),
                DatadiveClient.START_SESSION_EVENT
        );
        assertEquals(session_event.optString("session_id"), String.valueOf(timestamp));

        assertEquals(test_event.optString("event_type"), "test");
        assertEquals(test_event.optString("session_id"), String.valueOf(timestamp));
    }

    @Test
    public void testDefaultStartSessionWithTrackingSynchronous() {
        datadive.trackSessionEvents(true);

        long timestamp = System.currentTimeMillis();
        datadive.logEvent("test", null, null, null, null, null, timestamp, false);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();
        // trackSessions is true, start_session event is added
        assertEquals(getUnsentEventCount(), 2);

        // verify order of synchronous events
        JSONArray events = getUnsentEvents(2);
        JSONObject session_event = events.optJSONObject(0);
        JSONObject test_event = events.optJSONObject(1);

        assertEquals(session_event.optString("event_type"), DatadiveClient.START_SESSION_EVENT);
        assertEquals(
                session_event.optJSONObject("api_properties").optString("special"),
                DatadiveClient.START_SESSION_EVENT
        );
        assertEquals(session_event.optString("session_id"), String.valueOf(timestamp));

        assertEquals(test_event.optString("event_type"), "test");
        assertEquals(test_event.optString("session_id"), String.valueOf(timestamp));
    }

    @Test
    public void testDefaultTriggerNewSessionWithTracking() {
        datadive.trackSessionEvents(true);

        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        long sessionTimeoutMillis = 5 * 1000; //5s
        datadive.setSessionTimeoutMillis(sessionTimeoutMillis);

        // log 1st event, initialize first session
        long timestamp1 = System.currentTimeMillis();
        datadive.logEventAsync("test1", null, null, null, null, null, timestamp1, false);
        looper.runToEndOfTasks();
        // trackSessions is true, start_session event is added
        assertEquals(getUnsentEventCount(), 2);

        // log 2nd event past timeout, verify new session started
        long timestamp2 = timestamp1 + sessionTimeoutMillis;
        datadive.logEventAsync("test2", null, null, null, null, null, timestamp2, false);
        looper.runToEndOfTasks();
        // trackSessions is true, end_session and start_session events are added
        assertEquals(getUnsentEventCount(), 5);

        JSONArray events = getUnsentEvents(5);
        JSONObject startSession1 = events.optJSONObject(0);
        JSONObject event1 = events.optJSONObject(1);
        JSONObject endSession = events.optJSONObject(2);
        JSONObject startSession2 = events.optJSONObject(3);
        JSONObject event2 = events.optJSONObject(4);

        assertEquals(startSession1.optString("event_type"), DatadiveClient.START_SESSION_EVENT);
        assertEquals(
                startSession1.optJSONObject("api_properties").optString("special"),
                DatadiveClient.START_SESSION_EVENT
        );
        assertEquals(startSession1.optString("session_id"), String.valueOf(timestamp1));

        assertEquals(event1.optString("event_type"), "test1");
        assertEquals(event1.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event1.optString("timestamp"), String.valueOf(timestamp1));

        assertEquals(endSession.optString("event_type"), DatadiveClient.END_SESSION_EVENT);
        assertEquals(
                endSession.optJSONObject("api_properties").optString("special"),
                DatadiveClient.END_SESSION_EVENT
        );
        assertEquals(endSession.optString("session_id"), String.valueOf(timestamp1));

        assertEquals(startSession2.optString("event_type"), DatadiveClient.START_SESSION_EVENT);
        assertEquals(
                startSession2.optJSONObject("api_properties").optString("special"),
                DatadiveClient.START_SESSION_EVENT
        );
        assertEquals(startSession2.optString("session_id"), String.valueOf(timestamp2));

        assertEquals(event2.optString("event_type"), "test2");
        assertEquals(event2.optString("session_id"), String.valueOf(timestamp2));
        assertEquals(event2.optString("timestamp"), String.valueOf(timestamp2));
    }

    @Test
    public void testDefaultTriggerNewSessionWithTrackingSynchronous() {
        datadive.trackSessionEvents(true);

        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        long sessionTimeoutMillis = 5 * 1000; //5s
        datadive.setSessionTimeoutMillis(sessionTimeoutMillis);

        // log 1st event, initialize first session
        long timestamp1 = System.currentTimeMillis();
        datadive.logEvent("test1", null, null, null, null, null, timestamp1, false);
        looper.runToEndOfTasks();
        // trackSessions is true, start_session event is added
        assertEquals(getUnsentEventCount(), 2);

        // log 2nd event past timeout, verify new session started
        long timestamp2 = timestamp1 + sessionTimeoutMillis;
        datadive.logEvent("test2", null, null, null, null, null, timestamp2, false);
        looper.runToEndOfTasks();
        // trackSessions is true, end_session and start_session events are added
        assertEquals(getUnsentEventCount(), 5);

        // verify order of synchronous events
        JSONArray events = getUnsentEvents(5);
        JSONObject startSession1 = events.optJSONObject(0);
        JSONObject event1 = events.optJSONObject(1);
        JSONObject endSession = events.optJSONObject(2);
        JSONObject startSession2 = events.optJSONObject(3);
        JSONObject event2 = events.optJSONObject(4);

        assertEquals(startSession1.optString("event_type"), DatadiveClient.START_SESSION_EVENT);
        assertEquals(
                startSession1.optJSONObject("api_properties").optString("special"),
                DatadiveClient.START_SESSION_EVENT
        );
        assertEquals(startSession1.optString("session_id"), String.valueOf(timestamp1));

        assertEquals(event1.optString("event_type"), "test1");
        assertEquals(event1.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event1.optString("timestamp"), String.valueOf(timestamp1));

        assertEquals(endSession.optString("event_type"), DatadiveClient.END_SESSION_EVENT);
        assertEquals(
                endSession.optJSONObject("api_properties").optString("special"),
                DatadiveClient.END_SESSION_EVENT
        );
        assertEquals(endSession.optString("session_id"), String.valueOf(timestamp1));

        assertEquals(startSession2.optString("event_type"), DatadiveClient.START_SESSION_EVENT);
        assertEquals(
                startSession2.optJSONObject("api_properties").optString("special"),
                DatadiveClient.START_SESSION_EVENT
        );
        assertEquals(startSession2.optString("session_id"), String.valueOf(timestamp2));

        assertEquals(event2.optString("event_type"), "test2");
        assertEquals(event2.optString("session_id"), String.valueOf(timestamp2));
        assertEquals(event2.optString("timestamp"), String.valueOf(timestamp2));
    }

    @Test
    public void testDefaultExtendSessionWithTracking() {
        datadive.trackSessionEvents(true);

        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        long sessionTimeoutMillis = 5 * 1000; //5s
        datadive.setSessionTimeoutMillis(sessionTimeoutMillis);

        // log 3 events all just within session expiration window, verify all in same session
        long timestamp1 = System.currentTimeMillis();
        datadive.logEventAsync("test1", null, null, null, null, null, timestamp1, false);
        looper.runToEndOfTasks();
        // trackSessions is true, start_session event is added
        assertEquals(getUnsentEventCount(), 2);

        long timestamp2 = timestamp1 + sessionTimeoutMillis - 1;
        datadive.logEventAsync("test2", null, null, null, null, null, timestamp2, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 3);

        long timestamp3 = timestamp2 + sessionTimeoutMillis - 1;
        datadive.logEventAsync("test3", null, null, null, null, null, timestamp3, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 4);

        JSONArray events = getUnsentEvents(4);
        JSONObject startSession = events.optJSONObject(0);
        JSONObject event1 = events.optJSONObject(1);
        JSONObject event2 = events.optJSONObject(2);
        JSONObject event3 = events.optJSONObject(3);

        assertEquals(startSession.optString("event_type"), DatadiveClient.START_SESSION_EVENT);
        assertEquals(
                startSession.optJSONObject("api_properties").optString("special"),
                DatadiveClient.START_SESSION_EVENT
        );
        assertEquals(startSession.optString("session_id"), String.valueOf(timestamp1));

        assertEquals(event1.optString("event_type"), "test1");
        assertEquals(event1.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event1.optString("timestamp"), String.valueOf(timestamp1));

        assertEquals(event2.optString("event_type"), "test2");
        assertEquals(event2.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event2.optString("timestamp"), String.valueOf(timestamp2));

        assertEquals(event3.optString("event_type"), "test3");
        assertEquals(event3.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event3.optString("timestamp"), String.valueOf(timestamp3));
    }

    @Test
    public void testDefaultExtendSessionWithTrackingSynchronous() {
        datadive.trackSessionEvents(true);

        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        long sessionTimeoutMillis = 5 * 1000; //5s
        datadive.setSessionTimeoutMillis(sessionTimeoutMillis);

        // log 3 events all just within session expiration window, verify all in same session
        long timestamp1 = System.currentTimeMillis();
        datadive.logEvent("test1", null, null, null, null, null, timestamp1, false);
        looper.runToEndOfTasks();
        // trackSessions is true, start_session event is added
        assertEquals(getUnsentEventCount(), 2);

        long timestamp2 = timestamp1 + sessionTimeoutMillis - 1;
        datadive.logEvent("test2", null, null, null, null, null, timestamp2, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 3);

        long timestamp3 = timestamp2 + sessionTimeoutMillis - 1;
        datadive.logEventAsync("test3", null, null, null, null, null, timestamp3, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 4);

        // verify order of synchronous events
        JSONArray events = getUnsentEvents(4);
        JSONObject startSession = events.optJSONObject(0);
        JSONObject event1 = events.optJSONObject(1);
        JSONObject event2 = events.optJSONObject(2);
        JSONObject event3 = events.optJSONObject(3);

        assertEquals(startSession.optString("event_type"), DatadiveClient.START_SESSION_EVENT);
        assertEquals(
                startSession.optJSONObject("api_properties").optString("special"),
                DatadiveClient.START_SESSION_EVENT
        );
        assertEquals(startSession.optString("session_id"), String.valueOf(timestamp1));

        assertEquals(event1.optString("event_type"), "test1");
        assertEquals(event1.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event1.optString("timestamp"), String.valueOf(timestamp1));

        assertEquals(event2.optString("event_type"), "test2");
        assertEquals(event2.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event2.optString("timestamp"), String.valueOf(timestamp2));

        assertEquals(event3.optString("event_type"), "test3");
        assertEquals(event3.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(event3.optString("timestamp"), String.valueOf(timestamp3));
    }

    @Test
    public void testEnableAccurateTracking() {
        assertFalse(datadive.isUsingForegroundTracking());
        DatadiveCallbacks callBacks = new DatadiveCallbacks(datadive);
        assertTrue(datadive.isUsingForegroundTracking());
    }

    @Test
    public void testAccurateOnResumeStartSession() {
        long timestamp = System.currentTimeMillis();
        long [] timestamps = {timestamp};
        DatadiveCallbacks callBacks = new DatadiveCallbacksWithTime(datadive, timestamps);

        assertEquals(datadive.previousSessionId, -1);
        assertEquals(datadive.lastEventId, -1);
        assertEquals(datadive.lastEventTime, -1);
        assertFalse(datadive.isInForeground());

        callBacks.onActivityResumed(null);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();
        assertTrue(datadive.isInForeground());
        assertEquals(datadive.previousSessionId, timestamp);
        assertEquals(datadive.lastEventId, -1);
        assertEquals(datadive.lastEventTime, timestamp);
    }

    @Test
    public void testAccurateOnResumeStartSessionWithTracking() {
        datadive.trackSessionEvents(true);
        long timestamp = System.currentTimeMillis();
        long [] timestamps = {timestamp};
        DatadiveCallbacks callBacks = new DatadiveCallbacksWithTime(datadive, timestamps);

        assertEquals(datadive.previousSessionId, -1);
        assertEquals(datadive.lastEventId, -1);
        assertEquals(datadive.lastEventTime, -1);
        assertFalse(datadive.isInForeground());
        assertEquals(getUnsentEventCount(), 0);

        callBacks.onActivityResumed(null);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();
        assertTrue(datadive.isInForeground());
        assertEquals(datadive.previousSessionId, timestamp);
        assertEquals(datadive.lastEventId, 1);
        assertEquals(datadive.lastEventTime, timestamp);

        // verify that start session event sent
        assertEquals(getUnsentEventCount(), 1);
        JSONObject startSession = getLastUnsentEvent();
        assertEquals(startSession.optString("event_type"), DatadiveClient.START_SESSION_EVENT);
        assertEquals(
                startSession.optJSONObject("api_properties").optString("special"),
                DatadiveClient.START_SESSION_EVENT
        );
        assertEquals(
                startSession.optString("session_id"),
                String.valueOf(timestamp)
        );
        assertEquals(
            startSession.optString("timestamp"),
            String.valueOf(timestamp)
        );
    }

    @Test
    public void testAccurateOnPauseRefreshTimestamp() {
        long minTimeBetweenSessionsMillis = 5*1000; //5s
        long timestamp = System.currentTimeMillis();
        long [] timestamps = {timestamp, timestamp + minTimeBetweenSessionsMillis};
        DatadiveCallbacks callBacks = new DatadiveCallbacksWithTime(datadive, timestamps);

        assertEquals(datadive.previousSessionId, -1);
        assertEquals(datadive.lastEventId, -1);
        assertEquals(datadive.lastEventTime, -1);

        callBacks.onActivityResumed(null);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();
        assertEquals(datadive.previousSessionId, timestamps[0]);
        assertEquals(datadive.lastEventId, -1);
        assertEquals(datadive.lastEventTime, timestamps[0]);

        callBacks.onActivityPaused(null);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();
        assertEquals(datadive.previousSessionId, timestamps[0]);
        assertEquals(datadive.lastEventId, -1);
        assertEquals(datadive.lastEventTime, timestamps[1]);
        assertFalse(datadive.isInForeground());
    }

    @Test
    public void testAccurateOnPauseRefreshTimestampWithTracking() {
        datadive.trackSessionEvents(true);
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        long minTimeBetweenSessionsMillis = 5*1000; //5s
        datadive.setMinTimeBetweenSessionsMillis(minTimeBetweenSessionsMillis);
        long timestamp = System.currentTimeMillis();
        long [] timestamps = {timestamp, timestamp + minTimeBetweenSessionsMillis};
        DatadiveCallbacks callBacks = new DatadiveCallbacksWithTime(datadive, timestamps);

        assertEquals(datadive.previousSessionId, -1);
        assertEquals(datadive.lastEventId, -1);
        assertEquals(datadive.lastEventTime, -1);
        assertEquals(getUnsentEventCount(), 0);

        callBacks.onActivityResumed(null);
        looper.runToEndOfTasks();
        assertEquals(datadive.previousSessionId, timestamps[0]);
        assertEquals(datadive.lastEventId, 1);
        assertEquals(datadive.lastEventTime, timestamps[0]);
        assertEquals(getUnsentEventCount(), 1);

        // only refresh time, no session checking
        callBacks.onActivityPaused(null);
        looper.runToEndOfTasks();
        assertEquals(datadive.previousSessionId, timestamps[0]);
        assertEquals(datadive.lastEventId, 1);
        assertEquals(datadive.lastEventTime, timestamps[1]);
        assertEquals(getUnsentEventCount(), 1);
    }

    @Test
    public void testAccurateOnResumeTriggerNewSession() {
        long minTimeBetweenSessionsMillis = 5*1000; //5s
        datadive.setMinTimeBetweenSessionsMillis(minTimeBetweenSessionsMillis);
        long timestamp = System.currentTimeMillis();
        long [] timestamps = {
                timestamp,
                timestamp + 1,
                timestamp + 1 + minTimeBetweenSessionsMillis
        };
        DatadiveCallbacks callBacks = new DatadiveCallbacksWithTime(datadive, timestamps);

        assertEquals(datadive.previousSessionId, -1);
        assertEquals(datadive.lastEventId, -1);
        assertEquals(datadive.lastEventTime, -1);
        assertEquals(getUnsentEventCount(), 0);

        callBacks.onActivityResumed(null);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();
        assertEquals(datadive.previousSessionId, timestamps[0]);
        assertEquals(datadive.lastEventId, -1);
        assertEquals(datadive.lastEventTime, timestamps[0]);
        assertEquals(getUnsentEventCount(), 0);
        assertTrue(datadive.isInForeground());

        // only refresh time, no session checking
        callBacks.onActivityPaused(null);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();
        assertEquals(datadive.previousSessionId, timestamps[0]);
        assertEquals(datadive.lastEventId, -1);
        assertEquals(datadive.lastEventTime, timestamps[1]);
        assertEquals(getUnsentEventCount(), 0);
        assertFalse(datadive.isInForeground());

        // resume after min session expired window, verify new session started
        callBacks.onActivityResumed(null);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();
        assertEquals(datadive.previousSessionId, timestamps[2]);
        assertEquals(datadive.lastEventId, -1);
        assertEquals(datadive.lastEventTime, timestamps[2]);
        assertEquals(getUnsentEventCount(), 0);
        assertTrue(datadive.isInForeground());
    }

    @Test
    public void testAccurateOnResumeTriggerNewSessionWithTracking() {
        datadive.trackSessionEvents(true);
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        long minTimeBetweenSessionsMillis = 5*1000; //5s
        datadive.setMinTimeBetweenSessionsMillis(minTimeBetweenSessionsMillis);
        long timestamp = System.currentTimeMillis();
        long [] timestamps = {
                timestamp,
                timestamp + 1,
                timestamp + 1 + minTimeBetweenSessionsMillis
        };
        DatadiveCallbacks callBacks = new DatadiveCallbacksWithTime(datadive, timestamps);

        assertEquals(datadive.previousSessionId, -1);
        assertEquals(datadive.lastEventId, -1);
        assertEquals(datadive.lastEventTime, -1);
        assertEquals(getUnsentEventCount(), 0);

        callBacks.onActivityResumed(null);
        looper.runToEndOfTasks();
        assertEquals(datadive.previousSessionId, timestamps[0]);
        assertEquals(datadive.lastEventId, 1);
        assertEquals(datadive.lastEventTime, timestamps[0]);
        assertEquals(getUnsentEventCount(), 1);
        assertTrue(datadive.isInForeground());

        // only refresh time, no session checking
        callBacks.onActivityPaused(null);
        looper.runToEndOfTasks();
        assertEquals(datadive.previousSessionId, timestamps[0]);
        assertEquals(datadive.lastEventId, 1);
        assertEquals(datadive.lastEventTime, timestamps[1]);
        assertEquals(getUnsentEventCount(), 1);
        assertFalse(datadive.isInForeground());

        // resume after min session expired window, verify new session started
        callBacks.onActivityResumed(null);
        looper.runToEndOfTasks();
        assertEquals(datadive.previousSessionId, timestamps[2]);
        assertEquals(datadive.lastEventId, 3);
        assertEquals(datadive.lastEventTime, timestamps[2]);
        assertEquals(getUnsentEventCount(), 3);
        assertTrue(datadive.isInForeground());

        JSONArray events = getUnsentEvents(3);
        JSONObject startSession1 = events.optJSONObject(0);
        JSONObject endSession = events.optJSONObject(1);
        JSONObject startSession2 = events.optJSONObject(2);

        assertEquals(startSession1.optString("event_type"), DatadiveClient.START_SESSION_EVENT);
        assertEquals(
            startSession1.optJSONObject("api_properties").optString("special"),
            DatadiveClient.START_SESSION_EVENT
        );
        assertEquals(startSession1.optString("session_id"), String.valueOf(timestamps[0]));
        assertEquals(startSession1.optString("timestamp"), String.valueOf(timestamps[0]));

        assertEquals(endSession.optString("event_type"), DatadiveClient.END_SESSION_EVENT);
        assertEquals(
                endSession.optJSONObject("api_properties").optString("special"),
                DatadiveClient.END_SESSION_EVENT
        );
        assertEquals(endSession.optString("session_id"), String.valueOf(timestamps[0]));
        assertEquals(endSession.optString("timestamp"), String.valueOf(timestamps[1]));

        assertEquals(startSession2.optString("event_type"), DatadiveClient.START_SESSION_EVENT);
        assertEquals(
                startSession2.optJSONObject("api_properties").optString("special"),
                DatadiveClient.START_SESSION_EVENT
        );
        assertEquals(startSession2.optString("session_id"), String.valueOf(timestamps[2]));
        assertEquals(startSession2.optString("timestamp"), String.valueOf(timestamps[2]));
    }

    @Test
    public void testAccurateOnResumeExtendSession() {
        long minTimeBetweenSessionsMillis = 5*1000; //5s
        datadive.setMinTimeBetweenSessionsMillis(minTimeBetweenSessionsMillis);
        long timestamp = System.currentTimeMillis();
        long [] timestamps = {
                timestamp,
                timestamp + 1,
                timestamp + 1 + minTimeBetweenSessionsMillis - 1  // just inside session exp window
        };
        DatadiveCallbacks callBacks = new DatadiveCallbacksWithTime(datadive, timestamps);

        assertEquals(datadive.previousSessionId, -1);
        assertEquals(datadive.lastEventId, -1);
        assertEquals(datadive.lastEventTime, -1);

        callBacks.onActivityResumed(null);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();
        assertEquals(datadive.previousSessionId, timestamps[0]);
        assertEquals(datadive.lastEventId, -1);
        assertEquals(datadive.lastEventTime, timestamps[0]);

        callBacks.onActivityPaused(null);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();
        assertEquals(datadive.previousSessionId, timestamps[0]);
        assertEquals(datadive.lastEventId, -1);
        assertEquals(datadive.lastEventTime, timestamps[1]);
        assertFalse(datadive.isInForeground());

        callBacks.onActivityResumed(null);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();
        assertEquals(datadive.previousSessionId, timestamps[0]);
        assertEquals(datadive.lastEventId, -1);
        assertEquals(datadive.lastEventTime, timestamps[2]);
        assertTrue(datadive.isInForeground());
    }

    @Test
    public void testAccurateOnResumeExtendSessionWithTracking() {
        datadive.trackSessionEvents(true);
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        long minTimeBetweenSessionsMillis = 5*1000; //5s
        datadive.setMinTimeBetweenSessionsMillis(minTimeBetweenSessionsMillis);
        long timestamp = System.currentTimeMillis();
        long [] timestamps = {
                timestamp,
                timestamp + 1,
                timestamp + 1 + minTimeBetweenSessionsMillis - 1  // just inside session exp window
        };
        DatadiveCallbacks callBacks = new DatadiveCallbacksWithTime(datadive, timestamps);

        assertEquals(datadive.previousSessionId, -1);
        assertEquals(datadive.lastEventId, -1);
        assertEquals(datadive.lastEventTime, -1);
        assertEquals(getUnsentEventCount(), 0);

        callBacks.onActivityResumed(null);
        looper.runToEndOfTasks();
        assertEquals(datadive.previousSessionId, timestamps[0]);
        assertEquals(datadive.lastEventId, 1);
        assertEquals(datadive.lastEventTime, timestamps[0]);
        assertEquals(getUnsentEventCount(), 1);

        callBacks.onActivityPaused(null);
        looper.runToEndOfTasks();
        assertEquals(datadive.previousSessionId, timestamps[0]);
        assertEquals(datadive.lastEventId, 1);
        assertEquals(datadive.lastEventTime, timestamps[1]);
        assertFalse(datadive.isInForeground());
        assertEquals(getUnsentEventCount(), 1);

        callBacks.onActivityResumed(null);
        looper.runToEndOfTasks();
        assertEquals(datadive.previousSessionId, timestamps[0]);
        assertEquals(datadive.lastEventId, 1);
        assertEquals(datadive.lastEventTime, timestamps[2]);
        assertTrue(datadive.isInForeground());
        assertEquals(getUnsentEventCount(), 1);

        JSONObject event = getLastUnsentEvent();
        assertEquals(event.optString("event_type"), DatadiveClient.START_SESSION_EVENT);
        assertEquals(
            event.optJSONObject("api_properties").optString("special"),
            DatadiveClient.START_SESSION_EVENT
        );
        assertEquals(event.optString("session_id"), String.valueOf(timestamps[0]));
        assertEquals(event.optString("timestamp"), String.valueOf(timestamps[0]));
    }

    @Test
    public void testAccurateLogAsyncEvent() {
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        long minTimeBetweenSessionsMillis = 5*1000; //5s
        datadive.setMinTimeBetweenSessionsMillis(minTimeBetweenSessionsMillis);
        long timestamp = System.currentTimeMillis();
        long [] timestamps = {timestamp + minTimeBetweenSessionsMillis - 1};
        DatadiveCallbacks callBacks = new DatadiveCallbacksWithTime(datadive, timestamps);

        assertEquals(datadive.previousSessionId, -1);
        assertEquals(datadive.lastEventId, -1);
        assertEquals(datadive.lastEventTime, -1);
        assertEquals(getUnsentEventCount(), 0);
        assertFalse(datadive.isInForeground());

        // logging an event before onResume will force a session check
        datadive.logEventAsync("test", null, null, null, null, null, timestamp, false);
        looper.runToEndOfTasks();
        assertEquals(datadive.previousSessionId, timestamp);
        assertEquals(datadive.lastEventId, 1);
        assertEquals(datadive.lastEventTime, timestamp);
        assertEquals(getUnsentEventCount(), 1);

        callBacks.onActivityResumed(null);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();
        assertEquals(datadive.previousSessionId, timestamp);
        assertEquals(datadive.lastEventId, 1);
        assertEquals(datadive.lastEventTime, timestamps[0]);
        assertEquals(getUnsentEventCount(), 1);
        assertTrue(datadive.isInForeground());

        JSONObject event = getLastUnsentEvent();
        assertEquals(event.optString("event_type"), "test");
        assertEquals(event.optString("session_id"), String.valueOf(timestamp));
        assertEquals(event.optString("timestamp"), String.valueOf(timestamp));
    }

    @Test
    public void testAccurateLogAsyncEventWithTracking() {
        datadive.trackSessionEvents(true);
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        long minTimeBetweenSessionsMillis = 5*1000; //5s
        datadive.setMinTimeBetweenSessionsMillis(minTimeBetweenSessionsMillis);
        long timestamp = System.currentTimeMillis();
        long [] timestamps = {timestamp + minTimeBetweenSessionsMillis};
        DatadiveCallbacks callBacks = new DatadiveCallbacksWithTime(datadive, timestamps);

        assertEquals(datadive.previousSessionId, -1);
        assertEquals(datadive.lastEventId, -1);
        assertEquals(datadive.lastEventTime, -1);
        assertEquals(getUnsentEventCount(), 0);
        assertFalse(datadive.isInForeground());

        // logging an event before onResume will force a session check
        datadive.logEventAsync("test", null, null, null, null, null, timestamp, false);
        looper.runToEndOfTasks();
        assertEquals(datadive.previousSessionId, timestamp);
        assertEquals(datadive.lastEventId, 2);
        assertEquals(datadive.lastEventTime, timestamp);
        assertEquals(getUnsentEventCount(), 2);

        // onResume after session expires will start new session
        callBacks.onActivityResumed(null);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();
        assertEquals(datadive.previousSessionId, timestamps[0]);
        assertEquals(datadive.lastEventId, 4);
        assertEquals(datadive.lastEventTime, timestamps[0]);
        assertEquals(getUnsentEventCount(), 4);
        assertTrue(datadive.isInForeground());

        JSONArray events = getUnsentEvents(4);
        JSONObject startSession1 = events.optJSONObject(0);
        JSONObject event = events.optJSONObject(1);
        JSONObject endSession = events.optJSONObject(2);
        JSONObject startSession2 = events.optJSONObject(3);

        assertEquals(startSession1.optString("event_type"), DatadiveClient.START_SESSION_EVENT);
        assertEquals(
            startSession1.optJSONObject("api_properties").optString("special"),
            DatadiveClient.START_SESSION_EVENT
        );
        assertEquals(startSession1.optString("session_id"), String.valueOf(timestamp));
        assertEquals(startSession1.optString("timestamp"), String.valueOf(timestamp));

        assertEquals(event.optString("event_type"), "test");
        assertEquals(event.optString("session_id"), String.valueOf(timestamp));
        assertEquals(event.optString("timestamp"), String.valueOf(timestamp));

        assertEquals(endSession.optString("event_type"), DatadiveClient.END_SESSION_EVENT);
        assertEquals(
                endSession.optJSONObject("api_properties").optString("special"),
                DatadiveClient.END_SESSION_EVENT
        );
        assertEquals(endSession.optString("session_id"), String.valueOf(timestamp));
        assertEquals(endSession.optString("timestamp"), String.valueOf(timestamp));

        assertEquals(startSession2.optString("event_type"), DatadiveClient.START_SESSION_EVENT);
        assertEquals(
                startSession2.optJSONObject("api_properties").optString("special"),
                DatadiveClient.START_SESSION_EVENT
        );
        assertEquals(startSession2.optString("session_id"), String.valueOf(timestamps[0]));
        assertEquals(startSession2.optString("timestamp"), String.valueOf(timestamps[0]));
    }


    @Test
    public void testLogOutOfSessionEvent() {
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        long sessionTimeoutMillis = 5*1000; //1s
        datadive.setSessionTimeoutMillis(sessionTimeoutMillis);

        long timestamp1 = System.currentTimeMillis();
        datadive.logEventAsync("test1", null, null, null, null, null, timestamp1, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 1);

        // log out of session event just within session expiration window
        long timestamp2 = timestamp1 + sessionTimeoutMillis - 1;
        datadive.logEventAsync("outOfSession", null, null, null, null, null, timestamp2, true);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 2);

        // out of session events do not extend session, 2nd event will start new session
        long timestamp3 = timestamp1 + sessionTimeoutMillis;
        datadive.logEventAsync("test2", null, null, null, null, null, timestamp3, false);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 3);

        JSONArray events = getUnsentEvents(3);
        JSONObject event1 = events.optJSONObject(0);
        JSONObject outOfSessionEvent = events.optJSONObject(1);
        JSONObject event2 = events.optJSONObject(2);

        assertEquals(event1.optString("event_type"), "test1");
        assertEquals(event1.optString("session_id"), String.valueOf(timestamp1));
        assertEquals(outOfSessionEvent.optString("event_type"), "outOfSession");
        assertEquals(outOfSessionEvent.optString("session_id"), String.valueOf(-1));
        assertEquals(event2.optString("event_type"), "test2");
        assertEquals(event2.optString("session_id"), String.valueOf(timestamp3));
    }

    @Test
    public void testOnPauseFlushEvents() throws JSONException {
        long timestamp = System.currentTimeMillis();
        long [] timestamps = {
            timestamp, timestamp + 1, timestamp + 2,
            timestamp + 3, timestamp + 4, timestamp + 5,
        };
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        DatadiveCallbacks callBacks = new DatadiveCallbacksWithTime(datadive, timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        // log an event, should not be uploaded
        datadive.logEventAsync("testEvent", null, null, null, null, null, timestamps[0], false);
        looper.runOneTask();
        looper.runOneTask();
        assertEquals(getUnsentEventCount(), 1);

        // force client into background and verify flushing of events
        callBacks.onActivityPaused(null);
        looper.runOneTask();  // run the update server
        RecordedRequest request = runRequest(datadive);
        JSONArray events = getEventsFromRequest(request);
        assertEquals(events.length(), 1);
        assertEquals(events.getJSONObject(0).optString("event_type"), "testEvent");

        // verify that events have been cleared from client
        looper.runOneTask();
        assertEquals(getUnsentEventCount(), 0);
    }

    @Test
    public void testOnPauseFlushEventsDisabled() throws JSONException {
        long timestamp = System.currentTimeMillis();
        long [] timestamps = {
            timestamp, timestamp + 1, timestamp + 2,
            timestamp + 3, timestamp + 4, timestamp + 5,
        };
        datadive.setFlushEventsOnClose(false);
        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        DatadiveCallbacks callBacks = new DatadiveCallbacksWithTime(datadive, timestamps);
        Robolectric.getForegroundThreadScheduler().advanceTo(1);

        // log an event, should not be uploaded
        datadive.logEventAsync("testEvent", null, null, null, null, null, timestamps[0], false);
        looper.runOneTask();
        assertEquals(getUnsentEventCount(), 1);

        // force client into background and verify no flushing of events
        callBacks.onActivityPaused(null);
        looper.runOneTask();  // run the update server
        RecordedRequest request = runRequest(datadive);

        // flushing disabled, so no request should be sent
        assertNull(request);
        assertEquals(getUnsentEventCount(), 1);
    }

    @Test
    public void testIdentifyTriggerNewSession() throws JSONException {
        datadive.trackSessionEvents(true);

        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        long sessionTimeoutMillis = 5 * 1000; //5s
        datadive.setSessionTimeoutMillis(sessionTimeoutMillis);

        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);

        // log 1st identify, initialize first session
        Identify identify = new Identify().set("key", "value");
        datadive.identify(identify);
        looper.runToEndOfTasks();
        // trackSessions is true, start_session event is added
        assertEquals(getUnsentEventCount(), 1);
        assertEquals(getUnsentIdentifyCount(), 1);

        JSONArray events = getUnsentEvents(1);
        assertEquals(
            events.getJSONObject(0).optString("event_type"), DatadiveClient.START_SESSION_EVENT
        );
        JSONArray identifies = getUnsentIdentifys(1);
        JSONObject expected = new JSONObject().put("$set", new JSONObject().put("key", "value"));
        assertTrue(Utils.compareJSONObjects(
            identifies.getJSONObject(0).getJSONObject("user_properties"), expected
        ));
    }

    @Test
    public void testOutOfSessionIdentifyDoesNotTriggerNewSession() throws JSONException {
        datadive.trackSessionEvents(true);

        ShadowLooper looper = Shadows.shadowOf(datadive.logThread.getLooper());
        long sessionTimeoutMillis = 5 * 1000; //5s
        datadive.setSessionTimeoutMillis(sessionTimeoutMillis);

        assertEquals(getUnsentEventCount(), 0);
        assertEquals(getUnsentIdentifyCount(), 0);

        // log 1st identify, initialize first session
        Identify identify = new Identify().set("key", "value");
        datadive.identify(identify, true);
        looper.runToEndOfTasks();
        assertEquals(getUnsentEventCount(), 0);  // out of session, start session is not added
        assertEquals(getUnsentIdentifyCount(), 1);

        JSONArray identifies = getUnsentIdentifys(1);
        JSONObject expected = new JSONObject().put("$set", new JSONObject().put("key", "value"));
        assertTrue(Utils.compareJSONObjects(
            identifies.getJSONObject(0).getJSONObject("user_properties"), expected
        ));
    }

    @Test
    public void testSetUserIdAndStartNewSessionWithTracking() {
        datadive.trackSessionEvents(true);

        long timestamp = System.currentTimeMillis();
        datadive.logEventAsync("test", null, null, null, null, null, timestamp, false);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();

        // trackSessions is true, start_session event is added
        assertEquals(getUnsentEventCount(), 2);

        // set user id and validate session ended and new session started
        datadive.setUserId("test_new_user", true);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();

        // total of 4 events, start session, test event, end session, start session
        assertEquals(getUnsentEventCount(), 4);
        JSONArray events = getUnsentEvents(4);

        // verify pre setUserId events
        JSONObject session_event = events.optJSONObject(0);
        JSONObject test_event = events.optJSONObject(1);
        assertEquals(session_event.optString("event_type"), DatadiveClient.START_SESSION_EVENT);
        assertEquals(session_event.optString("user_id"), "null");
        assertEquals(
            session_event.optJSONObject("api_properties").optString("special"),
            DatadiveClient.START_SESSION_EVENT
        );
        assertEquals(session_event.optString("session_id"), String.valueOf(timestamp));

        assertEquals(test_event.optString("event_type"), "test");
        assertEquals(test_event.optString("session_id"), String.valueOf(timestamp));
        assertEquals(test_event.optString("user_id"), "null");

        // verify post setUserId events
        session_event = events.optJSONObject(2);
        assertEquals(session_event.optString("event_type"), DatadiveClient.END_SESSION_EVENT);
        assertEquals(session_event.optString("user_id"), "null");
        assertEquals(
            session_event.optJSONObject("api_properties").optString("special"),
            DatadiveClient.END_SESSION_EVENT
        );
        assertEquals(session_event.optString("session_id"), String.valueOf(timestamp));

        session_event = events.optJSONObject(3);
        assertEquals(session_event.optString("event_type"), DatadiveClient.START_SESSION_EVENT);
        assertEquals(session_event.optString("user_id"), "test_new_user");
        assertEquals(
            session_event.optJSONObject("api_properties").optString("special"),
            DatadiveClient.START_SESSION_EVENT
        );

        // the new event should have a newer session id
        assertTrue(session_event.optLong("session_id") > timestamp);
    }

    @Test
    public void testSetUserIdAndDoNotStartNewSessionWithTracking() {
        datadive.trackSessionEvents(true);

        long timestamp = System.currentTimeMillis();
        datadive.logEventAsync("test", null, null, null, null, null, timestamp, false);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();

        // trackSessions is true, start_session event is added
        assertEquals(getUnsentEventCount(), 2);

        // set user id and validate session ended and new session started
        datadive.setUserId("test_new_user", false);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();

        // still only 2 events, start session, test event
        assertEquals(getUnsentEventCount(), 2);
        JSONArray events = getUnsentEvents(2);

        // verify pre setUserId events
        JSONObject session_event = events.optJSONObject(0);
        JSONObject test_event = events.optJSONObject(1);
        assertEquals(session_event.optString("event_type"), DatadiveClient.START_SESSION_EVENT);
        assertEquals(session_event.optString("user_id"), "null");
        assertEquals(
            session_event.optJSONObject("api_properties").optString("special"),
            DatadiveClient.START_SESSION_EVENT
        );
        assertEquals(session_event.optString("session_id"), String.valueOf(timestamp));

        assertEquals(test_event.optString("event_type"), "test");
        assertEquals(test_event.optString("session_id"), String.valueOf(timestamp));
        assertEquals(test_event.optString("user_id"), "null");

        // verify same session id
        assertEquals(datadive.sessionId, timestamp);
    }

    @Test
    public void testSetUserIdAndStartNewSessionWithoutTracking() {
        datadive.trackSessionEvents(false);

        long timestamp = System.currentTimeMillis();
        datadive.logEventAsync("test", null, null, null, null, null, timestamp, false);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();

        // trackSessions is false, there should only be 1 event
        assertEquals(getUnsentEventCount(), 1);

        // set user id and validate session ended and new session started
        datadive.setUserId("test_new_user", true);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();

        // still only 1 event1, test event
        assertEquals(getUnsentEventCount(), 1);
        JSONArray events = getUnsentEvents(1);

        // verify pre setUserId events
        JSONObject session_event = events.optJSONObject(0);
        assertEquals(session_event.optString("event_type"), "test");
        assertEquals(session_event.optString("user_id"), "null");
        assertEquals(session_event.optString("session_id"), String.valueOf(timestamp));

        // log an event with new user id and session
        datadive.logEventAsync("test", null, null, null, null, null, timestamp, false);
        Shadows.shadowOf(datadive.logThread.getLooper()).runToEndOfTasks();

        // verify post set user id
        assertEquals(getUnsentEventCount(), 2);
        JSONObject test_event = getLastEvent();
        assertEquals(test_event.optString("event_type"), "test");
        assertEquals(test_event.optString("user_id"), "test_new_user");
        assertEquals(test_event.optLong("session_id"), datadive.sessionId);

        // there should be a new session id at least
        assertTrue(datadive.sessionId > timestamp);
        assertTrue(test_event.optLong("session_id") > timestamp);
    }
}
