package ai.sach.libraryup.ui.screens.settingsScreen.appprefernecs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Device-level look-and-feel preferences (library data lives in Room, not here). */
class AppPreferencesRepository(private val context: Context) {

    /** Material You: take the theme from the wallpaper (Android 12+) instead of LibraryUp indigo. */
    val wallpaperColors: Flow<Boolean> = context.dataStore.data.map { it[WALLPAPER_COLORS] ?: false }

    suspend fun setWallpaperColors(on: Boolean) {
        context.dataStore.edit { it[WALLPAPER_COLORS] = on }
    }

    private companion object {
        val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")
        val WALLPAPER_COLORS = booleanPreferencesKey("wallpaper_colors")
    }
}
