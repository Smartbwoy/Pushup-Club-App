package com.example.data.cloud

import android.content.Context

/**
 * Stores an optional public leaderboard nickname per Firebase UID so members never have
 * to expose their full Google account name on shared club leaderboards.
 */
class ClubNicknameStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getCustomNickname(uid: String?): String? {
        if (uid.isNullOrBlank()) return null
        return prefs.getString(keyFor(uid), null)?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun setCustomNickname(uid: String?, nickname: String) {
        if (uid.isNullOrBlank()) return
        val cleaned = sanitizeNickname(nickname)
        if (cleaned.isEmpty()) {
            prefs.edit().remove(keyFor(uid)).apply()
        } else {
            prefs.edit().putString(keyFor(uid), cleaned).apply()
        }
    }

    fun clearNickname(uid: String?) {
        if (uid.isNullOrBlank()) return
        prefs.edit().remove(keyFor(uid)).apply()
    }

    fun resolveDisplayName(uid: String?): String {
        val custom = getCustomNickname(uid)
        if (!custom.isNullOrBlank()) return custom
        return defaultAnonymizedName(uid)
    }

    private fun keyFor(uid: String): String = "nickname_$uid"

    companion object {
        private const val PREFS_NAME = "pushup_club_nicknames"

        fun sanitizeNickname(raw: String): String {
            return raw.trim().replace(Regex("\\s+"), " ").take(30)
        }

        fun defaultAnonymizedName(uid: String?): String {
            val suffix = uid?.filter { it.isLetterOrDigit() }?.takeLast(4)?.uppercase().orEmpty()
            return if (suffix.length >= 2) "Athlete-$suffix" else "Athlete"
        }

        /**
         * Extracts a 6..12 char uppercase alphanumeric club code from deep links such as
         * `pushupclub://join?code=K7M9P2` or plain invite text.
         */
        fun parseInviteCodeFromUrl(urlOrCode: String?): String? {
            if (urlOrCode.isNullOrBlank()) return null
            val trimmed = urlOrCode.trim()
            val codeParamMatch = Regex("""(?:[?&]|^)code=([A-Za-z0-9]{6,12})(?:[&#\s]|$)""")
                .find(trimmed)
            if (codeParamMatch != null) {
                return codeParamMatch.groupValues[1].uppercase()
            }
            val direct = trimmed.uppercase().filter { it.isLetterOrDigit() }
            return if (direct.length in 6..12) direct else null
        }

        fun buildInviteDeepLink(joinCode: String): String {
            val clean = joinCode.trim().uppercase().filter { it.isLetterOrDigit() }
            return "pushupclub://join?code=$clean"
        }

        fun buildInviteShareMessage(
            clubName: String,
            joinCode: String,
            playStoreUrl: String
        ): String {
            val cleanCode = joinCode.trim().uppercase()
            val deepLink = buildInviteDeepLink(cleanCode)
            return "Join my Pushup Club \"$clubName\"!\n" +
                "Tap to join in app: $deepLink\n" +
                "Or enter code: $cleanCode\n" +
                "Get Pushup Club on Google Play: $playStoreUrl"
        }
    }
}
