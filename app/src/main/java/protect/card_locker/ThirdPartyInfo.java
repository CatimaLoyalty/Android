package protect.card_locker;

public class ThirdPartyInfo {
    private final String mName;
    private final String mUrl;
    private final String mLicense;

    public ThirdPartyInfo(String name, String url, String license) {
        mName = name;
        mUrl = url;
        mLicense = license;
    }

    public String name() {
        return mName;
    }

    public String url() {
        return mUrl;
    }

    public String license() {
        return mLicense;
    }

    public String toHtml() {
        return String.format("<a href=\"%s\">%s</a> (%s)", url(), name(), license());
    }
}
