package protect.card_locker.ui.models

data class ThirdPartyInfo(
    val name: String,
    val url: String,
    val license: String
) {

    fun toHtml(): String {
        return String.format("<a href=\"%s\">%s</a> (%s)", url, name, license)
    }
}
