package ir.blue_saffron.fv;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

public class SP {
    private static final String MY_PREFS_NAME = "main_pref";
    static SharedPreferences sp;

    private static final String DEBUG = "debug";
    private static final String DEFAULT_PERSONS = "default_persons";
    private static final String LAST_PERSON_ID = "last_person_id";
    private static final String PERSON_NAME = "person_name_";


    public static void init(Context context) {
        sp = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
    }

    public static void putString(String key, String value) {
        sp.edit().putString(key, value).apply();
    }

    public static void putInt(String key, int value) {
        sp.edit().putInt(key, value).apply();
    }

    public static void increaseLastPersonId() {
        putInt(LAST_PERSON_ID, getLastPersonId() + 1);
    }

    public static int getLastPersonId() {
        return sp.getInt(LAST_PERSON_ID, 1);
    }

    public static void putPersonName(int id, String name) {
        putString(PERSON_NAME + id, name);
    }

    public static void removePersonName(int id) {
        sp.edit().remove(PERSON_NAME + id).apply();
    }

    public static String getPersonName(int id) {
        return sp.getString(PERSON_NAME + id, "Null");
    }

    public static void setDefaultPersonsDone(boolean status) {
        sp.edit().putBoolean(DEFAULT_PERSONS, status).apply();
    }

    public static boolean getDefaultPersonsDone() {
        return sp.getBoolean(DEFAULT_PERSONS, false);
    }

    public static void setDebug(boolean status) {
        sp.edit().putBoolean(DEBUG, status).apply();
    }

    public static boolean getDebug() {
        return sp.getBoolean(DEBUG, false);
    }
}
