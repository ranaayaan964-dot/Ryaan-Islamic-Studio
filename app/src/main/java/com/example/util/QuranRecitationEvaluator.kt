package com.example.util

enum class WordEvaluationStatus {
    CORRECT,        // Green (#10B981)
    MISPRONOUNCED,  // Red (#EF4444)
    MISSED          // Red (#EF4444)
}

data class EvaluatedWord(
    val originalWord: String,
    val cleanWord: String,
    val recognizedMatch: String?,
    val status: WordEvaluationStatus,
    val tajweedNote: String
)

data class RecitationEvaluationResult(
    val words: List<EvaluatedWord>,
    val accuracyPercentage: Int,
    val correctWordsCount: Int,
    val missedWordsCount: Int,
    val mispronouncedCount: Int,
    val overallFeedback: String
)

object QuranRecitationEvaluator {

    private val DIACRITICS_REGEX = Regex("[\u064B-\u065F\u0670\u06D6-\u06ED]")
    private val PUNCTUATION_REGEX = Regex("[•۝\u060C\u061B\u061F.،,!-]+")

    fun normalizeArabic(input: String): String {
        var text = DIACRITICS_REGEX.replace(input, "")
        text = PUNCTUATION_REGEX.replace(text, " ")
        text = text.replace(Regex("[أإآٱ]"), "ا")
        text = text.replace("ة", "ه")
        text = text.replace("ى", "ي")
        return text.trim().replace(Regex("\\s+"), " ")
    }

    fun evaluate(targetSurahText: String, recognizedSpeech: String): RecitationEvaluationResult {
        val targetRawWords = targetSurahText
            .replace("•", " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        val recognizedWords = normalizeArabic(recognizedSpeech)
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        val evaluatedWords = mutableListOf<EvaluatedWord>()
        var correctCount = 0
        var missedCount = 0
        var mispronouncedCount = 0

        var recIdx = 0

        for (targetWord in targetRawWords) {
            val cleanTarget = normalizeArabic(targetWord)
            if (cleanTarget.isBlank()) continue

            var bestStatus = WordEvaluationStatus.MISSED
            var bestMatch: String? = null
            var tajweedNote = "Word was skipped"

            val searchWindow = (recIdx until minOf(recIdx + 3, recognizedWords.size))
            for (idx in searchWindow) {
                val recWord = recognizedWords[idx]
                val distance = levenshteinDistance(cleanTarget, recWord)

                if (cleanTarget == recWord || distance == 0) {
                    bestStatus = WordEvaluationStatus.CORRECT
                    bestMatch = recWord
                    tajweedNote = "Accurate articulation ✓"
                    recIdx = idx + 1
                    break
                } else if (distance <= 1 && cleanTarget.length >= 3) {
                    bestStatus = WordEvaluationStatus.MISPRONOUNCED
                    bestMatch = recWord
                    tajweedNote = "Check Makhraj & Qalqalah"
                    recIdx = idx + 1
                    break
                }
            }

            if (bestStatus == WordEvaluationStatus.MISSED && recognizedWords.any { it == cleanTarget }) {
                bestStatus = WordEvaluationStatus.CORRECT
                bestMatch = cleanTarget
                tajweedNote = "Accurate articulation ✓"
            }

            when (bestStatus) {
                WordEvaluationStatus.CORRECT -> correctCount++
                WordEvaluationStatus.MISPRONOUNCED -> mispronouncedCount++
                WordEvaluationStatus.MISSED -> missedCount++
            }

            evaluatedWords.add(
                EvaluatedWord(
                    originalWord = targetWord,
                    cleanWord = cleanTarget,
                    recognizedMatch = bestMatch,
                    status = bestStatus,
                    tajweedNote = tajweedNote
                )
            )
        }

        val totalWords = evaluatedWords.size.coerceAtLeast(1)
        val score = ((correctCount.toDouble() / totalWords) * 100).toInt().coerceIn(0, 100)

        val feedback = when {
            score >= 90 -> "Masha'Allah! Excellent recitation with precise Makhraj and Tajweed rules."
            score >= 75 -> "Very good recitation. Minor hesitation or missed elongation. Review the highlighted words."
            score >= 50 -> "Good effort. Practice at a slower pace with deliberate stop pauses (Waqf)."
            else -> "Recitation heard. Focus on clear pronunciation of throat letters and recite steadily."
        }

        return RecitationEvaluationResult(
            words = evaluatedWords,
            accuracyPercentage = score,
            correctWordsCount = correctCount,
            missedWordsCount = missedCount,
            mispronouncedCount = mispronouncedCount,
            overallFeedback = feedback
        )
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
