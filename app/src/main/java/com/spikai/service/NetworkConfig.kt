package com.spikai.service

import android.content.Context

object NetworkConfig {
    /// Your backend base URL for API requests
    /// Replace this with your actual backend URL
    private const val backendURL = "https://us-central1-spik-backend.cloudfunctions.net/articles"
    
    /// Alternative: Read from environment or manifest if needed
    fun getBackendURL(context: Context? = null): String {
        // You can read from AndroidManifest.xml metadata, BuildConfig, or other sources
        context?.let {
            try {
                val appInfo = it.packageManager.getApplicationInfo(
                    it.packageName,
                    android.content.pm.PackageManager.GET_META_DATA
                )
                val metaData = appInfo.metaData
                val url = metaData?.getString("BackendURL")
                if (!url.isNullOrEmpty()) {
                    return url
                }
            } catch (e: Exception) {
                // Fall back to default URL if metadata reading fails
            }
        }
        return backendURL
    }
}
