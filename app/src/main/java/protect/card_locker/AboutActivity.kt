package protect.card_locker;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

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
        copyright.setText(content.getCopyright());
        TextView versionHistory = binding.versionHistorySub;
        versionHistory.setText(content.getVersionHistory());

        binding.versionHistory.setTag("https://catima.app/changelog/");
        binding.translate.setTag("https://hosted.weblate.org/engage/catima/");
        binding.license.setTag("https://github.com/CatimaLoyalty/Android/blob/main/LICENSE");
        binding.repo.setTag("https://github.com/CatimaLoyalty/Android/");
        binding.privacy.setTag("https://catima.app/privacy-policy/");
        binding.reportError.setTag("https://github.com/CatimaLoyalty/Android/issues");
        binding.rate.setTag("https://play.google.com/store/apps/details?id=me.hackerchick.catima");

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
        View.OnClickListener openExternalBrowser = view -> {
            Object tag = view.getTag();
            if (tag instanceof String && ((String) tag).startsWith("https://")) {
                (new OpenWebLinkHandler()).openBrowser(this, (String) tag);
            }
        };
        binding.versionHistory.setOnClickListener(openExternalBrowser);
        binding.translate.setOnClickListener(openExternalBrowser);
        binding.license.setOnClickListener(openExternalBrowser);
        binding.repo.setOnClickListener(openExternalBrowser);
        binding.privacy.setOnClickListener(openExternalBrowser);
        binding.reportError.setOnClickListener(openExternalBrowser);
        binding.rate.setOnClickListener(openExternalBrowser);

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
        binding.credits.setOnClickListener(null);
    }

    private void showCredits() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.credits)
                .setMessage(content.getContributorInfo())
                .setPositiveButton(R.string.ok, null)
                .show();
    }
}
