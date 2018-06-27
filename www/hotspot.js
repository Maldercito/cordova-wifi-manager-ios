var exec = require('cordova/exec');

module.exports = {
    connect: function (ssid, password, isWep) {
        return new Promise(function (resolve, reject) {
            exec(resolve, reject, 'hotspot', 'connect', [ssid, password, isWep]);
        })
    },
    disconnect: function (name) {
        return new Promise(function (resolve, reject) {
            exec(resolve, reject, 'hotspot', 'disconnect', [name]);
        })
    },
    getSSID: function (name) {
        return new Promise(function (resolve, reject) {
            exec(resolve, reject, 'hotspot', 'getSSID', [name]);
        })
    }
};