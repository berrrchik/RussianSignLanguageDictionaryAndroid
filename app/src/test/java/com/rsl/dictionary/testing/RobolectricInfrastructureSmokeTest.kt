package com.rsl.dictionary.testing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RobolectricInfrastructureSmokeTest {
    @Test
    fun sharedPreferences_androidContext_availableOnJvm() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences("robolectric-smoke", Context.MODE_PRIVATE)

        preferences.edit()
            .putString("status", "ready")
            .commit()

        assertNotNull(context)
        assertEquals("com.rsl.dictionary", context.packageName)
        assertEquals("ready", preferences.getString("status", null))
    }
}
