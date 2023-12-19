(function (root, factory, undef) {
    if (typeof exports === 'object') {
        // CommonJS
        module.exports = exports = factory(require('./core'), require('./enc-base64'), require('./md5'), require('./evpkdf'), require('./cipher-core'));
    } else {
        if (typeof define === 'function' && define.amd) {
            // AMD
            define(['./core', './enc-base64', './md5', './evpkdf', './cipher-core'], factory);
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
        var StreamCipher = C_lib.StreamCipher;
        var C_algo = C.algo; // Reusable objects

        var S = [];
        var C_ = [];
        var G = [];
        /**
         * Rabbit stream cipher algorithm.
         *
         * This is a legacy version that neglected to convert the key to little-endian.
         * This error doesn't affect the cipher's security,
         * but it does affect its compatibility with other implementations.
         */

        var RabbitLegacy = (C_algo.RabbitLegacy = StreamCipher.extend({
            _doReset: function () {
                // Shortcuts
                var K = this._key.words;
                var iv = this.cfg.iv; // Generate initial state values

                var X = (this._X = [
                    K[0],
                    (K[3] << 16) | (K[2] >>> 16),
                    K[1],
                    (K[0] << 16) | (K[3] >>> 16),
                    K[2],
                    (K[1] << 16) | (K[0] >>> 16),
                    K[3],
                    (K[2] << 16) | (K[1] >>> 16)
                ]); // Generate initial counter values

                var C = (this._C = [
                    (K[2] << 16) | (K[2] >>> 16),
                    (K[0] & 4294901760) | (K[1] & 65535),
                    (K[3] << 16) | (K[3] >>> 16),
                    (K[1] & 4294901760) | (K[2] & 65535),
                    (K[0] << 16) | (K[0] >>> 16),
                    (K[2] & 4294901760) | (K[3] & 65535),
                    (K[1] << 16) | (K[1] >>> 16),
                    (K[3] & 4294901760) | (K[0] & 65535)
                ]); // Carry bit

                this._b = 0; // Iterate the system four times

                for (var i = 0; i < 4; i++) {
                    nextState.call(this);
                } // Modify the counters

                for (var i = 0; i < 8; i++) {
                    C[i] ^= X[(i + 4) & 7];
                } // IV setup

                if (iv) {
                    // Shortcuts
                    var IV = iv.words;
                    var IV_0 = IV[0];
                    var IV_1 = IV[1]; // Generate four subvectors

                    var i0 = (((IV_0 << 8) | (IV_0 >>> 24)) & 16711935) | (((IV_0 << 24) | (IV_0 >>> 8)) & 4278255360);
                    var i2 = (((IV_1 << 8) | (IV_1 >>> 24)) & 16711935) | (((IV_1 << 24) | (IV_1 >>> 8)) & 4278255360);
                    var i1 = (i0 >>> 16) | (i2 & 4294901760);
                    var i3 = (i2 << 16) | (i0 & 65535); // Modify counter values

                    C[0] ^= i0;
                    C[1] ^= i1;
                    C[2] ^= i2;
                    C[3] ^= i3;
                    C[4] ^= i0;
                    C[5] ^= i1;
                    C[6] ^= i2;
                    C[7] ^= i3; // Iterate the system four times

                    for (var i = 0; i < 4; i++) {
                        nextState.call(this);
                    }
                }
            },
            _doProcessBlock: function (M, offset) {
                // Shortcut
                var X = this._X; // Iterate the system

                nextState.call(this); // Generate four keystream words

                S[0] = X[0] ^ (X[5] >>> 16) ^ (X[3] << 16);
                S[1] = X[2] ^ (X[7] >>> 16) ^ (X[5] << 16);
                S[2] = X[4] ^ (X[1] >>> 16) ^ (X[7] << 16);
                S[3] = X[6] ^ (X[3] >>> 16) ^ (X[1] << 16);

                for (var i = 0; i < 4; i++) {
                    // Swap endian
                    S[i] = (((S[i] << 8) | (S[i] >>> 24)) & 16711935) | (((S[i] << 24) | (S[i] >>> 8)) & 4278255360); // Encrypt

                    M[offset + i] ^= S[i];
                }
            },
            blockSize: 128 / 32,
            ivSize: 64 / 32
        }));

        function nextState() {
            // Shortcuts
            var X = this._X;
            var C = this._C; // Save old counter values

            for (var i = 0; i < 8; i++) {
                C_[i] = C[i];
            } // Calculate new counter values

            C[0] = (C[0] + 1295307597 + this._b) | 0;
            C[1] = (C[1] + 3545052371 + (C[0] >>> 0 < C_[0] >>> 0 ? 1 : 0)) | 0;
            C[2] = (C[2] + 886263092 + (C[1] >>> 0 < C_[1] >>> 0 ? 1 : 0)) | 0;
            C[3] = (C[3] + 1295307597 + (C[2] >>> 0 < C_[2] >>> 0 ? 1 : 0)) | 0;
            C[4] = (C[4] + 3545052371 + (C[3] >>> 0 < C_[3] >>> 0 ? 1 : 0)) | 0;
            C[5] = (C[5] + 886263092 + (C[4] >>> 0 < C_[4] >>> 0 ? 1 : 0)) | 0;
            C[6] = (C[6] + 1295307597 + (C[5] >>> 0 < C_[5] >>> 0 ? 1 : 0)) | 0;
            C[7] = (C[7] + 3545052371 + (C[6] >>> 0 < C_[6] >>> 0 ? 1 : 0)) | 0;

            if (C[7] >>> 0 < C_[7] >>> 0) {
                this._b = 1;
            } else {
                this._b = 0;
            } // Calculate the g-values

            for (var i = 0; i < 8; i++) {
                var gx = X[i] + C[i]; // Construct high and low argument for squaring

                var ga = gx & 65535;
                var gb = gx >>> 16; // Calculate high and low result of squaring

                var gh = ((((ga * ga) >>> 17) + ga * gb) >>> 15) + gb * gb;
                var gl = (((gx & 4294901760) * gx) | 0) + (((gx & 65535) * gx) | 0); // High XOR low

                G[i] = gh ^ gl;
            } // Calculate new state values

            X[0] = (G[0] + ((G[7] << 16) | (G[7] >>> 16)) + ((G[6] << 16) | (G[6] >>> 16))) | 0;
            X[1] = (G[1] + ((G[0] << 8) | (G[0] >>> 24)) + G[7]) | 0;
            X[2] = (G[2] + ((G[1] << 16) | (G[1] >>> 16)) + ((G[0] << 16) | (G[0] >>> 16))) | 0;
            X[3] = (G[3] + ((G[2] << 8) | (G[2] >>> 24)) + G[1]) | 0;
            X[4] = (G[4] + ((G[3] << 16) | (G[3] >>> 16)) + ((G[2] << 16) | (G[2] >>> 16))) | 0;
            X[5] = (G[5] + ((G[4] << 8) | (G[4] >>> 24)) + G[3]) | 0;
            X[6] = (G[6] + ((G[5] << 16) | (G[5] >>> 16)) + ((G[4] << 16) | (G[4] >>> 16))) | 0;
            X[7] = (G[7] + ((G[6] << 8) | (G[6] >>> 24)) + G[5]) | 0;
        }
        /**
         * Shortcut functions to the cipher's object interface.
         *
         * @example
         *
         *     var ciphertext = CryptoJS.RabbitLegacy.encrypt(message, key, cfg);
         *     var plaintext  = CryptoJS.RabbitLegacy.decrypt(ciphertext, key, cfg);
         */

        C.RabbitLegacy = StreamCipher._createHelper(RabbitLegacy);
    })();

    return CryptoJS.RabbitLegacy;
});
