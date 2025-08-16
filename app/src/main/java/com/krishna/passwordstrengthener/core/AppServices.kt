package com.krishna.passwordstrengthener.core

import android.content.Context
import com.krishna.passwordstrengthener.data.WeakPasswordProvider
import com.krishna.passwordstrengthener.model.LocalModelClient
import com.krishna.passwordstrengthener.model.ModelRepository
import com.krishna.passwordstrengthener.model.SmolLmLocalModelClient
import com.krishna.passwordstrengthener.scoring.DefaultPasswordScoringService
import com.krishna.passwordstrengthener.scoring.PasswordScoringService
import com.krishna.passwordstrengthener.session.PasswordSessionRepository

/**
 * Lightweight service locator for this POC.
 */
object AppServices {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // Repositories / Services (lazy singletons)
    val weakPasswordProvider: WeakPasswordProvider by lazy { WeakPasswordProvider() }
    val sessionRepository: PasswordSessionRepository by lazy { PasswordSessionRepository() }
    val scoringService: PasswordScoringService by lazy { DefaultPasswordScoringService() }
    val modelClient: LocalModelClient by lazy { SmolLmLocalModelClient() }

    // ModelRepository needs context at call site
    val modelRepository: ModelRepository by lazy { ModelRepository }

    fun requireAppContext(): Context = appContext
        ?: throw IllegalStateException("AppServices.init(context) must be called before use")
}
