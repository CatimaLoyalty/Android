package protect.card_locker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

public class CreditsDialog extends AppCompatDialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        SharedPreferences sp = requireActivity().getSharedPreferences("contributorPref", Context.MODE_PRIVATE);
        String str = sp.getString("contributors", "");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Our Contributors")
                .setMessage(str)
                .setPositiveButton("OK", (dialogInterface, i) -> {
                });
        return builder.create();
    }
}
