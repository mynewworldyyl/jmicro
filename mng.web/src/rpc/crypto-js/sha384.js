(function (root, factory, undef) {
    if (typeof exports === 'object') {
        // CommonJS
        module.exports = exports = factory(require('./core'), require('./x64-core'), require('./sha512'));
    } else {
        if (typeof define === 'function' && define.amd) {
            // AMD
            define(['./core', './x64-core', './sha512'], factory);
        } else {
            // Global (browser)
            factory(root.CryptoJS);
        }
    }
})(this, function (CryptoJS) {
    (function () {
        // Shortcuts
        var C = CryptoJS;
        var C_x64 = C.x64;
        var X64Word = C_x64.Word;
        var X64WordArray = C_x64.WordArray;
        var C_algo = C.algo;
        var SHA512 = C_algo.SHA512;
        /**
         * SHA-384 hash algorithm.
         */

        var SHA384 = (C_algo.SHA384 = SHA512.extend({
            _doReset: function () {
                this._hash = new X64WordArray.init([
                    new X64Word.init(3418070365, 3238371032),
                    new X64Word.init(1654270250, 914150663),
                    new X64Word.init(2438529370, 812702999),
                    new X64Word.init(355462360, 4144912697),
                    new X64Word.init(1731405415, 4290775857),
                    new X64Word.init(2394180231, 1750603025),
                    new X64Word.init(3675008525, 1694076839),
                    new X64Word.init(1203062813, 3204075428)
                ]);
            },
            _doFinalize: function () {
                var hash = SHA512._doFinalize.call(this);

                hash.sigBytes -= 16;
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
         *     var hash = CryptoJS.SHA384('message');
         *     var hash = CryptoJS.SHA384(wordArray);
         */

        C.SHA384 = SHA512._createHelper(SHA384);
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
         *     var hmac = CryptoJS.HmacSHA384(message, key);
         */

        C.HmacSHA384 = SHA512._createHmacHelper(SHA384);
    })();

    return CryptoJS.SHA384;
});
