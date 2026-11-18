package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CustomIcDefinition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.customIcDataStore by preferencesDataStore(name = "gatex_custom_ics")

class CustomIcStore(private val context: Context) {
    private val keyList = stringPreferencesKey("custom_ic_list_v1")

    val customIcs: Flow<List<CustomIcDefinition>> = context.customIcDataStore.data.map { prefs ->
        val raw = prefs[keyList].orEmpty()
        if (raw.isBlank()) emptyList() else decodeList(raw)
    }

    suspend fun saveCustomIc(def: CustomIcDefinition) {
        context.customIcDataStore.edit { prefs ->
            val current = decodeList(prefs[keyList].orEmpty()).toMutableList()
            val idx = current.indexOfFirst { it.id == def.id }
            if (idx >= 0) current[idx] = def else current.add(0, def)
            prefs[keyList] = encodeList(current)
        }
    }

    suspend fun deleteCustomIc(id: String) {
        context.customIcDataStore.edit { prefs ->
            val current = decodeList(prefs[keyList].orEmpty()).filterNot { it.id == id }
            prefs[keyList] = encodeList(current)
        }
    }

    suspend fun mergeCloudCustomIcs(cloudDefs: List<CustomIcDefinition>) {
        if (cloudDefs.isEmpty()) return
        context.customIcDataStore.edit { prefs ->
            val byId = LinkedHashMap<String, CustomIcDefinition>()
            decodeList(prefs[keyList].orEmpty()).forEach { byId[it.id] = it }
            cloudDefs.forEach { byId[it.id] = it }
            prefs[keyList] = encodeList(byId.values.toList())
        }
    }

    companion object {
        fun truthMapToEntries(truthMap: Map<Int, Int>): List<String> =
            truthMap.entries.sortedBy { it.key }.map { "${it.key}:${it.value}" }

        fun parseTruthEntries(entries: List<String>): Map<Int, Int> {
            val out = LinkedHashMap<Int, Int>()
            entries.forEach { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val k = parts[0].trim().toIntOrNull()
                    val v = parts[1].trim().toIntOrNull()
                    if (k != null && v != null) out[k] = v
                }
            }
            return out
        }

        fun truthMapToCompactString(truthMap: Map<Int, Int>): String =
            truthMapToEntries(truthMap).joinToString(",")

        fun parseCompactTruthString(raw: String): Map<Int, Int> =
            if (raw.isBlank()) emptyMap() else parseTruthEntries(raw.split(","))

        fun encodeList(list: List<CustomIcDefinition>): String {
            val arr = JSONArray()
            list.forEach { ic ->
                arr.put(JSONObject().apply {
                    put("id", ic.id)
                    put("name", ic.name)
                    put("shortCode", ic.shortCode)
                    put("description", ic.description)
                    put("inputLabels", JSONArray(ic.inputLabels))
                    put("outputLabels", JSONArray(ic.outputLabels))
                    put("truthEntries", JSONArray(truthMapToEntries(ic.truthMap)))
                })
            }
            return arr.toString()
        }

        fun decodeList(raw: String): List<CustomIcDefinition> {
            return runCatching {
                val arr = JSONArray(raw)
                val list = mutableListOf<CustomIcDefinition>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val inArr = obj.optJSONArray("inputLabels")
                    val outArr = obj.optJSONArray("outputLabels")
                    val trArr = obj.optJSONArray("truthEntries")
                    val inputs = mutableListOf<String>()
                    val outputs = mutableListOf<String>()
                    val entries = mutableListOf<String>()
                    if (inArr != null) for (j in 0 until inArr.length()) inputs.add(inArr.optString(j))
                    if (outArr != null) for (j in 0 until outArr.length()) outputs.add(outArr.optString(j))
                    if (trArr != null) for (j in 0 until trArr.length()) entries.add(trArr.optString(j))
                    list.add(
                        CustomIcDefinition(
                            id = obj.optString("id"),
                            name = obj.optString("name", "Custom IC"),
                            shortCode = obj.optString("shortCode", "CIC"),
                            description = obj.optString("description", ""),
                            inputLabels = inputs.ifEmpty { listOf("A", "B") },
                            outputLabels = outputs.ifEmpty { listOf("Y") },
                            truthMap = parseTruthEntries(entries)
                        )
                    )
                }
                list
            }.getOrDefault(emptyList())
        }
    }
}
