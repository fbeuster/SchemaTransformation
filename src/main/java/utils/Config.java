package utils;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * Created by Felix Beuster on 15.07.2016.
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

        try {
            InputStream io = new FileInputStream(new File("defautls.yaml"));
            Yaml yaml = new Yaml();
            defaults = (HashMap) yaml.load(io);

        } catch (FileNotFoundException e) {
            defaults = new HashMap<>();
        }
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

    public Boolean getBoolean(String key) {
        Object r;
        return (r = get(key)) == null ? false : (boolean) r;
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
        return (r = get(key)) == null ? "" : r.toString();
    }
}
