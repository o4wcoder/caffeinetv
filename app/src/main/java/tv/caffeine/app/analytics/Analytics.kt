package tv.caffeine.app.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import timber.log.Timber
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.IdentityProvider
import tv.caffeine.app.ui.CaffeineFragment

interface Analytics {
    fun initialize()
    fun trackEvent(event: AnalyticsEvent)
}

class LogAnalytics : Analytics {
    override fun initialize() {
        Timber.d("LogAnalytics initialization")
    }

    override fun trackEvent(event: AnalyticsEvent) {
        val logMessage = when(event) {
            is AnalyticsEvent.NewRegistration -> "New Registration, user ID = ${event.userId}"
            is AnalyticsEvent.SocialSignInClicked -> "Social sign in with ${event.identityProvider}"
            is AnalyticsEvent.Notification -> "Notification, user ID = ${event.userId}, notification = ${event.notification}"
            is AnalyticsEvent.NewAccountClicked -> "New Account clicked"
        }
        Timber.d(logMessage)
    }
}

data class NotificationEvent(val type: Type, val id: String?, val tag: String?, val isDisplayed: Boolean = true) {
    enum class Type {
        Received, Opened
    }
}

sealed class AnalyticsEvent {
    data class NewRegistration(val userId: CAID) : AnalyticsEvent()
    data class SocialSignInClicked(val identityProvider: IdentityProvider) : AnalyticsEvent()
    data class Notification(val userId: CAID?, val notification: NotificationEvent) : AnalyticsEvent()
    object NewAccountClicked : AnalyticsEvent()
}

enum class FirebaseEvent {
    ContinueWithFacebookClicked, FacebookSignInSuccess, FacebookContinueToMFA, FacebookContinueToSignUp,
    ContinueWithTwitterClicked, TwitterSignInSuccess, TwitterContinueToMFA, TwitterContinueToSignUp,
    SocialOAuthEdgeCase, NewAccountClicked, SignUpSuccess,
    SignInClicked, SignInSuccess, SignInContinueToMFA, SignInContinueToTerms, MFASignInSuccess
}

fun FirebaseAnalytics.logEvent(event: FirebaseEvent) {
    logEvent(event.name, null)
}

fun FirebaseAnalytics.logScreen(fragment: CaffeineFragment) {
    logEvent("Screen_${fragment.javaClass.simpleName}", null)
}
