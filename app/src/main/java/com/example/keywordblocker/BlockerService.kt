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
            showToast("🔥 Keyword Blocker Başlatıldı!")
        } catch (e: Throwable) {}
    }

    private val whitelistedDomains = listOf(
        "aistudio.google.com",
        "ai.google.dev"
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

            // 1. Klavyeden anlık yazılan metin kontrolü
            for (text in event.text) {
                if (checkText(text?.toString())) return
            }

            // 2. Olay kaynağını (Source Node) kontrol et
            val source = event.source
            if (source != null) {
                if (checkText(source.text?.toString()) || checkText(source.contentDescription?.toString())) {
                    return
                }
            }

            // 3. Ekrandaki aktif pencereyi tara
            val rootNode = rootInActiveWindow ?: return
            if (isWhitelisted(rootNode)) return

            scanNodeShallow(rootNode, 0)
        } catch (e: Throwable) {}
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

    private fun isWhitelisted(root: AccessibilityNodeInfo): Boolean {
        try {
            val text = root.text?.toString()?.lowercase() ?: ""
            val desc = root.contentDescription?.toString()?.lowercase() ?: ""
            if (whitelistedDomains.any { text.contains(it) || desc.contains(it) }) return true

            for (i in 0 until root.childCount) {
                val child = root.getChild(i) ?: continue
                val cText = child.text?.toString()?.lowercase() ?: ""
                val cDesc = child.contentDescription?.toString()?.lowercase() ?: ""
                if (whitelistedDomains.any { cText.contains(it) || cDesc.contains(it) }) return true
            }
        } catch (e: Throwable) {}
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
