package ir.greenmate.plantcare.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class PlantRepository(context: Context) {
    private val preferences = context.getSharedPreferences("greenmate_data", Context.MODE_PRIVATE)

    fun getAll(): List<Plant> {
        val raw = preferences.getString(KEY_PLANTS, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        Plant(
                            id = item.optLong("id"),
                            name = item.optString("name"),
                            species = item.optString("species"),
                            location = item.optString("location"),
                            wateringIntervalDays = item.optInt("interval", 7).coerceIn(1, 90),
                            lastWateredAt = item.optLong("lastWatered", System.currentTimeMillis()),
                            emoji = item.optString("emoji", "🪴"),
                            notes = item.optString("notes", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(plant: Plant) {
        val plants = getAll().toMutableList()
        val index = plants.indexOfFirst { it.id == plant.id }
        if (index >= 0) plants[index] = plant else plants.add(plant)
        persist(plants)
    }

    fun delete(id: Long) {
        persist(getAll().filterNot { it.id == id })
    }

    fun find(id: Long): Plant? = getAll().firstOrNull { it.id == id }

    private fun persist(plants: List<Plant>) {
        val array = JSONArray()
        plants.forEach { plant ->
            array.put(
                JSONObject().apply {
                    put("id", plant.id)
                    put("name", plant.name)
                    put("species", plant.species)
                    put("location", plant.location)
                    put("interval", plant.wateringIntervalDays)
                    put("lastWatered", plant.lastWateredAt)
                    put("emoji", plant.emoji)
                    put("notes", plant.notes)
                }
            )
        }
        preferences.edit().putString(KEY_PLANTS, array.toString()).apply()
    }

    companion object {
        private const val KEY_PLANTS = "plants_json"
    }
}
