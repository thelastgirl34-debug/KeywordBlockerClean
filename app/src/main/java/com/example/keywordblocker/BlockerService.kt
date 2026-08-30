package com.example.keywordblocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class BlockerService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            val info = AccessibilityServiceInfo().apply {
                eventTypes = AccessibilityEvent.TYPES_ALL_MASK
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                        AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                notificationTimeout = 20
            }
            serviceInfo = info
        } catch (e: Throwable) {}
    }

    // AI Studio'yu hem URL'den hem de sayfa içi buton/yazılardan tanıyan liste
    private val whitelistedTerms = listOf(
        "aistudio.google.com",
        "ai.google.dev",
        "aistudio",
        "ai studio",
        "google ai studio",
        "Playground",
        "system instructions",
        "get api key",
        "safety settings",
        "user prompt"
    )

    private val blockedWords = listOf(
        "lol hentai", "league hentai", "league of legends hentai",
        "lol r34", "lol rule34", "league r34", "league rule34",
        "lol nsfw", "league nsfw", "league of legends nsfw",
        "lol porn", "league porn", "leaguerule34", "leagueoflegendsnsfw",
        "kda hentai", "k/da hentai", "kda r34", "kda rule34", "kda nsfw", "kda porn", "k/da nsfw",
        "ahri hentai", "ahri r34", "ahri rule34", "ahri nsfw", "ahri porn",
        "evelynn hentai", "evelynn r34", "evelynn rule34", "evelynn nsfw", "evelynn porn",
        "kaisa hentai", "kai'sa hentai", "kaisa r34", "kai'sa r34", "kaisa nsfw", "kai'sa nsfw", "kaisa porn",
        "akali hentai", "akali r34", "akali rule34", "akali nsfw", "akali porn",
        "jinx hentai", "jinx r34", "jinx rule34", "jinx nsfw",
        "miss fortune hentai", "miss fortune r34", "miss fortune nsfw",
        "gwen hentai", "gwen r34", "riven hentai", "lux hentai", "lux r34",
        "seraphine hentai", "seraphine r34", "seraphine nsfw",
        "hentai", "ecchi", "doujin", "doujinshi", "rule34", "r34",
        "hanime", "nhentai", "hentaigasm", "hentaihaven", "e-hentai",
        "eromanga", "ero anime", "ahegao", "anime nsfw", "anime porn",
        "18+ anime", "anime 18+", "manga 18+", "tsumino", "hitomi.la",
        "fakku", "danbooru", "gelbooru", "pururin", "luscious"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null) return

            // 1. ADIM: TÜM AÇIK PENCERELERDE AI STUDIO KONTROLÜ YAP
            if (isAIStudioActive()) {
                return // AI Studio açıksa hiçbir işlem yapma, serbest bırak!
            }

            // 2. ADIM: AI Studio'da değilsek klavye ve ekrandaki kelimeleri avla
            for (text in event.text) {
                if (checkText(text?.toString())) return
            }

            val source = event.source
            if (source != null) {
                if (checkText(source.text?.toString()) || checkText(source.contentDescription?.toString())) {
                    return
                }
            }

            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                scanNodeShallow(rootNode, 0)
            }
        } catch (e: Throwable) {}
    }

    // Telefonun açık olan tüm katmanlarını (gizli adres çubuğu dahil) tarar
    private fun isAIStudioActive(): Boolean {
        try {
            val allWindows = windows
            if (allWindows != null && allWindows.isNotEmpty()) {
                for (window in allWindows) {
                    val root = window.root ?: continue
                    if (scanForWhitelist(root, 0)) return true
                }
            }

            val activeRoot = rootInActiveWindow
            if (activeRoot != null && scanForWhitelist(activeRoot, 0)) {
                return true
            }
        } catch (e: Throwable) {}
        return false
    }

    private fun scanForWhitelist(node: AccessibilityNodeInfo?, depth: Int): Boolean {
        if (node == null || depth > 7) return false
        try {
            val text = node.text?.toString()?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""

            if (whitelistedTerms.any { text.contains(it) || desc.contains(it) }) {
                return true
            }

            for (i in 0 until node.childCount) {
                if (scanForWhitelist(node.getChild(i), depth + 1)) {
                    return true
                }
            }
        } catch (e: Throwable) {}
        return false
    }

    private fun scanNodeShallow(node: AccessibilityNodeInfo?, depth: Int) {
        if (node == null || depth > 8) return
        try {
            val text = node.text?.toString()
            val desc = node.contentDescription?.toString()

            if (checkText(text) || checkText(desc)) {
                return
            }

            for (i in 0 until node.childCount) {
                scanNodeShallow(node.getChild(i), depth + 1)
            }
        } catch (e: Throwable) {}
    }

    private fun checkText(rawText: String?): Boolean {
        if (rawText.isNullOrEmpty()) return false
        val lower = rawText.lowercase()

        for (word in blockedWords) {
            if (lower.contains(word)) {
                triggerBlock(word)
                return true
            }
        }
        return false
    }

    private fun triggerBlock(matchedWord: String) {
        try {
            performGlobalAction(GLOBAL_ACTION_HOME)
            showToast("🚫 Yasaklı Kelime Engellendi: $matchedWord")
        } catch (e: Throwable) {}
    }

    private fun showToast(msg: String) {
        try {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Throwable) {}
    }

    override fun onInterrupt() {}
}
