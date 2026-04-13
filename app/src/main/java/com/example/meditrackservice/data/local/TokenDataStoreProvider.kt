package com.example.meditrackservice.data.local

import android.content.Context

object TokenDataStoreProvider {
    @Volatile
    private var INSTANCE: TokenDataStore? = null

    fun getInstance(context: Context): TokenDataStore {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: TokenDataStore(context.applicationContext).also {
                INSTANCE = it
            }
        }
    }
}