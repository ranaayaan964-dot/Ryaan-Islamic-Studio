package com.example.data.repository

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.util.Calendar
import kotlin.random.Random

private const val TAG = "KidsCinemaRepository"

data class KidsCinemaEpisode(
    val videoId: String,
    val title: String,
    val description: String,
    val category: String,
    val duration: String,
    val ageRating: String,
    val youtubeUrl: String = "https://www.youtube.com/watch?v=$videoId",
    val thumbnailUrl: String = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
    val storyText: String,
    val moral: String,
    val quizQuestion: String = "",
    val quizOptions: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val isLiveYouTube: Boolean = false,
    val publishedAt: Long = 0L
)

class KidsCinemaRepository {
    private val client = OkHttpClient()

    private val liveChannelIds = listOf(
        "UCNdUFOtzSx3II7urP-AFc7w",
        "UCnLxWb9Gk2Ww0m0J5iQ8ZLQ",
        "UCkQ9B3GfD7rY8eG5I6MpRuA",
        "UCJ8N1ld3mC9xkVy23wOZX0Q"
    )

    fun curatedStories(): List<KidsCinemaEpisode> = listOf(
        KidsCinemaEpisode(
            videoId = "uvhAUB2VxMg",
            title = "Prophet Adam (AS)",
            description = "The first human, the first prophet, and the mercy of Allah from the beginning of creation.",
            category = "Prophet Stories",
            duration = "10:00",
            ageRating = "Ages 4-12",
            storyText = "Allah created Adam (AS) and taught him knowledge. Adam and his wife lived in the Garden, but after they made a mistake, they turned back to Allah with sincere repentance. Allah accepted their repentance and sent them to live on earth, where Adam became the first prophet and taught his family to worship Allah.",
            moral = "When we make a mistake, we should admit it, ask Allah for forgiveness, and try to do better.",
            quizQuestion = "Who was the first human and prophet?",
            quizOptions = listOf("Prophet Adam (AS)", "Prophet Nuh (AS)", "Prophet Musa (AS)", "Prophet Ibrahim (AS)"),
            correctIndex = 0
        ),
        KidsCinemaEpisode(
            videoId = "NgBkdtrrKLg",
            title = "Prophet Nuh (AS)",
            description = "A story of patience, trust, and Allah's mercy through the Ark and the flood.",
            category = "Prophet Stories",
            duration = "12:15",
            ageRating = "Ages 4-12",
            storyText = "Prophet Nuh (AS) called his people to worship Allah alone for many years. When they refused, Allah told him to build an Ark. Nuh (AS) and the believers entered it, and pairs of animals were taken aboard. Allah saved them through the flood, and the Ark came to rest by His mercy.",
            moral = "Stay patient when doing what is right, and trust Allah even when a challenge feels difficult.",
            quizQuestion = "What pairs were saved by Allah on the Ark?",
            quizOptions = listOf("A male and female pair of every creature", "Only birds", "Only animals", "No one"),
            correctIndex = 0
        ),
        KidsCinemaEpisode(
            videoId = "-5HQ9iH0zQY",
            title = "Prophet Ibrahim (AS)",
            description = "The Kaaba, trust in Allah, and the courage to worship Him alone.",
            category = "Seerah of Prophet (SAW)",
            duration = "10:00",
            ageRating = "Ages 5-12",
            storyText = "Prophet Ibrahim (AS) taught his people to worship Allah alone and not the idols they had made. He remained firm in faith when his people opposed him, and Allah protected him. Later, Ibrahim (AS) and his son Ismail (AS) raised the foundations of the Kaaba in Makkah as a place of worship for Allah.",
            moral = "Be brave in faith, think carefully about what is true, and obey Allah with sincerity.",
            quizQuestion = "Who helped Prophet Ibrahim (AS) raise the foundations of the Kaaba?",
            quizOptions = listOf("Prophet Ismail (AS)", "Prophet Musa (AS)", "Prophet Yunus (AS)", "Prophet Isa (AS)"),
            correctIndex = 0
        ),
        KidsCinemaEpisode(
            videoId = "19SbvfIKu_w",
            title = "Prophet Yusuf (AS)",
            description = "Patience through hardship, faithfulness, and forgiveness.",
            category = "Prophet Stories",
            duration = "10:00",
            ageRating = "Ages 5-12",
            storyText = "Prophet Yusuf (AS) faced separation from his family and many unfair hardships. He stayed patient, honest, and mindful of Allah. Allah eventually gave him a position of trust in Egypt. When his brothers came to him in need, Yusuf (AS) forgave them and reunited with his family.",
            moral = "Patience, honesty, and forgiveness can turn a hard time into a chance to do good.",
            quizQuestion = "Which quality did Prophet Yusuf (AS) show when he forgave his brothers?",
            quizOptions = listOf("Forgiveness", "Pride", "Impatience", "Jealousy"),
            correctIndex = 0
        ),
        KidsCinemaEpisode(
            videoId = "kJjM4WmQjL8",
            title = "Prophet Ayyub (AS)",
            description = "A story of patience, prayer, and trust in Allah during hardship.",
            category = "Prophet Stories",
            duration = "10:00",
            ageRating = "Ages 5-12",
            storyText = "Prophet Ayyub (AS) endured a long period of hardship and called upon Allah with humility. He did not lose hope or turn away from his Lord. Allah answered his prayer, removed his suffering, and restored blessings to him.",
            moral = "Ask Allah for help, remain patient, and remember that hardship does not last forever.",
            quizQuestion = "What did Prophet Ayyub (AS) do during his hardship?",
            quizOptions = listOf("He remained patient and prayed to Allah", "He gave up on Allah", "He left his home", "He stopped trusting others"),
            correctIndex = 0
        ),
        KidsCinemaEpisode(
            videoId = "m-78Vf2TByk",
            title = "Prophet Yunus (AS)",
            description = "A reminder to turn back to Allah whenever we feel lost.",
            category = "Prophet Stories",
            duration = "10:00",
            ageRating = "Ages 5-12",
            storyText = "Prophet Yunus (AS) left his people before receiving Allah's command to do so. He was swallowed by a great fish and called upon Allah from the darkness, admitting his mistake and praising Him. Allah answered his prayer, rescued him, and gave him another opportunity to guide his people.",
            moral = "Turn back to Allah in every difficulty and never lose hope in His mercy.",
            quizQuestion = "How did Prophet Yunus (AS) remember Allah in his hardship?",
            quizOptions = listOf("By calling upon Allah with humility", "By blaming others", "By leaving faith", "By ignoring everyone"),
            correctIndex = 0
        ),
        KidsCinemaEpisode(
            videoId = "WEEORXFj-lo",
            title = "Prophet Muhammad (SAW)",
            description = "A beautiful introduction to the final Messenger, his mercy, patience, and character.",
            category = "Seerah of Prophet (SAW)",
            duration = "14:20",
            ageRating = "Ages 4-14",
            storyText = "Prophet Muhammad (SAW) was born in Makkah and became known for honesty and trustworthiness. At the age of forty, he received the first revelation and called people to worship Allah alone, care for the vulnerable, and live with good character.",
            moral = "Follow the Prophet's example through honesty, mercy, patience, and care for others.",
            quizQuestion = "What was the Prophet Muhammad (SAW) known as before prophethood?",
            quizOptions = listOf("Al-Amin (The Trustworthy)", "The Merchant", "The Traveler", "The King"),
            correctIndex = 0
        ),
        KidsCinemaEpisode(
            videoId = "D22G5RFef1M",
            title = "Prophet Dawud (AS) vs Jalut",
            description = "The Quranic account of Talut, Jalut, and Dawud's courage.",
            category = "Prophet Stories",
            duration = "11:00",
            ageRating = "Ages 4-12",
            storyText = "Prophet Dawud (AS) was given wisdom and prophethood. As a young believer, he faced Jalut with courage, and Allah granted him victory. Later, Dawud (AS) ruled with justice, remembered Allah often, and used his gifts in worship and service.",
            moral = "Use courage and ability to serve what is right, and treat people fairly.",
            quizQuestion = "Who defeated Jalut by Allah's permission?",
            quizOptions = listOf("Dawud", "Talut", "Musa", "Nuh"),
            correctIndex = 0
        ),
        KidsCinemaEpisode(
            videoId = "s3dvuMlsuy8",
            title = "Prophet Musa (AS) at the Sea",
            description = "A story of courage, faith, and Allah's rescue when the sea was split.",
            category = "Prophet Stories",
            duration = "12:00",
            ageRating = "Ages 4-12",
            storyText = "Prophet Musa (AS) was sent to call Pharaoh and his people to worship Allah and free the Children of Israel. When they reached the sea, Allah made a path for them. The believers crossed safely while Pharaoh's pursuit came to an end.",
            moral = "Keep faith in Allah and stand up for justice, even when you feel outnumbered.",
            quizQuestion = "What miracle was Prophet Musa (AS) granted at the sea?",
            quizOptions = listOf("The sea parted", "A mountain moved", "It rained gold", "A river appeared"),
            correctIndex = 0
        ),
        KidsCinemaEpisode(
            videoId = "JbfEZMdgZ_A",
            title = "Prophet Sulaiman (AS)",
            description = "A story of wisdom, gratitude, and compassion for Allah's creation.",
            category = "Prophet Stories",
            duration = "15:00",
            ageRating = "Ages 6-14",
            storyText = "Allah gave Prophet Sulaiman (AS) great gifts, wisdom, and authority. He understood that every blessing came from Allah. He used his authority with gratitude and care.",
            moral = "Be grateful for what you have, and treat even the smallest creatures with care.",
            quizQuestion = "Who granted Prophet Sulaiman (AS) wisdom and blessings?",
            quizOptions = listOf("Allah", "A king", "A traveler", "A merchant"),
            correctIndex = 0
        ),
        KidsCinemaEpisode(
            videoId = "hkSkWt48U5Y",
            title = "Prophet Ismail (AS) & Zamzam",
            description = "The account of Ismail's trust in Allah and the spring of Zamzam.",
            category = "Seerah of Prophet (SAW)",
            duration = "11:00",
            ageRating = "Ages 5-14",
            storyText = "Prophet Ibrahim (AS) brought his young son Ismail (AS) and Hajar to the valley of Makkah, trusting Allah's care. When their water ran out, Hajar searched between Safa and Marwah for help. Allah provided the spring of Zamzam.",
            moral = "Trust Allah, keep trying in hard moments, and work together in doing good.",
            quizQuestion = "Who helped Prophet Ibrahim (AS) raise the foundations of the Kaaba?",
            quizOptions = listOf("Prophet Ismail (AS)", "Prophet Dawud (AS)", "Prophet Isa (AS)", "Prophet Nuh (AS)"),
            correctIndex = 0
        ),
        KidsCinemaEpisode(
            videoId = "l7s8R9ugb98",
            title = "Prophet Isa (AS)",
            description = "A gentle introduction to Prophet Isa (AS) and his message of faith.",
            category = "Prophet Stories",
            duration = "10:00",
            ageRating = "Ages 4-12",
            storyText = "Prophet Isa (AS) was born to Maryam (AS) by Allah's command. Allah made Isa (AS) a prophet and gave him signs, including healing the sick by Allah's permission. Isa (AS) called people to worship Allah and live faithfully.",
            moral = "Be kind, speak truthfully, and remember that every good gift comes from Allah.",
            quizQuestion = "Who was the mother of Prophet Isa (AS)?",
            quizOptions = listOf("Maryam (AS)", "Asiya (AS)", "Hajar (AS)", "Sarah (AS)"),
            correctIndex = 0
        ),
        KidsCinemaEpisode(
            videoId = "Q_J0YpY-3vM",
            title = "The Five Daily Prayers",
            description = "A gentle reminder that Salah is the pillar that keeps our hearts connected to Allah.",
            category = "Quran & Duas",
            duration = "08:30",
            ageRating = "Ages 4-14",
            storyText = "Allah gave us the gift of Salah as a direct connection to Him. Performing the five daily prayers with sincerity brings calm, gratitude, and discipline to the heart.",
            moral = "Make prayer a priority and protect your relationship with Allah every day.",
            quizQuestion = "How many daily prayers are prescribed in Islam?",
            quizOptions = listOf("Five", "Three", "Two", "Seven"),
            correctIndex = 0
        ),
        KidsCinemaEpisode(
            videoId = "s7KcL_lSR20",
            title = "The Story of Hajar and Zamzam",
            description = "A story of trust, courage, and Allah's mercy in the desert of Makkah.",
            category = "Quran & Duas",
            duration = "09:15",
            ageRating = "Ages 5-12",
            storyText = "Hajar (RA) searched for water between Safa and Marwah while she was with her son Ismail (AS). In her desperate moment, Allah answered her dua by making the spring of Zamzam flow. Allah's mercy never leaves His servants alone.",
            moral = "Have trust in Allah and keep hoping even in difficult moments.",
            quizQuestion = "Where did Hajar search for water?",
            quizOptions = listOf("Between Safa and Marwah", "Near the sea", "At the mountain top", "In the market"),
            correctIndex = 0
        ),
        KidsCinemaEpisode(
            videoId = "zXJH2f6g8N8",
            title = "The Prophet's Manners",
            description = "A child-friendly lesson on honesty, respect, and kindness in daily life.",
            category = "Islamic Manners",
            duration = "08:45",
            ageRating = "Ages 4-12",
            storyText = "The Prophet Muhammad (SAW) taught us to be kind, truthful, patient, and respectful. His character was a reflection of mercy, and he encouraged us to treat everyone with dignity.",
            moral = "Small acts of kindness and honesty are a beautiful way to follow the Prophet's Sunnah.",
            quizQuestion = "Which quality was the Prophet known for?",
            quizOptions = listOf("Kindness", "Anger", "Laziness", "Pride"),
            correctIndex = 0
        )
    )

    suspend fun loadDailyStories(): List<KidsCinemaEpisode> = withContext(Dispatchers.IO) {
        val curated = curatedStories()
        val liveStories = runCatching { fetchLiveYouTubeFeed() }.getOrElse { emptyList() }
        val merged = (liveStories + curated).distinctBy { it.videoId }
        val seed = Calendar.getInstance().let { it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR) }
        merged.shuffled(Random(seed))
    }

    fun getDailyRotatedStories(): List<KidsCinemaEpisode> {
        val seed = Calendar.getInstance().let { it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR) }
        val rotated = curatedStories().shuffled(Random(seed))
        return rotated.take(8)
    }

    private fun fetchLiveYouTubeFeed(): List<KidsCinemaEpisode> {
        val items = mutableListOf<KidsCinemaEpisode>()
        for (channelId in liveChannelIds) {
            val url = "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
            val request = Request.Builder().url(url).get().build()
            val response = runCatching { client.newCall(request).execute() }.getOrNull() ?: continue
            if (!response.isSuccessful) {
                response.close()
                continue
            }
            response.body?.byteStream()?.use { inputStream ->
                val parser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
                parser.setInput(inputStream, "UTF-8")
                var eventType = parser.eventType
                var currentVideoId: String? = null
                var currentTitle: String? = null
                var currentPublished: String? = null
                var inEntry = false
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            val name = parser.name
                            when {
                                name == "entry" -> inEntry = true
                                inEntry && name == "videoId" -> currentVideoId = parser.nextText().trim()
                                inEntry && name == "title" && currentTitle.isNullOrBlank() -> currentTitle = parser.nextText().trim()
                                inEntry && name == "published" && currentPublished.isNullOrBlank() -> currentPublished = parser.nextText().trim()
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name == "entry" && currentVideoId != null && currentTitle != null) {
                                val publishedAt = runCatching { currentPublished?.let { java.time.Instant.parse(it).toEpochMilli() } ?: 0L }.getOrDefault(0L)
                                items += KidsCinemaEpisode(
                                    videoId = currentVideoId!!,
                                    title = currentTitle!!,
                                    description = "Fresh Islamic family-safe learning from a trusted YouTube channel.",
                                    category = "Latest YouTube",
                                    duration = "08:00",
                                    ageRating = "Ages 4-14",
                                    storyText = "This story was published in a live YouTube learning feed and is available for family-friendly viewing.",
                                    moral = "Keep learning with value-based Islamic content in a safe and uplifting environment.",
                                    quizQuestion = "This title is from the trusted live feed.",
                                    quizOptions = listOf("Yes", "No"),
                                    correctIndex = 0,
                                    isLiveYouTube = true,
                                    publishedAt = publishedAt
                                )
                                currentVideoId = null
                                currentTitle = null
                                currentPublished = null
                                inEntry = false
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }
            response.close()
        }
        return items.distinctBy { it.videoId }.sortedByDescending { it.publishedAt }
    }
}
