package com.khaled.frais.app

import android.content.pm.ApplicationInfo
import com.khaled.frais.utils.HPackages

object FilterClassifier {
    fun classify(appInfo: AppInfo): List<Int> {
        val tags = mutableListOf<Int>()
        val info = appInfo.applicationInfo ?: return tags
        val packageName = appInfo.packageName.lowercase()
        val label = appInfo.name.lowercase()

        // 1. System vs User
        if (info.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
            tags.add(FraisData.TAG_ID_SYSTEM)
        } else {
            tags.add(FraisData.TAG_ID_USER)
        }

        // 2. Games
        if (appInfo.isGame) {
            tags.add(FraisData.TAG_ID_GAMES)
        }

        // 3. Browsers
        if (isBrowser(packageName, label)) {
            tags.add(FraisData.TAG_ID_BROWSERS)
        }

        // 4. Social & Communication
        if (isSocial(info, packageName, label)) {
            tags.add(FraisData.TAG_ID_SOCIAL)
            tags.add(FraisData.TAG_ID_COMMUNICATION)
        }

        // 5. Media & Photography
        if (isMedia(info, packageName, label)) {
            tags.add(FraisData.TAG_ID_MEDIA)
        }
        if (isPhotography(info, packageName, label)) {
            tags.add(FraisData.TAG_ID_PHOTOGRAPHY)
        }

        // 6. Productivity
        if (isProductivity(info, packageName, label)) {
            tags.add(FraisData.TAG_ID_PRODUCTIVITY)
        }

        // 7. Finance
        if (isFinance(packageName, label)) {
            tags.add(FraisData.TAG_ID_FINANCE)
        }

        // 8. Education
        if (isEducation(packageName, label)) {
            tags.add(FraisData.TAG_ID_EDUCATION)
        }

        // 9. Shopping
        if (isShopping(packageName, label)) {
            tags.add(FraisData.TAG_ID_SHOPPING)
        }

        // 10. Development
        if (isDevelopment(packageName, label)) {
            tags.add(FraisData.TAG_ID_DEVELOPMENT)
        }

        // 11. Travel
        if (isTravel(packageName, label)) {
            tags.add(FraisData.TAG_ID_TRAVEL)
        }

        // 12. Health
        if (isHealth(packageName, label)) {
            tags.add(FraisData.TAG_ID_HEALTH)
        }

        // 12.1 Mapping & Navigation
        if (isMapping(info, packageName, label)) {
            tags.add(FraisData.TAG_ID_TRAVEL)
        }

        // 13. Tools
        if (isTools(info, packageName, label)) {
            tags.add(FraisData.TAG_ID_TOOLS)
        }

        // 14. Other
        if (tags.isEmpty() || (tags.size == 1 && (tags[0] == FraisData.TAG_ID_SYSTEM || tags[0] == FraisData.TAG_ID_USER))) {
            tags.add(FraisData.TAG_ID_OTHER)
        }

        return tags.distinct()
    }

    private fun isBrowser(pkg: String, label: String): Boolean {
        val browsers = listOf("com.android.chrome", "org.mozilla.firefox", "com.opera.browser", "com.microsoft.emmx", "com.brave.browser", "com.duckduckgo.mobile.android")
        return browsers.any { pkg.startsWith(it) } || label.contains("browser")
    }

    private fun isSocial(info: ApplicationInfo, pkg: String, label: String): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (info.category == ApplicationInfo.CATEGORY_SOCIAL) return true
        }
        val keywords = listOf("facebook", "instagram", "twitter", "whatsapp", "telegram", "discord", "social", "messenger", "chat", "tiktok", "snapchat")
        return keywords.any { pkg.contains(it) || label.contains(it) }
    }

    private fun isMedia(info: ApplicationInfo, pkg: String, label: String): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (info.category == ApplicationInfo.CATEGORY_VIDEO || info.category == ApplicationInfo.CATEGORY_AUDIO) return true
        }
        val keywords = listOf("youtube", "netflix", "spotify", "music", "video", "player", "movie", "tv", "stream", "hulu", "disney")
        return keywords.any { pkg.contains(it) || label.contains(it) }
    }

    private fun isPhotography(info: ApplicationInfo, pkg: String, label: String): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (info.category == ApplicationInfo.CATEGORY_IMAGE) return true
        }
        val keywords = listOf("camera", "photo", "gallery", "edit", "album", "filter", "snapshot")
        return keywords.any { pkg.contains(it) || label.contains(it) }
    }

    private fun isProductivity(info: ApplicationInfo, pkg: String, label: String): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (info.category == ApplicationInfo.CATEGORY_PRODUCTIVITY) return true
        }
        val keywords = listOf("office", "note", "todo", "calendar", "mail", "document", "spreadsheet", "presentation", "pdf", "scan", "workflow")
        return keywords.any { pkg.contains(it) || label.contains(it) }
    }

    private fun isFinance(pkg: String, label: String): Boolean {
        val keywords = listOf("bank", "wallet", "pay", "crypto", "stock", "invest", "finance", "money", "card", "paypal", "revolut")
        return keywords.any { pkg.contains(it) || label.contains(it) }
    }

    private fun isEducation(pkg: String, label: String): Boolean {
        val keywords = listOf("learn", "course", "school", "university", "dictionary", "translate", "language", "book", "read", "study")
        return keywords.any { pkg.contains(it) || label.contains(it) }
    }

    private fun isShopping(pkg: String, label: String): Boolean {
        val keywords = listOf("shop", "store", "buy", "amazon", "ebay", "aliexpress", "cart", "market")
        return keywords.any { pkg.contains(it) || label.contains(it) }
    }

    private fun isDevelopment(pkg: String, label: String): Boolean {
        val keywords = listOf("develop", "code", "github", "gitlab", "bitbucket", "studio", "ide", "terminal", "shell", "console", "debug", "compiler")
        return keywords.any { pkg.contains(it) || label.contains(it) }
    }

    private fun isTravel(pkg: String, label: String): Boolean {
        val keywords = listOf("travel", "trip", "flight", "hotel", "map", "uber", "grab", "taxi", "booking", "air", "train", "bus", "navigation")
        return keywords.any { pkg.contains(it) || label.contains(it) }
    }

    private fun isHealth(pkg: String, label: String): Boolean {
        val keywords = listOf("health", "fit", "run", "workout", "gym", "yoga", "diet", "sleep", "doctor", "med", "heart", "step")
        return keywords.any { pkg.contains(it) || label.contains(it) }
    }

    private fun isMapping(info: ApplicationInfo, pkg: String, label: String): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (info.category == ApplicationInfo.CATEGORY_MAPS) return true
        }
        val keywords = listOf("map", "navigation", "gps", "waze", "uber", "lyft", "grab", "taxi", "tracker")
        if (keywords.any { pkg.contains(it) || label.contains(it) }) return true
        
        // Also check for location permissions
        return HPackages.hasPermission(pkg, android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun isTools(info: ApplicationInfo, pkg: String, label: String): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (info.category == ApplicationInfo.CATEGORY_MAPS || info.category == ApplicationInfo.CATEGORY_NEWS) return true
        }
        val keywords = listOf("tool", "util", "manager", "explorer", "calculator", "clock", "weather", "setting", "backup", "cleaner", "antivirus")
        return keywords.any { pkg.contains(it) || label.contains(it) }
    }
}
