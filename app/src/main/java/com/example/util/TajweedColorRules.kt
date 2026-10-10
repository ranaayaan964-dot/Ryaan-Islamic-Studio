package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

data class TajweedRuleGuide(
    val nameEnglish: String,
    val nameArabic: String,
    val color: Color,
    val darkColor: Color,
    val description: String,
    val exampleArabic: String,
    val exampleTransliteration: String
)

object TajweedColorRules {

    // Light Theme Colors
    val ColorGhunnahLight = Color(0xFF0D9488)       // Teal/Emerald for Ghunnah (2 Harakah nasal)
    val ColorQalqalahLight = Color(0xFF1D4ED8)      // Royal Blue for Echo/Bounce
    val ColorMaddLight = Color(0xFFDC2626)          // Crimson/Red for Prolongation (4-6 Harakah)
    val ColorIkhfaIdghamLight = Color(0xFFD97706)   // Amber/Orange for Ikhfa & Idgham
    val ColorIqlabLight = Color(0xFF7E22CE)         // Purple for Iqlab (Conversion to Meem)

    // Dark Theme Colors
    val ColorGhunnahDark = Color(0xFF34D399)        // Bright Emerald
    val ColorQalqalahDark = Color(0xFF60A5FA)       // Sky Blue
    val ColorMaddDark = Color(0xFFF87171)           // Bright Coral
    val ColorIkhfaIdghamDark = Color(0xFFFBBF24)    // Golden Amber
    val ColorIqlabDark = Color(0xFFC084FC)          // Bright Lavender

    val guideRules = listOf(
        TajweedRuleGuide(
            nameEnglish = "Ghunnah (Nasalization)",
            nameArabic = "الغنة",
            color = ColorGhunnahLight,
            darkColor = ColorGhunnahDark,
            description = "Prolonged nasal hum for 2 counts on Noon or Meem with Shaddah (نّ / مّ).",
            exampleArabic = "إِنَّ الَّذِينَ",
            exampleTransliteration = "Inna allatheena"
        ),
        TajweedRuleGuide(
            nameEnglish = "Qalqalah (Echoing Bounce)",
            nameArabic = "القلقلة",
            color = ColorQalqalahLight,
            darkColor = ColorQalqalahDark,
            description = "Resonant echo bounce when stopping or on sukun of letters: Qaf (ق), Ta (ط), Ba (ب), Jeem (ج), Dal (د).",
            exampleArabic = "قُلْ هُوَ اللَّهُ أَحَدْ",
            exampleTransliteration = "Qul huwa Allahu ahad"
        ),
        TajweedRuleGuide(
            nameEnglish = "Madd (Prolongation)",
            nameArabic = "المدود",
            color = ColorMaddLight,
            darkColor = ColorMaddDark,
            description = "Elongate the vowel sound for 4 to 6 counts upon Madd signs (~, آ, ىٰ, وٰ).",
            exampleArabic = "وَلَا الضَّالِّينَ",
            exampleTransliteration = "Wa la-d-daalleen"
        ),
        TajweedRuleGuide(
            nameEnglish = "Ikhfa & Idgham (Hiding / Merging)",
            nameArabic = "الإخفاء والإدغام",
            color = ColorIkhfaIdghamLight,
            darkColor = ColorIkhfaIdghamDark,
            description = "Nasal blending of Nun Sakinah or Tanween before phonetic throat or lip letters.",
            exampleArabic = "مِن قَبْلُ",
            exampleTransliteration = "Min qablu"
        ),
        TajweedRuleGuide(
            nameEnglish = "Iqlab (Conversion to Meem)",
            nameArabic = "الإقلاب",
            color = ColorIqlabLight,
            darkColor = ColorIqlabDark,
            description = "Transformation of Nun Sakinah or Tanween into a delicate Meem sound before Ba (ب).",
            exampleArabic = "مِنْۢ بَعْدِ",
            exampleTransliteration = "Mim ba'di"
        )
    )

    /**
     * Builds an AnnotatedString with precise Tajweed coloration.
     */
    fun buildTajweedAnnotatedString(
        arabicText: String,
        isDark: Boolean,
        isEnabled: Boolean,
        defaultColor: Color
    ): AnnotatedString {
        if (!isEnabled || arabicText.isBlank()) {
            return buildAnnotatedString {
                append(arabicText)
            }
        }

        val ghunnahColor = if (isDark) ColorGhunnahDark else ColorGhunnahLight
        val qalqalahColor = if (isDark) ColorQalqalahDark else ColorQalqalahLight
        val maddColor = if (isDark) ColorMaddDark else ColorMaddLight
        val ikhfaColor = if (isDark) ColorIkhfaIdghamDark else ColorIkhfaIdghamLight
        val iqlabColor = if (isDark) ColorIqlabDark else ColorIqlabLight

        val qalqalahChars = setOf('ق', 'ط', 'ب', 'ج', 'د')
        val maddChars = setOf('~', 'ۤ', 'آ', 'ٱ', 'ٰ', 'ٓ')

        return buildAnnotatedString {
            var i = 0
            val len = arabicText.length

            while (i < len) {
                val ch = arabicText[i]

                // Check Madd symbols
                if (maddChars.contains(ch) || (i + 1 < len && arabicText[i + 1] == 'ٓ')) {
                    val start = i
                    val end = (i + 2).coerceAtMost(len)
                    pushStyle(SpanStyle(color = maddColor, fontWeight = FontWeight.Bold))
                    append(arabicText.substring(start, end))
                    pop()
                    i = end
                    continue
                }

                // Check Iqlab (small meem marker ۢ)
                if (ch == 'ۢ' || (i + 1 < len && arabicText[i + 1] == 'ۢ')) {
                    val start = i
                    val end = (i + 2).coerceAtMost(len)
                    pushStyle(SpanStyle(color = iqlabColor, fontWeight = FontWeight.SemiBold))
                    append(arabicText.substring(start, end))
                    pop()
                    i = end
                    continue
                }

                // Check Ghunnah: Noon (ن) or Meem (م) followed by Shaddah (ّ)
                if ((ch == 'ن' || ch == 'م') && i + 1 < len && arabicText[i + 1] == 'ّ') {
                    val start = i
                    // span across the letter, shaddah, and possible harakah
                    var end = i + 2
                    if (end < len && isHarakah(arabicText[end])) end++
                    pushStyle(SpanStyle(color = ghunnahColor, fontWeight = FontWeight.Bold))
                    append(arabicText.substring(start, end))
                    pop()
                    i = end
                    continue
                }

                // Check Qalqalah: letters 'ق', 'ط', 'ب', 'ج', 'د' with Sukun or at word boundary
                if (qalqalahChars.contains(ch)) {
                    val hasSukun = i + 1 < len && arabicText[i + 1] == 'ْ'
                    if (hasSukun) {
                        val start = i
                        val end = i + 2
                        pushStyle(SpanStyle(color = qalqalahColor, fontWeight = FontWeight.Bold))
                        append(arabicText.substring(start, end))
                        pop()
                        i = end
                        continue
                    }
                }

                // Default rendering
                append(ch)
                i++
            }
        }
    }

    private fun isHarakah(c: Char): Boolean {
        return c in '\u064B'..'\u0652' || c == '\u0670'
    }
}
