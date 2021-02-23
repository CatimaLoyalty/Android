package protect.card_locker;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.Util;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class UtilsTest
{
    @Test
    public void parseBalances()
    {
        assertEquals("1", Utils.parseCurrency("1", false).toPlainString());

        assertEquals("1", Utils.parseCurrency("1", true).toPlainString());
        assertEquals("1.00", Utils.parseCurrency("1.00", true).toPlainString());
        assertEquals("1.00", Utils.parseCurrency("1,00", true).toPlainString());

        assertEquals("25", Utils.parseCurrency("2.5", false).toPlainString());
        assertEquals("25", Utils.parseCurrency("2,5", false).toPlainString());
        assertEquals("205", Utils.parseCurrency("2.05", false).toPlainString());
        assertEquals("205", Utils.parseCurrency("2,05", false).toPlainString());

        assertEquals("2.5", Utils.parseCurrency("2.5", true).toPlainString());
        assertEquals("2.5", Utils.parseCurrency("2,5", true).toPlainString());
        assertEquals("2.05", Utils.parseCurrency("2.05", true).toPlainString());
        assertEquals("2.05", Utils.parseCurrency("2,05", true).toPlainString());
        assertEquals("2.50", Utils.parseCurrency("2.50", true).toPlainString());
        assertEquals("2.50", Utils.parseCurrency("2,50", true).toPlainString());

        assertEquals("995", Utils.parseCurrency("9.95", false).toPlainString());
        assertEquals("995", Utils.parseCurrency("9,95", false).toPlainString());

        assertEquals("9.95", Utils.parseCurrency("9.95", true).toPlainString());
        assertEquals("9.95", Utils.parseCurrency("9,95", true).toPlainString());

        assertEquals("1234", Utils.parseCurrency("1234", false).toPlainString());
        assertEquals("1234", Utils.parseCurrency("1.234", false).toPlainString());
        assertEquals("1234", Utils.parseCurrency("1,234", false).toPlainString());

        assertEquals("1234", Utils.parseCurrency("1234", true).toPlainString());
        assertEquals("1234.00", Utils.parseCurrency("1234.00", true).toPlainString());
        assertEquals("1234.00", Utils.parseCurrency("1.234.00", true).toPlainString());
        assertEquals("1234.00", Utils.parseCurrency("1.234,00", true).toPlainString());
        assertEquals("1234.00", Utils.parseCurrency("1,234.00", true).toPlainString());
        assertEquals("1234.00", Utils.parseCurrency("1,234,00", true).toPlainString());
    }

    @Test
    public void formatBalances()
    {
        Currency euro = Currency.getInstance("EUR");

        assertEquals("1", Utils.formatBalanceWithoutCurrencySymbol(new BigDecimal("1"), null));
        assertEquals("1.00", Utils.formatBalanceWithoutCurrencySymbol(new BigDecimal("1"), euro));

        assertEquals("25", Utils.formatBalanceWithoutCurrencySymbol(new BigDecimal("25"), null));
        assertEquals("25.00", Utils.formatBalanceWithoutCurrencySymbol(new BigDecimal("25"), euro));

        assertEquals("2", Utils.formatBalanceWithoutCurrencySymbol(new BigDecimal("2.5"), null));
        assertEquals("2.50", Utils.formatBalanceWithoutCurrencySymbol(new BigDecimal("2.5"), euro));

        assertEquals("2", Utils.formatBalanceWithoutCurrencySymbol(new BigDecimal("2.05"), null));
        assertEquals("2.05", Utils.formatBalanceWithoutCurrencySymbol(new BigDecimal("2.05"), euro));

        assertEquals("2", Utils.formatBalanceWithoutCurrencySymbol(new BigDecimal("2.50"), null));
        assertEquals("2.50", Utils.formatBalanceWithoutCurrencySymbol(new BigDecimal("2.50"), euro));

        assertEquals("995", Utils.formatBalanceWithoutCurrencySymbol(new BigDecimal("995"), null));
        assertEquals("995.00", Utils.formatBalanceWithoutCurrencySymbol(new BigDecimal("995"), euro));

        assertEquals("10", Utils.formatBalanceWithoutCurrencySymbol(new BigDecimal("9.95"), null));
        assertEquals("9.95", Utils.formatBalanceWithoutCurrencySymbol(new BigDecimal("9.95"), euro));

        assertEquals("1,234", Utils.formatBalanceWithoutCurrencySymbol(new BigDecimal("1234"), null));
        assertEquals("1,234.00", Utils.formatBalanceWithoutCurrencySymbol(new BigDecimal("1234"), euro));

        assertEquals("1,234", Utils.formatBalanceWithoutCurrencySymbol(new BigDecimal("1234.00"), null));
        assertEquals("1,234.00", Utils.formatBalanceWithoutCurrencySymbol(new BigDecimal("1234.00"), euro));
    }
}