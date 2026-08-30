package com.example.keywordblocker

import android.app.Activity
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
        }

        statusText = TextView(this).apply {
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }

        val button = Button(this).apply {
            text = "Erişilebilirlik Ayarlarını Aç"
            textSize = 16f
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        layout.addView(statusText)
        layout.addView(button)
        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        if (isAccessibilityServiceEnabled()) {
            statusText.text = "✅ KORUMA ŞU AN AKTİF VE ÇALIŞIYOR!\n\n(Test etmek için Chrome'da yasaklı bir kelime aratın)."
            statusText.setTextColor(0xFF2E7D32.toInt()) // Yeşil
        } else {
            statusText.text = "❌ KORUMA HENÜZ KAPALI!\n\nAşağıdaki butona basıp Keyword Blocker'ı 'Açık' yapın."
            statusText.setTextColor(0xFFC62828.toInt()) // Kırmızı
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (service in enabledServices) {
            if (service.resolveInfo.serviceInfo.packageName == packageName) {
                return true
            }
        }
        return false
    }
}
