import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object UniversalPreferences {
    const val PREF_NAME = "MyPrefs"
    val gson = Gson()

    fun <T> saveList(context: Context, key: String, list: List<T>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(list)
        prefs.edit().putString(key, json).apply()
    }

    inline fun <reified T> loadList(context: Context, key: String): List<T> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<T>>() {}.type
        return gson.fromJson(json, type)
    }
}

