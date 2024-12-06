package com.example.smslinkchecker;

import java.util.concurrent.TimeUnit;

import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;

import okhttp3.OkHttpClient;
import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.TlsVersion;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class NetworkClient {

    // SSL Pinning with Secure TLS Configuration
    // Adapted from https://codecrunchersx.pages.dev/secure-android-apps-with-ssl-pinning-easy-tutorial/
    public static OkHttpClient getPinnedHttpClient() {

        // Documentation: https://square.github.io/okhttp/features/https/
        // Enforce TLS 1.2 and 1.3
        ConnectionSpec secureConnectionSpec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3) // Enforce TLS 1.2 and 1.3
                .cipherSuites(
                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
                )
                .build();

        // Build OkHttpClient with CertificatePinner and additional configurations
        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)  // Set connection timeout
                .readTimeout(30, TimeUnit.SECONDS)     // Set read timeout
                .writeTimeout(30, TimeUnit.SECONDS)    // Set write timeout
                .retryOnConnectionFailure(true)        // Enable retry on connection failure
                .connectionSpecs(Arrays.asList(secureConnectionSpec, ConnectionSpec.CLEARTEXT)) // Enforce TLS and allow cleartext fallback
                .build();
    }
}

