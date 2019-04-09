package tv.caffeine.app.fpg

import org.junit.Assert.assertEquals
import org.junit.Test
import tv.caffeine.app.api.FeaturedGuideListing
import tv.caffeine.app.lobby.FeaturedGuideItem
import tv.caffeine.app.lobby.FeaturedProgramGuideListingBuilder
import java.util.concurrent.TimeUnit

/**
 * Common cases of the FPG listing are tested in {@link tv.caffeine.app.fpg.LoadFeaturedProgramGuideUseCaseTests}.
 * This class handles special cases in building the listing.
 */
class FeaturedProgramGuideListingBuilderTests {

    @Test
    fun `only the first listing item is expanded`() {
        val itemList = FeaturedProgramGuideListingBuilder(listOf(
                buildFeaturedGuideListing(0, 0),
                buildFeaturedGuideListing(1, 1)
        )).build()
        itemList.filter { it is FeaturedGuideItem.ListingItem }
                .forEachIndexed { index, featuredGuideItem ->
                    (featuredGuideItem as FeaturedGuideItem.ListingItem).isExpanded = index == 0
                }
    }

    @Test
    fun `listing within a single day`() {
        val day1TimeStamp = 0L
        val rawListing = listOf(
                buildFeaturedGuideListing(0, day1TimeStamp),
                buildFeaturedGuideListing(1, day1TimeStamp)
        )
        val itemList = FeaturedProgramGuideListingBuilder(rawListing).build()
        assertEquals(day1TimeStamp, (itemList[0] as FeaturedGuideItem.DateHeader).startTimestamp)
        assertEquals(rawListing[0].id, (itemList[1] as FeaturedGuideItem.ListingItem).listing.id)
        assertEquals(rawListing[1].id, (itemList[2] as FeaturedGuideItem.ListingItem).listing.id)
    }

    @Test
    fun `listing across 2 days`() {
        val day1TimeStamp = 0L
        val day2TimeStamp = TimeUnit.DAYS.toMillis(1)
        val rawListing = listOf(
                buildFeaturedGuideListing(0, day1TimeStamp),
                buildFeaturedGuideListing(1, day2TimeStamp),
                buildFeaturedGuideListing(2, day2TimeStamp)
        )
        val itemList = FeaturedProgramGuideListingBuilder(rawListing).build()
        assertEquals(day1TimeStamp, (itemList[0] as FeaturedGuideItem.DateHeader).startTimestamp)
        assertEquals(rawListing[0].id, (itemList[1] as FeaturedGuideItem.ListingItem).listing.id)
        assertEquals(day2TimeStamp, (itemList[2] as FeaturedGuideItem.DateHeader).startTimestamp)
        assertEquals(rawListing[1].id, (itemList[3] as FeaturedGuideItem.ListingItem).listing.id)
        assertEquals(rawListing[2].id, (itemList[4] as FeaturedGuideItem.ListingItem).listing.id)
    }

    private fun buildFeaturedGuideListing(id: Int, startTimestamp: Long): FeaturedGuideListing {
        val endTimestamp = startTimestamp + 1
        return FeaturedGuideListing("$id", "$id", "category $id", "title $id", startTimestamp, endTimestamp, "description $id", null, false)
    }
}

