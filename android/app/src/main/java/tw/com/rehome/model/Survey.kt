package tw.com.rehome.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Survey(
    val questions: List<Question>
)

@JsonClass(generateAdapter = true)
data class Question(
    val id: Int,
    @Json(name = "text") val question: String,
    val options: List<String>?
)