package tv.caffeine.app.stage

import com.google.gson.Gson
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.awaitAndParseErrors
import javax.inject.Inject

class GetSignedUserDetailsUseCase @Inject constructor(
    private val usersService: UsersService,
    private val gson: Gson
) {
    suspend operator fun invoke(caid: CAID) =
        usersService.signedUserDetails(caid).awaitAndParseErrors(gson)
}
