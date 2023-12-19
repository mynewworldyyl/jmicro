(function (root, factory, undef) {
    if (typeof exports === 'object') {
        // CommonJS
        module.exports = exports = factory(require('./core'), require('./sha256'));
    } else {
        if (typeof define === 'function' && define.amd) {
            // AMD
            define(['./core', './sha256'], factory);
        } else {
            // Global (browser)
            factory(root.CryptoJS);
        }
    }
})(this, function (CryptoJS) {
    (function () {
        // Shortcuts
        var C = CryptoJS;
        var C_lib = C.lib;
        var WordArray = C_lib.WordArray;
        var C_algo = C.algo;
        var SHA256 = C_algo.SHA256;
        /**
         * SHA-224 hash algorithm.
         */

        var SHA224 = (C_algo.SHA224 = SHA256.extend({
            _doReset: function () {
                this._hash = new WordArray.init([3238371032, 914150663, 812702999, 4144912697, 4290775857, 1750603025, 1694076839, 3204075428]);
            },
            _doFinalize: function () {
                var hash = SHA256._doFinalize.call(this);

                hash.sigBytes -= 4;
                return hash;
            }
        }));
        /**
         * Shortcut function to the hasher's object interface.
         *
         * @param {WordArray|string} message The message to hash.
         *
         * @return {WordArray} The hash.
         *
         * @static
         *
         * @example
         *
         *     var hash = CryptoJS.SHA224('message');
         *     var hash = CryptoJS.SHA224(wordArray);
         */

        C.SHA224 = SHA256._createHelper(SHA224);
        /**
         * Shortcut function to the HMAC's object interface.
         *
         * @param {WordArray|string} message The message to hash.
         * @param {WordArray|string} key The secret key.
         *
         * @return {WordArray} The HMAC.
         *
         * @static
         *
         * @example
         *
         *     var hmac = CryptoJS.HmacSHA224(message, key);
         */

        C.HmacSHA224 = SHA256._createHmacHelper(SHA224);
    })();

    return CryptoJS.SHA224;
});