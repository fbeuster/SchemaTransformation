package schemaTransformation.capsules;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by Felix on 15.07.2016.
 */
public class Config {

    private HashMap<String, Object> config;
    private HashMap<String, Object> defaults;

    public Config() {
        try {
            InputStream io = new FileInputStream(new File("config.yaml"));
            Yaml yaml = new Yaml();
            config = (HashMap) yaml.load(io);

        } catch (FileNotFoundException e) {
            config = new HashMap<>();
        }

        setDefaults();
    }

    public Object get(String key) {
        String[] keys   = key.split("\\.");
        HashMap map     = config;
        Object value    = null;

        for (String k : keys) {
            if (map == null) {
                break;
            }

            if (keys[keys.length - 1].equals(k)) {
                value = map.get(k);

            } else {
                map = (HashMap) map.get(k);
            }
        }

        if (value == null || map == null) {
            return getDefault(key);
        }

        return value;
    }

    public Object getDefault(String key) {
        String[] keys   = key.split("\\.");
        HashMap map     = defaults;
        Object value    = null;

        for (String k : keys) {
            if (map == null) {
                break;
            }

            if (keys[keys.length - 1].equals(k)) {
                value = map.get(k);

            } else {
                map = (HashMap) map.get(k);
            }
        }

        return value;
    }

    public String getString(String key) {
        Object r;
        return (r = get(key)) == null ? null : r.toString();
    }

    private void setDefaults() {
        HashMap<String, Object> fields = new HashMap<>();
        fields.put("array_suffixs", Relation.DEFAULT_ARRAY_SUFFIX);
        fields.put("object_suffix", Relation.DEFAULT_OBJECT_SUFFIX);
        fields.put("order_field_name", Relation.DEFAULT_ORDER_FIELD_NAME);
        fields.put("primary_key_name", Relation.DEFAULT_PRIMARY_KEY_NAME);
        fields.put("value_field_name", Relation.DEFAULT_VALUE_FIELD_NAME);

        HashMap<String, Object> transformation = new HashMap<>();
        transformation.put("fields", fields);

        defaults = new HashMap<>();
        defaults.put("transformation", transformation);
    }
}
