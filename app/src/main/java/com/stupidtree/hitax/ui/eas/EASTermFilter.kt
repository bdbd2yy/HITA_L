package com.stupidtree.hitax.ui.eas

import com.stupidtree.component.data.DataState
import com.stupidtree.hitax.data.model.eas.TermItem

object EASTermFilter {
    private const val MIN_YEAR = 2024
    private const val MAX_YEAR = 2034

    fun filterFromTargetAutumn(state: DataState<List<TermItem>>): DataState<List<TermItem>> {
        if (state.state != DataState.STATE.SUCCESS || state.data == null) {
            return state
        }
        val filtered = completeTermsInRange(state.data!!)
        val result = DataState(filtered, state.state)
        result.message = state.message
        result.listAction = state.listAction
        result.fromCache = state.fromCache
        result.stateRetried = state.stateRetried
        return result
    }

    private fun completeTermsInRange(rawTerms: List<TermItem>): List<TermItem> {
        val autumnCode = detectAutumnCode(rawTerms)
        val springCode = detectSpringCode(rawTerms)
        val summerCode = detectSummerCode(rawTerms)
        val existed = linkedMapOf<String, TermItem>()
        for (term in rawTerms) {
            if (!isWithinTargetRange(term)) {
                continue
            }
            val key = getRangeKey(term)
            if (key.isEmpty()) {
                continue
            }
            applyDisplayName(term, key)
            existed[key] = term
        }
        val completed = arrayListOf<TermItem>()
        for (calendarYear in MIN_YEAR..MAX_YEAR) {
            if (calendarYear != MIN_YEAR) {
                val springStartYear = calendarYear - 1
                val springKey = "$calendarYear-S"
                completed.add(existed[springKey] ?: createSpringTerm(springStartYear, springCode))
                val summerKey = "$calendarYear-U"
                completed.add(existed[summerKey] ?: createSummerTerm(springStartYear, summerCode))
            }
            val autumnKey = "$calendarYear-A"
            completed.add(existed[autumnKey] ?: createAutumnTerm(calendarYear, autumnCode))
        }
        return completed
    }

    private fun isWithinTargetRange(term: TermItem): Boolean {
        val key = getRangeKey(term)
        if (key.isEmpty()) {
            return false
        }
        val split = key.split("-")
        if (split.size != 2) {
            return false
        }
        val calendarYear = split[0].toIntOrNull() ?: return false
        val season = split[1]
        if (calendarYear < MIN_YEAR || calendarYear > MAX_YEAR) {
            return false
        }
        if (calendarYear == MIN_YEAR) {
            return season == "A"
        }
        if (calendarYear == MAX_YEAR) {
            return season == "A" || season == "S" || season == "U"
        }
        return true
    }

    private fun getRangeKey(term: TermItem): String {
        val startYear = parseStartYear(term)
        if (startYear == Int.MIN_VALUE) {
            return ""
        }
        return when (getSeason(term)) {
            SEASON_AUTUMN -> "$startYear-A"
            SEASON_SPRING -> "${startYear + 1}-S"
            SEASON_SUMMER -> "${startYear + 1}-U"
            else -> ""
        }
    }

    private fun applyDisplayName(term: TermItem, key: String) {
        val split = key.split("-")
        if (split.size != 2) {
            return
        }
        val year = split[0].toIntOrNull() ?: return
        val seasonText = when (split[1]) {
            "A" -> "秋季"
            "S" -> "春季"
            "U" -> "夏季"
            else -> return
        }
        term.name = "$year$seasonText"
    }

    private fun parseStartYear(term: TermItem): Int {
        val byCode = term.yearCode.take(4).toIntOrNull()
        if (byCode != null) {
            return byCode
        }
        return term.yearName.take(4).toIntOrNull() ?: Int.MIN_VALUE
    }

    private fun createAutumnTerm(startYear: Int, autumnCode: String): TermItem {
        val yearCode = "$startYear-${startYear + 1}"
        val yearName = "$startYear"
        val term = TermItem(yearCode, yearName, autumnCode, "秋季")
        term.name = "${startYear}秋季"
        term.isCurrent = false
        return term
    }

    private fun createSpringTerm(startYear: Int, springCode: String): TermItem {
        val yearCode = "$startYear-${startYear + 1}"
        val displayYear = startYear + 1
        val yearName = "$displayYear"
        val term = TermItem(yearCode, yearName, springCode, "春季")
        term.name = "${displayYear}春季"
        term.isCurrent = false
        return term
    }

    private fun createSummerTerm(startYear: Int, summerCode: String): TermItem {
        val yearCode = "$startYear-${startYear + 1}"
        val displayYear = startYear + 1
        val yearName = "$displayYear"
        val term = TermItem(yearCode, yearName, summerCode, "夏季")
        term.name = "${displayYear}夏季"
        term.isCurrent = false
        return term
    }

    private fun detectAutumnCode(terms: List<TermItem>): String {
        return terms.firstOrNull { getSeason(it) == SEASON_AUTUMN }?.termCode
            ?: "1"
    }

    private fun detectSpringCode(terms: List<TermItem>): String {
        return terms.firstOrNull { getSeason(it) == SEASON_SPRING }?.termCode
            ?: "2"
    }

    private fun detectSummerCode(terms: List<TermItem>): String {
        return terms.firstOrNull { getSeason(it) == SEASON_SUMMER }?.termCode
            ?: "3"
    }

    private fun getSeason(term: TermItem): Int {
        if (term.termName.contains("秋")) {
            return SEASON_AUTUMN
        }
        if (term.termName.contains("春")) {
            return SEASON_SPRING
        }
        if (term.termName.contains("夏")) {
            return SEASON_SUMMER
        }
        if (term.termName.contains("第一")) {
            return SEASON_AUTUMN
        }
        if (term.termName.contains("第二")) {
            return SEASON_SPRING
        }
        if (term.termName.contains("第三")) {
            return SEASON_SUMMER
        }
        if (term.termCode == "2") {
            return SEASON_SPRING
        }
        if (term.termCode == "3") {
            return SEASON_SUMMER
        }
        if (term.termCode == "1") {
            return SEASON_AUTUMN
        }
        return SEASON_UNKNOWN
    }

    private const val SEASON_UNKNOWN = 0
    private const val SEASON_SPRING = 1
    private const val SEASON_AUTUMN = 2
    private const val SEASON_SUMMER = 3
}
