package protect.card_locker;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import org.acra.ACRA;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.DialogConfigurationBuilder;
import org.acra.config.MailSenderConfigurationBuilder;
import org.acra.data.StringFormat;

import protect.card_locker.preferences.Settings;

public class LoyaltyCardLockerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize crash reporter (if enabled)
        if (BuildConfig.useAcraCrashReporter) {
            ACRA.init(this, new CoreConfigurationBuilder()
                    //core configuration:
                    .withBuildConfigClass(BuildConfig.class)
                    .withReportFormat(StringFormat.KEY_VALUE_LIST)
                    .withPluginConfigurations(
                            new DialogConfigurationBuilder()
                                    .withText(String.format(getString(R.string.acra_catima_has_crashed), getString(R.string.app_name)))
                                    .withCommentPrompt(getString(R.string.acra_explain_crash))
                                    .withResTheme(R.style.AppTheme)
                                    .build(),
                            new MailSenderConfigurationBuilder()
                                    .withMailTo("acra-crash@catima.app")
                                    .withSubject(String.format(getString(R.string.acra_crash_email_subject), getString(R.string.app_name)))
                                    .build()
                    )
            );
        }

        // Set theme
        Settings settings = new Settings(this);
        AppCompatDelegate.setDefaultNightMode(settings.getTheme());
    }
}
