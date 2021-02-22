package protect.card_locker;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class UtilsTest
{
    @Test
    public void parseBalances()
    {
        assertEquals("1000.00", Utils.parseCurrency("1.000.00").toPlainString());
        assertEquals("1000.50", Utils.parseCurrency("1.000.50").toPlainString());
        assertEquals("1000.50", Utils.parseCurrency("1.000,50").toPlainString());
        assertEquals("1000.50", Utils.parseCurrency("1,000,50").toPlainString());
        assertEquals("1000.50", Utils.parseCurrency("1,000.50").toPlainString());
        assertEquals("1000", Utils.parseCurrency("1000").toPlainString());
        assertEquals("995", Utils.parseCurrency("995").toPlainString());
        assertEquals("9.95", Utils.parseCurrency("9.95").toPlainString());
        assertEquals("9.95", Utils.parseCurrency("9,95").toPlainString());

        assertThrows(NumberFormatException.class, () -> Utils.parseCurrency(""));
        assertThrows(NumberFormatException.class, () -> Utils.parseCurrency("."));
        assertThrows(NumberFormatException.class, () -> Utils.parseCurrency(","));
        assertThrows(NumberFormatException.class, () -> Utils.parseCurrency(".,"));
        assertThrows(NumberFormatException.class, () -> Utils.parseCurrency("a"));
        assertThrows(NumberFormatException.class, () -> Utils.parseCurrency("......................"));
    }
}