package protect.card_locker

data class ThirdPartyInfo(
    val name: String,
    val url: String,
    val license: String
) {
    fun toHtml(): String {
        return "<a href=\"$url\">$name</a> ($license)"
    }
}