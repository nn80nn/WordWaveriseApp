package com.wordwaverise.wordwaveriseapp.util

/**
 * Outcome of a background sync. [OFFLINE] is reserved for a genuinely unreachable
 * network — a rejected token or a server error is [FAILED] and must not be reported
 * to the user as "no internet".
 */
enum class SyncResult {
    SUCCESS,
    OFFLINE,
    FAILED
}
