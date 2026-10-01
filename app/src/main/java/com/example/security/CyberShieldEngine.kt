package com.example.security

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import java.io.File
import java.util.Locale

data class UrlScanResult(
    val url: String,
    val isSafe: Boolean,
    val threatScore: Int, // 0 - 100
    val threatLevel: ThreatLevel,
    val detectedThreats: List<String>,
    val recommendation: String
)

data class FraudMessageResult(
    val messageText: String,
    val isFraud: Boolean,
    val scamType: String,
    val threatLevel: ThreatLevel,
    val warningMessage: String,
    val defensiveAction: String
)

data class DeviceSecurityReport(
    val overallSecurityScore: Int, // 0 - 100
    val isRooted: Boolean,
    val isUsbDebuggingActive: Boolean,
    val isVpnActive: Boolean,
    val isNetworkSecure: Boolean,
    val securityStatusLabel: String,
    val detectedVulnerabilities: List<String>,
    val hardeningRecommendations: List<String>
)

enum class ThreatLevel {
    SAFE,
    CAUTION,
    HIGH_RISK_FRAUD,
    MALWARE_THREAT
}

class CyberShieldEngine(private val context: Context) {

    companion object {
        private val SUSPICIOUS_TLDS = setOf(
            "xyz", "top", "tk", "ml", "ga", "cf", "gq", "work", "click",
            "buzz", "fit", "rest", "surf", "monster", "cam", "icu"
        )

        private val FINANCIAL_KEYWORDS = listOf(
            "sbi", "hdfc", "icici", "pnb", "axis", "kotak", "paytm",
            "phonepe", "gpay", "bhim", "kyc", "aadhaar", "pan", "otp",
            "reward", "lottery", "electricity", "refund", "cashback",
            "bonus", "free-recharge", "yono", "bank"
        )

        private val SCAM_PHRASES = listOf(
            "account blocked" to "Banking KYC Scam",
            "kyc update" to "KYC Phishing Fraud",
            "electricity will be disconnected" to "Electricity Bill Scam",
            "won prize" to "Lottery Prize Fraud",
            "won lottery" to "Lottery Prize Fraud",
            "kbc" to "KBC Lottery Impersonation",
            "earn money by liking" to "Task / Part-Time Job Scam",
            "part time job" to "Task / Part-Time Job Scam",
            "income tax refund" to "Tax Refund Phishing",
            "share otp" to "OTP Theft Hazard",
            "send otp" to "OTP Theft Hazard",
            "install apk" to "Malicious APK Distribution",
            "download apk" to "Malicious APK Distribution",
            "card will be deactivated" to "Credit/Debit Card Fraud",
            "claim reward" to "Phishing Reward Trap"
        )
    }

    /**
     * Scans and audits any URL or link for phishing, typosquatting, and malware indicators.
     */
    fun scanUrl(rawUrl: String): UrlScanResult {
        var url = rawUrl.trim()
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = "https://$url"
        }

        val detectedThreats = mutableListOf<String>()
        var score = 0 // higher = more dangerous

        val isHttp = url.startsWith("http://", ignoreCase = true)
        if (isHttp) {
            score += 25
            detectedThreats.add("Unencrypted HTTP Protocol (डेटा चोरी का खतरा)")
        }

        val domain = extractDomain(url).lowercase(Locale.ROOT)

        // Check for raw IP addresses used as domain
        val ipRegex = Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")
        if (ipRegex.matches(domain.substringBefore(":"))) {
            score += 50
            detectedThreats.add("Direct IP Address Host (फ़िशिंग सर्वर का सीधा IP)")
        }

        // Check suspicious TLD
        val tld = domain.substringAfterLast(".", "")
        if (SUSPICIOUS_TLDS.contains(tld)) {
            score += 35
            detectedThreats.add("High-Risk Domain Extension (.$tld संदेहास्पद डोमेन)")
        }

        // Check for financial impersonation in non-official domains
        val containsFinancial = FINANCIAL_KEYWORDS.any { domain.contains(it) }
        val isOfficialDomain = isKnownLegitimateDomain(domain)

        if (containsFinancial && !isOfficialDomain) {
            score += 45
            detectedThreats.add("Bank / Financial Brand Impersonation (बैंक या KYC का फ़र्ज़ी डोमेन)")
        }

        // Check for excessive hyphens or subdomains (typosquatting)
        if (domain.count { it == '-' } >= 2 || domain.count { it == '.' } >= 3) {
            score += 20
            detectedThreats.add("Suspicious Domain Structure / Typosquatting")
        }

        val threatLevel = when {
            score >= 60 -> ThreatLevel.MALWARE_THREAT
            score >= 35 -> ThreatLevel.HIGH_RISK_FRAUD
            score >= 15 -> ThreatLevel.CAUTION
            else -> ThreatLevel.SAFE
        }

        val recommendation = when (threatLevel) {
            ThreatLevel.MALWARE_THREAT -> "खतरनाक लिंक! इस लिंक को कभी न खोलें। यह आपकी बैंकिंग जानकारी या डिवाइस से डेटा चुरा सकता है।"
            ThreatLevel.HIGH_RISK_FRAUD -> "सावधान! यह फ़िशिंग या फ़र्ज़ी वेबसाइट हो सकती है। कोई भी पासवर्ड या OTP न भरें।"
            ThreatLevel.CAUTION -> "चेतावनी: अपरिचित वेबसाइट है। सतर्कता से जांचने के बाद ही आगे बढ़ें।"
            ThreatLevel.SAFE -> "यह लिंक सुरक्षित प्रतीत होता है।"
        }

        return UrlScanResult(
            url = url,
            isSafe = score < 30,
            threatScore = score.coerceIn(0, 100),
            threatLevel = threatLevel,
            detectedThreats = detectedThreats,
            recommendation = recommendation
        )
    }

    /**
     * Analyzes SMS, WhatsApp, or Telegram messages for banking fraud and cyber scam patterns.
     */
    fun analyzeMessageForFraud(text: String): FraudMessageResult {
        val lowerText = text.lowercase(Locale.ROOT)
        var detectedScam: String? = null
        var threatScore = 0

        for ((phrase, scamType) in SCAM_PHRASES) {
            if (lowerText.contains(phrase)) {
                detectedScam = scamType
                threatScore += 40
                break
            }
        }

        // Check for urgency words combined with links or phone numbers
        val urgencyWords = listOf("immediately", "urgent", "tonight", "24 hours", "तुरंत", "बंद हो जाएगा", "ब्लॉक")
        val hasUrgency = urgencyWords.any { lowerText.contains(it) }
        val hasLink = lowerText.contains("http") || lowerText.contains(".com") || lowerText.contains(".xyz") || lowerText.contains("bit.ly")

        if (hasUrgency && hasLink) {
            threatScore += 35
            if (detectedScam == null) detectedScam = "Urgency-Driven Phishing Trap"
        }

        if (lowerText.contains("otp") && (lowerText.contains("share") || lowerText.contains("bhejo") || lowerText.contains("send"))) {
            threatScore += 50
            detectedScam = "Direct OTP Theft Attempt"
        }

        val isFraud = threatScore >= 40
        val threatLevel = when {
            threatScore >= 70 -> ThreatLevel.MALWARE_THREAT
            threatScore >= 40 -> ThreatLevel.HIGH_RISK_FRAUD
            threatScore >= 20 -> ThreatLevel.CAUTION
            else -> ThreatLevel.SAFE
        }

        val warning = if (isFraud) {
            "🚨 साइबर फ़्रॉड चेतावनी: यह संदेश '$detectedScam' जैसा प्रतीत होता है!"
        } else {
            "संदेश सुरक्षित लगता है। कोई स्पष्ट फ़्रॉड पैटर्न नहीं मिला।"
        }

        val action = if (isFraud) {
            "1. किसी भी लिंक पर क्लिक न करें। 2. अपना OTP या पासवर्ड कभी साझा न करें। 3. यदि बैंक का दावा है तो सीधे बैंक की आधिकारिक हेल्पलाइन या 1930 राष्ट्रीय साइबर हेल्पलाइन पर संपर्क करें।"
        } else {
            "हमेशा सतर्क रहें और कभी भी अनजान नंबरों पर वित्तीय जानकारी न भेजें।"
        }

        return FraudMessageResult(
            messageText = text,
            isFraud = isFraud,
            scamType = detectedScam ?: "None",
            threatLevel = threatLevel,
            warningMessage = warning,
            defensiveAction = action
        )
    }

    /**
     * Audits device security posture (Root status, ADB debugging, network security).
     */
    fun auditDeviceSecurity(): DeviceSecurityReport {
        val vulnerabilities = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        var score = 100

        // 1. Root check
        val isRooted = checkRootStatus()
        if (isRooted) {
            score -= 35
            vulnerabilities.add("Device has Root Superuser Access (रूट एक्सेस सक्रिय है)")
            recommendations.add("रूटेड डिवाइस पर बैंकिंग और पर्सनल डेटा असुरक्षित हो सकता है।")
        }

        // 2. USB Debugging check
        val isAdb = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (e: Exception) {
            false
        }
        if (isAdb) {
            score -= 15
            vulnerabilities.add("USB Debugging is Enabled (डेवलपर डिबगिंग सक्रिय)")
            recommendations.add("यदि आवश्यकता न हो, तो Settings से USB Debugging बंद कर दें।")
        }

        // 3. Network & VPN check
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork
        val caps = connectivityManager?.getNetworkCapabilities(activeNetwork)

        val isVpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ?: false
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false

        if (isWifi && !isVpn) {
            // General Wi-Fi recommendation
            recommendations.add("पब्लिक वाई-फाई का उपयोग करते समय सुरक्षित VPN का उपयोग करें।")
        }

        val statusLabel = when {
            score >= 90 -> "🛡️ अभेद्य सुरक्षा (Fortified Shield)"
            score >= 75 -> "🟢 सुरक्षित (Secure)"
            score >= 50 -> "🟡 मध्यम जोखिम (Moderate Risk)"
            else -> "🔴 उच्च जोखिम (High Vulnerability)"
        }

        return DeviceSecurityReport(
            overallSecurityScore = score.coerceIn(0, 100),
            isRooted = isRooted,
            isUsbDebuggingActive = isAdb,
            isVpnActive = isVpn,
            isNetworkSecure = isVpn || !isRooted,
            securityStatusLabel = statusLabel,
            detectedVulnerabilities = vulnerabilities,
            hardeningRecommendations = recommendations
        )
    }

    private fun checkRootStatus(): Boolean {
        val rootPaths = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in rootPaths) {
            if (File(path).exists()) return true
        }

        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }

        return false
    }

    private fun extractDomain(url: String): String {
        return try {
            val clean = url.removePrefix("https://").removePrefix("http://")
            clean.substringBefore("/").substringBefore("?").substringBefore(":")
        } catch (e: Exception) {
            url
        }
    }

    private fun isKnownLegitimateDomain(domain: String): Boolean {
        val legitimate = listOf(
            "onlinesbi.sbi", "sbi.co.in", "hdfcbank.com", "icicibank.com",
            "axisbank.com", "kotak.com", "paytm.com", "phonepe.com",
            "google.com", "apple.com", "amazon.in", "flipkart.com",
            "npci.org.in", "rbi.org.in", "uidai.gov.in", "incometax.gov.in"
        )
        return legitimate.any { domain == it || domain.endsWith(".$it") }
    }
}
