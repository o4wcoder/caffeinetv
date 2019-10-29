package tv.caffeine.app.repository

import com.google.gson.Gson
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.PaginatedFollowers
import tv.caffeine.app.api.model.UserContainer
import tv.caffeine.app.api.model.awaitAndParseErrors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsersRepository @Inject constructor(
    private val usersService: UsersService,
    private val gson: Gson
) {
    suspend fun getFollowersList(caid: CAID): PaginatedFollowers = usersService.listFollowers(caid)

    suspend fun markNotificationsViewed(caid: CAID): CaffeineResult<UserContainer> = usersService.notificationsViewed(caid).awaitAndParseErrors(gson)

    // TODO: add the rest and refactor app wide
}
