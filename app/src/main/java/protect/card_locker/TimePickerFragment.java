package protect.card_locker;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    int hour,minute;
    Calendar calendar;
    Context context;
    View view;
    String am_pm;

    public TimePickerFragment(Context context ,View view) {

        this.context = context;
        this.view = view;

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        calendar = Calendar.getInstance();
        hour = calendar.get(Calendar.HOUR_OF_DAY);
        minute = calendar.get(Calendar.MINUTE);

        return new TimePickerDialog(getActivity(),this,hour,minute,false);
    }

    @Override
    public void onTimeSet(TimePicker timePicker, int hour, int minute) {

        calendar.set(Calendar.HOUR_OF_DAY,hour);
        calendar.set(Calendar.MINUTE,minute);



     if (hour >= 12){

         am_pm = "PM";

         if (hour>12) {
             hour -=12;
         }
     }
     else {
         am_pm = "AM";

         if (hour ==0) {

             hour = 12;
         }
         if (minute ==0) {

         }

     }


        Toast.makeText(context, "time is "+ hour+" : "+minute+" "+am_pm, Toast.LENGTH_SHORT).show();





    }
}
