package com.mezanur.mydailymanager;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.*;
import android.content.Intent;
import android.net.Uri;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebViewAssetLoader;
import java.util.concurrent.Executor;

public class MainActivity extends FragmentActivity {
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private static final int FILE_CHOOSER = 4102;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("mdm_native", MODE_PRIVATE);
        webView = new WebView(this);
        setContentView(webView);

        WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
            .build();

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        webView.addJavascriptInterface(new NativeBridge(), "AndroidBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("https://appassets.androidplatform.net/assets/")) return false;
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = params.createIntent();
                try { startActivityForResult(intent, FILE_CHOOSER); }
                catch (Exception e) { fileCallback = null; return false; }
                return true;
            }
        });
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    private class NativeBridge {
        @JavascriptInterface public boolean biometricAvailable() {
            return androidx.biometric.BiometricManager.from(MainActivity.this)
                .canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
                    | androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS;
        }
        @JavascriptInterface public boolean biometricRegistered() {
            return prefs.getBoolean("bio_registered", false);
        }
        @JavascriptInterface public void registerBiometric() {
            runOnUiThread(() -> authenticate("Confirm device biometric to enable unlock", true));
        }
        @JavascriptInterface public void unlockBiometric() {
            runOnUiThread(() -> authenticate("Unlock My Daily Manager", false));
        }
        private void authenticate(String prompt, boolean registering) {
            Executor executor = ContextCompat.getMainExecutor(MainActivity.this);
            BiometricPrompt biometricPrompt = new BiometricPrompt(MainActivity.this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        if (registering) prefs.edit().putBoolean("bio_registered", true).apply();
                        webView.evaluateJavascript("window.onNativeBiometricResult && window.onNativeBiometricResult(true," + registering + ")", null);
                    }
                    @Override public void onAuthenticationError(int code, CharSequence message) {
                        super.onAuthenticationError(code, message);
                        webView.evaluateJavascript("window.onNativeBiometricResult && window.onNativeBiometricResult(false," + registering + ")", null);
                    }
                    @Override public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                    }
                });
            BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("My Daily Manager")
                .setSubtitle(prompt)
                .setAllowedAuthenticators(BiometricPrompt.Authenticators.BIOMETRIC_WEAK
                    | BiometricPrompt.Authenticators.DEVICE_CREDENTIAL)
                .build();
            biometricPrompt.authenticate(info);
        }
        @JavascriptInterface public void removeBiometric() {
            prefs.edit().putBoolean("bio_registered", false).apply();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER && fileCallback != null) {
            Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            fileCallback.onReceiveValue(result);
            fileCallback = null;
        }
    }
    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
