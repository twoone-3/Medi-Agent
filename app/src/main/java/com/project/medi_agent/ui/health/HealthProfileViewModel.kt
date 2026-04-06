package com.project.medi_agent.ui.health

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.project.medi_agent.data.AppDatabase
import com.project.medi_agent.data.LocalKnowledgeRepository
import com.project.medi_agent.data.KnowledgeItem
import com.project.medi_agent.ui.HealthProfile
import kotlinx.coroutines.launch
import android.util.Log

data class HealthEntry(val key: String, val value: String)

class HealthProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application.applicationContext)
    private val kbRepo = LocalKnowledgeRepository(application.applicationContext)
    private val gson = Gson()

    private val _entries = MutableLiveData<List<HealthEntry>>(emptyList())
    val entries: LiveData<List<HealthEntry>> = _entries

    private val _kbSummary = MutableLiveData<KnowledgeItem?>(null)
    val kbSummary: LiveData<KnowledgeItem?> = _kbSummary

    fun loadAll() {
        viewModelScope.launch {
            try {
                var list = db.healthProfileDao().getAllProfiles()
                // debug logs removed
                // 如果数据库为空，尝试从旧的 SharedPreferences 仓库迁移（兼容历史数据）
                if (list.isEmpty()) {
                    try {
                        val spRepo = com.project.medi_agent.data.HealthProfileRepository(getApplication())
                        val map = spRepo.getAll()
                        if (map.isNotEmpty()) {
                            map.forEach { (k, v) ->
                                db.healthProfileDao().insertProfile(com.project.medi_agent.ui.HealthProfile(key = k, content = v))
                            }
                            // 重新读取
                            list = db.healthProfileDao().getAllProfiles()
                            // migration completed
                        }
                        } catch (e: Exception) {
                            // ignore migration errors
                        }
                }
                _entries.postValue(list.map { HealthEntry(it.key, it.content) })
            } catch (e: Exception) {
                _entries.postValue(emptyList())
            }
        }
    }

    fun update(key: String, value: String) {
        viewModelScope.launch {
            try {
                db.healthProfileDao().insertProfile(HealthProfile(key = key, content = value))
                loadAll()
            } catch (_: Exception) {
            }
        }
    }

    fun remove(key: String) {
        viewModelScope.launch {
            try {
                db.healthProfileDao().deleteProfile(key)
                loadAll()
            } catch (_: Exception) {
            }
        }
    }

    fun exportJson(): String {
        val list = _entries.value ?: emptyList()
        val map = list.associate { it.key to it.value }
        return gson.toJson(map)
    }

    fun loadKbSummary(query: String?) {
        val hits = kbRepo.topHits(query, 1)
        _kbSummary.postValue(hits.firstOrNull())
    }
}


