package com.project.medi_agent.data

import android.content.Context
import com.google.gson.Gson

data class KnowledgeItem(
    val id: String = "",
    val title: String = "",
    val tags: List<String> = emptyList(),
    val content: String = ""
)

/**
 * 负责从 assets/medical_knowledge.json 加载本地医学知识条目并提供简单检索。
 */
class LocalKnowledgeRepository(private val context: Context) {
    private val gson = Gson()
    private var cache: List<KnowledgeItem>? = null

    private fun loadIfNeeded() {
        if (cache != null) return
        try {
            val text = context.assets.open("medical_knowledge.json").bufferedReader().use { it.readText() }
            val arr = gson.fromJson(text, Array<KnowledgeItem>::class.java)
            cache = arr?.toList() ?: emptyList()
        } catch (_: Exception) {
            cache = emptyList()
        }
    }

    fun topHits(query: String?, topN: Int = 1): List<KnowledgeItem> {
        if (query.isNullOrBlank()) return emptyList()
        loadIfNeeded()
        val kb = cache ?: return emptyList()
        val qTokens = query.lowercase().split(Regex("\\W+")).filter { it.isNotBlank() }
        if (qTokens.isEmpty()) return emptyList()
        val scored = kb.map { item ->
            val hay = (item.title + " " + item.content + " " + item.tags.joinToString(" ")).lowercase()
            val score = qTokens.sumOf { token -> Regex(Regex.escape(token)).findAll(hay).count() }
            Pair(item, score)
        }.filter { it.second > 0 }
        return scored.sortedByDescending { it.second }.map { it.first }.take(topN)
    }
}

