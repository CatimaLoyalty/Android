package protect.card_locker;

public interface ParseResultListDisambiguatorCallback {
    void onUserChoseParseResult(ParseResult parseResult);
    void onUserDismissedSelector();
}
