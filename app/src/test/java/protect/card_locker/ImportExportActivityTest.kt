package protect.card_locker

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import protect.card_locker.compose.theme.CatimaTheme
import protect.card_locker.importexport.DataFormat

@RunWith(RobolectricTestRunner::class)
class ImportExportActivityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun registerIntentHandler(handler: String) {
        // Add something that will 'handle' the given intent type
        val packageManager = RuntimeEnvironment.getApplication().packageManager

        val info = ResolveInfo().apply {
            isDefault = true
            activityInfo = ActivityInfo().apply {
                applicationInfo = ApplicationInfo().apply {
                    packageName = "does.not.matter"
                }
                name = "DoesNotMatter"
                exported = true
            }
        }

        val intent = Intent(handler)

        if (handler == Intent.ACTION_GET_CONTENT) {
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
        }

        shadowOf(packageManager).addResolveInfoForIntent(intent, info)
    }

    @Test
    fun testImportExportScreenDisplaysAllOptions() {
        registerIntentHandler(Intent.ACTION_PICK)
        registerIntentHandler(Intent.ACTION_GET_CONTENT)

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val importOptions = listOf(
            ImportOption(
                title = context.getString(R.string.importCatima),
                message = context.getString(R.string.importCatimaMessage),
                dataFormat = DataFormat.Catima
            ),
            ImportOption(
                title = "Fidme",
                message = context.getString(R.string.importFidmeMessage),
                dataFormat = DataFormat.Fidme,
                isBeta = true
            )
        )

        composeTestRule.setContent {
            CatimaTheme {
                ImportExportScreen(
                    onBackPressedDispatcher = null,
                    importOptions = importOptions,
                    dialogState = ImportExportDialogState.None,
                    onDialogStateChange = {},
                    onExportWithPassword = {},
                    onImportSelected = {},
                    onImportWithPassword = { _, _ -> },
                    onShareExport = {}
                )
            }
        }

        // Verify export section is displayed (exportName appears as title and button text)
        composeTestRule
            .onAllNodesWithText(context.getString(R.string.exportName))
            .assertCountEquals(2)

        // Verify import section is displayed
        composeTestRule
            .onNodeWithText(context.getString(R.string.importOptionFilesystemTitle))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.importOptionFilesystemButton))
            .assertIsDisplayed()
    }

    @Test
    fun testImportTypeSelectionDialogDisplaysOptions() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val importOptions = listOf(
            ImportOption(
                title = "Catima",
                message = "Import from Catima",
                dataFormat = DataFormat.Catima
            ),
            ImportOption(
                title = "Fidme",
                message = "Import from Fidme",
                dataFormat = DataFormat.Fidme,
                isBeta = true
            )
        )

        composeTestRule.setContent {
            CatimaTheme {
                ImportExportScreen(
                    onBackPressedDispatcher = null,
                    importOptions = importOptions,
                    dialogState = ImportExportDialogState.ImportTypeSelection,
                    onDialogStateChange = {},
                    onExportWithPassword = {},
                    onImportSelected = {},
                    onImportWithPassword = { _, _ -> },
                    onShareExport = {}
                )
            }
        }

        // Verify import type selection dialog is displayed
        composeTestRule
            .onNodeWithText(context.getString(R.string.chooseImportType))
            .assertIsDisplayed()

        // Verify options are shown
        composeTestRule
            .onNodeWithText("Catima")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Fidme (BETA)")
            .assertIsDisplayed()
    }

    @Test
    fun testExportPasswordDialogDisplayed() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val importOptions = emptyList<ImportOption>()

        composeTestRule.setContent {
            CatimaTheme {
                ImportExportScreen(
                    onBackPressedDispatcher = null,
                    importOptions = importOptions,
                    dialogState = ImportExportDialogState.ExportPassword,
                    onDialogStateChange = {},
                    onExportWithPassword = {},
                    onImportSelected = {},
                    onImportWithPassword = { _, _ -> },
                    onShareExport = {}
                )
            }
        }

        // Verify export password dialog is displayed
        composeTestRule
            .onNodeWithText(context.getString(R.string.exportPassword))
            .assertIsDisplayed()
    }
}