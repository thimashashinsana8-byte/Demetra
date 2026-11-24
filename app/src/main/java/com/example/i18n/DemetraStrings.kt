package com.example.i18n

import androidx.compose.runtime.Immutable

enum class AppLanguage(val code: String, val displayName: String, val nativeLabel: String) {
    ENGLISH("en", "English", "English"),
    SINHALA("si", "Sinhala", "සිංහල"),
    TAMIL("ta", "Tamil", "தமிழ்")
}

@Immutable
data class LocalizedCopy(
    val tagline: String,
    val welcomeTitle: String,
    val navHome: String,
    val navDonateOrExplore: String,
    val navGardenOrRequests: String,
    val navImpactOrHistory: String,
    val navSettings: String,
    val quickDonateCta: String,
    val smartRelayTitle: String,
    val harvestGardenTitle: String,
    val harvestGardenTagline: String,
    val communityGroveTitle: String,
    val askPipHint: String,
    val verifyHandoverCta: String,
    val availableFoodTitle: String,
    val requestFoodCta: String,
    val fullBloomBadge: String
)

private val ENGLISH_COPY = LocalizedCopy(
    tagline = "Nourishing Communities. Growing Kindness.",
    welcomeTitle = "Welcome to Demetra",
    navHome = "Home",
    navDonateOrExplore = "Explore",
    navGardenOrRequests = "Garden",
    navImpactOrHistory = "Impact",
    navSettings = "Settings",
    quickDonateCta = "Donate Surplus Food",
    smartRelayTitle = "Smart Relay Match",
    harvestGardenTitle = "Harvest Garden",
    harvestGardenTagline = "Every Good Deed Leaves Something Growing.",
    communityGroveTitle = "Community Grove",
    askPipHint = "Ask Pip about food safety, listings, or routes...",
    verifyHandoverCta = "Verify Handover",
    availableFoodTitle = "Available Food Near You",
    requestFoodCta = "Request Food",
    fullBloomBadge = "Full Bloom Achieved"
)

private val SINHALA_COPY = LocalizedCopy(
    tagline = "ප්‍රජාවන් පෝෂණය කරමු. කරුණාව වපුරමු.",
    welcomeTitle = "ඩිමීට්‍රා වෙත සාදරයෙන් පිළිගනිමු",
    navHome = "මුල් පිටුව",
    navDonateOrExplore = "සොයන්න",
    navGardenOrRequests = "උද්‍යානය",
    navImpactOrHistory = "දායකත්වය",
    navSettings = "සැකසුම්",
    quickDonateCta = "අතිරික්ත ආහාර පරිත්‍යාග කරන්න",
    smartRelayTitle = "Smart Relay ගැළපුම",
    harvestGardenTitle = "අස්වනු උද්‍යානය",
    harvestGardenTagline = "සෑම යහපත් ක්‍රියාවක්ම මලක් පිපීමට හේතු වේ.",
    communityGroveTitle = "ප්‍රජා වන උයන",
    askPipHint = "ආහාර සුරක්ෂිතතාව හෝ බෙදාහැරීම් ගැන Pip ගෙන් අසන්න...",
    verifyHandoverCta = "භාරදීම තහවුරු කරන්න",
    availableFoodTitle = "ඔබ අසල ඇති ආහාර",
    requestFoodCta = "ආහාර ඉල්ලුම් කරන්න",
    fullBloomBadge = "සම්පූර්ණ මල් පිපීම ලබා ගන්නා ලදී"
)

private val TAMIL_COPY = LocalizedCopy(
    tagline = "சமூகங்களை ஊட்டுவோம். கருணையை வளர்ப்போம்.",
    welcomeTitle = "டிமிட்ராவிற்கு நல்வரவு",
    navHome = "முகப்பு",
    navDonateOrExplore = "தேடு",
    navGardenOrRequests = "தோட்டம்",
    navImpactOrHistory = "தாக்கம்",
    navSettings = "அமைப்புகள்",
    quickDonateCta = "உணவு தானம் செய்க",
    smartRelayTitle = "Smart Relay பொருத்தம்",
    harvestGardenTitle = "அறுவடைத் தோட்டம்",
    harvestGardenTagline = "ஒவ்வொரு நல்ல செயலும் ஒரு மலரை மலரச் செய்கிறது.",
    communityGroveTitle = "சமூகத் தோப்பு",
    askPipHint = "உணவு பாதுகாப்பு அல்லது வழிகள் குறித்து Pip-இடம் கேளுங்கள்...",
    verifyHandoverCta = "கையளிப்பை உறுதிப்படுத்துக",
    availableFoodTitle = "அருகிலுள்ள உணவுகள்",
    requestFoodCta = "உணவு கோருக",
    fullBloomBadge = "முழு மலர்ச்சி சாதனை"
)

fun stringsFor(language: AppLanguage): LocalizedCopy = when (language) {
    AppLanguage.ENGLISH -> ENGLISH_COPY
    AppLanguage.SINHALA -> SINHALA_COPY
    AppLanguage.TAMIL -> TAMIL_COPY
}
