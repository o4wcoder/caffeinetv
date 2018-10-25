package tv.caffeine.app.explore

import tv.caffeine.app.api.SearchUserItem

sealed class Findings(val data: Array<SearchUserItem>) {
    class Explore(data: Array<SearchUserItem>) : Findings(data)
    class Search(data: Array<SearchUserItem>) : Findings(data)
}
