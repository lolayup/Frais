package com.khaled.frais.utils

import com.khaled.frais.app.FraisData
import org.apache.commons.text.similarity.LevenshteinDistance

/** 使用莱文斯坦距离 (Levenshtein distance) 实现模糊搜索 */
object FuzzySearch {
    init {
        System.loadLibrary("frais-engine")
    }

    private external fun nativeSearch(raw: String, query: String, useFuzzy: Boolean): Boolean

    /**
     * 两个字符串差异小于原始字符串长度 且 原始字符串依次包含输入字符串的每个字符 则显示在搜索结果中
     * @param raw 需要匹配的原始字符串
     * @param query 输入的字符串
     */
    fun search(raw: String?, query: String?): Boolean {
        if (query.isNullOrEmpty()) return true
        if (raw.isNullOrEmpty()) return false
        return nativeSearch(raw, query, FraisData.fuzzySearch)
    }
}
