package tw.com.rehome.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Result(
    val mood: String,
    val mode: String,
    val destination: String,
    val activity: String,
    val sensory: String,
    val why: String,
    val cta: String
)