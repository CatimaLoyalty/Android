package protect.card_locker;

import com.google.zxing.BarcodeFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Companies {
    static HashMap<String, List<Company>> companies = new HashMap<>();

    public List<Company> getByISOCode(String isoCode) {
        switch (isoCode) {
            case "NL":
                return NLCompanies();
            default:
                throw new IllegalArgumentException(isoCode + " is not supported");
        }
    }

    public List<Company> NLCompanies() {
        List<Company> comps = new ArrayList<>();

        if (!companies.containsKey("NL")) {
            comps.add(new Company.Builder("Albert Heijn").addBarcodeFormat(BarcodeFormat.EAN_13).create());
            comps.add(new Company.Builder("Air Miles").addBarcodeFormat(BarcodeFormat.EAN_13).addPrefix("470").create());
            comps.add(new Company.Builder("HEMA").addBarcodeFormat(BarcodeFormat.QR_CODE).create());
            companies.put("NL", comps);
        }

        return comps;
    };
}
