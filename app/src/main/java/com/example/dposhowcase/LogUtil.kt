package com.example.dposhowcase

import android.util.Log

object LogUtil {
    private const val TAG = "DPO_APP"

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun e(message: String, error: Throwable? = null) {
        if (error != null) {
            Log.e(TAG, message, error)
        } else {
            Log.e(TAG, message)
        }
    }

    fun w(message: String) {
        Log.w(TAG, message)
    }
}