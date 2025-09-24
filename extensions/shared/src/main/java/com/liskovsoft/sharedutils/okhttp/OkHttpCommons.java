package com.liskovsoft.sharedutils.okhttp;

import com.liskovsoft.sharedutils.okhttp.interceptors.UnzippingInterceptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import okhttp3.CipherSuite;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Protocol;

public final class OkHttpCommons {
    private static final String TAG = OkHttpCommons.class.getSimpleName();
    public static final long CONNECT_TIMEOUT_MS = 20_000;
    public static final long READ_TIMEOUT_MS = 20_000;
    public static final long WRITE_TIMEOUT_MS = 20_000;
    public static boolean enableProfiler = true;

    private OkHttpCommons() {
        
    }

    // This is nearly equal to the cipher suites supported in Chrome 51, current as of 2016-05-25.
    // All of these suites are available on Android 7.0; earlier releases support a subset of these
    // suites. https://github.com/square/okhttp/issues/1972
    private static final CipherSuite[] APPROVED_CIPHER_SUITES = new CipherSuite[] {
            // TLSv1.3
            CipherSuite.TLS_AES_128_GCM_SHA256,
            CipherSuite.TLS_AES_256_GCM_SHA384,
            CipherSuite.TLS_CHACHA20_POLY1305_SHA256,
            CipherSuite.TLS_AES_128_CCM_SHA256,
            // Robolectric error (no such field). Constructing manually.
            //CipherSuite.TLS_AES_256_CCM_8_SHA256,
            CipherSuite.forJavaName("TLS_AES_256_CCM_8_SHA256"),

            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,
            CipherSuite.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256,

            // Note that the following cipher suites are all on HTTP/2's bad cipher suites list. We'll
            // continue to include them until better suites are commonly available. For example, none
            // of the better cipher suites listed above shipped with Android 4.4 or Java 7.
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,
            CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256,
            CipherSuite.TLS_RSA_WITH_AES_256_GCM_SHA384,
            CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA, // should be commented out?
            CipherSuite.TLS_RSA_WITH_3DES_EDE_CBC_SHA,

            // Change TLS fingerprint by altering default cipher list
            // From original fix
            CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
            // From NewPipe Downloader
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA
    };

    private static void setupConnectionParams(OkHttpClient.Builder okBuilder) {
        // Setup default timeout
        // https://stackoverflow.com/questions/39219094/sockettimeoutexception-in-retrofit
        okBuilder.connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        okBuilder.readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        okBuilder.writeTimeout(WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        // Imitate 'keepAlive' = false (yt throttle fix? Cause slow video loading?)
        // https://stackoverflow.com/questions/70873186/how-to-disable-connection-pooling-and-make-a-new-connection-for-each-request-in
        // https://stackoverflow.com/questions/63047533/connection-pool-okhttp
        // NOTE: SocketTimeoutException fix: setup connection pool with 0 (!) idle connections!
        //okBuilder.connectionPool(new ConnectionPool(0, READ_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        //okBuilder.connectionPool(new ConnectionPool(10, 24, TimeUnit.HOURS)); // Video unavailable fix???
        okBuilder.connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES)); // fix npe on pool dispose???
    }

    /**
     * Fixing SSL handshake timed out (probably provider issues in some countries)
     */
    private static void setupConnectionFix(OkHttpClient.Builder okBuilder) {
        // Alter cipher list to create unique TLS fingerprint
        ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .cipherSuites(APPROVED_CIPHER_SUITES)
                .build();
        okBuilder.connectionSpecs(Arrays.asList(cs, ConnectionSpec.CLEARTEXT));
    }

    /**
     * Fix for {@link okhttp3.internal.http2.StreamResetException}: stream was reset: CANCEL<br/>
     * Force HTTP 1.1 protocol<br/>
     * Happen when frequently do interrupt/create stream<br/>
     * https://stackoverflow.com/questions/53648852/how-to-solve-okhttp3-internal-http2-streamresetexception-stream-was-reset-refu<br/>
     * https://github.com/square/okhttp/issues/3955
     */
    private static void fixStreamResetError(Builder okBuilder) {
        okBuilder.protocols(Collections.singletonList(Protocol.HTTP_1_1));
    }
    
    public static OkHttpClient.Builder setupBuilder(OkHttpClient.Builder okBuilder) {
        setupConnectionFix(okBuilder);
        setupConnectionParams(okBuilder);
        fixStreamResetError(okBuilder); // Should I move the line to Retrofit utils?
        enableDecompression(okBuilder);

        return okBuilder;
    }

    /**
     * Checks that response is compressed and do uncompress if needed.
     */
    private static void enableDecompression(OkHttpClient.Builder builder) {
        // Add gzip/deflate/br support
        //builder.addInterceptor(BrotliInterceptor.INSTANCE);
        builder.addInterceptor(new UnzippingInterceptor());
    }
}
