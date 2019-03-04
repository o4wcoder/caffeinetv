package tv.caffeine.app.api

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import androidx.test.runner.AndroidJUnit4
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tv.caffeine.app.CaffeineApplication
import tv.caffeine.app.di.ASSETS_BASE_URL
import java.text.NumberFormat

@RunWith(AndroidJUnit4::class)
class TransactionHistoryItemTests {
    private val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
    private lateinit var resources: Resources
    private val username = "user"
    private val fontColor = ""
    private val numberFormat = NumberFormat.getNumberInstance()

    @Before
    fun setup() {
        resources = ApplicationProvider.getApplicationContext<CaffeineApplication>().resources
    }

    private fun parse(json: String): TransactionHistoryItem {
        val intermediate = gson.fromJson<HugeTransactionHistoryItem>(json, HugeTransactionHistoryItem::class.java)
        return intermediate.convert()!!
    }

    @Test
    fun `successful cash out`() {
        val json = """
            {
                "cost": 18182,
                "created_at": 1551420282,
                "id": "769589f7-3626-4738-b474-5fdb56176d9b",
                "state": "deposited",
                "type": "CashOut",
                "value": 20000
            }
        """
        val transaction = parse(json)
        val string = transaction.costString(resources, numberFormat, username, fontColor)
        assertEquals("Your cash out of <b>\$200.00</b> for <img src=\"purplecoin\"> <b>18,182</b> was successful", string)
    }

    @Test
    fun `pending cash out`() {
        val json = """
            {
                "cost": 18182,
                "created_at": 1551420282,
                "id": "769589f7-3626-4738-b474-5fdb56176d9b",
                "state": "pending",
                "type": "CashOut",
                "value": 20000
            }
        """
        val transaction = parse(json)
        val string = transaction.costString(resources, numberFormat, username, fontColor)
        assertEquals("Your cash out of <b>\$200.00</b> for <img src=\"purplecoin\"> <b>18,182</b> is pending", string)
    }

    @Test
    fun `failed cash out`() {
        val json = """
            {
                "cost": 18182,
                "created_at": 1551314691,
                "id": "b60f0557-15b7-4b11-b30f-f901f5407379",
                "state": "failed",
                "type": "CashOut",
                "value": 20000
            }
        """
        val transaction = parse(json)
        val string = transaction.costString(resources, numberFormat, username, fontColor)
        assertEquals("Your cash out of <b>\$200.00</b> for <img src=\"purplecoin\"> <b>18,182</b> failed", string)
    }

    @Test
    fun `send single digital item`() {
        val json = """
            {
                "assets": {
                    "preview_image_path": "/digital-items/wave_preview.08f6e074246e9723ed29782e6cc089b1.png",
                    "scene_kit_path": "/digital-items/wave.64fc3c5136253ad7ebd8f5e432926de2.zip",
                    "static_image_path": "/digital-items/wave.d5b73650198f9bd1b9883a1f4cf0fc77.png",
                    "web_asset_path": "/digital-items/wave.c45001c8292dcd368c6c5f09d7d24ee6.json"
                },
                "cost": 3,
                "created_at": 1551311444,
                "id": "8a219464-828f-4fb5-b353-552e91435cd6",
                "name": "Wave",
                "plural_name": "Waves",
                "quantity": 1,
                "recipient": "CAID403DFF440C3E420E8D4F83ACE8115432",
                "type": "SendDigitalItem",
                "value": 9
            }
        """
        val transaction = parse(json)
        val string = transaction.costString(resources, numberFormat, username, fontColor)
        assertEquals("You sent <img src=\"$ASSETS_BASE_URL/digital-items/wave.d5b73650198f9bd1b9883a1f4cf0fc77.png\"> for <img src=\"goldcoin\"> <b>3</b>, watching <font color=\"\"><b>user</b></font>", string)
    }

    @Test
    fun `send multiple digital items`() {
        val json = """
            {
                "assets": {
                    "preview_image_path": "/digital-items/hooray_preview.de1abb5a926184e6afaf9cfd81dd45b7.png",
                    "scene_kit_path": "/digital-items/hooray!.4bf1d1cf2cb4f411946f2c45af4d8f0b.zip",
                    "static_image_path": "/digital-items/hooray!.5b6bbc20d263c55031a1987cd0f3d39e.png",
                    "web_asset_path": "/digital-items/hooray!.06bb02067b6228b57f82b1def215bf3a.json"
                },
                "cost": 75,
                "created_at": 1551311563,
                "id": "9766276f-61ac-4b18-b56f-1bc8a9455da7",
                "name": "Hooray!",
                "plural_name": "Hoorays!",
                "quantity": 3,
                "recipient": "CAIDC92CFD723D314FA6AA86D8A22E5350B9",
                "type": "SendDigitalItem",
                "value": 225
            }
        """
        val transaction = parse(json)
        val string = transaction.costString(resources, numberFormat, username, fontColor)
        assertEquals("You sent <img src=\"$ASSETS_BASE_URL/digital-items/hooray!.5b6bbc20d263c55031a1987cd0f3d39e.png\"><b>x3</b> for <img src=\"goldcoin\"> <b>75</b>, watching <font color=\"\"><b>user</b></font>", string)
    }

    @Test
    fun `receive single digital item`() {
        val json = """
            {
                "assets": {
                    "preview_image_path": "/digital-items/donut_preview.31e13d8913c13f80ac0fc615dc57134a.png",
                    "scene_kit_path": "/digital-items/donut.0fbf8261da076b8df849f88b1f80f632.zip",
                    "static_image_path": "/digital-items/donut.d81956e8a2033c3d4c0117ebe306f913.png",
                    "web_asset_path": "/digital-items/donut.31e3c9ab8cb330646d4fc6f8ea3445f1.json"
                },
                "cost": 50,
                "created_at": 1551314233,
                "id": "1aa4ba35-8b43-41ac-a07c-33a7b7fcdd85",
                "name": "Donut",
                "plural_name": "Donuts",
                "quantity": 1,
                "sender": "CAIDD68D380006DE4BA4BA3B667AF1C7E34A",
                "type": "ReceiveDigitalItem",
                "value": 150
            }
        """
        val transaction = parse(json)
        val string = transaction.costString(resources, numberFormat, username, fontColor)
        assertEquals("<font color=\"\"><b>user</b></font> sent <img src=\"$ASSETS_BASE_URL/digital-items/donut.d81956e8a2033c3d4c0117ebe306f913.png\">, <b>Caffeine</b> awarded <img src=\"purplecoin\"> <b>150</b>", string)
    }

    @Test
    fun `receive multiple digital items`() {
        val json = """
            {
                "assets": {
                    "preview_image_path": "/digital-items/cheer_preview.48a41d9c91e95e45c07cfedd57c8f7a1.png",
                    "scene_kit_path": "/digital-items/cheer.a1ba46ac333980e7f5287dc23c91b871.zip",
                    "static_image_path": "/digital-items/cheer.6cce94649395a5d439404423c1b52b2a.png",
                    "web_asset_path": "/digital-items/cheer.e391506575d05760dbd1ea920c749772.json"
                },
                "cost": 100,
                "created_at": 1551314249,
                "id": "cfdfc6de-9620-4546-baa5-788ccef577fb",
                "name": "Cheer",
                "plural_name": "Cheers",
                "quantity": 4,
                "sender": "CAIDD68D380006DE4BA4BA3B667AF1C7E34A",
                "type": "ReceiveDigitalItem",
                "value": 300
            }
        """
        val transaction = parse(json)
        val string = transaction.costString(resources, numberFormat, username, fontColor)
        assertEquals("<font color=\"\"><b>user</b></font> sent <img src=\"$ASSETS_BASE_URL/digital-items/cheer.6cce94649395a5d439404423c1b52b2a.png\"><b>x4</b>, <b>Caffeine</b> awarded <img src=\"purplecoin\"> <b>300</b>", string)
    }

    @Test
    fun `purchase with dollars`() {
        val json = """
            {
                "bundle_id": "gold_0",
                "cost": 12300,
                "cost_currency_code": "USD",
                "created_at": 1551420218,
                "id": "c9e64d2b-c9dc-4a5e-9e0f-f17edfe960cf",
                "type": "Bundle",
                "value": 1864
            }
        """
        val transaction = parse(json)
        val string = transaction.costString(resources, numberFormat, username, fontColor)
        assertEquals("You purchased <img src=\"goldcoin\"> <b>1,864</b> for <b>\$123.00</b>", string)
    }

    @Test
    fun `purchase with credits`() {
        val json = """
            {
                "bundle_id": "gold_150",
                "cost": 900,
                "cost_currency_code": "CREDITS",
                "created_at": 1551392947,
                "id": "f404ebeb-5270-4360-845c-15e877824ab8",
                "type": "Bundle",
                "value": 150
            }
        """
        val transaction = parse(json)
        val string = transaction.costString(resources, numberFormat, username, fontColor)
        assertEquals("You redeemed <img src=\"purplecoin\"> <b>900</b> for <img src=\"goldcoin\"> <b>150</b>", string)
    }

    @Test
    fun `credit adjustment`() {
        val json = """
            {
                "created_at": 1551314598,
                "currency_code": "CREDITS",
                "id": "90c552d2-8575-4194-871b-99e81caaa739",
                "quantity": 12345,
                "type": "Adjustment"
            }
        """
        val transaction = parse(json)
        val string = transaction.costString(resources, numberFormat, username, fontColor)
        assertEquals("<b>Caffeine</b> awarded you <img src=\"purplecoin\"> <b>12,345</b>", string)
    }

    @Test
    fun `debit adjustment`() {
        val json = """
            {
                "created_at": 1551311895,
                "currency_code": "CREDITS",
                "id": "24568f11-3856-40df-b3c8-0d76a1da84b7",
                "quantity": -12,
                "type": "Adjustment"
            }
        """
        val transaction = parse(json)
        val string = transaction.costString(resources, numberFormat, username, fontColor)
        assertEquals("<b>Caffeine</b> debited you <img src=\"purplecoin\"> <b>12</b>", string)
    }

}
