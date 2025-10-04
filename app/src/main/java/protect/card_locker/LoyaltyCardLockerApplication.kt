package protect.card_locker

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import org.acra.ACRA
import org.acra.config.CoreConfigurationBuilder
import org.acra.config.DialogConfigurationBuilder
import org.acra.config.MailSenderConfigurationBuilder
import org.acra.data.StringFormat
import protect.card_locker.core.WidgetSettingsManager
import protect.card_locker.core.dataStore
import protect.card_locker.preferences.Settings


class LoyaltyCardLockerApplication : Application() {

    companion object {
        lateinit var settingsManager: WidgetSettingsManager
            private set
    }

    override fun onCreate() {
        super.onCreate()
        settingsManager = WidgetSettingsManager(dataStore)

        // Initialize crash reporter (if enabled)
        if (BuildConfig.useAcraCrashReporter) {
            ACRA.init(
                this, CoreConfigurationBuilder()
                    //core configuration:
                    .withBuildConfigClass(BuildConfig::class.java)
                    .withReportFormat(StringFormat.KEY_VALUE_LIST)
                    .withPluginConfigurations(
                        DialogConfigurationBuilder()
                            .withText(
                                String.format(
                                    getString(R.string.acra_catima_has_crashed),
                                    getString(R.string.app_name)
                                )
                            )
                            .withCommentPrompt(getString(R.string.acra_explain_crash))
                            .withResTheme(R.style.AppTheme)
                            .build(),
                        MailSenderConfigurationBuilder()
                            .withMailTo("acra-crash@catima.app")
                            .withSubject(
                                String.format(
                                    getString(R.string.acra_crash_email_subject),
                                    getString(R.string.app_name)
                                )
                            )
                            .build()
                    )
            )
        }

        // Set theme
        val settings = Settings(this)
        AppCompatDelegate.setDefaultNightMode(settings.theme)
    }

    // For tests
    fun setTestSettingsManager(manager: WidgetSettingsManager) {
        settingsManager = manager
    }
}