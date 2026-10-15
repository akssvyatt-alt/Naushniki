package com.example.headphonepopup

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.sin
import kotlin.math.cos

private fun dp(context: Context, value: Float): Float =
    value * context.resources.displayMetrics.density

private fun roundedBackground(
    colors: IntArray,
    radius: Float,
    strokeColor: Int = Color.TRANSPARENT,
    strokeWidth: Float = 0f
): GradientDrawable = GradientDrawable(
    GradientDrawable.Orientation.TL_BR,
    colors
).apply {
    cornerRadius = radius
    if (strokeWidth > 0f) setStroke(strokeWidth.toInt().coerceAtLeast(1), strokeColor)
}

class MainActivity : Activity() {

    private fun makeText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(13, 15, 22)
        window.navigationBarColor = Color.rgb(13, 15, 22)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(this@MainActivity, 24f).toInt(), dp(this@MainActivity, 28f).toInt(),
                dp(this@MainActivity, 24f).toInt(), dp(this@MainActivity, 24f).toInt())
            background = roundedBackground(
                intArrayOf(Color.rgb(13, 15, 22), Color.rgb(25, 29, 42)),
                0f
            )
        }

        root.addView(makeText("vivo", 17f, Color.rgb(106, 170, 255), true))
        root.addView(makeText("TWS EXPERIENCE", 12f, Color.LTGRAY, true).apply {
            letterSpacing = 0.2f
            setPadding(0, 0, 0, dp(this@MainActivity, 10f).toInt())
        })

        root.addView(makeText("Наушники нового уровня", 25f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(this@MainActivity, 8f).toInt(),
                0, dp(this@MainActivity, 4f).toInt())
        })

        root.addView(makeText(
            "Объёмная анимация кейса и плавное появление карточки",
            14f, Color.rgb(171, 178, 196)
        ).apply {
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(this@MainActivity, 12f).toInt())
        })

        val preview = VivoHeadphonesView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1f
            )
            minimumHeight = dp(this@MainActivity, 250f).toInt()
        }
        root.addView(preview)

        val permissionButton = Button(this).apply {
            text = "1. Проверить разрешения"
            setOnClickListener { requestNeededPermissions() }
        }
        root.addView(permissionButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(this, 52f).toInt()
        ).apply { topMargin = dp(this@MainActivity, 12f).toInt() })

        val animationButton = Button(this).apply {
            text = "2. Запустить анимацию"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, PopupActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
        root.addView(animationButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(this, 52f).toInt()
        ).apply { topMargin = dp(this@MainActivity, 8f).toInt() })

        setContentView(root)
    }

    private fun requestNeededPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                100
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this)
        ) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        } else {
            android.widget.Toast.makeText(
                this,
                "Разрешения Bluetooth и отображения поверх окон проверены",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
}

class PopupActivity : Activity() {

    private var animationView: VivoHeadphonesView? = null
    private var entranceAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(0.48f)

        val deviceName = intent.getStringExtra("device_name") ?: "vivo TWS"
        val battery = intent.getIntExtra("battery", -1)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(
                dp(this@PopupActivity, 22f).toInt(),
                dp(this@PopupActivity, 18f).toInt(),
                dp(this@PopupActivity, 22f).toInt(),
                dp(this@PopupActivity, 18f).toInt()
            )
            background = roundedBackground(
                intArrayOf(Color.rgb(34, 39, 53), Color.rgb(16, 19, 28)),
                dp(this@PopupActivity, 30f),
                Color.argb(55, 255, 255, 255),
                dp(this@PopupActivity, 1f)
            )
            elevation = dp(this@PopupActivity, 24f)
        }

        root.addView(TextView(this).apply {
            text = "CONNECTED"
            textSize = 11f
            letterSpacing = 0.22f
            setTextColor(Color.rgb(104, 225, 171))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        })

        root.addView(TextView(this).apply {
            text = deviceName
            textSize = 24f
            setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            maxLines = 2
            setPadding(0, dp(this@PopupActivity, 5f).toInt(), 0, 0)
        })

        animationView = VivoHeadphonesView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(this@PopupActivity, 265f).toInt()
            )
        }
        root.addView(animationView)

        val batteryText = if (battery in 0..100) "$battery%" else "Подключено"
        root.addView(TextView(this).apply {
            text = "◉ $batteryText"
            textSize = 15f
            setTextColor(Color.rgb(205, 216, 235))
            gravity = Gravity.CENTER
            setPadding(0, dp(this@PopupActivity, 2f).toInt(),
                0, dp(this@PopupActivity, 12f).toInt())
        })

        val closeButton = TextView(this).apply {
            text = "Готово"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = roundedBackground(
                intArrayOf(Color.rgb(66, 112, 232), Color.rgb(79, 88, 224)),
                dp(this@PopupActivity, 17f)
            )
            setPadding(0, dp(this@PopupActivity, 13f).toInt(),
                0, dp(this@PopupActivity, 13f).toInt())
            setOnClickListener { closeAnimated() }
        }
        root.addView(closeButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        val container = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(
                dp(this@PopupActivity, 20f).toInt(),
                dp(this@PopupActivity, 16f).toInt(),
                dp(this@PopupActivity, 20f).toInt(),
                dp(this@PopupActivity, 16f).toInt()
            )
            addView(root, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
            alpha = 0f
            scaleX = 0.82f
            scaleY = 0.82f
            translationY = dp(this@PopupActivity, 60f)
        }

        setContentView(container)

        container.post {
            entranceAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 650
                interpolator = OvershootInterpolator(0.7f)
                addUpdateListener {
                    val p = it.animatedValue as Float
                    container.alpha = p.coerceIn(0f, 1f)
                    container.scaleX = 0.82f + 0.18f * p
                    container.scaleY = 0.82f + 0.18f * p
                    container.translationY = dp(this@PopupActivity, 60f) * (1f - p)
                }
                start()
            }
        }
    }

    private fun closeAnimated() {
        val view = findViewById<View>(android.R.id.content)
        view.animate()
            .alpha(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .translationY(dp(this, 35f))
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { finish() }
            .start()
    }

    override fun onDestroy() {
        entranceAnimator?.cancel()
        animationView?.stopAnimation()
        super.onDestroy()
    }
}

class VivoHeadphonesView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var openProgress = 0f
    private var floatPhase = 0f
    private var glowPhase = 0f
    private var animator: ValueAnimator? = null

    init {
        startAnimation()
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener {
                val p = it.animatedValue as Float
                openProgress = when {
                    p < 0.2f -> p / 0.2f
                    p < 0.8f -> 1f
                    else -> (1f - p) / 0.2f
                }.coerceIn(0f, 1f)
                floatPhase = p * (Math.PI * 2.0).toFloat()
                glowPhase = p
                invalidate()
            }
            start()
        }
    }

    fun stopAnimation() {
        animator?.cancel()
        animator = null
    }

    private fun fill(color: Int) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.shader = null
        paint.clearShadowLayer()
    }

    private fun linear(
        x0: Float, y0: Float, x1: Float, y1: Float,
        colors: IntArray
    ) {
        paint.shader = LinearGradient(
            x0, y0, x1, y1, colors, null, Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
    }

    private fun drawOval(
        canvas: Canvas, cx: Float, cy: Float,
        rx: Float, ry: Float, color: Int
    ) {
        fill(color)
        canvas.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, paint)
    }

    private fun drawEarbud(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        scale: Float,
        angle: Float,
        highlight: Boolean
    ) {
        canvas.save()
        canvas.rotate(angle, cx, cy)
        canvas.translate(cx, cy)
        canvas.scale(scale, scale)

        // Soft shadow beneath the earbud.
        fill(Color.argb(65, 0, 0, 0))
        paint.maskFilter = android.graphics.BlurMaskFilter(10f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        canvas.drawOval(-28f, 57f, 31f, 74f, paint)
        paint.maskFilter = null

        // Silicone tip.
        linear(-18f, 0f, 18f, 0f, intArrayOf(
            Color.rgb(185, 199, 218),
            Color.rgb(246, 249, 255),
            Color.rgb(131, 149, 175)
        ))
        canvas.drawOval(-16f, -7f, 16f, 12f, paint)

        // Main earbud body.
        linear(-22f, -35f, 22f, 35f, intArrayOf(
            Color.rgb(255, 255, 255),
            Color.rgb(228, 235, 245),
            Color.rgb(151, 165, 188),
            Color.rgb(244, 248, 255)
        ))
        canvas.drawOval(-25f, -35f, 25f, 30f, paint)

        // Dark acoustic detail.
        fill(Color.rgb(89, 105, 128))
        canvas.drawOval(-12f, -25f, 12f, -8f, paint)
        fill(Color.rgb(215, 226, 242))
        canvas.drawOval(-8f, -22f, 8f, -11f, paint)

        // Stem.
        val stem = Path().apply {
            moveTo(-10f, 16f)
            cubicTo(-12f, 28f, -10f, 48f, -5f, 60f)
            cubicTo(0f, 68f, 13f, 65f, 14f, 56f)
            lineTo(10f, 24f)
            close()
        }
        linear(-8f, 20f, 14f, 60f, intArrayOf(
            Color.rgb(250, 252, 255),
            Color.rgb(185, 199, 220),
            Color.rgb(244, 248, 255)
        ))
        canvas.drawPath(stem, paint)

        // Small microphone and status light.
        fill(Color.rgb(103, 119, 144))
        canvas.drawCircle(4f, 48f, 2.6f, paint)
        fill(if (highlight) Color.rgb(101, 229, 185) else Color.rgb(128, 147, 170))
        canvas.drawCircle(4f, 39f, 2f, paint)

        if (highlight) {
            fill(Color.argb(110, 255, 255, 255))
            canvas.drawOval(-18f, -28f, -11f, -8f, paint)
        }

        canvas.restore()
        paint.shader = null
    }

    private fun drawCase(canvas: Canvas, cx: Float, cy: Float, scale: Float) {
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(scale, scale)

        // Ambient shadow.
        fill(Color.argb(95, 0, 0, 0))
        paint.maskFilter = android.graphics.BlurMaskFilter(24f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        canvas.drawOval(-124f, 62f, 124f, 111f, paint)
        paint.maskFilter = null

        // Base body with a curved silhouette.
        val base = Path().apply {
            moveTo(-116f, 5f)
            cubicTo(-116f, -15f, -94f, -25f, -67f, -25f)
            lineTo(67f, -25f)
            cubicTo(94f, -25f, 116f, -15f, 116f, 5f)
            lineTo(109f, 54f)
            cubicTo(105f, 79f, 84f, 91f, 57f, 91f)
            lineTo(-57f, 91f)
            cubicTo(-84f, 91f, -105f, 79f, -109f, 54f)
            close()
        }

        linear(-115f, -15f, 110f, 85f, intArrayOf(
            Color.rgb(250, 252, 255),
            Color.rgb(206, 216, 233),
            Color.rgb(121, 139, 166),
            Color.rgb(224, 233, 246)
        ))
        canvas.drawPath(base, paint)

        // Front gloss.
        val gloss = Path().apply {
            moveTo(-102f, 27f)
            cubicTo(-67f, 43f, 60f, 43f, 102f, 26f)
            lineTo(98f, 54f)
            cubicTo(87f, 78f, 55f, 80f, 0f, 80f)
            cubicTo(-57f, 80f, -87f, 75f, -98f, 54f)
            close()
        }
        linear(-80f, 28f, 70f, 78f, intArrayOf(
            Color.rgb(224, 233, 246),
            Color.rgb(166, 183, 209),
            Color.rgb(241, 246, 255)
        ))
        canvas.drawPath(gloss, paint)

        // Lid pivot and opening motion.
        canvas.save()
        val lidAngle = -openProgress * 42f
        canvas.rotate(lidAngle, 0f, -19f)

        val lid = Path().apply {
            moveTo(-110f, -16f)
            cubicTo(-107f, -42f, -83f, -57f, -54f, -59f)
            cubicTo(-18f, -64f, 48f, -64f, 76f, -53f)
            cubicTo(98f, -44f, 111f, -31f, 109f, -14f)
            cubicTo(77f, -3f, -70f, -2f, -110f, -16f)
            close()
        }
        linear(-100f, -62f, 80f, -7f, intArrayOf(
            Color.rgb(255, 255, 255),
            Color.rgb(221, 231, 246),
            Color.rgb(152, 171, 201)
        ))
        canvas.drawPath(lid, paint)

        // Fine rim highlight.
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.8f
        paint.color = Color.argb(170, 255, 255, 255)
        canvas.drawPath(lid, paint)
        paint.style = Paint.Style.FILL

        // Inner lid.
        fill(Color.rgb(177, 193, 217))
        canvas.drawOval(-75f, -37f, 75f, -14f, paint)
        fill(Color.rgb(219, 229, 244))
        canvas.drawOval(-63f, -34f, 63f, -19f, paint)

        canvas.restore()

        // Seam around the charging case.
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = Color.argb(135, 94, 111, 139)
        canvas.drawArc(-108f, -22f, 108f, 25f, 2f, 176f, false, paint)
        paint.style = Paint.Style.FILL

        // Small status LED with a subtle glow.
        val ledGlow = 0.65f + 0.35f * sin(glowPhase * Math.PI * 2).toFloat()
        fill(Color.argb((55 * ledGlow).toInt(), 77, 255, 195))
        paint.maskFilter = android.graphics.BlurMaskFilter(13f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        canvas.drawCircle(0f, 56f, 9f, paint)
        paint.maskFilter = null
        fill(Color.rgb(90, 232, 187))
        canvas.drawCircle(0f, 56f, 3.5f, paint)

        // Brand mark.
        paint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        paint.textSize = 15f
        paint.textAlign = Paint.Align.CENTER
        fill(Color.rgb(111, 130, 157))
        canvas.drawText("vivo", 0f, 28f, paint)

        canvas.restore()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val density = resources.displayMetrics.density
        val scale = (minOf(w / 330f, h / 300f) * 0.94f).coerceAtLeast(0.35f)

        canvas.save()
        canvas.translate(w / 2f, h / 2f)

        // Dark cinematic backdrop.
        val bgRadius = minOf(w, h) * 0.72f
        paint.shader = RadialGradient(
            0f, 0f, bgRadius,
            intArrayOf(
                Color.argb(75, 63, 103, 183),
                Color.argb(28, 43, 67, 124),
                Color.TRANSPARENT
            ),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(0f, 0f, bgRadius, paint)
        paint.shader = null

        // Floating particles.
        for (i in 0 until 13) {
            val angle = i * 2.399f
            val radius = (75f + (i % 4) * 19f) * scale
            val px = cos(angle + floatPhase * 0.12f) * radius
            val py = sin(angle + floatPhase * 0.12f) * radius * 0.72f
            val alpha = (45 + 30 * (0.5f + 0.5f * sin(floatPhase + i))).toInt()
                .coerceIn(20, 80)
            fill(Color.argb(alpha, 174, 204, 255))
            canvas.drawCircle(px, py, (1.2f + (i % 3) * 0.45f) * density, paint)
        }

        val bob = sin(floatPhase * 2f).toFloat() * 3.5f * density

        // Earbuds rise out of the case while the lid opens.
        val lift = openProgress * 47f * scale
        val separation = 49f * scale
        val leftX = -separation - openProgress * 9f * scale
        val rightX = separation + openProgress * 9f * scale
        val earY = -37f * scale - lift + bob

        drawEarbud(
            canvas,
            leftX,
            earY,
            scale * 0.91f,
            -16f - openProgress * 11f,
            true
        )
        drawEarbud(
            canvas,
            rightX,
            earY - 3f * scale,
            scale * 0.91f,
            14f + openProgress * 9f,
            false
        )

        drawCase(
            canvas,
            0f,
            37f * scale + bob * 0.25f,
            scale
        )

        // Floor reflection.
        paint.shader = LinearGradient(
            0f, 104f * scale, 0f, 135f * scale,
            intArrayOf(
                Color.argb(32, 143, 181, 255),
                Color.TRANSPARENT
            ),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawOval(
            -100f * scale, 100f * scale,
            100f * scale, 125f * scale, paint
        )
        paint.shader = null

        canvas.restore()
    }
}

class BtReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BluetoothDevice.ACTION_ACL_CONNECTED) return

        val device: BluetoothDevice? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(
                    BluetoothDevice.EXTRA_DEVICE,
                    BluetoothDevice::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

        val deviceName = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                "vivo TWS"
            } else {
                device?.name ?: "vivo TWS"
            }
        } catch (_: SecurityException) {
            "vivo TWS"
        }

        val popup = Intent(context, PopupActivity::class.java).apply {
            putExtra("device_name", deviceName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        try {
            context.startActivity(popup)
        } catch (_: Exception) {
            // Android may restrict activity launches from background receivers.
        }
    }
}
