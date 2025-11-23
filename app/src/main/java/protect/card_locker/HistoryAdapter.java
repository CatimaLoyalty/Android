package protect.card_locker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<HistoryEntity> historyItems;

    public HistoryAdapter(List<HistoryEntity> historyItems) {
        this.historyItems = historyItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryEntity historyItem = historyItems.get(position);

        // Set card name
        holder.tvCardName.setText(historyItem.cardName);

        // Set timestamp
        Calendar now = Calendar.getInstance();
        Calendar timestamp = Calendar.getInstance();
        timestamp.setTimeInMillis(historyItem.timestamp);

        // Reset time part for day comparison
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);

        timestamp.set(Calendar.HOUR_OF_DAY, 0);
        timestamp.set(Calendar.MINUTE, 0);
        timestamp.set(Calendar.SECOND, 0);
        timestamp.set(Calendar.MILLISECOND, 0);

        long diff = now.getTimeInMillis() - timestamp.getTimeInMillis();
        long days = diff / (24 * 60 * 60 * 1000);

        if (days == 0) {
            holder.tvDate.setText(R.string.today);
        } else if (days == 1) {
            holder.tvDate.setText(R.string.yesterday);
        } else if (days < 7) {
            holder.tvDate.setText(holder.itemView.getContext().getString(R.string.last_7_days));
        } else {
            holder.tvDate.setText(DateFormat.getDateInstance().format(historyItem.timestamp));
        }
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvCardName;
        public TextView tvDate;

        public ViewHolder(View itemView) {
            super(itemView);
            tvCardName = itemView.findViewById(R.id.tvCardName);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}
