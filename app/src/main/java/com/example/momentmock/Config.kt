package com.example.momentmock

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CommentConfig(
    var avatarPath: String? = null,
    var randomAvatar: Boolean = true,
    var name: String = "",
    var timeMillis: Long = 0L,
    var text: String = "",
    var replyTo: String = "",
)

data class MomentConfig(
    var nickname: String = "",
    var avatarPath: String? = null,
    var content: String = "",
    var timeMillis: Long = 0L,
    var location: String = "",
    var imagePaths: MutableList<String> = mutableListOf(),
    var videoFrame: String? = null,
    var articleEnabled: Boolean = false,
    var articleTitle: String = "",
    var articleDesc: String = "",
    var articleSource: String = "",
    var articleThumb: String? = null,
    var likeCount: Int = 0,
    var comments: MutableList<CommentConfig> = mutableListOf(),
) {
    companion object {
        private val fmtFull = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA)
        private val fmtDate = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)
        private val fmtTime = SimpleDateFormat("kk:mm", Locale.CHINA)

        fun formatFull(millis: Long): String = if (millis <= 0) "" else fmtFull.format(Date(millis))

        fun nowMillis(): Long = System.currentTimeMillis()

        fun parseFull(text: String): Long? = runCatching { fmtFull.parse(text)?.time }.getOrNull()

        fun formatDetail(millis: Long): String {
            if (millis <= 0) return ""
            val now = Calendar.getInstance()
            val t = Calendar.getInstance().apply { timeInMillis = millis }
            val sameYear = now.get(Calendar.YEAR) == t.get(Calendar.YEAR)
            val sameMonth = sameYear && now.get(Calendar.MONTH) == t.get(Calendar.MONTH)
            val today = sameMonth && now.get(Calendar.DAY_OF_MONTH) == t.get(Calendar.DAY_OF_MONTH)

            var yesterday = sameMonth && now.get(Calendar.DAY_OF_MONTH) - 1 == t.get(Calendar.DAY_OF_MONTH)
            if (!yesterday &&
                (sameYear && now.get(Calendar.MONTH) - 1 == t.get(Calendar.MONTH) ||
                    now.get(Calendar.YEAR) - t.get(Calendar.YEAR) == 1)
            ) {
                val back = Calendar.getInstance().apply {
                    timeInMillis = now.timeInMillis
                    add(Calendar.DAY_OF_MONTH, -1)
                }
                yesterday = back.get(Calendar.YEAR) == t.get(Calendar.YEAR) &&
                    back.get(Calendar.MONTH) == t.get(Calendar.MONTH) &&
                    back.get(Calendar.DAY_OF_MONTH) == t.get(Calendar.DAY_OF_MONTH)
            }

            val time = fmtTime.format(Date(millis))
            return when {
                today -> time
                yesterday -> "昨天 $time"
                else -> fmtDate.format(Date(millis)) + " " + time
            }
        }
    }
}

object ConfigStore {
    private const val PREF = "moment_config"
    private const val KEY = "json_v2"

    fun load(ctx: Context): MomentConfig {
        val raw = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return MomentConfig(timeMillis = MomentConfig.nowMillis())
        return runCatching { fromJson(JSONObject(raw)) }.getOrElse {
            MomentConfig(timeMillis = MomentConfig.nowMillis())
        }
    }

    fun save(ctx: Context, cfg: MomentConfig) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString(KEY, toJson(cfg).toString())
            .apply()
    }

    private fun toJson(c: MomentConfig) = JSONObject().apply {
        put("nickname", c.nickname)
        put("avatarPath", c.avatarPath ?: "")
        put("content", c.content)
        put("timeMillis", c.timeMillis)
        put("location", c.location)
        put("imagePaths", JSONArray(c.imagePaths))
        put("videoFrame", c.videoFrame ?: "")
        put("articleEnabled", c.articleEnabled)
        put("articleTitle", c.articleTitle)
        put("articleDesc", c.articleDesc)
        put("articleSource", c.articleSource)
        put("articleThumb", c.articleThumb ?: "")
        put("likeCount", c.likeCount)
        put("comments", JSONArray().apply {
            c.comments.forEach { m ->
                put(JSONObject().apply {
                    put("avatarPath", m.avatarPath ?: "")
                    put("randomAvatar", m.randomAvatar)
                    put("name", m.name)
                    put("timeMillis", m.timeMillis)
                    put("text", m.text)
                    put("replyTo", m.replyTo)
                })
            }
        })
    }

    private fun fromJson(o: JSONObject) = MomentConfig(
        nickname = o.optString("nickname"),
        avatarPath = o.optString("avatarPath").ifBlank { null },
        content = o.optString("content"),
        timeMillis = o.optLong("timeMillis", 0L).takeIf { it > 0 }
            ?: MomentConfig.parseFull(o.optString("timeText")) ?: 0L,
        location = o.optString("location"),
        imagePaths = mutableListOf<String>().apply {
            val arr = o.optJSONArray("imagePaths") ?: JSONArray()
            for (i in 0 until arr.length()) add(arr.getString(i))
        },
        videoFrame = o.optString("videoFrame").ifBlank { null },
        articleEnabled = o.optBoolean("articleEnabled"),
        articleTitle = o.optString("articleTitle"),
        articleDesc = o.optString("articleDesc"),
        articleSource = o.optString("articleSource"),
        articleThumb = o.optString("articleThumb").ifBlank { null },
        likeCount = o.optInt("likeCount", 0),
        comments = mutableListOf<CommentConfig>().apply {
            val arr = o.optJSONArray("comments") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val m = arr.getJSONObject(i)
                add(
                    CommentConfig(
                        avatarPath = m.optString("avatarPath").ifBlank { null },
                        randomAvatar = m.optBoolean("randomAvatar", true),
                        name = m.optString("name"),
                        timeMillis = m.optLong("timeMillis", 0L).takeIf { it > 0 }
                            ?: MomentConfig.parseFull(m.optString("time")) ?: 0L,
                        text = m.optString("text"),
                        replyTo = m.optString("replyTo"),
                    ),
                )
            }
        },
    )
}
