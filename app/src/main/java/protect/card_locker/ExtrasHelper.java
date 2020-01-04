package protect.card_locker;

import androidx.annotation.NonNull;

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

    @NonNull
    public LinkedHashMap<String, String> getAllValues(String language)
    {
        return getAllValues(new String[]{language});
    }

    @NonNull
    public LinkedHashMap<String, String> getAllValues(String[] languages)
    {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();

        // Get least preferred language (last in list) first
        // Then go further and further to the start of the list (preferred language)
        for(int i = (languages.length - 1); i >= 0; i--)
        {
            LinkedHashMap<String, String> languageValues = extras.get(languages[i]);

            if(languageValues != null)
            {
                values.putAll(languageValues);
            }
        }

        return values;
    }
}
