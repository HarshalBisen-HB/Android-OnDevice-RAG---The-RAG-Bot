// Author: Harshal R. Bisen
// Main Application class and core configuration.

package com.ml.hrb.h

import android.app.Application
import com.ml.hrb.h.data.ObjectBoxStore
import com.ml.hrb.h.di.AppModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module

class Hdoc : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@Hdoc)
            modules(AppModule().module)
        }
        ObjectBoxStore.init(this)
    }
}
