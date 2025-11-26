package com.agildesenvolvimento.coletor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Cordova plugin to receive barcode scans from Zebra DataWedge using broadcast intents.
 *
 * Configure your DataWedge profile with:
 *   - Intent Output: Enabled
 *   - Intent Action: com.agil.ACTION
 *   - Intent Delivery: Broadcast
 */
public class DataWedgePlugin extends CordovaPlugin {

    private static final String ACTION_START_LISTENING = "startListening";
    private static final String DW_INTENT_ACTION = "com.agil.ACTION";

    private CallbackContext scanCallbackContext;
    private BroadcastReceiver scanReceiver;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        if (ACTION_START_LISTENING.equals(action)) {
            this.scanCallbackContext = callbackContext;
            registerReceiver();

            // Keep callback alive for multiple scan events
            PluginResult noResult = new PluginResult(PluginResult.Status.NO_RESULT);
            noResult.setKeepCallback(true);
            callbackContext.sendPluginResult(noResult);
            return true;
        }
        return false;
    }

    private void registerReceiver() {
        if (scanReceiver != null) {
            return;
        }

        final Context context = this.cordova.getActivity().getApplicationContext();

        scanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (!DW_INTENT_ACTION.equals(intent.getAction())) {
                    return;
                }

                String data = intent.getStringExtra("com.symbol.datawedge.data_string");
                if (data == null) {
                    return;
                }

                if (scanCallbackContext != null) {
                    try {
                        JSONObject obj = new JSONObject();
                        obj.put("barcode", data);

                        PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
                        result.setKeepCallback(true);
                        scanCallbackContext.sendPluginResult(result);
                    } catch (JSONException e) {
                        PluginResult error = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
                        error.setKeepCallback(true);
                        scanCallbackContext.sendPluginResult(error);
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(DW_INTENT_ACTION);
        context.registerReceiver(scanReceiver, filter);
    }

    @Override
    public void onReset() {
        unregisterReceiver();
        super.onReset();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver();
        super.onDestroy();
    }

    private void unregisterReceiver() {
        if (scanReceiver != null) {
            try {
                this.cordova.getActivity().getApplicationContext().unregisterReceiver(scanReceiver);
            } catch (IllegalArgumentException e) {
                // ignore
            }
            scanReceiver = null;
        }
    }
}
