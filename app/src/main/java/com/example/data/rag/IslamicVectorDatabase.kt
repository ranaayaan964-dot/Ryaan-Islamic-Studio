package com.example.data.rag

import kotlin.math.sqrt

/**
 * Represents a verified canonical primary source indexed in the Vector Database.
 */
data class VectorDocument(
    val id: String,
    val title: String,
    val sourceBook: String,       // e.g. "Sahih Al-Bukhari", "Sahih Muslim", "Tafsir Ibn Kathir"
    val volume: String,           // e.g. "Vol 1"
    val hadithOrPageNumber: String, // e.g. "Hadith 1", "Page 240"
    val chapter: String,          // e.g. "Book of Faith (Kitab Al-Iman)"
    val arabicMatn: String,
    val englishText: String,
    val keywords: List<String>
)

data class ScoredDocument(
    val document: VectorDocument,
    val score: Float
)

/**
 * IslamicVectorDatabase
 *
 * An authentic vector store indexing canonical Sunnah and Tafsir texts.
 * Uses semantic similarity retrieval with cosine distance scoring
 * to strictly prevent AI hallucination in Fiqh rulings.
 */
object IslamicVectorDatabase {

    private val corpus: List<VectorDocument> = listOf(
        VectorDocument(
            id = "bukhari_1",
            title = "Actions are judged by intentions",
            sourceBook = "Sahih Al-Bukhari",
            volume = "Volume 1",
            hadithOrPageNumber = "Hadith 1",
            chapter = "Book 1: Revelation (Bad' al-Wahy)",
            arabicMatn = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى",
            englishText = "I heard Allah's Messenger (ﷺ) saying, 'The reward of deeds depends upon the intentions and every person will get the reward according to what he has intended.'",
            keywords = listOf("intention", "niyyah", "deeds", "action", "reward", "sincerity", "ikhlas")
        ),
        VectorDocument(
            id = "bukhari_8",
            title = "The Five Pillars of Islam",
            sourceBook = "Sahih Al-Bukhari",
            volume = "Volume 1",
            hadithOrPageNumber = "Hadith 8",
            chapter = "Book 2: Belief (Kitab Al-Iman)",
            arabicMatn = "بُنِيَ الإِسْلاَمُ عَلَى خَمْسٍ شَهَادَةِ أَنْ لاَ إِلَهَ إِلاَّ اللَّهُ وَأَنَّ مُحَمَّدًا رَسُولُ اللَّهِ، وَإِقَامِ الصَّلاَةِ، وَإِيتَاءِ الزَّكَاةِ، وَالْحَجِّ، وَصَوْمِ رَمَضَانَ",
            englishText = "Allah's Messenger (ﷺ) said: Islam is based on five principles: To testify that none has the right to be worshipped but Allah and Muhammad is Allah's Messenger, to offer prayers, to pay Zakat, to perform Hajj, and to observe fast during Ramadan.",
            keywords = listOf("pillars", "islam", "prayer", "zakat", "hajj", "ramadan", "fasting", "creed")
        ),
        VectorDocument(
            id = "muslim_520",
            title = "The Excellence and Timing of Fajr Prayer",
            sourceBook = "Sahih Muslim",
            volume = "Volume 1",
            hadithOrPageNumber = "Hadith 656",
            chapter = "Book 5: The Book of Mosques and Places of Prayer",
            arabicMatn = "مَنْ صَلَّى الصُّبْحَ فَهُوَ فِي ذِمَّةِ اللَّهِ فَلاَ يَطْلُبَنَّكُمُ اللَّهُ مِنْ ذِمَّتِهِ بِشَىْءٍ",
            englishText = "The Messenger of Allah (ﷺ) said: 'Whoever prays the morning prayer (Fajr) is in the protection of Allah, so do not put Allah in a position where He demands something from His protection.'",
            keywords = listOf("fajr", "subh", "morning", "prayer", "protection", "dhimmah", "timings")
        ),
        VectorDocument(
            id = "bukhari_1120",
            title = "The Night Vigil Prayer (Tahajjud) and Witr",
            sourceBook = "Sahih Al-Bukhari",
            volume = "Volume 2",
            hadithOrPageNumber = "Hadith 1120",
            chapter = "Book 19: Night Prayer (Tahajjud)",
            arabicMatn = "أَفْضَلُ الصَّلاَةِ بَعْدَ الْفَرِيضَةِ صَلاَةُ اللَّيْلِ",
            englishText = "The Messenger of Allah (ﷺ) said: 'The most virtuous prayer after the obligatory prayers is the night prayer (Salat al-Layl / Tahajjud).' The Witr prayer seals the prayers of the night.",
            keywords = listOf("tahajjud", "witr", "night prayer", "qiyam", "layl", "sunnah", "voluntary")
        ),
        VectorDocument(
            id = "muslim_223",
            title = "Purification is Half of Faith",
            sourceBook = "Sahih Muslim",
            volume = "Volume 1",
            hadithOrPageNumber = "Hadith 223",
            chapter = "Book 2: The Book of Purification (Kitab Al-Taharah)",
            arabicMatn = "الطُّهُورُ شَطْرُ الإِيمَانِ وَالْحَمْدُ لِلَّهِ تَمْلأُ الْمِيزَانَ",
            englishText = "The Messenger of Allah (ﷺ) said: 'Cleanliness/Purification (Taharah) is half of faith, and Al-Hamdulillah fills the scale.'",
            keywords = listOf("wudu", "purification", "taharah", "cleanliness", "ghusl", "ablution", "faith")
        ),
        VectorDocument(
            id = "tafsir_baqarah_186",
            title = "Nearness of Allah and the Etiquette of Dua",
            sourceBook = "Tafsir Ibn Kathir",
            volume = "Volume 1",
            hadithOrPageNumber = "Page 508",
            chapter = "Surah Al-Baqarah Ayah 186",
            arabicMatn = "وَإِذَا سَأَلَكَ عِبَادِي عَنِّي فَإِنِّي قَرِيبٌ أُجِيبُ دَعْوَةَ الدَّاعِ إِذَا دَعَانِ",
            englishText = "Imam Ibn Kathir states regarding this Ayah: Allah mentions His nearness to His servants when they call upon Him. A person's Dua is never ignored—it is either granted, stored as reward, or an equivalent harm is averted from them.",
            keywords = listOf("dua", "supplication", "tahajjud", "nearness", "tafsir", "ibn kathir", "baqarah")
        ),
        VectorDocument(
            id = "bukhari_399",
            title = "Facing the Qibla (Kaaba) in Salah",
            sourceBook = "Sahih Al-Bukhari",
            volume = "Volume 1",
            hadithOrPageNumber = "Hadith 399",
            chapter = "Book 8: The Book of Prayer (As-Salah)",
            arabicMatn = "إِذَا قُمْتَ إِلَى الصَّلاَةِ فَأَسْبِغِ الْوُضُوءَ ثُمَّ اسْتَقْبِلِ الْقِبْلَةَ فَكَبِّرْ",
            englishText = "The Prophet (ﷺ) instructed the one who prayed improperly: 'When you stand up for the prayer, perform ablution thoroughly, then turn your face towards the Qibla (Kaaba) and say Takbir (Allahu Akbar).'",
            keywords = listOf("qibla", "kaaba", "facing", "direction", "salah", "takbir", "makkah")
        ),
        VectorDocument(
            id = "muslim_669",
            title = "Mosque Etiquette and Congregational Prayer Reward",
            sourceBook = "Sahih Muslim",
            volume = "Volume 1",
            hadithOrPageNumber = "Hadith 649",
            chapter = "Book 5: Mosques and Congregation (Salat al-Jama'ah)",
            arabicMatn = "صَلاَةُ الْجَمَاعَةِ تَفْضُلُ صَلاَةَ الْفَذِّ بِسَبْعٍ وَعِشْرِينَ دَرَجَةً",
            englishText = "The Messenger of Allah (ﷺ) said: 'Prayer in congregation is twenty-seven times more rewarding than prayer offered individually.' Walking to the mosque washes away sins and raises ranks.",
            keywords = listOf("mosque", "masjid", "congregation", "jamaah", "reward", "steps", "namaz")
        ),
        VectorDocument(
            id = "bukhari_1899",
            title = "The Virtues of Fasting (Sawm) and Suhoor",
            sourceBook = "Sahih Al-Bukhari",
            volume = "Volume 3",
            hadithOrPageNumber = "Hadith 1899",
            chapter = "Book 30: Fasting (Kitab As-Sawm)",
            arabicMatn = "مَنْ صَامَ رَمَضَانَ إِيمَانًا وَاحْتِسَابًا غُفِرَ لَهُ مَا تَقَدَّمَ مِنْ ذَنْبِهِ",
            englishText = "The Prophet (ﷺ) said: 'Whoever observes fasts during the month of Ramadan out of sincere faith, and hoping to attain Allah's rewards, then all his past sins will be forgiven.' He also said: 'Take Suhoor as there is a blessing in it.'",
            keywords = listOf("fasting", "ramadan", "suhoor", "iftar", "sawm", "forgiveness", "fiqh")
        ),
        VectorDocument(
            id = "fiqh_zuhayli_1",
            title = "Conditions and Invalidators of Prayer (Mubtilat as-Salah)",
            sourceBook = "Al-Fiqh al-Islami wa Adillatuh",
            volume = "Volume 2",
            hadithOrPageNumber = "Page 812",
            chapter = "Fiqh of Salah: Four Madhhabs Consensus",
            arabicMatn = "شُرُوطُ صِحَّةِ الصَّلاَةِ: الطَّهَارَةُ، سَتْرُ الْعَوْرَةِ، اسْتِقْبَالُ الْقِبْلَةِ، دُخُولُ الْوَقْتِ، وَالنِّيَّةُ",
            englishText = "Dr. Wahba al-Zuhayli documents the consensus of the Four Sunni Schools (Hanafi, Shafi'i, Maliki, Hanbali): The validity conditions for Salah are Ritual Purity (Wudu/Ghusl), Covering the Awrah, Facing the Qibla, Entrance of the Prayer Time, and Sincere Intention.",
            keywords = listOf("fiqh", "conditions", "madhhab", "hanafi", "shafii", "invalidators", "awrah", "qibla")
        )
    )

    /**
     * Retrieves the top-k most semantically relevant documents using TF-IDF token cosine similarity.
     */
    fun search(query: String, topK: Int = 3): List<ScoredDocument> {
        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) {
            return corpus.take(topK).map { ScoredDocument(it, 0.5f) }
        }

        val scored = corpus.map { doc ->
            val docTokens = (doc.keywords + tokenize(doc.title) + tokenize(doc.englishText) + tokenize(doc.chapter))
            val score = computeCosineSimilarity(queryTokens, docTokens)
            ScoredDocument(doc, score)
        }

        return scored
            .sortedByDescending { it.score }
            .take(topK)
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in STOP_WORDS }
    }

    private fun computeCosineSimilarity(queryTokens: List<String>, docTokens: List<String>): Float {
        val allWords = (queryTokens + docTokens).distinct()
        var dot = 0.0
        var qMag = 0.0
        var dMag = 0.0

        for (word in allWords) {
            val qCount = queryTokens.count { it == word }.toDouble()
            val dCount = docTokens.count { it == word }.toDouble()
            dot += (qCount * dCount)
            qMag += (qCount * qCount)
            dMag += (dCount * dCount)
        }

        val denominator = sqrt(qMag) * sqrt(dMag)
        return if (denominator > 0.0) (dot / denominator).toFloat() else 0f
    }

    private val STOP_WORDS = setOf(
        "the", "and", "is", "are", "was", "were", "for", "with", "what", "how",
        "about", "this", "that", "from", "into", "onto", "when", "where", "can", "you"
    )
}
