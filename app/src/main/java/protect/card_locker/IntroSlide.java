package protect.card_locker;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class IntroSlide extends Fragment
{
    int _layout;

    @Override
    public void setArguments(Bundle bundle)
    {
        _layout = bundle.getInt("layout");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(_layout, container, false);
        return v;
    }
}
