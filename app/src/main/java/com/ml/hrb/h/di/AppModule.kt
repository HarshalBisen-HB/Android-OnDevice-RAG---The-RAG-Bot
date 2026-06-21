// Author: Harshal R. Bisen
// Dependency injection module configuration.

package com.ml.hrb.h.di

import android.content.ContentResolver
import android.content.Context
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
@ComponentScan("com.ml.hrb.h")
class AppModule {
    @Factory
    fun contentResolver(context: Context): ContentResolver = context.contentResolver
}
