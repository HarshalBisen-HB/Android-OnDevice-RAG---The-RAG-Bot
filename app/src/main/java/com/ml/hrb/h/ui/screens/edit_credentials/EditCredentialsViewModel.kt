// Author: Harshal R. Bisen
// ViewModel for managing user API keys and credentials.

package com.ml.hrb.h.ui.screens.edit_credentials

import androidx.lifecycle.ViewModel
import com.ml.hrb.h.data.GeminiAPIKey
import com.ml.hrb.h.data.HFAccessToken
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class EditCredentialsViewModel(
    private val geminiAPIKey: GeminiAPIKey,
    private val hfAccessToken: HFAccessToken,
) : ViewModel() {
    fun getGeminiAPIKey(): String? = geminiAPIKey.getAPIKey()

    fun saveGeminiAPIKey(apiKey: String) {
        geminiAPIKey.saveAPIKey(apiKey)
    }

    fun getHFAccessToken(): String? = hfAccessToken.getToken()

    fun saveHFAccessToken(accessToken: String) {
        hfAccessToken.saveToken(accessToken)
    }
}
