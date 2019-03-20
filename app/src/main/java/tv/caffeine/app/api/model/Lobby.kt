package tv.caffeine.app.api.model

import tv.caffeine.app.di.IMAGES_BASE_URL

class Lobby(val tags: Map<String, Tag>, val content: Map<String, Content>, val header: Header, val sections: Array<Section>) {
    class Tag(val id: String, val name: String, val color: String)
    class Content(val id: String, val type: String, val name: String, val iconImagePath: String, val bannerImagePath: String) {
        val iconImageUrl get() = "$IMAGES_BASE_URL$iconImagePath"
    }
    class Section(val id: String, val type: String, val name: String?, val emptyMessage: String?, val broadcasters: Array<Broadcaster>?, val categories: Array<Category>?)
    data class Broadcaster(val id: String, val type: String, val user: User, val tagId: String,
                      val broadcast: Broadcast?,
                      val lastBroadcast: Broadcast?,
                      val followingViewers: List<User>,
                      val followingViewersCount: Int)
    class Category(val id: String, val name: String, val broadcasters: Array<Broadcaster>)
    class Header(val avatarCard: MiniUser? = null)
    class MiniUser(val username: String)
}
