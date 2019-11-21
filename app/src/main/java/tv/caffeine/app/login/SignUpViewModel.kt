package tv.caffeine.app.login

import androidx.databinding.Bindable
import tv.caffeine.app.api.SignUpAccount
import tv.caffeine.app.api.SignUpBody
import tv.caffeine.app.repository.AccountRepository
import tv.caffeine.app.ui.CaffeineViewModel
import javax.inject.Inject

class SignUpViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : CaffeineViewModel() {

    var email = ""
        set(value) {
            field = value
            notifyChange()
        }
    var username = ""
        set(value) {
            field = value
            notifyChange()
        }
    var password = ""
        set(value) {
            field = value
            notifyChange()
        }
    var birthdate = ""
        set(value) {
            field = value
            notifyChange()
        }

    var passwordIsValid = false
        set(value) {
            field = value
            notifyChange()
        }

    @Bindable
    fun isSignUpButtonEnabled() = email.isNotEmpty() && username.isNotEmpty() && passwordIsValid && birthdate.isNotEmpty()

    suspend fun signIn(signUpBody: SignUpBody) =
        accountRepository.signUp(signUpBody)

    fun getSignUpBody(dob: String, recaptchaToken: String?, arkoseToken: String?, iid: String?): SignUpBody {
        val countryCode = "US"
        val account = SignUpAccount(username, password, email, dob, countryCode)
        return SignUpBody(account, iid, true, recaptchaToken, arkoseToken)
    }
}