package protect.card_locker.currency;

import java.util.Currency;

import androidx.annotation.Nullable;

public class CatimaCurrency {

    private final Currency mCurrency;
    private final String mSpecialSymbol;

    public CatimaCurrency(Currency currency) {
        mCurrency = currency;
        mSpecialSymbol = null;
    }

    public CatimaCurrency(String currencySymbol) {
        mSpecialSymbol = currencySymbol;
        mCurrency = null;
    }

    public int getDefaultFractionDigits() {
        if (mCurrency != null) {
            return mCurrency.getDefaultFractionDigits();
        }

        return 0;
    }

    public String getSymbol() {
        if (mCurrency != null) {
            return mCurrency.getSymbol();
        }

        return mSpecialSymbol;
    }

    public String getCurrencyCode() {
        if (mCurrency != null) {
            return mCurrency.getCurrencyCode();
        }
        return mSpecialSymbol;
    }

    @Nullable
    public Currency getCurrency() {
        return mCurrency;
    }

}
