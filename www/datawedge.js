var exec = require('cordova/exec');

var DataWedge = {
    /**
     * Inicia o listener para receber leituras do DataWedge.
     * callback de sucesso é chamado várias vezes, sempre que houver um scan.
     */
    startListening: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'DataWedgePlugin', 'startListening', []);
    },

    /**
     * (Opcional) Força recriar/atualizar o profile manualmente.
     * Normalmente o plugin já faz isso automaticamente na inicialização.
     */
    createOrUpdateProfile: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'DataWedgePlugin', 'createOrUpdateProfile', []);
    }
};

module.exports = DataWedge;
