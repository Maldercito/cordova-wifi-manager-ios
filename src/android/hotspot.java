package cordova.wifi.ios;

import org.apache.cordova.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.lang.Runnable;

import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.ScanResult;
import android.net.ConnectivityManager;

import android.content.Context;

import android.content.pm.PackageManager;
import android.os.Build.VERSION;

import android.util.Log;

public class hotspot extends CordovaPlugin {

    private WifiManager wifiManager;
    private CallbackContext callbackContext;

    private String SSID;
    private String PASS;

    private static final String TAG = "WifiManger";
    private static final int API_VERSION = VERSION.SDK_INT;

    private static boolean NETWORK_MANUALLY_ADDED;
    private static final int CONNECTION_CODE = 0;
    private static final int SSID_CODE = 1;
    private static final String ACCESS_COARSE_LOCATION = android.Manifest.permission.ACCESS_COARSE_LOCATION;

    private static final int WIFI_STATE_ENABLED = 3;

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.wifiManager = (WifiManager) cordova.getActivity().getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        boolean wifiIsEnabled = verifyWifiEnabled();

        if (!wifiIsEnabled) {
            callbackContext.error("WIFI_NOT_ENABLED");
            return false;
        }

        if (action.equals("connect")) {
            SSID = data.getString(0);
            PASS = data.getString(1);

            connect(callbackContext, SSID, PASS);
        } else if (action.equals("disconnect")) {
            String ssid = data.getString(0);
            remove(callbackContext, ssid);
        } else if (action.equals("getSSID")) {
            getSSID(callbackContext);
        } else {
            callbackContext.error("ACTION_NO_SUPPORTED");
            return false;
        }
        return true;
    }

    private boolean verifyWifiEnabled() {
        Log.e(TAG, "VERIFYWIFI: verifyWifiEnabled entered.");

        if (!wifiManager.isWifiEnabled()) {
            Log.e(TAG, "VERIFYWIFI: Enabling wi-fi...");
            if (wifiManager.setWifiEnabled(true)) {
                Log.e(TAG, "VERIFYWIFI: Wi-fi enabled");
            } else {
                Log.e(TAG, "VERIFYWIFI: Wi-fi could not be enabled!");
                return false;
            }
        } else {
            Log.e(TAG, "VERIFYWIFI: Wi-fi enabled");
            return true;
        }

        if (wifiManager.getWifiState() != WIFI_STATE_ENABLED) {
            return verifyWifiEnabled();
        }

        return true;
    }

    protected WifiConfiguration setNetwork(String ssid, String key) {
        // WifiConfiguration wifiConfiguration = new WifiConfiguration();

        // wifiConfiguration.SSID = ssid;

        // wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        // wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

        // wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        // wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);

        // wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        // wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);

        // wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        // wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        // wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

        // wifiConfiguration.preSharedKey = "\"".concat(key).concat("\"");

        // return wifiConfiguration;

        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = ssid;
        // wifiConfiguration.preSharedKey = key;
        wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wifiConfiguration.preSharedKey = "\"".concat(key).concat("\"");

        return wifiConfiguration;
    }

    private int ssidToNetworkId(String ssid) {
        Log.e(TAG, "SSIDTONETWORKID: passed SSID: " + ssid);

        try {
            int maybeNetId = Integer.parseInt(ssid);
            Log.e(TAG, " SSIDTONETWORKID: passed SSID is integer, probably a Network ID: " + ssid);
            return maybeNetId;
        } catch (NumberFormatException e) {
            List<WifiConfiguration> currentNetworks = wifiManager.getConfiguredNetworks();
            int networkId = -1;

            // For each network in the list, compare the SSID with the given one
            for (WifiConfiguration test : currentNetworks) {
                if (test.SSID != null && test.SSID.equals("\"".concat(ssid).concat("\""))) {
                    if (test.toString().contains("cname=android.uid.system")) {
                        Log.e(TAG, "SSIDTONETWORKID: ssid found but configured by android.uid.system");
                        NETWORK_MANUALLY_ADDED = true;
                    } else {
                        Log.e(TAG, "SSIDTONETWORKID: ssid found:" + test.SSID);
                        NETWORK_MANUALLY_ADDED = false;
                    }
                    return test.networkId;
                } else {
                    NETWORK_MANUALLY_ADDED = false;
                }
            }
            Log.e(TAG, "SSIDTONETWORKID: networkId: " + networkId);
            return networkId;
        }
    }

    private int add(String ssid, String password) {
        Log.e(TAG, "ADD: add entered.");

        WifiConfiguration wifi = setNetwork(ssid, password);
        wifi.networkId = ssidToNetworkId(ssid);
        Log.e(TAG, "ADD: NETWORKID_" + wifi.networkId);

        int netId = wifi.networkId;

        if (netId > -1) {
            netId = wifiManager.updateNetwork(wifi);
            Log.e(TAG, "ADD: update network. NETWORKID: " + netId);
        }
        if (netId == -1) {
            netId = wifiManager.addNetwork(wifi);
            Log.e(TAG, "ADD: add new network. NETWORKID: " + netId);
        }

        if (API_VERSION < 26) {
            wifiManager.saveConfiguration();
        }

        return netId;
    }

    private boolean waitForConnection(CallbackContext callbackContext, int networkIdToConnect, String ssid, String pass)
            throws JSONException {
        final int TIMES_TO_RETRY = 60;
        for (int i = 0; i < TIMES_TO_RETRY; i++) {
            WifiInfo info = wifiManager.getConnectionInfo();
            NetworkInfo.DetailedState connectionState = info.getDetailedStateOf(info.getSupplicantState());

            boolean isConnected =
                    // need to ensure we're on correct network because sometimes this code is
                    // reached before the initial network has disconnected
                    info.getNetworkId() == networkIdToConnect
                            && (connectionState == NetworkInfo.DetailedState.CONNECTED ||
                            // Android seems to sometimes get stuck in OBTAINING_IPADDR after it has
                            // received one
                                    (connectionState == NetworkInfo.DetailedState.OBTAINING_IPADDR
                                            && info.getIpAddress() != 0));

            if (isConnected) {
                Log.e(TAG, "WAIT_TO_CONNECT: connection completed");
                callbackContext.success(pass);
                return true;
            }
            Log.e(TAG,
                    "WAIT_TO_CONNECT: Got " + connectionState.name() + " on " + (i + 1) + " out of " + TIMES_TO_RETRY);
            final int ONE_SECOND = 1000;

            try {
                Thread.sleep(ONE_SECOND);
            } catch (InterruptedException e) {
                Log.e(TAG, "WAIT_TO_CONNECT: " + e.getMessage());
                callbackContext.error("INTERRUPT_EXCEPTION");
                return false;
            }
        }

        if (wifiManager.removeNetwork(networkIdToConnect)) {
            if (API_VERSION < 26) {
                wifiManager.saveConfiguration();
            }
        }
        callbackContext.error("CONNECT_FAILED_TIMEOUT");
        Log.e(TAG, "WAIT_TO_CONNECT: Network failed to finish connecting within the timeout");
        return false;
    }

    protected void requestPermission(int requestCode) {
        cordova.requestPermission(this, requestCode, ACCESS_COARSE_LOCATION);
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults)
            throws JSONException {

        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                callbackContext.error("PERMISSION_DENIED");
                return;
            }
        }

        switch (requestCode) {
        case CONNECTION_CODE:
            connect(callbackContext, SSID, PASS);
            break;
        case SSID_CODE:
            getSSID(callbackContext);
            break;
        }
    }

    private void connect(CallbackContext callbackContext, String ssid, String password) throws JSONException {
        Log.e(TAG, "CONNECT: connect entered.");

        final CallbackContext _callbackContext = callbackContext;
        final String _ssid = ssid;
        final String _password = password;

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    int networkId = -1;
                    if (API_VERSION >= 26) {
                        Log.e(TAG, "CONNECT: Add network with api version >=26");
                        networkId = add(_ssid, _password);
                    } else {
                        if (cordova.hasPermission(ACCESS_COARSE_LOCATION)) {
                            List<ScanResult> scanResults = wifiManager.getScanResults();
                            Log.e(TAG, "CONNECT: " + scanResults.size());
                            for (ScanResult scan : scanResults) {
                                Log.e(TAG, "CONNECT: " + scan.SSID);
                                if (scan.SSID.equals(_ssid))
                                    networkId = add(_ssid, _password);
                            }
                        } else
                            requestPermission(CONNECTION_CODE);
                    }

                    if (networkId > -1) {
                        if (NETWORK_MANUALLY_ADDED) {
                            Log.e(TAG, "CONNECT: network already connected");
                            _callbackContext.error("NETWORK_ALREADY_CONNECTED");
                        } else {
                            wifiManager.enableNetwork(networkId, true);
                            waitForConnection(_callbackContext, networkId, _ssid, _password);
                        }
                    } else {
                        Log.e(TAG, "CONNECT: ssid not found");
                        _callbackContext.error("SSID_NOT_FOUND");
                    }

                } catch (Exception e) {
                    Log.e(TAG, "EXPECTION ON CONNECTION: " + e.getMessage());
                    _callbackContext.error("EXPECTION_ON_CONNECTION");
                }
            }
        });

    }

    private void remove(CallbackContext callbackContext, String ssid) throws JSONException {
        Log.e(TAG, "REMOVE: remove entered.");
        final CallbackContext _callbackContext = callbackContext;
        final String _ssid = ssid;

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    int networkIdToRemove = ssidToNetworkId(_ssid);
                    if (networkIdToRemove > -1) {
                        if (wifiManager.removeNetwork(networkIdToRemove)) {
                            if (API_VERSION < 26) {
                                wifiManager.saveConfiguration();
                            }
                            Log.e(TAG, "REMOVE: Network removed successfully.");
                            _callbackContext.success("NETWORK_REMOVED");
                        } else {
                            Log.e(TAG, "REMOVE: unable to remove network");
                            _callbackContext.error("UNABLE_TO_REMOVE_NETWORK");
                        }
                    } else {
                        Log.e(TAG, "REMOVE: Network not found");
                        _callbackContext.error("NETWORK_ID_NOT_FOUND");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "EXPECTION ON REMOVE: " + e.getMessage());
                    _callbackContext.error("EXPECTION_ON_REMOVE");
                }
            }
        });
    }

    private void getSSID(CallbackContext callbackContext) throws JSONException {
        Log.e(TAG, "GETSSID: check Connection entered.");

        final CallbackContext _callbackContext = callbackContext;

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    // if (cordova.hasPermission(ACCESS_COARSE_LOCATION)) {
                    WifiInfo info = wifiManager.getConnectionInfo();
                    if (info != null) {
                        String ssid = info.getSSID();
                        if (ssid.isEmpty() || ssid.equalsIgnoreCase("0x")) {
                            Log.e(TAG, "GETSSID: No network info");
                            _callbackContext.error("NO_SSID_INFO");
                        } else {
                            if (ssid.startsWith("\"") && ssid.endsWith("\""))
                                ssid = ssid.substring(1, ssid.length() - 1);

                            int networkId = ssidToNetworkId(ssid);

                            if (NETWORK_MANUALLY_ADDED) {
                                Log.e(TAG, "SSIDTONETWORKID: ssid found but configured by android.uid.system");
                                _callbackContext.error("NETWORK_MANUALLY_ADDED");
                            } else {
                                Log.e(TAG, "GETSSID: " + ssid);
                                _callbackContext.success(ssid);
                            }
                        }
                    } else {
                        Log.e(TAG, "GETSSID: No network connected");
                        _callbackContext.error("NO_NETWORK_CONNECTED");
                    }
                    // } else
                    // requestPermission(SSID_CODE);
                } catch (Exception e) {
                    Log.e(TAG, "EXPECTION ON GETSSID: " + e.getMessage());
                    _callbackContext.error("EXPECTION_ON_GETSSID");
                }
            }
        });
    }
}
