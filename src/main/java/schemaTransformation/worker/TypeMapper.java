package schemaTransformation.worker;

import com.google.gson.JsonElement;

/**
 * Created by Felix Beuster on 03.07.2016.
 */
public class TypeMapper {

    public static int TYPE_ARRAY    = 0;
    public static int TYPE_BOOL     = 1;
    public static int TYPE_ID       = 2;
    public static int TYPE_NULL     = 3;
    public static int TYPE_NUMBER   = 4;
    public static int TYPE_OBJECT   = 5;
    public static int TYPE_ORDER    = 6;
    public static int TYPE_STRING   = 7;

    public static String STRING_ARRAY    = "array";
    public static String STRING_BOOL     = "boolean";
    public static String STRING_ID       = "id";
    public static String STRING_NULL     = "null";
    public static String STRING_NUMBER   = "number";
    public static String STRING_OBJECT   = "object";
    public static String STRING_ORDER    = "order";
    public static String STRING_STRING   = "string";

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
