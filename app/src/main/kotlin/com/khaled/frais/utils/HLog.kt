package com.khaled.frais.utils

import android.util.Log

object HLog {
    private const val TAG = "Frais"
    fun i(tag: String, string: String) = Log.i(tag, string)
    fun e(t: Throwable) = Log.e(TAG, t.stackTraceToString())
}