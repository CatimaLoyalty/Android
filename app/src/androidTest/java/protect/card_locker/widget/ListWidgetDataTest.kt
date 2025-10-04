package protect.card_locker.widget

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.zxing.BarcodeFormat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import protect.card_locker.CatimaBarcode
import protect.card_locker.DBHelper
import protect.card_locker.Group
import protect.card_locker.ListWidget
import protect.card_locker.TestHelpers
import protect.card_locker.core.WidgetSettings
import java.math.BigDecimal
import java.util.Date

@RunWith(AndroidJUnit4::class)
class ListWidgetDataTest {

    private lateinit var context: Context
    private lateinit var dbHelper: DBHelper
    private val listWidget = ListWidget() // Instance to call the function on

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Use a test-specific database name and delete it before each test
        context.deleteDatabase("test_catima.db")
        dbHelper = TestHelpers.getEmptyDb(context)

        // ARRANGE: Populate the database with predictable test data
        populateTestData()
    }

    @After
    fun tearDown() {
        dbHelper.close()
    }

    @Test
    fun testFilter_showsOnlyStarredCards() {
        // ARRANGE: Create settings to filter for only starred cards
        val settings = WidgetSettings(starFilter = true)

        // ACT: Call the function we are testing
        val result = listWidget.loadAndFilterCards(context, settings)

        // ASSERT: Verify the result is correct
        assertThat(result).hasSize(2)
        assertThat(result.all { it.starStatus == 1 }).isTrue()
    }

    @Test
    fun testFilter_showsOnlyUnarchivedCards() {
        // ARRANGE: Create settings for unarchived cards
        val settings = WidgetSettings(archiveFilter = DBHelper.LoyaltyCardArchiveFilter.Unarchived)

        // ACT
        val result = listWidget.loadAndFilterCards(context, settings)

        // ASSERT
        assertThat(result).hasSize(2)
        assertThat(result.all { it.archiveStatus == 0 }).isTrue()
    }

    @Test
    fun testSorting_sortsByStoreNameInAscOrder() {
        // ARRANGE: Create settings for alphabetical sorting in Ascending order
        // unarchived starred card comes first
        val settings = WidgetSettings(sortOrder = DBHelper.LoyaltyCardOrder.Alpha)

        // ACT
        val result = listWidget.loadAndFilterCards(context, settings)

        // ASSERT: Use Truth's utility to check if the list is ordered
        assertThat(result[0].store).isEqualTo("Super Store")
        assertThat(result[1].store).isEqualTo("Corner Shop")
        assertThat(result[2].store).isEqualTo("Mega Mart")
        assertThat(result[3].store).isEqualTo("Archived Card")
    }

    @Test
    fun testSorting_sortsByStoreNameInDescOrder() {
        // ARRANGE: Create settings for alphabetical sorting in Descending order
        val settings = WidgetSettings(
            sortOrder = DBHelper.LoyaltyCardOrder.Alpha,
            sortOrderDirection = DBHelper.LoyaltyCardOrderDirection.Descending
        )

        // ACT
        val result = listWidget.loadAndFilterCards(context, settings)

        // ASSERT: Use Truth's utility to check if the list is ordered
        assertThat(result.first().store).isEqualTo("Super Store") // 'A' comes first
    }

    @Test
    fun testFilter_GroupCardWithStarredFilter() {
        // ARRANGE: Create settings for alphabetical sorting
        val settings = WidgetSettings(group = "one", starFilter = true)

        // ACT
        val result = listWidget.loadAndFilterCards(context, settings)

        // ASSERT: Use Truth's utility to check if the list is ordered
        assertThat(result.size).isEqualTo(1)
        assertThat(result.first().store).isEqualTo("Super Store") // 'A' comes first
    }

    @Test
    fun testFilter_GroupCardWithStarredAndArchivedFilter() {
        // ARRANGE: Create settings for alphabetical sorting
        val settings = WidgetSettings(
            group = "two",
            starFilter = true,
            archiveFilter = DBHelper.LoyaltyCardArchiveFilter.Archived
        )

        // ACT
        val result = listWidget.loadAndFilterCards(context, settings)

        // ASSERT: Use Truth's utility to check if the list is ordered
        assertThat(result.size).isEqualTo(1)
        assertThat(result.first().store).isEqualTo("Mega Mart") // 'A' comes first
    }

    @Test
    fun testFilter_ParticularGroupCard() {
        // ARRANGE: Third group has 2 cards, one starred and one not
        val settings = WidgetSettings(group = "third")

        // ACT
        val result = listWidget.loadAndFilterCards(context, settings)

        // ASSERT: Use Truth's utility to check if the list is ordered
        assertThat(result.size).isEqualTo(2)

        assertThat(result[0].store).isEqualTo("Corner Shop")
        assertThat(result[0].archiveStatus).isEqualTo(0)
        assertThat(result[0].starStatus).isEqualTo(0)

        assertThat(result[1].store).isEqualTo("Archived Card")
        assertThat(result[1].archiveStatus).isEqualTo(1)
        assertThat(result[1].starStatus).isEqualTo(0)
    }

    @Test
    fun testFilter_validFromFilterInAscendingOrder() {
        // ARRANGE:
        // Order should be by store name:
        // "Mega Mart" -> "Archived Card" -> "Corner Shop" -> "Super Store"
        val settings = WidgetSettings(sortOrder = DBHelper.LoyaltyCardOrder.ValidFrom)

        // ACT
        val result = listWidget.loadAndFilterCards(context, settings)

        // ASSERT: Use Truth's utility to check if the list is ordered
        assertThat(result.size).isEqualTo(4)

        assertThat(result[0].store).isEqualTo("Super Store")
        assertThat(result[1].store).isEqualTo("Corner Shop")
        assertThat(result[2].store).isEqualTo("Mega Mart")
        assertThat(result[3].store).isEqualTo("Archived Card")
    }

    @Test
    fun testFilter_validFromFilterInDescendingOrder() {
        // ARRANGE:
        // Order should be by store name:
        // "Super Store" -> "Corner Shop" -> "Archived Card" -> "Mega Mart"
        val settings = WidgetSettings(
            sortOrder = DBHelper.LoyaltyCardOrder.ValidFrom,
            sortOrderDirection = DBHelper.LoyaltyCardOrderDirection.Descending
        )

        // ACT
        val result = listWidget.loadAndFilterCards(context, settings)

        // ASSERT: Use Truth's utility to check if the list is ordered
        assertThat(result.size).isEqualTo(4)

        assertThat(result[0].store).isEqualTo("Super Store")
        assertThat(result[1].store).isEqualTo("Corner Shop")
        assertThat(result[2].store).isEqualTo("Mega Mart")
        assertThat(result[3].store).isEqualTo("Archived Card")
    }

    @Test
    fun testFilter_expiryFilterInAscendingOrder() {
        // ARRANGE:
        // Order should be by store name:
        // "Mega Mart" -> "Super Store" -> "Corner Shop" -> "Archived Card"
        val settings = WidgetSettings(sortOrder = DBHelper.LoyaltyCardOrder.Expiry)

        // ACT
        val result = listWidget.loadAndFilterCards(context, settings)

        // ASSERT: Use Truth's utility to check if the list is ordered
        assertThat(result.size).isEqualTo(4)

        assertThat(result[0].store).isEqualTo("Super Store")
        assertThat(result[1].store).isEqualTo("Corner Shop")
        assertThat(result[2].store).isEqualTo("Mega Mart")
        assertThat(result[3].store).isEqualTo("Archived Card")
    }

    @Test
    fun testFilter_expiryFilterInDescendingOrder() {
        // ARRANGE:
        // Order should be by store name:
        // "Super Store" -> "Corner Shop" -> "Archived Card" -> "Mega Mart"
        val settings = WidgetSettings(
            sortOrder = DBHelper.LoyaltyCardOrder.Expiry,
            sortOrderDirection = DBHelper.LoyaltyCardOrderDirection.Descending
        )

        // ACT
        val result = listWidget.loadAndFilterCards(context, settings)

        // ASSERT: Use Truth's utility to check if the list is ordered
        assertThat(result.size).isEqualTo(4)

        assertThat(result[0].store).isEqualTo("Super Store")
        assertThat(result[1].store).isEqualTo("Corner Shop")
        assertThat(result[2].store).isEqualTo("Mega Mart")
        assertThat(result[3].store).isEqualTo("Archived Card")
    }

    @Test
    fun testFilter_validFromFilterInAscendingOrderFromParticularGroup() {
        // ARRANGE:
        // "Corner Shop" -> "Super Store"
        val settings = WidgetSettings(
            sortOrder = DBHelper.LoyaltyCardOrder.ValidFrom,
            group = "third"
        )

        // ACT
        val result = listWidget.loadAndFilterCards(context, settings)

        // ASSERT: Use Truth's utility to check if the list is ordered
        assertThat(result.size).isEqualTo(2)

        assertThat(result[0].store).isEqualTo("Corner Shop")
        assertThat(result[1].store).isEqualTo("Archived Card")
    }

    @Test
    fun testFilter_expiryFilterInAscendingOrderFromParticularGroup() {
        // ARRANGE:
        // "Super Store" -> "Corner Shop"
        val settings = WidgetSettings(
            sortOrder = DBHelper.LoyaltyCardOrder.Expiry,
            group = "third"
        )

        // ACT
        val result = listWidget.loadAndFilterCards(context, settings)

        // ASSERT: Use Truth's utility to check if the list is ordered
        assertThat(result.size).isEqualTo(2)

        assertThat(result[0].store).isEqualTo("Corner Shop")
        assertThat(result[1].store).isEqualTo("Archived Card")
    }

    private fun populateTestData() {
        // This is a simplified example. You would use your app's real methods
        // to insert these cards into the test database.
        val database = dbHelper.writableDatabase

        val date1 = Date(1664891088000) // 4 Oct 2022 01:44:48 PM
        val date2 = Date(1759575846000) // 4 Oct 2025 11:04:06 PM
        val date3 = Date(1696427088000) // 4 Oct 2023 01:44:48 PM
        val date4 = Date(1728049488000) // 4 Oct 2024 01:44:48 PM
        val date5 = Date(1762263888000) // 4 Nov 2025 01:44:48 PM

        // Card 1: Starred, Unarchived, Group A
        val card1 = DBHelper.insertLoyaltyCard(
            database,
            "Super Store",
            "Note 1",
            date2,
            date5,
            BigDecimal(0),
            null,
            "cardId1",
            null,
            CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A),
            null,
            1,
            0,
            0
        )

        // Card 2: Starred, Archived, Group A
        val card2 = DBHelper.insertLoyaltyCard(
            database,
            "Mega Mart",
            "Note 2",
            date1,
            date4,
            BigDecimal(0),
            null,
            "cardId2",
            null,
            CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A),
            null,
            1,
            0,
            1
        )

        // Card 3: Not Starred, Unarchived, Group B
        val card3 = DBHelper.insertLoyaltyCard(
            database,
            "Corner Shop",
            "Note 3",
            date4,
            date5,
            BigDecimal(0),
            null,
            "cardId3",
            null,
            CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A),
            null,
            0,
            0,
            0
        )

        // Card 4: Not Starred, Archived, No Group
        val card4 = DBHelper.insertLoyaltyCard(
            database,
            "Archived Card",
            "Note 4",
            date3,
            date5,
            BigDecimal(0),
            null,
            "cardId4",
            null,
            CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A),
            null,
            0,
            0,
            1
        )

        // Insert 1st group
        DBHelper.insertGroup(database, "one")
        // Get the group
        val group1 = DBHelper.getGroup(database, "one")
        // Add card1 to group one
        DBHelper.setLoyaltyCardGroups(database, card1.toInt(), listOf(group1))

        // Insert 2nd group
        DBHelper.insertGroup(database, "two")
        // Get the group
        val group2 = DBHelper.getGroup(database, "two")
        // add 2nd card into 2nd group
        DBHelper.setLoyaltyCardGroups(database, card2.toInt(), listOf(group2))

        // Insert 3rd group
        DBHelper.insertGroup(database, "third")
        // Get the group
        val group3 = DBHelper.getGroup(database, "third")
        // add 1st and 3rd card into 3rd group
        DBHelper.setLoyaltyCardGroups(database, card3.toInt(), listOf(group3))
        DBHelper.setLoyaltyCardGroups(database, card4.toInt(), listOf(group3))
    }
}