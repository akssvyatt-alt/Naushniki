package com.example.headphonepopup

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#121216"))
            setPadding(64, 64, 64, 64)
        }

        val title = TextView(this).apply {
            text = "🎧 vivo TWS Popup"
            textSize = 26f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }

        val btnPermissions = Button(this).apply {
            text = "1. Выдать разрешения"
            setOnClickListener { requestNeededPermissions() }
        }

        val btnTest = Button(this).apply {
            text = "2. Проверить анимацию vivo"
            setOnClickListener {
                val intent = Intent(this@MainActivity, PopupActivity::class.java)
                intent.putExtra("DEVICE_NAME", "vivo TWS")
                intent.putExtra("BATTERY", 95)
                startActivity(intent)
            }
        }

        layout.addView(title)
        layout.addView(btnPermissions)
        layout.addView(btnTest)
        setContentView(layout)
    }

    private fun requestNeededPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 101)
            }
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Включи разрешение «Поверх других окон»", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            Toast.makeText(this, "Все разрешения выданы!", Toast.LENGTH_SHORT).show()
        }
    }
}

class PopupActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deviceName = intent.getStringExtra("DEVICE_NAME") ?: "vivo TWS"
        val battery = intent.getIntExtra("BATTERY", -1)

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#77000000"))
            setOnClickListener { closeWithAnimation() }
        }

        val cardBg = GradientDrawable().apply {
            setColor(Color.parseColor("#1E1E26"))
            cornerRadius = 80f
            setStroke(3, Color.parseColor("#383846"))
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = cardBg
            setPadding(56, 64, 56, 56)
            elevation = 36f
            translationY = 1200f
            setOnClickListener { }
        }

        val cardParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
            setMargins(40, 0, 40, 72)
        }

        val stageBg = GradientDrawable().apply {
            setColor(Color.parseColor("#1E1E26"))
            cornerRadius = 60f
            setStroke(2, Color.parseColor("#2E4A38"))
        }

        val spriteStage = FrameLayout(this).apply {
            background = stageBg
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
        }
        val stageParams = LinearLayout.LayoutParams(520, 520).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Запускаем точную графику кейса vivo с глянцевыми чашечками
        val customView = VivoHeadphonesView(this)
        spriteStage.addView(customView, FrameLayout.LayoutParams(520, 520))
        customView.startVivoAnimation()

        val nameView = TextView(this).apply {
            text = deviceName
            textSize = 23f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 8)
        }

        val batText = if (battery in 0..100) {
            "🟢 Подключено • 🔋 L: $battery% | R: $battery%"
        } else {
            "🟢 Подключено • 🔋 L: 100% | Кейс: 100% | R: 100%"
        }

        val statusView = TextView(this).apply {
            text = batText
            textSize = 14f
            setTextColor(Color.parseColor("#4ADE80"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }

        val closeBtnBg = GradientDrawable().apply {
            setColor(Color.parseColor("#2E2E3A"))
            cornerRadius = 40f
        }

        val closeBtn = Button(this).apply {
            text = "Готово"
            setTextColor(Color.WHITE)
            background = closeBtnBg
            setOnClickListener { closeWithAnimation() }
        }

        card.addView(spriteStage, stageParams)
        card.addView(nameView)
        card.addView(statusView)
        card.addView(closeBtn)
        root.addView(card, cardParams)
        setContentView(root)

        card.animate()
            .translationY(0f)
            .setDuration(550)
            .setInterpolator(OvershootInterpolator(1.05f))
            .start()

        handler.postDelayed({ closeWithAnimation() }, 6000)
    }

    private fun closeWithAnimation() {
        handler.removeCallbacksAndMessages(null)
        val root = window.decorView.findViewById<FrameLayout>(android.R.id.content).getChildAt(0) as? FrameLayout
        val card = root?.getChildAt(0)
        if (card != null) {
            card.animate()
                .translationY(1200f)
                .setDuration(260)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction {
                    finish()
                    overridePendingTransition(0, 0)
                }
                .start()
        } else {
            finish()
        }
    }
}

// Идеальная отрисовка твоих наушников vivo: вытянутый матовый кейс + глянцевые лунки внутри
class VivoHeadphonesView(context: Context) : View(context) {
    private var openProgress = 0f
    private var budsLift = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    fun startVivoAnimation() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900
            startDelay = 350
            interpolator = OvershootInterpolator(1.1f)
            addUpdateListener {
                openProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            startDelay = 950
            interpolator = OvershootInterpolator(1.3f)
            addUpdateListener {
                budsLift = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f + 40f

        // Наушники, вылетающие из глянцевых лунок вверх
        if (openProgress > 0.15f) {
            val liftY = cy - 25f - (budsLift * 140f)
            val spreadX = 48f + (budsLift * 38f)

            drawGlossyBud(canvas, cx - spreadX, liftY, isLeft = true)
            drawGlossyBud(canvas, cx + spreadX, liftY, isLeft = false)
        }

        // Твои глянцевые чёрные лунки для посадки наушников внутри кейса
        paint.color = Color.parseColor("#06060A")
        rect.set(cx - 105f, cy - 30f, cx + 105f, cy + 30f)
        canvas.drawRoundRect(rect, 30f, 30f, paint)

        // Зеркальные блики на глянцевых лунках
        paint.color = Color.parseColor("#33FFFFFF")
        canvas.drawOval(cx - 80f, cy - 22f, cx - 28f, cy - 6f, paint)
        canvas.drawOval(cx + 28f, cy - 22f, cx + 80f, cy - 6f, paint)

        // Верхняя крышка кейса (правильной вытянутой овальной формы)
        val lidOffset = openProgress * 75f
        paint.color = Color.parseColor("#1B1B22")
        rect.set(cx - 122f, cy - 105f - lidOffset, cx + 122f, cy + 10f - lidOffset)
        canvas.drawRoundRect(rect, 85f, 85f, paint)

        // Нижняя часть матового кейса vivo
        paint.color = Color.parseColor("#131318")
        rect.set(cx - 128f, cy - 18f, cx + 128f, cy + 125f)
        canvas.drawRoundRect(rect, 85f, 85f, paint)

        // Линия стыка половинок кейса
        paint.color = Color.parseColor("#2E2E3C")
        paint.strokeWidth = 3f
        canvas.drawLine(cx - 115f, cy + 10f, cx + 115f, cy + 10f, paint)

        // Брендовый логотип vivo
        paint.color = Color.parseColor("#646478")
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("vivo", cx, cy + 60f, paint)

        // Зеленый индикатор батареи на корпусе
        paint.color = if (openProgress > 0.15f) Color.parseColor("#4ADE80") else Color.parseColor("#333333")
        canvas.drawCircle(cx, cy + 92f, 6f, paint)
    }

    private fun drawGlossyBud(canvas: Canvas, x: Float, y: Float, isLeft: Boolean) {
        val dir = if (isLeft) -1f else 1f
        paint.color = Color.parseColor("#0B0B10")
        // Ножка глянцевого наушника
        rect.set(x - 10f, y, x + 10f, y + 72f)
        canvas.drawRoundRect(rect, 9f, 9f, paint)
        // Капсула и амбушюра
        canvas.drawCircle(x + dir * 14f, y + 14f, 25f, paint)
        paint.color = Color.parseColor("#20202A")
        canvas.drawCircle(x + dir * 30f, y + 15f, 15f, paint)
        // Блик на корпусе наушника
        paint.color = Color.parseColor("#77FFFFFF")
        canvas.drawCircle(x + dir * 7f, y + 4f, 5f, paint)
    }
}

class BtReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED) {
            val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            val deviceName = try {
                device?.name ?: "vivo TWS"
            } catch (e: SecurityException) {
                "vivo TWS"
            }

            val batteryLevel = try {
                val method = device?.javaClass?.getMethod("getBatteryLevel")
                (method?.invoke(device) as? Int) ?: -1
            } catch (e: Exception) {
                -1
            }

            val popupIntent = Intent(context, PopupActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("DEVICE_NAME", deviceName)
                putExtra("BATTERY", batteryLevel)
            }
            context.startActivity(popupIntent)
        }
    }
}
