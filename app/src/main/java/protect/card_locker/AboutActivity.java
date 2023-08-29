package protect.card_locker;

import android.os.Bundle;
import android.text.Spanned;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;


import protect.card_locker.databinding.AboutActivityBinding;

public class AboutActivity extends CatimaAppCompatActivity {

    private static final String TAG = "Catima";

    private AboutActivityBinding binding;
    private AboutContent content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = AboutActivityBinding.inflate(getLayoutInflater());
        content = new AboutContent(this);
        setTitle(content.getPageTitle());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        enableToolbarBackButton();

        TextView copyright = binding.creditsSub;
        copyright.setText(content.getCopyrightShort());
        TextView versionHistory = binding.versionHistorySub;
        versionHistory.setText(content.getVersionHistory());

        binding.versionHistory.setTag("https://catima.app/changelog/");
        binding.translate.setTag("https://hosted.weblate.org/engage/catima/");
        binding.license.setTag("https://github.com/CatimaLoyalty/Android/blob/main/LICENSE");
        binding.repo.setTag("https://github.com/CatimaLoyalty/Android/");
        binding.privacy.setTag("https://catima.app/privacy-policy/");
        binding.reportError.setTag("https://github.com/CatimaLoyalty/Android/issues");
        binding.rate.setTag("https://play.google.com/store/apps/details?id=me.hackerchick.catima");
        binding.donate.setTag("https://catima.app/contribute/#donating");

        boolean installedFromGooglePlay = Utils.installedFromGooglePlay(this);
        // Hide Google Play rate button if not on Google Play
        binding.rate.setVisibility(installedFromGooglePlay ? View.VISIBLE : View.GONE);
        // Hide donate button on Google Play (Google Play doesn't allow donation links)
        binding.donate.setVisibility(installedFromGooglePlay ? View.GONE : View.VISIBLE);

        bindClickListeners();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        content.destroy();
        clearClickListeners();
        binding = null;
    }

    private void bindClickListeners() {
        binding.versionHistory.setOnClickListener(this::showHistory);
        binding.translate.setOnClickListener(this::openExternalBrowser);
        binding.license.setOnClickListener(this::showLicense);
        binding.repo.setOnClickListener(this::openExternalBrowser);
        binding.privacy.setOnClickListener(this::showPrivacy);
        binding.reportError.setOnClickListener(this::openExternalBrowser);
        binding.rate.setOnClickListener(this::openExternalBrowser);
        binding.donate.setOnClickListener(this::openExternalBrowser);

        binding.credits.setOnClickListener(view -> showCredits());
    }

    private void clearClickListeners() {
        binding.versionHistory.setOnClickListener(null);
        binding.translate.setOnClickListener(null);
        binding.license.setOnClickListener(null);
        binding.repo.setOnClickListener(null);
        binding.privacy.setOnClickListener(null);
        binding.reportError.setOnClickListener(null);
        binding.rate.setOnClickListener(null);
        binding.donate.setOnClickListener(null);

        binding.credits.setOnClickListener(null);
    }

    private void showCredits() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.credits)
                .setMessage(content.getContributorInfo())
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private void showHistory(View view) {
        showHTML(R.string.version_history, content.getHistoryInfo(), view);
    }

    private void showLicense(View view) {
        showHTML(R.string.license, content.getLicenseInfo(), view);
    }

    private void showPrivacy(View view) {
        showHTML(R.string.privacy_policy, content.getPrivacyInfo(), view);
    }

    private void showHTML(@StringRes int title, final Spanned text, View view) {
        int dialogContentPadding = getResources().getDimensionPixelSize(R.dimen.alert_dialog_content_padding);
        TextView textView = new TextView(this);
        textView.setText(text);
        Utils.makeTextViewLinksClickable(textView, text);
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(textView);
        scrollView.setPadding(dialogContentPadding, dialogContentPadding / 2, dialogContentPadding, 0);
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(scrollView)
                .setPositiveButton(R.string.ok, null)
                .setNeutralButton(R.string.view_online, (dialog, which) -> openExternalBrowser(view))
                .show();
    }

    private void openExternalBrowser(View view) {
        Object tag = view.getTag();
        if (tag instanceof String && ((String) tag).startsWith("https://")) {
            (new OpenWebLinkHandler()).openBrowser(this, (String) tag);
        }
    }
}
