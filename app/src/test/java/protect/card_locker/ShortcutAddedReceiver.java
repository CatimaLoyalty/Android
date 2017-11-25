package protect.card_locker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class ShortcutAddedReceiver extends BroadcastReceiver
{
    public static final String SHORTCUT_ADD_REQUEST = "com.android.launcher.action.INSTALL_SHORTCUT";

    private Intent _request = null;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        _request = intent;
    }

    public void registerReceiver(Context context)
    {
        context.registerReceiver(this, new IntentFilter(SHORTCUT_ADD_REQUEST));
    }

    public void unregisterReceiver(Context context)
    {
        context.unregisterReceiver(this);
    }

    public Intent lastRequest()
    {
        return _request;
    }

    public void reset()
    {
        _request = null;
    }
}
