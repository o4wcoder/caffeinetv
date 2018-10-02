package tv.caffeine.app.api.model

class Lobby {
    class Result(val tags: Map<String, Tag>, val content: Map<String, Content>, val header: Any, val sections: Array<Section>)
    class Tag(val id: String, val name: String, val color: String)
    class Content(val id: String, val type: String, val name: String, val iconImagePath: String, val bannerImagePath: String) {
        val iconImageUrl get() = "https://images.caffeine.tv$iconImagePath"
    }
    class Section(val id: String, val type: String, val name: String, val emptyMessage: String?, val broadcasters: Array<Broadcaster>?, val categories: Array<Category>?)
    class Broadcaster(val id: String, val type: String, val user: User, val tagId: String,
                      val broadcast: Broadcast?,
                      val lastBroadcast: Broadcast?,
                      val followingViewers: Array<User>,
                      val followingViewersCount: Int)
    class Category(val id: String, val name: String, val broadcasters: Array<Broadcaster>)
}