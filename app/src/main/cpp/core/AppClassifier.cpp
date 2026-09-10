#include "AppClassifier.hpp"
#include <algorithm>

namespace frais {

// Tag IDs from FraisData.kt
enum TagID {
    GAMES = -1,
    SOCIAL = -2,
    COMMUNICATION = -3,
    PRODUCTIVITY = -4,
    MEDIA = -5,
    PHOTOGRAPHY = -6,
    FINANCE = -7,
    EDUCATION = -8,
    TOOLS = -9,
    BROWSERS = -10,
    SHOPPING = -11,
    OTHER = -14,
    DEVELOPMENT = -16,
    TRAVEL = -17,
    HEALTH = -18
};

// Android ApplicationInfo Categories
enum AndroidCategory {
    CAT_AUDIO = 1,
    CAT_VIDEO = 2,
    CAT_IMAGE = 3,
    CAT_SOCIAL = 4,
    CAT_NEWS = 5,
    CAT_MAPS = 6,
    CAT_PRODUCTIVITY = 7,
    CAT_GAME = 0
};

std::vector<int> AppClassifier::classify(const std::string& packageName, const std::string& label, int category) {
    std::vector<int> tags;
    std::string pkg = packageName;
    std::transform(pkg.begin(), pkg.end(), pkg.begin(), ::tolower);
    std::string lbl = label;
    std::transform(lbl.begin(), lbl.end(), lbl.begin(), ::tolower);

    // Browsers
    if (containsAny(pkg, {"com.android.chrome", "org.mozilla.firefox", "com.opera.browser", "com.microsoft.emmx", "com.brave.browser", "com.duckduckgo.mobile.android"}) ||
        lbl.find("browser") != std::string::npos) {
        tags.push_back(BROWSERS);
    }

    // Social & Communication
    if (category == CAT_SOCIAL || containsAny(pkg, {"facebook", "instagram", "twitter", "whatsapp", "telegram", "discord", "social", "messenger", "chat", "tiktok", "snapchat"}) ||
        containsAny(lbl, {"facebook", "instagram", "twitter", "whatsapp", "telegram", "discord", "social", "messenger", "chat", "tiktok", "snapchat"})) {
        tags.push_back(SOCIAL);
        tags.push_back(COMMUNICATION);
    }

    // Media
    if (category == CAT_VIDEO || category == CAT_AUDIO || containsAny(pkg, {"youtube", "netflix", "spotify", "music", "video", "player", "movie", "tv", "stream", "hulu", "disney"}) ||
        containsAny(lbl, {"youtube", "netflix", "spotify", "music", "video", "player", "movie", "tv", "stream", "hulu", "disney"})) {
        tags.push_back(MEDIA);
    }

    // Photography
    if (category == CAT_IMAGE || containsAny(pkg, {"camera", "photo", "gallery", "edit", "album", "filter", "snapshot"}) ||
        containsAny(lbl, {"camera", "photo", "gallery", "edit", "album", "filter", "snapshot"})) {
        tags.push_back(PHOTOGRAPHY);
    }

    // Productivity
    if (category == CAT_PRODUCTIVITY || containsAny(pkg, {"office", "note", "todo", "calendar", "mail", "document", "spreadsheet", "presentation", "pdf", "scan", "workflow"}) ||
        containsAny(lbl, {"office", "note", "todo", "calendar", "mail", "document", "spreadsheet", "presentation", "pdf", "scan", "workflow"})) {
        tags.push_back(PRODUCTIVITY);
    }

    // Finance
    if (containsAny(pkg, {"bank", "wallet", "pay", "crypto", "stock", "invest", "finance", "money", "card", "paypal", "revolut"}) ||
        containsAny(lbl, {"bank", "wallet", "pay", "crypto", "stock", "invest", "finance", "money", "card", "paypal", "revolut"})) {
        tags.push_back(FINANCE);
    }

    // Education
    if (containsAny(pkg, {"learn", "course", "school", "university", "dictionary", "translate", "language", "book", "read", "study"}) ||
        containsAny(lbl, {"learn", "course", "school", "university", "dictionary", "translate", "language", "book", "read", "study"})) {
        tags.push_back(EDUCATION);
    }

    // Shopping
    if (containsAny(pkg, {"shop", "store", "buy", "amazon", "ebay", "aliexpress", "cart", "market"}) ||
        containsAny(lbl, {"shop", "store", "buy", "amazon", "ebay", "aliexpress", "cart", "market"})) {
        tags.push_back(SHOPPING);
    }

    // Development
    if (containsAny(pkg, {"develop", "code", "github", "gitlab", "bitbucket", "studio", "ide", "terminal", "shell", "console", "debug", "compiler"}) ||
        containsAny(lbl, {"develop", "code", "github", "gitlab", "bitbucket", "studio", "ide", "terminal", "shell", "console", "debug", "compiler"})) {
        tags.push_back(DEVELOPMENT);
    }

    // Travel & Mapping
    if (category == CAT_MAPS || containsAny(pkg, {"travel", "trip", "flight", "hotel", "map", "uber", "grab", "taxi", "booking", "air", "train", "bus", "navigation", "gps", "waze", "lyft", "tracker"}) ||
        containsAny(lbl, {"travel", "trip", "flight", "hotel", "map", "uber", "grab", "taxi", "booking", "air", "train", "bus", "navigation", "gps", "waze", "lyft", "tracker"})) {
        tags.push_back(TRAVEL);
    }

    // Health
    if (containsAny(pkg, {"health", "fit", "run", "workout", "gym", "yoga", "diet", "sleep", "doctor", "med", "heart", "step"}) ||
        containsAny(lbl, {"health", "fit", "run", "workout", "gym", "yoga", "diet", "sleep", "doctor", "med", "heart", "step"})) {
        tags.push_back(HEALTH);
    }

    // Tools
    if (category == CAT_NEWS || containsAny(pkg, {"tool", "util", "manager", "explorer", "calculator", "clock", "weather", "setting", "backup", "cleaner", "antivirus"}) ||
        containsAny(lbl, {"tool", "util", "manager", "explorer", "calculator", "clock", "weather", "setting", "backup", "cleaner", "antivirus"})) {
        tags.push_back(TOOLS);
    }

    if (tags.empty()) {
        tags.push_back(OTHER);
    }

    // Unique tags
    std::sort(tags.begin(), tags.end());
    tags.erase(std::unique(tags.begin(), tags.end()), tags.end());

    return tags;
}

bool AppClassifier::isGame(const std::string& packageName, int category, bool hasGameMetadata) {
    if (category == CAT_GAME || hasGameMetadata) return true;

    std::string pkg = packageName;
    std::transform(pkg.begin(), pkg.end(), pkg.begin(), ::tolower);

    std::vector<std::string> knownGamePrefixes = {
        "com.tencent.tmgp", "com.netease", "com.mihoyo", "com.supercell",
        "com.roblox", "com.mojang", "com.epicgames", "com.valvesoftware",
        "com.activision", "com.ea.", "com.ubisoft", "com.square_enix",
        "com.bandainamcoent", "com.nintendo", "com.sega", "com.gameloft",
        "com.zynga", "com.kabam", "com.rovio", "com.playrix", "com.king",
        "com.popcap.", "com.rockstargames.", "com.nianticlabs.", "com.garena.",
        "com.playgendary.", "com.scopely.", "com.outfit7.", "com.miniclip.",
        "com.voodoo.", "com.playrix.", "com.wildlife.", "com.tfgco."
    };

    if (std::any_of(knownGamePrefixes.begin(), knownGamePrefixes.end(), [&](const std::string& prefix) {
        return pkg.compare(0, prefix.length(), prefix) == 0;
    })) return true;

    std::vector<std::string> gameKeywords = {
        ".game", "game.", ".rpg", ".simulation", ".simulator", ".puzzle",
        ".arcade", ".racing", ".battle", ".sports", ".action", ".adventure",
        ".strategy", ".casino", ".cards", ".trivia", ".board", ".word",
        ".unity", ".godot", ".libgdx", ".unreal"
    };

    return containsAny(pkg, gameKeywords);
}

bool AppClassifier::containsAny(const std::string& target, const std::vector<std::string>& keywords) {
    return std::any_of(keywords.begin(), keywords.end(), [&](const std::string& keyword) {
        return target.find(keyword) != std::string::npos;
    });
}

} // namespace frais
