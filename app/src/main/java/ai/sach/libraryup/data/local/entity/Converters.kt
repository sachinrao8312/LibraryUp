package ai.sach.rentup.data.local.entity

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromEmergencyContactList(value: List<EmergencyContact>): String {
        val gson = Gson()
        val type = object : TypeToken<List<EmergencyContact>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toEmergencyContactList(value: String): List<EmergencyContact> {
        val gson = Gson()
        val type = object : TypeToken<List<EmergencyContact>>() {}.type
        return gson.fromJson(value, type)
    }
}