package utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Created by Felix Beuster on 03.07.2016.
 */
public class Types {

    public static int TYPE_ARRAY        = 0;
    public static int TYPE_ARRAY_ID     = 1;
    public static int TYPE_ARRAY_ORDER  = 2;
    public static int TYPE_BOOL         = 3;
    public static int TYPE_ID           = 4;
    public static int TYPE_NULL         = 5;
    public static int TYPE_NUMBER       = 6;
    public static int TYPE_OBJECT       = 7;
    public static int TYPE_STRING       = 8;

    public static String SQL_ARRAY          = "INT";
    public static String SQL_ARRAY_ID       = "INT";
    public static String SQL_ARRAY_ORDER    = "INT";
    public static String SQL_BOOL           = "BOOLEAN";
    public static String SQL_ID             = "INT";
    public static String SQL_NULL           = "NULL";
    public static String SQL_NUMBER         = "DOUBLE";
    public static String SQL_OBJECT         = "INT";
    public static String SQL_STRING         = "MEDIUMTEXT";

    public static String STRING_ARRAY       = "array";
    public static String STRING_ARRAY_ID    = "array_id";
    public static String STRING_ARRAY_ORDER = "order";
    public static String STRING_BOOL        = "boolean";
    public static String STRING_ID          = "id";
    public static String STRING_NULL        = "null";
    public static String STRING_NUMBER      = "number";
    public static String STRING_OBJECT      = "object";
    public static String STRING_STRING      = "string";

    public static String constantToSQL(int type) {
        if (        type == TYPE_ARRAY      ) { return SQL_ARRAY;
        } else if ( type == TYPE_ARRAY_ID   ) { return SQL_ARRAY_ID;
        } else if ( type == TYPE_ARRAY_ORDER) { return SQL_ARRAY_ORDER;
        } else if ( type == TYPE_BOOL       ) { return SQL_BOOL;
        } else if ( type == TYPE_ID         ) { return SQL_ID;
        } else if ( type == TYPE_NUMBER     ) { return SQL_NUMBER;
        } else if ( type == TYPE_OBJECT     ) { return SQL_OBJECT;
        } else if ( type == TYPE_STRING     ) { return SQL_STRING;
        } else {                                return SQL_NULL;
        }
    }

    public static String constantToString(int type) {
        if (        type == TYPE_ARRAY      ) { return STRING_ARRAY;
        } else if ( type == TYPE_ARRAY_ID   ) { return STRING_ARRAY_ID;
        } else if ( type == TYPE_ARRAY_ORDER) { return STRING_ARRAY_ORDER;
        } else if ( type == TYPE_BOOL       ) { return STRING_BOOL;
        } else if ( type == TYPE_ID         ) { return STRING_ID;
        } else if ( type == TYPE_NUMBER     ) { return STRING_NUMBER;
        } else if ( type == TYPE_OBJECT     ) { return STRING_OBJECT;
        } else if ( type == TYPE_STRING     ) { return STRING_STRING;
        } else {                                return STRING_NULL;
        }
    }

    public static int jsonElementToInt(JsonElement e) {
        if (e.isJsonObject()) {         return TYPE_OBJECT;
        } else if (e.isJsonArray()) {   return TYPE_ARRAY;
        } else if (e.isJsonNull()) {    return TYPE_NULL;
        } else {
            JsonPrimitive p = e.getAsJsonPrimitive();

            if (p.isString()) {         return TYPE_STRING;
            } else if (p.isNumber()) {  return TYPE_NUMBER;
            } else if (p.isBoolean()) { return TYPE_BOOL;
            } else {                    return TYPE_NULL;
            }
        }
    }

    public static String jsonElementToJsonString(JsonElement e) {
        Config config = new Config();

        if (config.getBoolean("extraction.features.simple_prop_types")) {
            // method body for simple property types
            if (e.isJsonObject()) {         return "JsonObject";
            } else if (e.isJsonArray()) {   return "JsonArray";
            } else if (e.isJsonNull()) {    return "JsonNull";

            } else {
                JsonPrimitive p = e.getAsJsonPrimitive();

                if (p.isString()) {         return "class com.google.gson.JsonPrimitive.String";
                } else if (p.isNumber()) {  return "class com.google.gson.JsonPrimitive.Number";
                } else if (p.isBoolean()) { return "class com.google.gson.JsonPrimitive.Boolean";
                } else if (p.isJsonNull()) {return "class com.google.gson.JsonPrimitive.JsonNull";
                } else {                    return null;
                }
            }

        } else {
            // alternative method body for exact property types
            if(!e.isJsonPrimitive()) {
                return e.getClass().toString();

            } else {
                JsonPrimitive p = e.getAsJsonPrimitive();

                if (p.isString()) {         return "class com.google.gson.JsonPrimitive.String";
                } else if (p.isNumber()) {  return "class com.google.gson.JsonPrimitive.Number";
                } else if (p.isBoolean()) { return "class com.google.gson.JsonPrimitive.Boolean";
                } else if (p.isJsonNull()) {return "class com.google.gson.JsonPrimitive.JsonNull";
                } else {                    return null;
                }
            }
        }
    }

    public static int jsonToInt(JsonElement element) {
        String elementString = element.toString();

        if (elementString.contains("JsonArray") ||
                elementString.contains("class com.google.gson.JsonArray") ){
            return TYPE_ARRAY;

        } else if(elementString.contains("JsonObject") ||
                elementString.contains("class com.google.gson.JsonObject")) {
            return TYPE_OBJECT;

        } else if(elementString.contains("class com.google.gson.JsonPrimitive.String")) {
            return TYPE_STRING;

        } else if(elementString.contains("class com.google.gson.JsonPrimitive.Number")) {
            return TYPE_NUMBER;

        } else if(elementString.contains("class com.google.gson.JsonPrimitive.Boolean")) {
            return TYPE_BOOL;

        } else {
            return TYPE_NULL;
        }
    }

    public static int jsonStringToInt(String elementString) {
        if (elementString.contains("JsonArray") ||
                elementString.contains("class com.google.gson.JsonArray") ){
            return TYPE_ARRAY;

        } else if(elementString.contains("JsonObject") ||
                elementString.contains("class com.google.gson.JsonObject")) {
            return TYPE_OBJECT;

        } else if(elementString.contains("class com.google.gson.JsonPrimitive.String")) {
            return TYPE_STRING;

        } else if(elementString.contains("class com.google.gson.JsonPrimitive.Number")) {
            return TYPE_NUMBER;

        } else if(elementString.contains("class com.google.gson.JsonPrimitive.Boolean")) {
            return TYPE_BOOL;

        } else {
            return TYPE_NULL;
        }
    }

    public static String jsonToString(JsonElement element) {
        String elementString = element.toString();

        if (elementString.contains("JsonArray") ||
                elementString.contains("class com.google.gson.JsonArray") ){
            return STRING_ARRAY;

        } else if(elementString.contains("JsonObject") ||
                elementString.contains("class com.google.gson.JsonObject")) {
            return STRING_OBJECT;

        } else if(elementString.contains("class com.google.gson.JsonPrimitive.String")) {
            return STRING_STRING;

        } else if(elementString.contains("class com.google.gson.JsonPrimitive.Number")) {
            return STRING_NUMBER;

        } else if(elementString.contains("class com.google.gson.JsonPrimitive.Boolean")) {
            return STRING_BOOL;

        } else {
            return STRING_NULL;
        }
    }
}
