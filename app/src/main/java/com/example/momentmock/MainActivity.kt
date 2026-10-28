package com.example.momentmock

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import kotlin.math.min
import kotlin.math.roundToInt

class MainActivity : Activity() {

    private companion object {

        const val REQ_PICK = 1001
        const val REQ_PICK_VIDEO = 1002
        const val TARGET_WIDTH_DP = 400f
    }

    private lateinit var cfg: MomentConfig
    private var pendingPick: ((Uri) -> Unit)? = null
    private var showingMoment = false

    override fun attachBaseContext(newBase: Context) {
        val baseDm = newBase.resources.displayMetrics
        val minSide = min(baseDm.widthPixels, baseDm.heightPixels)

        val widthDp = (minSide / baseDm.density).roundToInt()
        val target = when {
            widthDp >= 600 -> 700f
            widthDp >= 533 -> 600f
            else -> TARGET_WIDTH_DP
        }

        val density = minSide / target
        val dpi = (160f * density).toInt()

        val c = Configuration(newBase.resources.configuration)
        c.densityDpi = dpi
        val ctx = newBase.createConfigurationContext(c)

        val dm = ctx.resources.displayMetrics
        dm.density = density
        dm.scaledDensity = density * c.fontScale
        dm.densityDpi = dpi
        super.attachBaseContext(ctx)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.navigationBarColor = 0xFFFFFFFF.toInt()
        var flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        window.decorView.systemUiVisibility = flags

        cfg = ConfigStore.load(this)
        showConfig()
    }

    private fun showConfig() {
        showingMoment = false
        window.statusBarColor = 0xFFFFFFFF.toInt()
        val screen = ConfigScreen(
            ctx = this,
            cfg = cfg,
            onChange = { ConfigStore.save(this, cfg) },
            pickImage = { cb -> pickImage(cb) },
            pickVideo = { cb -> pickVideo(cb) },
            onGenerate = { showMoment() },
        )
        setContentView(screen.build())
    }

    private fun showMoment() {
        showingMoment = true
        window.statusBarColor = 0xFFEDEDED.toInt()
        val moment = buildMoment()
        setContentView(MomentScreen(this, moment, onBack = { showConfig() }).build())
    }

    private fun buildMoment(): SnsMoment {
        val comments = cfg.comments.map { c ->
            val avatar = if (c.randomAvatar) Images.randomAvatar(this) else Images.load(this, c.avatarPath)
            SnsComment(
                avatar = avatar,
                name = c.name,
                time = MomentConfig.formatDetail(c.timeMillis),
                text = c.text,
                replyTo = c.replyTo.ifBlank { null },
            )
        }
        val article = if (cfg.articleEnabled) {
            SnsArticle(
                title = cfg.articleTitle,
                desc = cfg.articleDesc,
                thumb = Images.load(this, cfg.articleThumb),
                source = cfg.articleSource,
            )
        } else {
            null
        }
        val video = if (article == null) Images.load(this, cfg.videoFrame) else null
        val images = if (article == null && video == null) {
            cfg.imagePaths.mapNotNull { Images.load(this, it) }
        } else {
            emptyList()
        }
        return SnsMoment(
            avatar = Images.load(this, cfg.avatarPath),
            nickname = cfg.nickname,
            content = cfg.content,
            images = images,
            video = video,
            timeText = MomentConfig.formatDetail(cfg.timeMillis),
            location = cfg.location.ifBlank { null },
            likes = Images.randomAvatars(this, cfg.likeCount),
            comments = comments,
            article = article,
        )
    }

    private fun pickImage(cb: (Uri) -> Unit) {
        pendingPick = cb
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, null), REQ_PICK)
    }

    private fun pickVideo(cb: (Uri) -> Unit) {
        pendingPick = cb
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, null), REQ_PICK_VIDEO)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQ_PICK || requestCode == REQ_PICK_VIDEO) {
            if (resultCode == RESULT_OK) data?.data?.let { uri -> pendingPick?.invoke(uri) }
            pendingPick = null
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (showingMoment) showConfig() else super.onBackPressed()
    }
}
