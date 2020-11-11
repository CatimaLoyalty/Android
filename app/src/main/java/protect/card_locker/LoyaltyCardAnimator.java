package protect.card_locker;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.view.View;

public class LoyaltyCardAnimator {

    private static AnimatorSet selectedViewIn, defaultViewOut, selectedViewOut, defaultViewIn;

    public static void flipView(Context inputContext, final View inputSelectedView, final View inputDefaultView, boolean inputItemSelected) {

        selectedViewIn = (AnimatorSet) AnimatorInflater.loadAnimator(inputContext, R.animator.flip_left_in);
        defaultViewOut = (AnimatorSet) AnimatorInflater.loadAnimator(inputContext, R.animator.flip_right_out);
        selectedViewOut = (AnimatorSet) AnimatorInflater.loadAnimator(inputContext, R.animator.flip_left_out);
        defaultViewIn = (AnimatorSet) AnimatorInflater.loadAnimator(inputContext, R.animator.flip_right_in);

        final AnimatorSet showFrontAnim = new AnimatorSet();
        final AnimatorSet showBackAnim = new AnimatorSet();

        selectedViewIn.setTarget(inputSelectedView);
        defaultViewOut.setTarget(inputDefaultView);
        showFrontAnim.playTogether(selectedViewIn, defaultViewOut);

        selectedViewOut.setTarget(inputSelectedView);
        defaultViewIn.setTarget(inputDefaultView);
        showBackAnim.playTogether(defaultViewIn, selectedViewOut);

        if (inputItemSelected) {
            showFrontAnim.start();
        } else {
            showBackAnim.start();
        }
    }

}
