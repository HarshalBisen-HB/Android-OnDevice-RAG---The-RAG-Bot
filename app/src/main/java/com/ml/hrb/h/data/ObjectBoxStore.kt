// Author: Harshal R. Bisen
// Initialization and management of the ObjectBox database store.

package com.ml.hrb.h.data

import android.content.Context
import io.objectbox.BoxStore

object ObjectBoxStore {
    lateinit var store: BoxStore
        private set

    fun init(context: Context) {
        store = MyObjectBox.builder().androidContext(context).build()
    }
}
