package com.bluesolution.driver.data.preferences

import android.content.Context
import android.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdPreference @Inject constructor(@ApplicationContext context: Context) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    fun getstoredId(): String {
        return prefs.getString("id", "")!!
    }
    fun setStoredId(query: String) {
        prefs.edit().putString("id", query).apply()
    }
}