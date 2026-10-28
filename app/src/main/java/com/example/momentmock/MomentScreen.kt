package com.example.momentmock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.PorterDuff
import android.graphics.Paint
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView

class MomentScreen(
    private val ctx: Context,
    private val moment: SnsMoment,
    private val onBack: (() -> Unit)? = null,
) {

    private val mp = ViewGroup.LayoutParams.MATCH_PARENT
    private val wc = ViewGroup.LayoutParams.WRAP_CONTENT

    private val colorName = 0xFF576B95.toInt()
    private val colorContent = 0xE6000000.toInt()
    private val colorMeta = 0x4D000000.toInt()
    private val colorCommentText = 0xE5000000.toInt()
    private val colorCardBg = 0xFFF7F7F7.toInt()
    private val colorHeaderBg = 0xFFEDEDED.toInt()

    private val WECHAT_LINE_SPACING_MULTIPLIER = 1.1f

    private val IMPROVE_STYLE = true

    private val VIDEO_SHORT_DP = 169f
    private val VIDEO_LONG_DP = 225f
    private val PLAY_ICON_PX = 120

    fun build(): View {
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }
        root.addView(buildNavBar())

        val scroll = ScrollView(ctx).apply {

            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(buildContent(), ViewGroup.LayoutParams(mp, wc))
        }
        root.addView(scroll, LinearLayout.LayoutParams(mp, 0, 1f))
        root.addView(buildFooter())
        return root
    }

    private fun buildNavBar(): View {
        val bar = FrameLayout(ctx).apply { setBackgroundColor(colorHeaderBg) }

        val back = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_sns_back)
            setOnClickListener { onBack?.invoke() }
        }
        bar.addView(
            back,
            FrameLayout.LayoutParams(ctx.dp(24f), ctx.dp(48f)).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                marginStart = ctx.dp(8f)
            },
        )

        val center = FrameLayout(ctx).apply { setPadding(ctx.dp(40f), 0, ctx.dp(40f), 0) }
        bar.addView(center, FrameLayout.LayoutParams(mp, mp))
        val title = text("详情", 17f, 0xFFE6000000.toInt()).apply {
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        center.addView(title, FrameLayout.LayoutParams(wc, mp, Gravity.CENTER))

        applyWeChatBold(title)

        val more = ImageView(ctx).apply { setImageResource(R.drawable.ic_sns_more) }
        bar.addView(
            more,
            FrameLayout.LayoutParams(ctx.dp(24f), ctx.dp(24f)).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                marginEnd = ctx.dp(16f)
            },
        )

        bar.layoutParams = LinearLayout.LayoutParams(mp, ctx.dp(48f))
        return bar
    }

    private fun buildContent(): View {

        val column = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.dp(12f), 0, ctx.dp(12f), 0)
        }
        column.addView(buildPost())
        if (moment.likes.isNotEmpty() || moment.comments.isNotEmpty()) {
            column.addView(buildCard())
        }
        return column
    }

    private fun buildPost(): View {

        val wrap = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, ctx.dp(16f), 0, ctx.dp(14f))
        }

        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }

        val avatar = RoundedImageView(ctx).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            cornerFactor = 0.1f
            if (moment.avatar != null) setImageBitmap(moment.avatar) else setImageResource(R.drawable.bhm)
        }
        row.addView(
            avatar,
            LinearLayout.LayoutParams(ctx.dp(42f), ctx.dp(42f)).apply { topMargin = ctx.dp(1f) },
        )

        val afc = RelativeLayout(ctx)

        val content = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        content.id = View.generateViewId()

        val nameRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val name = text(moment.nickname, 17f, colorName).apply {
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END

            applyWeChatBold(this)
        }
        nameRow.addView(
            name,
            LinearLayout.LayoutParams(0, wc, 1f).apply { marginEnd = ctx.dp(3f) },
        )
        content.addView(nameRow)

        if (moment.content.isNotBlank()) {
            content.addView(
                text(moment.content, 17f, colorContent),
                LinearLayout.LayoutParams(mp, wc).apply { topMargin = ctx.dp(4f) },
            )
        }

        moment.article?.let { article ->
            content.addView(
                buildArticleCard(article),
                LinearLayout.LayoutParams(mp, wc).apply { topMargin = ctx.dp(12f) },
            )
            if (article.source.isNotBlank()) {
                content.addView(
                    text(wechatSep(article.source), 14f, 0xFF576B95.toInt()),
                    LinearLayout.LayoutParams(mp, wc).apply { topMargin = ctx.dp(8f) },
                )
            }
        }

        if (moment.video != null) {
            content.addView(
                buildVideo(),
                LinearLayout.LayoutParams(mp, wc).apply { topMargin = ctx.dp(12f) },
            )
        } else if (moment.images.isNotEmpty()) {
            content.addView(
                buildImages(),
                LinearLayout.LayoutParams(mp, wc).apply { topMargin = ctx.dp(12f) },
            )
        }

        afc.addView(content, RelativeLayout.LayoutParams(mp, wc))

        if (moment.timeText.isNotBlank() || !moment.location.isNullOrBlank()) {
            afc.addView(
                buildMetaRow(),
                RelativeLayout.LayoutParams(wc, wc).apply {

                    addRule(RelativeLayout.BELOW, content.id)
                    marginEnd = ctx.dp(32f)
                },
            )
        }

        val commentBtn = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_sns_post_more)
        }
        afc.addView(
            commentBtn,
            RelativeLayout.LayoutParams(ctx.dp(32f), ctx.dp(20f)).apply {
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                addRule(RelativeLayout.ALIGN_PARENT_END)
                bottomMargin = ctx.dp(1f)
            },
        )

        row.addView(afc, LinearLayout.LayoutParams(0, wc, 1f).apply { marginStart = ctx.dp(8f) })
        wrap.addView(row)
        return wrap
    }

    private fun buildArticleCard(article: SnsArticle): View {
        val improve = IMPROVE_STYLE
        val pad = ctx.dp(3f)

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFFF7F7F7.toInt())
            setPadding(pad, pad, pad, pad)
            if (improve) background = rounded(0xFFF7F7F7.toInt(), 8f)
        }

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            minimumHeight = ctx.dp(50f)
        }

        val thumb = RoundedImageView(ctx).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageBitmap(article.thumb)
            cornerRadius = ctx.dpf(if (improve) 2f else 0f)
        }
        row.addView(
            thumb,
            LinearLayout.LayoutParams(ctx.dp(50f), ctx.dp(50f)).apply {
                setMargins(pad, pad, pad, pad)
            },
        )

        val column = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = ctx.dp(50f)
        }

        val title = text(article.title, 14f, 0xFFE6000000.toInt()).apply {
            ellipsize = android.text.TextUtils.TruncateAt.END

            maxLines = 2
            if (improve) {

                val fm = paint.fontMetricsInt
                setLineSpacing(textSize * 1.4f - (fm.descent - fm.ascent), 1f)
            }
        }
        column.addView(title, LinearLayout.LayoutParams(mp, wc))

        if (article.desc.isNotBlank()) {
            val desc = text(article.desc, 12f, 0xFF8C000000.toInt()).apply {
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            column.addView(desc, LinearLayout.LayoutParams(mp, wc))
        }

        row.addView(
            column,
            LinearLayout.LayoutParams(0, wc, 1f).apply { marginStart = ctx.dp(if (improve) 8f else 6f) },
        )
        card.addView(row, LinearLayout.LayoutParams(mp, wc))
        return card
    }

    private fun buildMetaRow(): View {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, ctx.dp(8f), 0, ctx.dp(2f))
        }

        val inner = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        inner.addView(text(moment.timeText, 14f, colorMeta))
        val loc = moment.location
        if (!loc.isNullOrBlank()) {

            val tv = text(wechatSep(loc), 14f, colorName).apply {
                isSingleLine = true
                ellipsize = null
                setHorizontallyScrolling(true)
                isHorizontalFadingEdgeEnabled = true
                setFadingEdgeLength(ctx.dp(32f))
            }
            inner.addView(tv, LinearLayout.LayoutParams(0, wc, 1f).apply { marginStart = ctx.dp(5f) })
        }
        row.addView(inner, LinearLayout.LayoutParams(0, wc, 1f))

        row.addView(
            ImageView(ctx).apply {
                setImageResource(R.drawable.ic_sns_delete)
                colorFilter = PorterDuffColorFilter(0xFF576B95.toInt(), PorterDuff.Mode.SRC_IN)
                setPadding(ctx.dp(4f), 0, ctx.dp(8f), 0)
                scaleType = ImageView.ScaleType.FIT_CENTER
            },
            LinearLayout.LayoutParams(ctx.dp(28f), mp),
        )
        return row
    }

    private fun imageCellSize(): Int {
        val screenW = ctx.resources.displayMetrics.widthPixels
        var z = (screenW - ctx.dp(103f)) / 3
        if (z > ctx.dp(130f)) z = ctx.dp(130f)
        if (z <= 10) z = 150
        val cap = ctx.dp(120f)
        return if (z >= cap) cap else z
    }

    private fun singleImageSize(bmp: Bitmap): Pair<Int, Int> {
        val maxSide = ctx.dp(200f).toDouble()
        val minSide = ctx.dp(44f).toDouble()
        val w0 = bmp.width.toDouble()
        val h0 = bmp.height.toDouble()
        if (w0 <= 0.0 || h0 <= 0.0) {
            val fallback = ctx.dp(160f)
            return fallback to fallback
        }
        val scale = minOf(maxSide / w0, maxSide / h0)
        var w = w0 * scale
        var h = h0 * scale
        if (w < minSide) {
            val s = minSide / w
            w *= s
            h *= s
        }
        if (h < minSide) {
            val s = minSide / h
            w *= s
            h *= s
        }
        if (w > maxSide) w = maxSide
        if (h > maxSide) h = maxSide
        return w.toInt() to h.toInt()
    }

    private fun buildVideo(): View {
        val bmp = moment.video ?: return View(ctx)
        val landscape = bmp.width > bmp.height
        val w = if (landscape) ctx.dp(VIDEO_LONG_DP) else ctx.dp(VIDEO_SHORT_DP)
        val h = if (landscape) ctx.dp(VIDEO_SHORT_DP) else ctx.dp(VIDEO_LONG_DP)

        val cell = FrameLayout(ctx)
        cell.addView(
            RoundedImageView(ctx).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                cornerRadius = ctx.dpf(2f)
                setBackgroundColor(0xFFF7F7F7.toInt())
                setImageBitmap(bmp)
            },
            FrameLayout.LayoutParams(mp, mp),
        )
        cell.addView(
            PlayIconView(ctx),
            FrameLayout.LayoutParams(PLAY_ICON_PX, PLAY_ICON_PX, Gravity.CENTER),
        )

        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(cell, LinearLayout.LayoutParams(w, h))
        return row
    }

    private fun buildImages(): View {
        val images = moment.images

        val side = imageCellSize()
        if (images.size == 1) {

            val (w, h) = singleImageSize(images[0])
            val iv = RoundedImageView(ctx).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageBitmap(images[0])
            }

            val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(iv, LinearLayout.LayoutParams(w, h))
            return row
        }

        val cols = 3
        val gap = ctx.dp(3f)
        val grid = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        images.chunked(cols).forEachIndexed { rowIndex, chunk ->
            val line = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            chunk.forEachIndexed { index, bmp ->
                val cell = SquareImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setImageBitmap(bmp)
                }

                line.addView(
                    cell,
                    LinearLayout.LayoutParams(side, side).apply {
                        if (index > 0) marginStart = gap
                    },
                )
            }
            grid.addView(
                line,
                LinearLayout.LayoutParams(mp, wc).apply { if (rowIndex > 0) topMargin = gap },
            )
        }
        return grid
    }

    private fun buildCard(): View {
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(colorCardBg, 4f)
        }

        if (moment.likes.isNotEmpty()) card.addView(buildLikes())

        if (moment.comments.isNotEmpty()) {

            if (moment.likes.isNotEmpty()) {
                card.addView(
                    View(ctx).apply { setBackgroundColor(0xFFE5E5E5.toInt()) },
                    LinearLayout.LayoutParams(mp, 1),
                )
            }
            card.addView(buildComments())
        }

        card.layoutParams = LinearLayout.LayoutParams(mp, wc)
        return card
    }

    private fun buildLikes(): View {
        val dm = ctx.resources.displayMetrics
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val rotation = wm.defaultDisplay.rotation
        val screenMin =
            if (rotation == 0 || rotation == 2) minOf(dm.widthPixels, dm.heightPixels)
            else maxOf(dm.widthPixels, dm.heightPixels)

        val icon = ctx.dp(22f)
        val iconMargin = ctx.dp(14f)
        val avatar = ctx.dp(32f)
        val bottomGap = ctx.dp(6f)
        val sidePadding = ctx.dp(12f)

        val f = screenMin - sidePadding * 2 - (icon + iconMargin * 2)

        val base = ctx.dp(4f)
        var bestGap = base
        var bestLeft = Int.MAX_VALUE
        var g = 4.0f
        while (g <= 8.0001f) {
            val gapPx = ctx.dp(g)
            val step = avatar + gapPx
            val rem = if (f > base) (f - base) % step else 0
            val left = if (rem >= avatar) rem - avatar else rem + gapPx
            if (left < bestLeft) {
                bestLeft = left
                bestGap = gapPx
            }
            g += 0.25f
        }

        val step = avatar + bestGap
        var perRow = f / step
        if (f % step > avatar) perRow++
        perRow = perRow.coerceAtLeast(1)

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, ctx.dp(8f), 0, ctx.dp(6f))
        }

        val heart = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_sns_heart)

            scaleType = ImageView.ScaleType.FIT_XY
        }
        row.addView(
            heart,
            LinearLayout.LayoutParams(icon, icon).apply {
                leftMargin = iconMargin
                rightMargin = iconMargin
                topMargin = (avatar - icon) / 2
                gravity = Gravity.TOP
            },
        )

        val grid = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        moment.likes.chunked(perRow).forEach { chunk ->
            val line = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            chunk.forEach { bmp ->
                val iv = RoundedImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setImageBitmap(bmp)
                    cornerFactor = 0.1f
                }
                line.addView(
                    iv,
                    LinearLayout.LayoutParams(avatar, avatar).apply {
                        marginEnd = bestGap
                        bottomMargin = bottomGap
                    },
                )
            }
            grid.addView(line, LinearLayout.LayoutParams(wc, wc))
        }

        row.addView(grid, LinearLayout.LayoutParams(0, wc, 1f))
        return row
    }

    private fun buildComments(): View {
        val list = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        moment.comments.forEachIndexed { index, comment ->

            list.addView(
                buildCommentRow(comment, first = index == 0),
                LinearLayout.LayoutParams(mp, wc),
            )
        }
        return list
    }

    private fun buildCommentRow(comment: SnsComment, first: Boolean): View {

        val wrap = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colorCardBg)
        }

        if (!first) {
            wrap.addView(
                View(ctx).apply { setBackgroundColor(0x1A000000) },
                LinearLayout.LayoutParams(mp, 1).apply {
                    marginStart = ctx.dp(50f)
                    marginEnd = ctx.dp(14f)
                },
            )
        }

        val body = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, ctx.dp(12f), 0, ctx.dp(12f))
        }

        if (first) {

            val icon = ImageView(ctx).apply {
                setImageResource(R.drawable.ic_sns_comment)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                colorFilter = PorterDuffColorFilter(0xFF576B95.toInt(), PorterDuff.Mode.SRC_IN)
            }
            body.addView(
                icon,
                LinearLayout.LayoutParams(ctx.dp(22f), ctx.dp(22f)).apply {
                    marginStart = ctx.dp(14f)
                    marginEnd = ctx.dp(14f)
                    topMargin = ctx.dp(5f)
                },
            )
        } else {

            body.addView(View(ctx), LinearLayout.LayoutParams(ctx.dp(50f), 1))
        }

        val avatar = RoundedImageView(ctx).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageBitmap(comment.avatar)
            cornerFactor = 0.1f
        }
        body.addView(
            avatar,
            LinearLayout.LayoutParams(ctx.dp(32f), ctx.dp(32f)).apply { marginEnd = ctx.dp(8f) },
        )

        val textColumn = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        val nameRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        nameRow.addView(
            text(comment.name, 15f, colorName).apply { applyWeChatBold(this) },
            LinearLayout.LayoutParams(0, wc, 1f),
        )
        nameRow.addView(
            text(comment.time, 12f, colorMeta, unit = TypedValue.COMPLEX_UNIT_SP),
        )
        textColumn.addView(nameRow, LinearLayout.LayoutParams(mp, wc))

        val bodyText = text(comment.text, 15f, colorCommentText)
        comment.replyTo?.takeIf { it.isNotBlank() }?.let { target ->

            val prefix = "回复$target: "
            val spannable = SpannableStringBuilder(prefix).append(comment.text)
            val nameStart = "回复".length
            val nameEnd = nameStart + target.length
            spannable.setSpan(
                ForegroundColorSpan(colorName),
                nameStart,
                nameEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            spannable.setSpan(WeChatBoldSpan(), nameStart, nameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            bodyText.text = spannable
        }
        textColumn.addView(bodyText, LinearLayout.LayoutParams(mp, wc).apply { topMargin = ctx.dp(3f) })

        body.addView(
            textColumn,
            LinearLayout.LayoutParams(0, wc, 1f).apply {
                marginEnd = ctx.dp(14f)
            },
        )

        wrap.addView(body)
        return wrap
    }

    private fun buildFooter(): View {
        val bar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            setBackgroundColor(0xFFF7F7F7.toInt())
            setPadding(ctx.dp(8f), ctx.dp(8f), 0, 0)
            minimumHeight = ctx.dp(56f)
        }

        val left = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
        }

        val field = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.WHITE, 8f)
            minimumHeight = ctx.dp(40f)
        }

        val input = EditText(ctx).apply {
            hint = "发表评论："
            setHintTextColor(0x4D000000)
            setTextColor(0xE5000000.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_PX, ctx.textPx(15f))

            setLineSpacing(0f, WECHAT_LINE_SPACING_MULTIPLIER)
            setPadding(ctx.dp(8f), 0, ctx.dp(8f), 0)
            gravity = Gravity.CENTER_VERTICAL
            maxHeight = ctx.dp(84f)
            minimumHeight = ctx.dp(40f)
            background = null
            isFocusable = false
            isCursorVisible = false
            includeFontPadding = true
        }
        field.addView(input, LinearLayout.LayoutParams(mp, wc))

        left.addView(
            field,
            LinearLayout.LayoutParams(0, wc, 1f).apply { bottomMargin = ctx.dp(8f) },
        )

        left.addView(
            ImageView(ctx).apply {
                setImageResource(R.drawable.ic_sns_emoji)
                scaleType = ImageView.ScaleType.FIT_XY
            },
            LinearLayout.LayoutParams(ctx.dp(32f), ctx.dp(32f)).apply {
                marginStart = ctx.dp(8f)
                bottomMargin = ctx.dp(12f)
            },
        )

        left.addView(
            ImageView(ctx).apply {
                setImageResource(R.drawable.ic_sns_image)
                scaleType = ImageView.ScaleType.FIT_XY
                colorFilter = PorterDuffColorFilter(0xE5000000.toInt(), PorterDuff.Mode.SRC_IN)
            },
            LinearLayout.LayoutParams(ctx.dp(32f), ctx.dp(32f)).apply {
                marginStart = ctx.dp(8f)
                bottomMargin = ctx.dp(12f)
            },
        )

        bar.addView(left, LinearLayout.LayoutParams(0, wc, 1f))

        val right = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(ctx.dp(8f), 0, ctx.dp(8f), 0)
        }
        right.addView(
            text("发送", 16f, 0xFF33000000.toInt()).apply {
                gravity = Gravity.CENTER
                background = rounded(0xFF0D000000.toInt(), 8f)
                minimumWidth = ctx.dp(60f)
            },
            LinearLayout.LayoutParams(wc, ctx.dp(32f)).apply { bottomMargin = ctx.dp(12f) },
        )
        bar.addView(right, LinearLayout.LayoutParams(wc, wc))

        bar.layoutParams = LinearLayout.LayoutParams(mp, wc)
        return bar
    }

    private fun wechatSep(s: String): String =
        s.replace(Regex("[\\s\\u00a0\\u3000]*·[\\s\\u00a0\\u3000]*"), "\u2006·\u2006")

    private fun text(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false,
        unit: Int = TypedValue.COMPLEX_UNIT_DIP,
    ): TextView = TextView(ctx).apply {
        text = value

        setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            ctx.textPx(size, sp = unit == TypedValue.COMPLEX_UNIT_SP),
        )
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        includeFontPadding = true

        setLineSpacing(0f, WECHAT_LINE_SPACING_MULTIPLIER)
    }

    private fun applyWeChatBold(tv: TextView) {
        val p = tv.paint
        p.style = Paint.Style.FILL_AND_STROKE
        p.strokeWidth = 0.8f
    }

    private class WeChatBoldSpan : CharacterStyle() {
        override fun updateDrawState(tp: TextPaint) {
            tp.style = Paint.Style.FILL_AND_STROKE
            tp.strokeWidth = 0.8f
        }
    }

    private fun rounded(color: Int, radiusDp: Float): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = ctx.dpf(radiusDp)
        }
}

private class PlayIconView(ctx: Context) : View(ctx) {

    private val disc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x26000000
    }

    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val ring = Path().apply {
        fillType = Path.FillType.EVEN_ODD
        addCircle(60f, 60f, 60f, Path.Direction.CW)
        addCircle(60f, 60f, 56.4f, Path.Direction.CW)
    }

    private val triangle = Path().apply {
        moveTo(48.0f, 38.16954f)
        cubicTo(48.52208f, 38.16954f, 49.035122f, 38.305786f, 49.48842f, 38.564808f)
        lineTo(82.44173f, 57.39527f)
        cubicTo(83.88028f, 58.2173f, 84.380066f, 60.049866f, 83.55804f, 61.48842f)
        cubicTo(83.29227f, 61.95351f, 82.906815f, 62.338963f, 82.44173f, 62.60473f)
        lineTo(49.48842f, 81.43519f)
        cubicTo(48.049866f, 82.25722f, 46.2173f, 81.75743f, 45.39527f, 80.31888f)
        cubicTo(45.136246f, 79.865585f, 45.0f, 79.35254f, 45.0f, 78.83046f)
        lineTo(45.0f, 41.16954f)
        cubicTo(45.0f, 39.512684f, 46.343147f, 38.16954f, 48.0f, 38.16954f)
        close()
    }

    override fun onDraw(canvas: Canvas) {
        val s = width / 120f
        canvas.save()
        canvas.scale(s, s)
        canvas.drawCircle(60f, 60f, 60f, disc)
        canvas.drawPath(ring, white)
        canvas.drawPath(triangle, white)
        canvas.restore()
    }
}
