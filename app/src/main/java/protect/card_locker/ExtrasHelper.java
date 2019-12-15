package protect.card_locker;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class ExtrasHelper {
    private HashMap<String, LinkedHashMap<String, String>> extras = new HashMap<>();

    public ExtrasHelper fromJSON(JSONObject json) throws JSONException
    {
        Iterator<String> languages = json.keys();

        while(languages.hasNext())
        {
            String language = languages.next();
            Iterator<String> keys = json.getJSONObject(language).keys();

            while(keys.hasNext())
            {
                String key = keys.next();
                addLanguageValue(language, key, json.getJSONObject(language).getString(key));
            }
        }

        return this;
    }

    public JSONObject toJSON() throws JSONException
    {
        return new JSONObject(extras);
    }

    public void addLanguageValue(String language, String key, String value)
    {
        if(!extras.containsKey(language))
        {
            extras.put(language, new LinkedHashMap<String, String>());
        }

        LinkedHashMap<String, String> values = extras.get(language);
        values.put(key, value);

        extras.put(language, values);
    }

    public HashMap<String, String> getAllValues(String language)
    {
        return extras.get(language);
    }
}
