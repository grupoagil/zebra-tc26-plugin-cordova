package com.agildesenvolvimento.coletor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Cordova plugin para integrar Zebra DataWedge (TC26, etc) com apps Cordova.
 *
 * Funcionalidades:
 *  - Criação automática de Profile no DataWedge (PROFILE_NAME = "ColetorAgil")
 *  - Configuração automática de:
 *      * Barcode input
 *      * Intent output (action = com.agil.ACTION, delivery = broadcast)
 *      * Associação com o app com.agildesenvolvimento.coletor
 *  - Recebimento das leituras via Intent e envio para o JavaScript.
 *
 * IMPORTANTE:
 *  - Este plugin assume:
 *      * packageName / applicationId = com.agildesenvolvimento.coletor
 *      * Intent Action configurado = com.agil.ACTION
 */
public class DataWedgePlugin extends CordovaPlugin {

    private static final String ACTION_START_LISTENING = "startListening";
    private static final String ACTION_CREATE_OR_UPDATE_PROFILE = "createOrUpdateProfile";

    private static final String DW_API_ACTION = "com.symbol.datawedge.api.ACTION";
    private static final String DW_API_CREATE_PROFILE = "com.symbol.datawedge.api.CREATE_PROFILE";
    private static final String DW_API_SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG";

    // Nomes fixos para seu cenário
    private static final String PROFILE_NAME = "ColetorAgil";
    private static final String APP_PACKAGE = "com.agildesenvolvimento.coletor";
    private static final String INTENT_ACTION = "com.agil.ACTION";

    private CallbackContext scanCallbackContext;
    private BroadcastReceiver scanReceiver;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        // Ao inicializar o plugin, já tenta criar/configurar o profile automaticamente.
        final Context context = cordova.getActivity().getApplicationContext();
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    createOrUpdateDataWedgeProfile(context);
                } catch (Exception e) {
                    // não quebrar o app se der erro na configuração
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        if (ACTION_START_LISTENING.equals(action)) {
            this.scanCallbackContext = callbackContext;
            registerScanReceiver();

            // Mantém callback vivo
            PluginResult noResult = new PluginResult(PluginResult.Status.NO_RESULT);
            noResult.setKeepCallback(true);
            callbackContext.sendPluginResult(noResult);
            return true;
        }

        if (ACTION_CREATE_OR_UPDATE_PROFILE.equals(action)) {
            final Context context = this.cordova.getActivity().getApplicationContext();
            final CallbackContext cb = callbackContext;
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        createOrUpdateDataWedgeProfile(context);
                        PluginResult ok = new PluginResult(PluginResult.Status.OK, "Profile configurado.");
                        cb.sendPluginResult(ok);
                    } catch (Exception e) {
                        PluginResult error = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
                        cb.sendPluginResult(error);
                    }
                }
            });
            return true;
        }

        return false;
    }

    /**
     * Cria/atualiza o profile no DataWedge usando a API oficial via intents.
     */
    private void createOrUpdateDataWedgeProfile(Context context) {
        // 1) Criar profile (se já existir, o DataWedge apenas ignora)
        Intent createIntent = new Intent();
        createIntent.setAction(DW_API_ACTION);
        createIntent.putExtra(DW_API_CREATE_PROFILE, PROFILE_NAME);
        context.sendBroadcast(createIntent);

        // 2) Montar configuração completa do profile
        Bundle profileConfig = new Bundle();
        profileConfig.putString("PROFILE_NAME", PROFILE_NAME);
        profileConfig.putString("PROFILE_ENABLED", "true");
        profileConfig.putString("CONFIG_MODE", "UPDATE");

        // --- Configuração do plugin BARCODE ---
        Bundle barcodeProps = new Bundle();
        // Exemplos de parâmetros. Em muitos casos, apenas o plugin habilitado já funciona com defaults.
        // barcodeProps.putString("scanner_input_enabled", "true");

        Bundle barcodeConfig = new Bundle();
        barcodeConfig.putString("PLUGIN_NAME", "BARCODE");
        barcodeConfig.putString("RESET_CONFIG", "true");
        barcodeConfig.putBundle("PARAM_LIST", barcodeProps);

        // --- Configuração do plugin INTENT ---
        Bundle intentProps = new Bundle();
        intentProps.putString("intent_output_enabled", "true");
        intentProps.putString("intent_action", INTENT_ACTION);
        // 0 = startActivity, 1 = startService, 2 = broadcast
        intentProps.putString("intent_delivery", "2");

        Bundle intentConfig = new Bundle();
        intentConfig.putString("PLUGIN_NAME", "INTENT");
        intentConfig.putString("RESET_CONFIG", "true");
        intentConfig.putBundle("PARAM_LIST", intentProps);

        // Adicionar plugins à lista
        ArrayList<Bundle> pluginConfigList = new ArrayList<Bundle>();
        pluginConfigList.add(barcodeConfig);
        pluginConfigList.add(intentConfig);

        profileConfig.putParcelableArrayList("PLUGIN_CONFIG", pluginConfigList);

        // --- Associação do profile ao app ---
        Bundle appConfig = new Bundle();
        appConfig.putString("PACKAGE_NAME", APP_PACKAGE);
        // "*" => todas as activities do app
        appConfig.putStringArray("ACTIVITY_LIST", new String[] { "*" });

        profileConfig.putParcelableArray("APP_LIST", new Bundle[] { appConfig });

        // 3) Enviar configuração
        Intent setConfigIntent = new Intent();
        setConfigIntent.setAction(DW_API_ACTION);
        setConfigIntent.putExtra(DW_API_SET_CONFIG, profileConfig);
        context.sendBroadcast(setConfigIntent);
    }

    /**
     * Registra BroadcastReceiver para receber os intents de leitura.
     */
    private void registerScanReceiver() {
        if (scanReceiver != null) {
            return;
        }

        final Context context = this.cordova.getActivity().getApplicationContext();

        scanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (intent == null) return;

                String action = intent.getAction();
                if (action == null || !INTENT_ACTION.equals(action)) {
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
        filter.addAction(INTENT_ACTION);
        context.registerReceiver(scanReceiver, filter);
    }

    @Override
    public void onReset() {
        unregisterScanReceiver();
        super.onReset();
    }

    @Override
    public void onDestroy() {
        unregisterScanReceiver();
        super.onDestroy();
    }

    private void unregisterScanReceiver() {
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
