package tv.caffeine.app.ui

import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.runner.AndroidJUnit4
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tv.caffeine.app.CaffeineApplication
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.api.model.User
import tv.caffeine.app.lobby.release.OnlineBroadcaster

@RunWith(AndroidJUnit4::class)
class AvatarOverlapLiveBadgeViewModelTests {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val imagesBaseUrl = "https://images.caffeine.tv"
    private val topPick = "Top Pick"
    private val friendsWatchingText = "Friends Watching Text"

    @MockK
    lateinit var fakeOnlineBroadcaster: OnlineBroadcaster
    @MockK
    lateinit var fakeBroadcaster: Lobby.Broadcaster

    var subject: AvatarOverlapLiveBadgeViewModel = AvatarOverlapLiveBadgeViewModel(
        ApplicationProvider.getApplicationContext<CaffeineApplication>()
    )

    private val followers1 = (0..0).map { makeUser("avatarImageUrl$it") }
    private val followers4 = (0..3).map { makeUser("avatarImageUrl$it") }

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { fakeOnlineBroadcaster.broadcaster } returns fakeBroadcaster
    }

    @Test
    fun `live badge is visible on init, all else gone`() {
        testOnlyLiveBadgeVisible()
    }

    /* On Lobby */

    @Test
    fun `live badge is visible when broadcaster titleText is null and followers is null, all else gone`() {
        loadBroadcasterWithNoFollowersandNotitleText()
        testOnlyLiveBadgeVisible()
    }

    @Test
    fun `online broadcaster has server text that supercedes everything`() {
        loadBroadcasterWithServerText()
        testBroadcasterHasServerTextToSupercedeAll()
    }

    @Test
    fun `state is correct for online broadcaster with one follower`() {
        loadBroadcasterWithFollowers(hasMany = false)
        testFollowers(isStage = false, testMany = false)
    }

    @Test
    fun `state is correct for online broadcater with many followers`() {
        loadBroadcasterWithFollowers(hasMany = true)
        testFollowers(isStage = false, testMany = true)
    }

    @Test
    fun `live badge returns when online broadcaster goes from one follower to none`() {
        loadBroadcasterWithFollowers(hasMany = false)
        testFollowers(isStage = false, testMany = false) // pre-condition

        loadBroadcasterWithNoFollowersandNotitleText()
        testOnlyLiveBadgeVisible()
    }

    @Test
    fun `live badge returns when online broadcaster goes from many followers to none`() {
        loadBroadcasterWithFollowers(hasMany = true)
        testFollowers(isStage = false, testMany = true) // pre-condition

        loadBroadcasterWithNoFollowersandNotitleText()
        testOnlyLiveBadgeVisible()
    }

    @Test
    fun `server text supercedes from many followers to online broadcaster has server text`() {
        loadBroadcasterWithFollowers(hasMany = true)
        testFollowers(isStage = false, testMany = true) // pre-condition

        loadBroadcasterWithServerText()
        testBroadcasterHasServerTextToSupercedeAll()
    }

    /* On Stage */

    @Test
    fun `live badge is visible when stage followers is null, all else gone`() {
        subject.stageFollowers = null
        testOnlyLiveBadgeVisible()
    }

    @Test
    fun `state is correct with one stage follower`() {
        subject.stageFollowers = followers1
        testFollowers(isStage = true, testMany = false)
    }

    @Test
    fun `state is correct with many stage followers`() {
        subject.stageFollowers = followers4
        testFollowers(isStage = true, testMany = true)
    }

    @Test
    fun `state is correct after stage followers goes from one to null`() {
        subject.stageFollowers = followers1
        testFollowers(isStage = true, testMany = false) // pre-condition

        subject.stageFollowers = null
        testOnlyLiveBadgeVisible()
    }

    @Test
    fun `state is correct after stage followers goes from many to null`() {
        subject.stageFollowers = followers4
        testFollowers(isStage = true, testMany = true) // pre-condition

        subject.stageFollowers = null
        testOnlyLiveBadgeVisible()
    }

    @Test
    fun `state is correct from one stage follower to many`() {
        subject.stageFollowers = followers1
        testFollowers(isStage = true, testMany = false) // pre-condition

        subject.stageFollowers = followers4
        testFollowers(isStage = true, testMany = true)
    }

    @Test
    fun `state is correct from many stage followers to one`() {
        subject.stageFollowers = followers4
        testFollowers(isStage = true, testMany = true) // pre-condition

        subject.stageFollowers = followers1
        testFollowers(isStage = true, testMany = false)
    }

    private fun loadBroadcasterWithNoFollowersandNotitleText() {
        every { fakeOnlineBroadcaster.friendsWatchingText } returns null
        every { fakeOnlineBroadcaster.badgeText } returns null
        every { fakeBroadcaster.followingViewers } returns null
        subject.lobbyBroadcaster = fakeOnlineBroadcaster
    }

    private fun loadBroadcasterWithServerText() {
        every { fakeOnlineBroadcaster.friendsWatchingText } returns friendsWatchingText
        every { fakeOnlineBroadcaster.badgeText } returns topPick
        every { fakeBroadcaster.followingViewers } returns followers4
        subject.lobbyBroadcaster = fakeOnlineBroadcaster
    }

    private fun loadBroadcasterWithFollowers(hasMany: Boolean) {
        every { fakeOnlineBroadcaster.friendsWatchingText } returns friendsWatchingText
        every { fakeOnlineBroadcaster.badgeText } returns friendsWatchingText
        every { fakeBroadcaster.followingViewers } returns if (hasMany) followers4 else followers1
        subject.lobbyBroadcaster = fakeOnlineBroadcaster
    }

    private fun testOnlyLiveBadgeVisible() {
        assertTrue(subject.getLiveBadgeVisibility() == View.VISIBLE)
        assertTrue(subject.getAvatar1Visibility() == View.GONE)
        assertTrue(subject.getAvatar2Visibility() == View.GONE)
        assertTrue(subject.getTitleViewVisibility() == View.GONE)
        assertTrue(subject.getAvatar1Url().isNullOrEmpty())
        assertTrue(subject.getAvatar2Url().isNullOrEmpty())
        assertTrue(subject.titleText.isNullOrEmpty())
        assertTrue(subject.friendsWatchingText.isNullOrEmpty())
    }

    private fun testBroadcasterHasServerTextToSupercedeAll() {
        assertTrue(subject.getLiveBadgeVisibility() == View.GONE)
        assertTrue(subject.getAvatar1Visibility() == View.GONE)
        assertTrue(subject.getAvatar2Visibility() == View.GONE)
        assertTrue(subject.getTitleViewVisibility() == View.VISIBLE)
        assertTrue(subject.getAvatar1Url().isNullOrEmpty())
        assertTrue(subject.getAvatar2Url().isNullOrEmpty())
        assertTrue(subject.titleText == topPick)
        assertFalse(subject.isTitleTextSmallMargin())
    }

    private fun testFollowers(isStage: Boolean, testMany: Boolean) {
        if (isStage) {
            assertTrue(subject.titleText == subject.friendsWatchingText)
        } else {
            assertTrue(subject.titleText == friendsWatchingText)
        }

        if (testMany) testManyFollowers() else testOnlyOneFollower()
    }

    private fun testOnlyOneFollower() {
        assertTrue(subject.getLiveBadgeVisibility() == View.GONE)
        assertTrue(subject.getAvatar1Visibility() == View.VISIBLE)
        assertTrue(subject.getAvatar2Visibility() == View.GONE)
        assertTrue(subject.getTitleViewVisibility() == View.VISIBLE)
        assertTrue(subject.getAvatar1Url() == imagesBaseUrl + "avatarImageUrl0")
        assertTrue(subject.getAvatar2Url().isNullOrEmpty())
        assertTrue(subject.isTitleTextSmallMargin())
    }

    private fun testManyFollowers() {
        assertTrue(subject.getLiveBadgeVisibility() == View.GONE)
        assertTrue(subject.getAvatar1Visibility() == View.VISIBLE)
        assertTrue(subject.getAvatar2Visibility() == View.VISIBLE)
        assertTrue(subject.getTitleViewVisibility() == View.VISIBLE)
        assertTrue(subject.getAvatar1Url() == imagesBaseUrl + "avatarImageUrl0")
        assertTrue(subject.getAvatar2Url() == imagesBaseUrl + "avatarImageUrl1")
        assertTrue(subject.isTitleTextSmallMargin())
    }

    private fun makeUser(avatarImageUrl: String) =
        User(
            "caid",
            "username",
            "name",
            "email",
            avatarImageUrl,
            100,
            100,
            false,
            false,
            "broadcastId",
            "stageId",
            mapOf(),
            mapOf(),
            21,
            "bio",
            "countryCode",
            "countryName",
            "gender",
            false,
            false,
            null,
            null,
            false,
            false
        )
}