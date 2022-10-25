package protect.card_locker;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import protect.card_locker.async.TaskHandler;
import protect.card_locker.databinding.BarcodeLayoutBinding;

public class BarcodeSelectorAdapter extends ArrayAdapter<CatimaBarcodeWithValue> {
    private static final String TAG = "Catima";

    private final TaskHandler mTasks = new TaskHandler();
    private final BarcodeSelectorListener mListener;

    private static class ViewHolder {
        ImageView image;
        TextView text;
    }

    public interface BarcodeSelectorListener {
        void onRowClicked(int inputPosition, View view);
    }

    public BarcodeSelectorAdapter(Context context, ArrayList<CatimaBarcodeWithValue> barcodes, BarcodeSelectorListener barcodeSelectorListener) {
        super(context, 0, barcodes);
        mListener = barcodeSelectorListener;
    }

    public void setBarcodes(ArrayList<CatimaBarcodeWithValue> barcodes) {
        clear();
        addAll(barcodes);
        notifyDataSetChanged();
        mTasks.flushTaskList(TaskHandler.TYPE.BARCODE, true, false, false);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CatimaBarcodeWithValue catimaBarcodeWithValue = getItem(position);
        CatimaBarcode catimaBarcode = catimaBarcodeWithValue.catimaBarcode();
        String value = catimaBarcodeWithValue.value();

        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            BarcodeLayoutBinding barcodeLayoutBinding = BarcodeLayoutBinding.inflate(inflater, parent, false);
            convertView = barcodeLayoutBinding.getRoot();
            viewHolder.image = barcodeLayoutBinding.barcodeImage;
            viewHolder.text = barcodeLayoutBinding.barcodeName;
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        createBarcodeOption(viewHolder.image, catimaBarcode.format().name(), value, viewHolder.text);

        View finalConvertView = convertView;
        convertView.setOnClickListener(view -> mListener.onRowClicked(position, finalConvertView));

        return convertView;
    }

    public boolean isValid(View view) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        return viewHolder.image.getTag() != null && (boolean) viewHolder.image.getTag();
    }

    private void createBarcodeOption(final ImageView image, final String formatType, final String cardId, final TextView text) {
        final CatimaBarcode format = CatimaBarcode.fromName(formatType);

        image.setImageBitmap(null);
        image.setClipToOutline(true);

        if (image.getHeight() == 0) {
            // The size of the ImageView is not yet available as it has not
            // yet been drawn. Wait for it to be drawn so the size is available.
            image.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            Log.d(TAG, "Global layout finished, type: + " + formatType + ", width: " + image.getWidth());
                            image.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                            Log.d(TAG, "Generating barcode for type " + formatType);

                            BarcodeImageWriterTask barcodeWriter = new BarcodeImageWriterTask(getContext(), image, cardId, format, text, true, null, true);
                            mTasks.executeTask(TaskHandler.TYPE.BARCODE, barcodeWriter);
                        }
                    });
        } else {
            Log.d(TAG, "Generating barcode for type " + formatType);
            BarcodeImageWriterTask barcodeWriter = new BarcodeImageWriterTask(getContext(), image, cardId, format, text, true, null, true);
            mTasks.executeTask(TaskHandler.TYPE.BARCODE, barcodeWriter);
        }
    }
}
