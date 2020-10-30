package ai.datadive.unity.plugins;

import android.app.Application;
import android.content.Context;

import ai.datadive.api.Datadive;
import ai.datadive.api.Identify;
import ai.datadive.api.Revenue;
import ai.datadive.api.TrackingOptions;
import ai.datadive.api.Utils;

import org.json.JSONException;
import org.json.JSONObject;

public class DatadivePlugin {

    public static JSONObject ToJSONObject(String jsonString) {
        JSONObject properties = null;
        try {
            properties = new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return properties;
    }

    public static void init(String instanceName, Context context, String apiKey) {
        Datadive.getInstance(instanceName).initialize(context, apiKey);
    }

    public static void init(String instanceName, Context context, String apiKey, String userId) {
        Datadive.getInstance(instanceName).initialize(context, apiKey, userId);
    }

    public static void setTrackingOptions(String instanceName, String trackingOptionsJson) {
        JSONObject trackingOptionsDict = ToJSONObject(trackingOptionsJson);
        TrackingOptions trackingOptions = new TrackingOptions();

        if (trackingOptionsDict.optBoolean("disableADID", false)) {
            trackingOptions.disableAdid();
        }
        if (trackingOptionsDict.optBoolean("disableCarrier", false)) {
            trackingOptions.disableCarrier();
        }
        if (trackingOptionsDict.optBoolean("disableCity", false)) {
            trackingOptions.disableCity();
        }
        if (trackingOptionsDict.optBoolean("disableCountry", false)) {
            trackingOptions.disableCountry();
        }
        if (trackingOptionsDict.optBoolean("disableDeviceBrand", false)) {
            trackingOptions.disableDeviceBrand();
        }
        if (trackingOptionsDict.optBoolean("disableDeviceManufacturer", false)) {
            trackingOptions.disableDeviceManufacturer();
        }
        if (trackingOptionsDict.optBoolean("disableDeviceModel", false)) {
            trackingOptions.disableDeviceModel();
        }
        if (trackingOptionsDict.optBoolean("disableDMA", false)) {
            trackingOptions.disableDma();
        }
        if (trackingOptionsDict.optBoolean("disableIPAddress", false)) {
            trackingOptions.disableIpAddress();
        }
        if (trackingOptionsDict.optBoolean("disableLanguage", false)) {
            trackingOptions.disableLanguage();
        }
        if (trackingOptionsDict.optBoolean("disableLatLng", false)) {
            trackingOptions.disableLatLng();
        }
        if (trackingOptionsDict.optBoolean("disableOSName", false)) {
            trackingOptions.disableOsName();
        }
        if (trackingOptionsDict.optBoolean("disableOSVersion", false)) {
            trackingOptions.disableOsVersion();
        }
        if (trackingOptionsDict.optBoolean("disableApiLevel", false)) {
            trackingOptions.disableApiLevel();
        }
        if (trackingOptionsDict.optBoolean("disablePlatform", false)) {
            trackingOptions.disablePlatform();
        }
        if (trackingOptionsDict.optBoolean("disableRegion", false)) {
            trackingOptions.disableRegion();
        }
        if (trackingOptionsDict.optBoolean("disableVersionName", false)) {
            trackingOptions.disableVersionName();
        }
        Datadive.getInstance(instanceName).setTrackingOptions(trackingOptions);
    }

    public static void enableForegroundTracking(String instanceName, Application app) {
        Datadive.getInstance(instanceName).enableForegroundTracking(app);
    }

    public static void enableCoppaControl(String instanceName) {
        Datadive.getInstance(instanceName).enableCoppaControl();
    }

    public static void disableCoppaControl(String instanceName) {
        Datadive.getInstance(instanceName).disableCoppaControl();
    }

    public static void setLibraryName(String instanceName, String libraryName) {
        Datadive.getInstance(instanceName).setLibraryName(libraryName);
    }

    public static void setLibraryVersion(String instanceName, String libraryVersion) {
        Datadive.getInstance(instanceName).setLibraryVersion(libraryVersion);
    }

    public static void setServerUrl(String instanceName, String serverUrl) {
        Datadive.getInstance(instanceName).setServerUrl(serverUrl);
    }

    @Deprecated
    public static void startSession() { return; }

    @Deprecated
    public static void endSession() { return; }

    public static void logEvent(String instanceName, String event) {
        Datadive.getInstance(instanceName).logEvent(event);
    }

    public static void logEvent(String instanceName, String event, String jsonProperties) {
        Datadive.getInstance(instanceName).logEvent(event, ToJSONObject(jsonProperties));
    }

    public static void logEvent(String instanceName, String event, String jsonProperties, boolean outOfSession) {
        Datadive.getInstance(instanceName).logEvent(event, ToJSONObject(jsonProperties), outOfSession);
    }

    public static void uploadEvents(String instanceName) {
        Datadive.getInstance(instanceName).uploadEvents();
    }

    public static void useAdvertisingIdForDeviceId(String instanceName) {
        Datadive.getInstance(instanceName).useAdvertisingIdForDeviceId();
    }

    public static void setOffline(String instanceName, boolean offline) {
        Datadive.getInstance(instanceName).setOffline(offline);
    }

    public static void setUserId(String instanceName, String userId) {
        Datadive.getInstance(instanceName).setUserId(userId);
    }

    public static void setOptOut(String instanceName, boolean enabled) {
        Datadive.getInstance(instanceName).setOptOut(enabled);
    }

    public static void setMinTimeBetweenSessionsMillis(String instanceName, long minTimeBetweenSessionsMillis) {
        Datadive.getInstance(instanceName).setMinTimeBetweenSessionsMillis(minTimeBetweenSessionsMillis);
    }

    public static void setUserProperties(String instanceName, String jsonProperties) {
        Datadive.getInstance(instanceName).setUserProperties(ToJSONObject(jsonProperties));
    }

    public static void logRevenue(String instanceName, double amount) {
        Datadive.getInstance(instanceName).logRevenue(amount);
    }

    public static void logRevenue(String instanceName, String productId, int quantity, double price) {
        Datadive.getInstance(instanceName).logRevenue(productId, quantity, price);
    }

    public static void logRevenue(String instanceName, String productId, int quantity, double price, String receipt, String receiptSignature) {
        Datadive.getInstance(instanceName).logRevenue(productId, quantity, price, receipt, receiptSignature);
    }

    public static void logRevenue(String instanceName, String productId, int quantity, double price, String receipt, String receiptSignature, String revenueType, String jsonProperties) {
        Revenue revenue = new Revenue().setQuantity(quantity).setPrice(price);
        if (!Utils.isEmptyString(productId)) {
            revenue.setProductId(productId);
        }
        if (!Utils.isEmptyString(receipt) && !Utils.isEmptyString(receiptSignature)) {
            revenue.setReceipt(receipt, receiptSignature);
        }
        if (!Utils.isEmptyString(revenueType)) {
            revenue.setRevenueType(revenueType);
        }
        if (!Utils.isEmptyString(jsonProperties)) {
            revenue.setEventProperties(ToJSONObject(jsonProperties));
        }
        Datadive.getInstance(instanceName).logRevenueV2(revenue);
    }

    public static String getDeviceId(String instanceName) {
        return Datadive.getInstance(instanceName).getDeviceId();
    }

    public static void setDeviceId(String instanceName, String deviceId) {
        Datadive.getInstance(instanceName).setDeviceId(deviceId);
    }

    public static void regenerateDeviceId(String instanceName) { Datadive.getInstance(instanceName).regenerateDeviceId(); }

    public static void trackSessionEvents(String instanceName, boolean enabled) {
        Datadive.getInstance(instanceName).trackSessionEvents(enabled);
    }

    public static long getSessionId(String instanceName) { return Datadive.getInstance(instanceName).getSessionId(); }

    // User Property Operations

    // clear user properties
    public static void clearUserProperties(String instanceName) {
        Datadive.getInstance(instanceName).clearUserProperties();
    }

    // unset user property
    public static void unsetUserProperty(String instanceName, String property) {
        Datadive.getInstance(instanceName).identify(new Identify().unset(property));
    }

    // setOnce user property
    public static void setOnceUserProperty(String instanceName, String property, boolean value) {
        Datadive.getInstance(instanceName).identify(new Identify().setOnce(property, value));
    }

    public static void setOnceUserProperty(String instanceName, String property, double value) {
        Datadive.getInstance(instanceName).identify(new Identify().setOnce(property, value));
    }

    public static void setOnceUserProperty(String instanceName, String property, float value) {
        Datadive.getInstance(instanceName).identify(new Identify().setOnce(property, value));
    }

    public static void setOnceUserProperty(String instanceName, String property, int value) {
        Datadive.getInstance(instanceName).identify(new Identify().setOnce(property, value));
    }

    public static void setOnceUserProperty(String instanceName, String property, long value) {
        Datadive.getInstance(instanceName).identify(new Identify().setOnce(property, value));
    }

    public static void setOnceUserProperty(String instanceName, String property, String value) {
        Datadive.getInstance(instanceName).identify(new Identify().setOnce(property, value));
    }

    public static void setOnceUserPropertyDict(String instanceName, String property, String values) {
        Datadive.getInstance(instanceName).identify(new Identify().setOnce(property, ToJSONObject(values)));
    }

    public static void setOnceUserPropertyList(String instanceName, String property, String values) {
        JSONObject properties = ToJSONObject(values);
        if (properties == null) {
            return;
        }
        Datadive.getInstance(instanceName).identify(new Identify().setOnce(
            property, properties.optJSONArray("list")
        ));
    }

    public static void setOnceUserProperty(String instanceName, String property, boolean[] values) {
        Datadive.getInstance(instanceName).identify(new Identify().setOnce(property, values));
    }

    public static void setOnceUserProperty(String instanceName, String property, double[] values) {
        Datadive.getInstance(instanceName).identify(new Identify().setOnce(property, values));
    }

    public static void setOnceUserProperty(String instanceName, String property, float[] values) {
        Datadive.getInstance(instanceName).identify(new Identify().setOnce(property, values));
    }

    public static void setOnceUserProperty(String instanceName, String property, int[] values) {
        Datadive.getInstance(instanceName).identify(new Identify().setOnce(property, values));
    }

    public static void setOnceUserProperty(String instanceName, String property, long[] values) {
        Datadive.getInstance(instanceName).identify(new Identify().setOnce(property, values));
    }

    public static void setOnceUserProperty(String instanceName, String property, String[] values) {
        Datadive.getInstance(instanceName).identify(new Identify().setOnce(property, values));
    }

    // set user property
    public static void setUserProperty(String instanceName, String property, boolean value) {
        Datadive.getInstance(instanceName).identify(new Identify().set(property, value));
    }

    public static void setUserProperty(String instanceName, String property, double value) {
        Datadive.getInstance(instanceName).identify(new Identify().set(property, value));
    }

    public static void setUserProperty(String instanceName, String property, float value) {
        Datadive.getInstance(instanceName).identify(new Identify().set(property, value));
    }

    public static void setUserProperty(String instanceName, String property, int value) {
        Datadive.getInstance(instanceName).identify(new Identify().set(property, value));
    }

    public static void setUserProperty(String instanceName, String property, long value) {
        Datadive.getInstance(instanceName).identify(new Identify().set(property, value));
    }

    public static void setUserProperty(String instanceName, String property, String value) {
        Datadive.getInstance(instanceName).identify(new Identify().set(property, value));
    }

    public static void setUserPropertyDict(String instanceName, String property, String values) {
        Datadive.getInstance(instanceName).identify(new Identify().set(property, ToJSONObject(values)));
    }

    public static void setUserPropertyList(String instanceName, String property, String values) {
        JSONObject properties = ToJSONObject(values);
        if (properties == null) {
            return;
        }
        Datadive.getInstance(instanceName).identify(new Identify().set(
                property, properties.optJSONArray("list")
        ));
    }

    public static void setUserProperty(String instanceName, String property, boolean[] values) {
        Datadive.getInstance(instanceName).identify(new Identify().set(property, values));
    }

    public static void setUserProperty(String instanceName, String property, double[] values) {
        Datadive.getInstance(instanceName).identify(new Identify().set(property, values));
    }

    public static void setUserProperty(String instanceName, String property, float[] values) {
        Datadive.getInstance(instanceName).identify(new Identify().set(property, values));
    }

    public static void setUserProperty(String instanceName, String property, int[] values) {
        Datadive.getInstance(instanceName).identify(new Identify().set(property, values));
    }

    public static void setUserProperty(String instanceName, String property, long[] values) {
        Datadive.getInstance(instanceName).identify(new Identify().set(property, values));
    }

    public static void setUserProperty(String instanceName, String property, String[] values) {
        Datadive.getInstance(instanceName).identify(new Identify().set(property, values));
    }

    // add
    public static void addUserProperty(String instanceName, String property, double value) {
        Datadive.getInstance(instanceName).identify(new Identify().add(property, value));
    }

    public static void addUserProperty(String instanceName, String property, float value) {
        Datadive.getInstance(instanceName).identify(new Identify().add(property, value));
    }

    public static void addUserProperty(String instanceName, String property, int value) {
        Datadive.getInstance(instanceName).identify(new Identify().add(property, value));
    }

    public static void addUserProperty(String instanceName, String property, long value) {
        Datadive.getInstance(instanceName).identify(new Identify().add(property, value));
    }

    public static void addUserProperty(String instanceName, String property, String value) {
        Datadive.getInstance(instanceName).identify(new Identify().add(property, value));
    }

    public static void addUserPropertyDict(String instanceName, String property, String values) {
        Datadive.getInstance(instanceName).identify(new Identify().add(property, ToJSONObject(values)));
    }

    // append user property
    public static void appendUserProperty(String instanceName, String property, boolean value) {
        Datadive.getInstance(instanceName).identify(new Identify().append(property, value));
    }

    public static void appendUserProperty(String instanceName, String property, double value) {
        Datadive.getInstance(instanceName).identify(new Identify().append(property, value));
    }

    public static void appendUserProperty(String instanceName, String property, float value) {
        Datadive.getInstance(instanceName).identify(new Identify().append(property, value));
    }

    public static void appendUserProperty(String instanceName, String property, int value) {
        Datadive.getInstance(instanceName).identify(new Identify().append(property, value));
    }

    public static void appendUserProperty(String instanceName, String property, long value) {
        Datadive.getInstance(instanceName).identify(new Identify().append(property, value));
    }

    public static void appendUserProperty(String instanceName, String property, String value) {
        Datadive.getInstance(instanceName).identify(new Identify().append(property, value));
    }

    public static void appendUserPropertyDict(String instanceName, String property, String values) {
        Datadive.getInstance(instanceName).identify(new Identify().append(property, ToJSONObject(values)));
    }

    public static void appendUserPropertyList(String instanceName, String property, String values) {
        JSONObject properties = ToJSONObject(values);
        if (properties == null) {
            return;
        }
        Datadive.getInstance(instanceName).identify(new Identify().append(
                property, properties.optJSONArray("list")
        ));
    }

    public static void appendUserProperty(String instanceName, String property, boolean[] values) {
        Datadive.getInstance(instanceName).identify(new Identify().append(property, values));
    }

    public static void appendUserProperty(String instanceName, String property, double[] values) {
        Datadive.getInstance(instanceName).identify(new Identify().append(property, values));
    }

    public static void appendUserProperty(String instanceName, String property, float[] values) {
        Datadive.getInstance(instanceName).identify(new Identify().append(property, values));
    }

    public static void appendUserProperty(String instanceName, String property, int[] values) {
        Datadive.getInstance(instanceName).identify(new Identify().append(property, values));
    }

    public static void appendUserProperty(String instanceName, String property, long[] values) {
        Datadive.getInstance(instanceName).identify(new Identify().append(property, values));
    }

    public static void appendUserProperty(String instanceName, String property, String[] values) {
        Datadive.getInstance(instanceName).identify(new Identify().append(property, values));
    }
}
