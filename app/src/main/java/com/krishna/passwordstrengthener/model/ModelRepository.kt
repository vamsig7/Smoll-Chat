package com.krishna.passwordstrengthener.model

import android.content.Context
import android.content.SharedPreferences

object ModelRepository {
    private const val PREFS = "password_strengthener_prefs"
    private const val KEY_MODEL_PATH = "model_path_abs"
    private const val KEY_MODEL_FILE = "model_file_name"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveModel(context: Context, absPath: String, fileName: String) {
        prefs(context).edit().putString(KEY_MODEL_PATH, absPath)
            .putString(KEY_MODEL_FILE, fileName)
            .apply()
    }

    fun getModelPath(context: Context): String? = prefs(context).getString(KEY_MODEL_PATH, null)
    fun getModelFileName(context: Context): String? = prefs(context).getString(KEY_MODEL_FILE, null)

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_MODEL_PATH).remove(KEY_MODEL_FILE).apply()
    }
}
