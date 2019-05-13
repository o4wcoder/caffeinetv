package tv.caffeine.app.lobby

import tv.caffeine.app.api.FeaturedGuideListing
import tv.caffeine.app.lobby.FeaturedGuideItem.ListingItem
import java.util.Calendar
import java.util.LinkedList
import java.util.concurrent.TimeUnit

class FeaturedProgramGuideListingBuilder(private val rawListings: List<FeaturedGuideListing>) {

    fun build(): List<FeaturedGuideItem> {
        val listingItems = rawListings.map { ListingItem(it) }.apply {
            getOrNull(0)?.isExpanded = true
        }
        return buildDateHeader(listingItems)
    }

    private fun buildDateHeader(listingItems: List<ListingItem>): List<FeaturedGuideItem> {
        val timestampIndices = mutableListOf<Int>()
        listingItems.getOrNull(0)?.let { timestampIndices.add(0) }
        for (i in 1 until listingItems.size) {
            if (isDifferentDay(listingItems[i - 1].listing, listingItems[i].listing)) {
                timestampIndices.add(i)
            }
        }
        val featuredGuideItems = LinkedList<FeaturedGuideItem>(listingItems)
        for (i in timestampIndices.reversed()) {
            featuredGuideItems.add(i, FeaturedGuideItem.DateHeader(listingItems[i].listing.startTimestamp))
        }
        return featuredGuideItems
    }

    private fun isDifferentDay(listing1: FeaturedGuideListing, listing2: FeaturedGuideListing): Boolean {
        val calendar1 = Calendar.getInstance().apply {
            timeInMillis = TimeUnit.SECONDS.toMillis(listing1.startTimestamp)
        }
        val calendar2 = Calendar.getInstance().apply {
            timeInMillis = TimeUnit.SECONDS.toMillis(listing2.startTimestamp)
        }
        return calendar1.get(Calendar.DAY_OF_YEAR) != calendar2.get(Calendar.DAY_OF_YEAR) ||
                calendar1.get(Calendar.YEAR) != calendar2.get(Calendar.YEAR)
    }
}
