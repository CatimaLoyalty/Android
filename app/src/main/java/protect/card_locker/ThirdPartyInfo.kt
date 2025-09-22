package protect.card_locker

class ThirdPartyInfo(
    private val mName: String,
    private val mUrl: String,
    private val mLicense: String
) {
    fun name(): String {
        return mName
    }

    fun url(): String {
        return mUrl
    }

    fun license(): String {
        return mLicense
    }

    fun toHtml(): String {
        return String.format("<a href=\"%s\">%s</a> (%s)", url(), name(), license())
    }
}
