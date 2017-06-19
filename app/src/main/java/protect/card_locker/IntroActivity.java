package protect.card_locker;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;


public class IntroActivity extends AppIntro
{
    @Override
    public void init(Bundle savedInstanceState)
    {
        addIntroSlide(R.layout.intro1_layout);
        addIntroSlide(R.layout.intro2_layout);
        addIntroSlide(R.layout.intro3_layout);
        addIntroSlide(R.layout.intro4_layout);
        addIntroSlide(R.layout.intro5_layout);
        addIntroSlide(R.layout.intro6_layout);
    }

    private void addIntroSlide(@LayoutRes int layout)
    {
        Fragment slide = new IntroSlide();
        Bundle args = new Bundle();
        args.putInt("layout", layout);
        slide.setArguments(args);
        addSlide(slide);
    }

    @Override
    public void onSkipPressed(Fragment fragment) {
        finish();
    }

    @Override
    public void onDonePressed(Fragment fragment) {
        finish();
    }
}


