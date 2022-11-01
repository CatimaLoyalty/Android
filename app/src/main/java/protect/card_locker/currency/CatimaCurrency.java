package protect.card_locker.currency;

import java.util.Currency;

import androidx.annotation.Nullable;

public class CatimaCurrency {

    private Currency mCurrency;
    private String mSpecialSymbol = "%";

    private static final String Points = "Points";
    private static final String Percentage = "Percentage";
    private static final String None = "";

    public CatimaCurrency(Currency currency, String currencySymbol) {
        mCurrency = currency;
        mSpecialSymbol = currencySymbol;
    }

    public CatimaCurrency fromCurrency(Currency currency) {
        if (currency == null) {
            mCurrency = null;
            mSpecialSymbol = CatimaCurrency.None;
        } else {
            mCurrency = currency;
            mSpecialSymbol = currency.getSymbol();
        }

        return this;
    }

    public CatimaCurrency fromCurrencyCode(String currencyCode) {
        //Points
        if (currencyCode == null) {
            mCurrency = null;
            mSpecialSymbol = CatimaCurrency.Points;
        } else if (currencyCode.equals("%")) {
            mCurrency = null;
            mSpecialSymbol = CatimaCurrency.Percentage;
        } else {
            mCurrency = Currency.getInstance(currencyCode);
            mSpecialSymbol = CatimaCurrency.None;
        }

        return this;
    }

    public int getDefaultFractionDigits() {
        if (mCurrency != null) {
            return mCurrency.getDefaultFractionDigits();
        } else if (mSpecialSymbol.equals(CatimaCurrency.Percentage)) {
            return 2;
        }
        return 0;
    }

    public String getSymbol() {
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
