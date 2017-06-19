package protect.card_locker;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.res.builder.RobolectricPackageManager;

import static org.robolectric.Shadows.shadowOf;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23)
public class ImportExportActivityTest
{
    private void registerIntentHandler(String handler)
    {
        // Add something that will 'handle' the given intent type
        RobolectricPackageManager packageManager = shadowOf(RuntimeEnvironment.application.getPackageManager());

        ResolveInfo info = new ResolveInfo();
        info.isDefault = true;

        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.packageName = "does.not.matter";
        info.activityInfo = new ActivityInfo();
        info.activityInfo.applicationInfo = applicationInfo;
        info.activityInfo.name = "DoesNotMatter";
        info.activityInfo.exported = true;

        Intent intent = new Intent(handler);
        if(handler.equals(Intent.ACTION_PICK))
        {
            intent.setData(Uri.parse("file://"));
        }

        if(handler.equals(Intent.ACTION_GET_CONTENT))
        {
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
        }

        packageManager.addResolveInfoForIntent(intent, info);
    }

    private void checkVisibility(Activity activity, int state, int divider, int title, int message, int button)
    {
        View dividerView = activity.findViewById(divider);
        View titleView = activity.findViewById(title);
        View messageView = activity.findViewById(message);
        View buttonView = activity.findViewById(button);

        assertEquals(state, dividerView.getVisibility());
        assertEquals(state, titleView.getVisibility());
        assertEquals(state, messageView.getVisibility());
        assertEquals(state, buttonView.getVisibility());
    }

    @Test
    public void testImportFilesystemOption()
    {
        for(boolean isInstalled : new Boolean[]{false, true})
        {
            int visibility = isInstalled ? View.VISIBLE : View.GONE;

            if(isInstalled)
            {
                registerIntentHandler(Intent.ACTION_PICK);
            }

            Activity activity = Robolectric.setupActivity(ImportExportActivity.class);

            checkVisibility(activity, visibility, R.id.dividerImportFilesystem,
                    R.id.importOptionFilesystemTitle, R.id.importOptionFilesystemExplanation,
                    R.id.importOptionFilesystemButton);

            // Should always be gone, as its provider is never installed
            checkVisibility(activity, View.GONE, R.id.dividerImportApplication,
                    R.id.importOptionApplicationTitle, R.id.importOptionApplicationExplanation,
                    R.id.importOptionApplicationButton);

            // Import from file system should always be present

            checkVisibility(activity, View.VISIBLE, R.id.dividerImportFixed,
                    R.id.importOptionFixedTitle, R.id.importOptionFixedExplanation,
                    R.id.importOptionFixedButton);
        }
    }

    @Test
    public void testImportApplicationOption()
    {
        for(boolean isInstalled : new Boolean[]{false, true})
        {
            int visibility = isInstalled ? View.VISIBLE : View.GONE;

            if(isInstalled)
            {
                registerIntentHandler(Intent.ACTION_GET_CONTENT);
            }

            Activity activity = Robolectric.setupActivity(ImportExportActivity.class);

            checkVisibility(activity, visibility, R.id.dividerImportApplication,
                    R.id.importOptionApplicationTitle, R.id.importOptionApplicationExplanation,
                    R.id.importOptionApplicationButton);

            // Should always be gone, as its provider is never installed
            checkVisibility(activity, View.GONE, R.id.dividerImportFilesystem,
                    R.id.importOptionFilesystemTitle, R.id.importOptionFilesystemExplanation,
                    R.id.importOptionFilesystemButton);

            // Import from file system should always be present

            checkVisibility(activity, View.VISIBLE, R.id.dividerImportFixed,
                    R.id.importOptionFixedTitle, R.id.importOptionFixedExplanation,
                    R.id.importOptionFixedButton);
        }
    }

    @Test
    public void testAllOptionsAvailable()
    {
        registerIntentHandler(Intent.ACTION_PICK);
        registerIntentHandler(Intent.ACTION_GET_CONTENT);

        Activity activity = Robolectric.setupActivity(ImportExportActivity.class);

        checkVisibility(activity, View.VISIBLE, R.id.dividerImportApplication,
                R.id.importOptionApplicationTitle, R.id.importOptionApplicationExplanation,
                R.id.importOptionApplicationButton);

        checkVisibility(activity, View.VISIBLE, R.id.dividerImportFilesystem,
                R.id.importOptionFilesystemTitle, R.id.importOptionFilesystemExplanation,
                R.id.importOptionFilesystemButton);

        checkVisibility(activity, View.VISIBLE, R.id.dividerImportFixed,
                R.id.importOptionFixedTitle, R.id.importOptionFixedExplanation,
                R.id.importOptionFixedButton);
    }
}
