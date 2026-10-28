package com.example.momentmock

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import java.util.Calendar

class ConfigScreen(
    private val ctx: Activity,
    private val cfg: MomentConfig,
    private val onChange: () -> Unit,
    private val pickImage: ((Uri) -> Unit) -> Unit,
    private val pickVideo: ((Uri) -> Unit) -> Unit,
    private val onGenerate: () -> Unit,
) {

    private val bgPage = 0xFFF2F3F5.toInt()
    private val bgCard = Color.WHITE
    private val bgInput = 0xFFF6F7F8.toInt()
    private val cText = 0xFF191919.toInt()
    private val cHint = 0xFFAEAEB2.toInt()
    private val cLabel = 0xFF8A8A8E.toInt()
    private val cPrimary = 0xFF07C160.toInt()

    private val mp = ViewGroup.LayoutParams.MATCH_PARENT
    private val wc = ViewGroup.LayoutParams.WRAP_CONTENT

    private lateinit var scroll: ScrollView
    private lateinit var content: LinearLayout

    private var imagesScrollX = 0
    private var scrollImagesToEnd = false

    fun build(): View {
        val root = FrameLayout(ctx).apply { setBackgroundColor(bgPage) }
        val column = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        val bar = FrameLayout(ctx).apply { setBackgroundColor(Color.WHITE) }
        bar.addView(
            label("朋友圈生成", 18f, cText, bold = true).apply {
                setPadding(ctx.dp(20f), 0, 0, 0)
                gravity = Gravity.CENTER_VERTICAL
            },
            FrameLayout.LayoutParams(mp, ctx.dp(56f)),
        )
        column.addView(bar.designed(LinearLayout.LayoutParams(mp, ctx.dp(56f))))

        content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.dp(16f), ctx.dp(16f), ctx.dp(16f), ctx.dp(16f))
        }
        scroll = ScrollView(ctx).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(content)
        }
        column.addView(scroll, LinearLayout.LayoutParams(mp, 0, 1f))
        column.addView(buildBottomBar())
        root.addView(column, FrameLayout.LayoutParams(mp, mp))

        renderContent()
        return root
    }

    private fun refresh() {
        val y = if (::scroll.isInitialized) scroll.scrollY else 0
        renderContent()
        if (::scroll.isInitialized) scroll.post { scroll.scrollTo(0, y) }
    }

    private fun renderContent() {
        content.removeAllViews()
        content.addView(buildAuthor())
        content.addView(buildText())
        content.addView(buildImages())
        content.addView(buildVideo())
        content.addView(buildArticle())
        content.addView(buildLikes())
        content.addView(buildComments())
    }

    private fun buildAuthor(): View = card {
        add(sectionTitle("发布人"))
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val avatar = RoundedImageView(ctx).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            cornerRadius = ctx.dpf(10f)
            val bmp = Images.load(ctx, cfg.avatarPath)
            if (bmp != null) setImageBitmap(bmp) else {
                background = rounded(bgInput, 10f)
                setImageResource(android.R.drawable.ic_menu_camera)
                setPadding(ctx.dp(16f), ctx.dp(16f), ctx.dp(16f), ctx.dp(16f))
            }
            setOnClickListener {
                pick { uri ->
                    cfg.avatarPath = Images.saveFromUri(ctx, uri)
                    onChange()
                    refresh()
                }
            }
        }
        row.addView(avatar, LinearLayout.LayoutParams(ctx.dp(64f), ctx.dp(64f)))

        val name = input("昵称", cfg.nickname, 15f) { cfg.nickname = it; onChange() }
        row.addView(name, LinearLayout.LayoutParams(0, wc, 1f).apply { marginStart = ctx.dp(14f) })

        add(row)
    }

    private fun buildText(): View = card {
        add(sectionTitle("正文"))
        add(multiline(cfg.content) { cfg.content = it; onChange() })
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, ctx.dp(10f), 0, 0)
        }
        row.addView(timeField(cfg.timeMillis) { cfg.timeMillis = it; onChange(); refresh() },
            LinearLayout.LayoutParams(0, wc, 1f).apply { marginEnd = ctx.dp(8f) })
        row.addView(input("位置", cfg.location, 14f) { cfg.location = it; onChange() },
            LinearLayout.LayoutParams(0, wc, 1f))
        add(row)
    }

    private fun buildImages(): View = card {
        add(sectionTitle("图片"))
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        cfg.imagePaths.forEachIndexed { index, name ->
            row.addView(thumb(name) {
                Images.delete(ctx, cfg.imagePaths.removeAt(index))
                onChange()
                refresh()
            }, LinearLayout.LayoutParams(ctx.dp(76f), ctx.dp(76f)).apply {
                marginEnd = ctx.dp(10f)
            })
        }
        if (cfg.imagePaths.size < 9) {
            row.addView(addTile {
                pick { uri ->
                    val name = Images.saveFromUri(ctx, uri)
                    if (name != null) {
                        clearVideo()
                        clearArticle()
                        cfg.imagePaths.add(name)
                        scrollImagesToEnd = true
                    }
                    onChange()
                    refresh()
                }
            }, LinearLayout.LayoutParams(ctx.dp(76f), ctx.dp(76f)))
        }
        val scroller = HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        }
        scroller.setOnScrollChangeListener { _, x, _, _, _ -> imagesScrollX = x }
        scroller.post {
            if (scrollImagesToEnd) {
                scrollImagesToEnd = false
                scroller.fullScroll(View.FOCUS_RIGHT)
            } else {
                scroller.scrollTo(imagesScrollX, 0)
            }
        }
        add(scroller)
    }

    private fun buildVideo(): View = card {
        add(sectionTitle("视频"))
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val frame = cfg.videoFrame
        if (frame != null) {
            row.addView(thumb(frame) {
                Images.delete(ctx, cfg.videoFrame)
                cfg.videoFrame = null
                onChange()
                refresh()
            }, LinearLayout.LayoutParams(ctx.dp(76f), ctx.dp(76f)))
        } else {
            row.addView(addTile {
                pickVideo { uri ->
                    val name = Images.saveVideoFrame(ctx, uri)
                    if (name != null) {
                        cfg.imagePaths.forEach { Images.delete(ctx, it) }
                        cfg.imagePaths.clear()
                        clearArticle()
                        cfg.videoFrame = name
                    }
                    onChange()
                    refresh()
                }
            }, LinearLayout.LayoutParams(ctx.dp(76f), ctx.dp(76f)))
        }
        add(row)
    }

    private fun clearVideo() {
        Images.delete(ctx, cfg.videoFrame)
        cfg.videoFrame = null
    }

    private fun clearArticle() {
        cfg.articleEnabled = false
    }

    private fun buildArticle(): View = card {
        val head = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        head.addView(sectionTitle("公众号卡片").apply {
            layoutParams = LinearLayout.LayoutParams(0, wc, 1f)
        })
        head.addView(Switch(ctx).apply {
            isChecked = cfg.articleEnabled
            setOnCheckedChangeListener { _, v ->
                cfg.articleEnabled = v
                if (v) {
                    cfg.imagePaths.forEach { Images.delete(ctx, it) }
                    cfg.imagePaths.clear()
                    clearVideo()
                }
                onChange()
                refresh()
            }
        })
        add(head)

        if (cfg.articleEnabled) {
            add(
                input("标题", cfg.articleTitle, 15f) { cfg.articleTitle = it; onChange() }
                    .designed(LinearLayout.LayoutParams(mp, wc).apply { topMargin = ctx.dp(10f) }),
            )
            add(
                multiline(cfg.articleDesc, lines = 2, hintText = "描述") { cfg.articleDesc = it; onChange() }
                    .designed(LinearLayout.LayoutParams(mp, wc).apply { topMargin = ctx.dp(10f) }),
            )
            add(
                input("来源", cfg.articleSource, 15f) { cfg.articleSource = it; onChange() }
                    .designed(LinearLayout.LayoutParams(mp, wc).apply { topMargin = ctx.dp(10f) }),
            )

            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.addView(
                RoundedImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    cornerRadius = ctx.dpf(8f)
                    val bmp = Images.load(ctx, cfg.articleThumb)
                    if (bmp != null) setImageBitmap(bmp) else {
                        background = rounded(bgInput, 8f)
                        setImageResource(android.R.drawable.ic_menu_gallery)
                        setPadding(ctx.dp(14f), ctx.dp(14f), ctx.dp(14f), ctx.dp(14f))
                    }
                    setOnClickListener {
                        pick { uri ->
                            cfg.articleThumb = Images.saveFromUri(ctx, uri)
                            onChange()
                            refresh()
                        }
                    }
                },
                LinearLayout.LayoutParams(ctx.dp(60f), ctx.dp(60f)),
            )
            row.addView(label("封面图", 14f, cLabel).apply { setPadding(ctx.dp(12f), 0, 0, 0) })
            add(row.designed(LinearLayout.LayoutParams(mp, wc).apply { topMargin = ctx.dp(10f) }))
        }
    }

    private fun buildLikes(): View = card {
        add(sectionTitle("点赞"))
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(stepButton("−") {
            cfg.likeCount = (cfg.likeCount - 1).coerceAtLeast(0)
            onChange()
            refresh()
        })
        row.addView(label(cfg.likeCount.toString(), 17f, cText, bold = true).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(ctx.dp(64f), ctx.dp(40f))
        })
        row.addView(stepButton("+") {
            cfg.likeCount = (cfg.likeCount + 1).coerceAtMost(300)
            onChange()
            refresh()
        })
        add(row)
    }

    private fun buildComments(): View = card {
        val head = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        head.addView(sectionTitle("评论").apply { layoutParams = LinearLayout.LayoutParams(0, wc, 1f) })
        head.addView(label("+ 添加", 14f, cPrimary, bold = true).apply {
            setPadding(ctx.dp(8f), ctx.dp(8f), ctx.dp(8f), ctx.dp(8f))
            setOnClickListener {
                cfg.comments.add(CommentConfig(timeMillis = MomentConfig.nowMillis()))
                onChange()
                refresh()
            }
        })
        add(head)

        cfg.comments.forEachIndexed { index, comment -> add(commentCard(index, comment)) }
    }

    private fun commentCard(index: Int, c: CommentConfig): View {
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.dp(12f), ctx.dp(12f), ctx.dp(12f), ctx.dp(12f))
            background = rounded(0xFFF9FAFB.toInt(), 12f)
        }

        val top = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val avatar = RoundedImageView(ctx).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            cornerRadius = ctx.dpf(8f)
            val bmp = Images.load(ctx, c.avatarPath)
            if (bmp != null) setImageBitmap(bmp) else {
                background = rounded(bgInput, 8f)
                setImageResource(android.R.drawable.ic_menu_myplaces)
                setPadding(ctx.dp(10f), ctx.dp(10f), ctx.dp(10f), ctx.dp(10f))
            }
            setOnClickListener {
                c.randomAvatar = false
                pick { uri ->
                    c.avatarPath = Images.saveFromUri(ctx, uri)
                    onChange()
                    refresh()
                }
            }
        }
        top.addView(avatar, LinearLayout.LayoutParams(ctx.dp(44f), ctx.dp(44f)))

        val random = CheckBox(ctx).apply {
            text = "随机"
            textSize = 13f
            setTextColor(cLabel)
            isChecked = c.randomAvatar
            buttonTintList = android.content.res.ColorStateList.valueOf(cPrimary)
            setOnCheckedChangeListener { _, v ->
                c.randomAvatar = v
                if (v) {
                    c.avatarPath?.let { Images.delete(ctx, it) }
                    c.avatarPath = null
                }
                onChange()
                refresh()
            }
        }
        top.addView(random, LinearLayout.LayoutParams(0, wc, 1f).apply { marginStart = ctx.dp(8f) })

        top.addView(label("删除", 13f, 0xFFE05B5B.toInt()).apply {
            setPadding(ctx.dp(8f), ctx.dp(8f), 0, ctx.dp(8f))
            setOnClickListener {
                c.avatarPath?.let { Images.delete(ctx, it) }
                cfg.comments.removeAt(index)
                onChange()
                refresh()
            }
        })
        box.addView(top)

        val line2 = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, ctx.dp(10f), 0, 0)
        }
        line2.addView(input("昵称", c.name, 14f) { c.name = it; onChange() },
            LinearLayout.LayoutParams(0, wc, 1f).apply { marginEnd = ctx.dp(8f) })
        line2.addView(timeField(c.timeMillis) { c.timeMillis = it; onChange(); refresh() },
            LinearLayout.LayoutParams(0, wc, 1f))
        box.addView(line2)

        box.addView(input("评论内容", c.text, 14f, multiline = true) { c.text = it; onChange() }
            .designed(LinearLayout.LayoutParams(mp, wc).apply { topMargin = ctx.dp(8f) }))

        box.addView(input("回复谁", c.replyTo, 14f) { c.replyTo = it; onChange() }
            .designed(LinearLayout.LayoutParams(mp, wc).apply { topMargin = ctx.dp(8f) }))

        return box.designed(LinearLayout.LayoutParams(mp, wc).apply { topMargin = ctx.dp(10f) })
    }

    private fun buildBottomBar(): View {
        val bar = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(ctx.dp(16f), ctx.dp(12f), ctx.dp(16f), ctx.dp(12f))
        }
        bar.addView(
            TextView(ctx).apply {
                text = "生成"
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                background = rounded(cPrimary, 14f)
                setOnClickListener { onGenerate() }
            },
            LinearLayout.LayoutParams(mp, ctx.dp(50f)),
        )
        return bar
    }

    private fun timeField(value: Long, onPicked: (Long) -> Unit): TextView {
        val shown = MomentConfig.formatFull(value)
        return TextView(ctx).apply {
            text = shown.ifBlank { "选择时间" }
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            setTextColor(if (shown.isBlank()) cHint else cText)
            setPadding(ctx.dp(12f), ctx.dp(10f), ctx.dp(12f), ctx.dp(10f))
            background = rounded(bgInput, 10f)
            isSingleLine = true
            setOnClickListener { openPicker(value, onPicked) }
        }
    }

    private fun openPicker(current: Long, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance()
        if (current > 0) cal.timeInMillis = current
        DatePickerDialog(
            ctx,
            { _, y, m, d ->
                val afterDate = Calendar.getInstance().apply {
                    timeInMillis = cal.timeInMillis
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, d)
                }
                TimePickerDialog(
                    ctx,
                    { _, hh, mm ->
                        afterDate.set(Calendar.HOUR_OF_DAY, hh)
                        afterDate.set(Calendar.MINUTE, mm)
                        onPicked(afterDate.timeInMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true,
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    private fun LinearLayout.add(v: View): View {
        addView(v)
        return v
    }

    private fun card(block: LinearLayout.() -> Unit): View {
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.dp(16f), ctx.dp(16f), ctx.dp(16f), ctx.dp(16f))
            background = rounded(bgCard, 16f)
        }
        block(box)
        box.layoutParams = LinearLayout.LayoutParams(mp, wc).apply { bottomMargin = ctx.dp(12f) }
        return box
    }

    private fun sectionTitle(text: String): TextView {
        val tv = label(text, 13f, cLabel)
        tv.setPadding(0, 0, 0, ctx.dp(10f))
        return tv
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false) = TextView(ctx).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        includeFontPadding = false
    }

    private fun input(
        hintText: String,
        value: String,
        size: Float,
        multiline: Boolean = false,
        onChangeText: (String) -> Unit,
    ): EditText = EditText(ctx).apply {
        hint = hintText
        setText(value)
        setSelection(text.length)
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        setTextColor(cText)
        setHintTextColor(cHint)
        setPadding(ctx.dp(12f), ctx.dp(10f), ctx.dp(12f), ctx.dp(10f))
        background = rounded(bgInput, 10f)
        isSingleLine = !multiline
        if (multiline) {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            gravity = Gravity.TOP
        } else {
            inputType = InputType.TYPE_CLASS_TEXT
        }
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = onChangeText(s?.toString().orEmpty())
        })
    }

    private fun multiline(
        value: String,
        lines: Int = 4,
        hintText: String = "",
        onChangeText: (String) -> Unit,
    ): EditText =
        input(hintText, value, 15f, multiline = true, onChangeText).apply { minLines = lines }

    private fun thumb(name: String, onDelete: () -> Unit): View {
        val box = FrameLayout(ctx)
        box.addView(
            RoundedImageView(ctx).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                cornerRadius = ctx.dpf(10f)
                Images.load(ctx, name)?.let { setImageBitmap(it) }
            },
            FrameLayout.LayoutParams(mp, mp),
        )
        box.addView(
            TextView(ctx).apply {
                text = "×"
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = rounded(0xCC000000.toInt(), 10f)
                setOnClickListener { onDelete() }
            },
            FrameLayout.LayoutParams(ctx.dp(20f), ctx.dp(20f), Gravity.END or Gravity.TOP).apply {
                topMargin = -ctx.dp(6f)
                marginEnd = -ctx.dp(6f)
            },
        )
        return box
    }

    private fun addTile(onClick: () -> Unit): View = TextView(ctx).apply {
        text = "+"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 26f)
        setTextColor(cHint)
        gravity = Gravity.CENTER
        background = rounded(bgInput, 10f)
        setOnClickListener { onClick() }
    }

    private fun stepButton(text: String, onClick: () -> Unit): View = TextView(ctx).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
        setTextColor(cText)
        gravity = Gravity.CENTER
        background = rounded(bgInput, 10f)
        setOnClickListener { onClick() }
    }.designed(LinearLayout.LayoutParams(ctx.dp(40f), ctx.dp(40f)))

    private fun rounded(color: Int, radiusDp: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = ctx.dpf(radiusDp)
    }

    private fun pick(cb: (Uri) -> Unit) = pickImage(cb)

    private fun View.designed(lp: ViewGroup.LayoutParams): View {
        layoutParams = lp
        return this
    }
}
