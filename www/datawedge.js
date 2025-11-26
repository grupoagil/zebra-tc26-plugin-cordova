var exec = require('cordova/exec');

var DataWedge = {
    /**
     * Start listening for barcode scans from DataWedge.
     * successCallback will be called every time a barcode is read:
     *   function(result) { console.log(result.barcode); }
     */
    startListening: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'DataWedgePlugin', 'startListening', []);
    }
};

module.exports = DataWedge;
