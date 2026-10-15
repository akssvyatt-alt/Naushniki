import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
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
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

// 1. ГЛАВНЫЙ ЭКРАН (Настройка разрешений и тест)
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
            text = "🎧 Кастомная анимация"
            textSize = 24f
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
            text = "2. Проверить анимацию"
            setOnClickListener {
                val intent = Intent(this@MainActivity, PopupActivity::class.java)
                intent.putExtra("DEVICE_NAME", "Мои Наушники Pro")
                startActivity(intent)
            }
        }

        layout.addView(title)
        layout.addView(btnPermissions)
        layout.addView(btnTest)
        setContentView(layout)
    }

    private fun requestNeededPermissions() {
        // Разрешение на Bluetooth для Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 101)
            }
        }
        // Разрешение "Поверх других окон" (нужно для показа из фона)
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

// 2. ВСПЛЫВАЮЩЕЕ ОКНО С АНИМАЦИЕЙ
class PopupActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deviceName = intent.getStringExtra("DEVICE_NAME") ?: "Беспроводные наушники"

        // Полупрозрачный фон на весь экран (закрывается при касании мимо карточки)
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#66000000"))
            setOnClickListener { closeWithAnimation() }
        }

        // Сама карточка внизу экрана
        val cardBg = GradientDrawable().apply {
            setColor(Color.parseColor("#1E1E26"))
            cornerRadius = 72f
            setStroke(3, Color.parseColor("#383846"))
        }

        val card = Linear


Layout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = cardBg
            setPadding(64, 72, 64, 64)
            elevation = 32f
            translationY = 1000f // Прячем карточку вниз перед стартом анимации
            setOnClickListener { /* Игнорируем клик внутри карточки */ }
        }

        val cardParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
            setMargins(48, 0, 48, 80)
        }

        // Иконка наушников с пульсацией
        val iconView = TextView(this).apply {
            text = "🎧"
            textSize = 64f
            gravity = Gravity.CENTER
            scaleX = 0f
            scaleY = 0f
        }

        val nameView = TextView(this).apply {
            text = deviceName
            textSize = 22f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 8)
        }

        val statusView = TextView(this).apply {
            text = "⚡ Подключено • Звук активен"
            textSize = 15f
            setTextColor(Color.parseColor("#4ADE80"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }

        val closeBtnBg = GradientDrawable().apply {
            setColor(Color.parseColor("#2E2E3A"))
            cornerRadius = 40f
        }

        val closeBtn = Button(this).apply {
            text = "Отлично"
            setTextColor(Color.WHITE)
            background = closeBtnBg
            setOnClickListener { closeWithAnimation() }
        }

        card.addView(iconView)
        card.addView(nameView)
        card.addView(statusView)
        card.addView(closeBtn)
        root.addView(card, cardParams)
        setContentView(root)

        // Пружинистый выезд карточки снизу
        card.animate()
            .translationY(0f)
            .setDuration(550)
            .setInterpolator(OvershootInterpolator(1.1f))
            .start()

        // Анимация появления и покачивания наушников
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 1.15f, 1f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f, 1.15f, 1f)
        val rotation = PropertyValuesHolder.ofFloat(View.ROTATION, -15f, 10f, 0f)
        ObjectAnimator.ofPropertyValuesHolder(iconView, scaleX, scaleY, rotation).apply {
            duration = 700
            startDelay = 150
            start()
        }

        // Автозакрытие через 4.5 секунды
        handler.postDelayed({ closeWithAnimation() }, 4500)
    }

    private fun closeWithAnimation() {
        handler.removeCallbacksAndMessages(null)
        val root = window.decorView.findViewById<FrameLayout>(android.R.id.content).getChildAt(0) as? FrameLayout
        val card = root?.getChildAt(0)
        if (card != null) {
            card.animate()
                .translationY(1000f)
                .setDuration(250)
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

// 3. ФОНОВЫЙ ПЕРЕХВАТЧИК ПОДКЛЮЧЕНИЯ BLUETOOTH
class BtReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED) {
            val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                "DEPRECATION"
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            val deviceName = try {


device?.name ?: "Наушники"
            } catch (e: SecurityException) {
                "Наушники"
            }

            val popupIntent = Intent(context, PopupActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("DEVICE_NAME", deviceName)
            }
            context.startActivity(popupIntent)
        }
    }
}


