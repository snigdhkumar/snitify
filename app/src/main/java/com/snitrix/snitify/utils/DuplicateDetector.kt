package com.snitrix.snitify.utils

import com.snitrix.snitify.data.model.Song
import java.text.Normalizer
import kotlin.math.max

object DuplicateDetector {

    private val VARIANT_KEYWORDS = setOf(
        "remix", "live", "acoustic", "slowed", "reverb", "instrumental",
        "karaoke", "cover", "extended", "radio edit", "explicit", "clean",
        "unplugged", "lofi", "rework", "orchestral", "demo", "vip"
    )

    /**
     * Stage 1 — Multi-Pass String Normalization
     */
    fun normalizeString(input: String): String {
        if (input.isBlank()) return ""

        // 1. Unicode NFD Decomposition (strip accents/diacritics like é -> e)
        var s = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

        // 2. Lowercase
        s = s.lowercase()

        // 3. Strip featuring / feat / ft wrappers
        s = s.replace(Regex("\\b(ft|feat|featuring)\\.?\\s+[^)\\]]+"), "")

        // 4. Strip invisible characters and non-alphanumeric punctuation
        s = s.replace(Regex("[^a-z0-9\\s]"), " ")

        // 5. Collapse multiple spaces & trim
        return s.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Extract protected version variant keywords from title
     */
    fun extractVariantTags(title: String): Set<String> {
        val normalized = title.lowercase()
        return VARIANT_KEYWORDS.filter { keyword ->
            normalized.contains(keyword)
        }.toSet()
    }

    /**
     * Compute Levenshtein distance between two strings
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[m][n]
    }

    /**
     * Compute Token Sort Ratio similarity (0.0 to 1.0)
     */
    fun tokenSortRatio(str1: String, str2: String): Double {
        val norm1 = normalizeString(str1)
        val norm2 = normalizeString(str2)

        if (norm1 == norm2) return 1.0
        if (norm1.isBlank() || norm2.isBlank()) return 0.0

        val sortedTokens1 = norm1.split(" ").sorted().joinToString(" ")
        val sortedTokens2 = norm2.split(" ").sorted().joinToString(" ")

        if (sortedTokens1 == sortedTokens2) return 1.0

        val maxLen = max(sortedTokens1.length, sortedTokens2.length)
        if (maxLen == 0) return 1.0

        val dist = levenshteinDistance(sortedTokens1, sortedTokens2)
        return 1.0 - (dist.toDouble() / maxLen.toDouble())
    }

    /**
     * Stage 2, 3 & 4 — Multi-Stage Duplicate Check for a Pair of Tracks
     */
    fun isDuplicateTrack(trackA: Song, trackB: Song): Boolean {
        val normTitleA = normalizeString(trackA.title)
        val normTitleB = normalizeString(trackB.title)

        val normArtistA = normalizeString(trackA.artist)
        val normArtistB = normalizeString(trackB.artist)

        // Stage 2 — Exact Match
        if (normTitleA == normTitleB && normArtistA == normArtistB) {
            return true
        }

        // Stage 4 — Variant Exclusion Guard
        val variantsA = extractVariantTags(trackA.title)
        val variantsB = extractVariantTags(trackB.title)
        if (variantsA != variantsB) {
            // Version mismatch (e.g. Acoustic vs Original) -> NOT a duplicate!
            return false
        }

        // Length difference pruning
        if (kotlin.math.abs(normTitleA.length - normTitleB.length) > 4) {
            return false
        }

        // Stage 3 — Fuzzy Token Sort Ratio Matching
        val simTitle = tokenSortRatio(normTitleA, normTitleB)
        if (simTitle < 0.90) return false

        val simArtist = tokenSortRatio(normArtistA, normArtistB)
        if (simArtist < 0.90) return false

        // Weighted Composite Score: 70% Title + 30% Artist
        val compositeScore = (0.70 * simTitle) + (0.30 * simArtist)
        return compositeScore >= 0.92
    }

    /**
     * High-Performance Bucket Candidate Duplicate Check against existing song list
     */
    fun isDuplicateAgainstList(candidate: Song, existingList: List<Song>, bucketMap: Map<String, List<Song>>? = null): Boolean {
        val normTitle = normalizeString(candidate.title)
        val normArtist = normalizeString(candidate.artist)
        val exactKey = "$normTitle||$normArtist"

        // 1. Direct ID match
        if (existingList.any { it.id == candidate.id }) return true

        // 2. Candidate Bucket Search using 2-letter prefix
        val bucketKey = if (normTitle.length >= 2) normTitle.substring(0, 2) else normTitle
        val candidatesToCheck = bucketMap?.get(bucketKey) ?: existingList

        for (existing in candidatesToCheck) {
            val exTitle = normalizeString(existing.title)
            val exArtist = normalizeString(existing.artist)

            // Fast Exact Key match
            if ("$exTitle||$exArtist" == exactKey) return true

            // Multi-Stage Fuzzy & Variant check
            if (isDuplicateTrack(candidate, existing)) {
                return true
            }
        }
        return false
    }

    /**
     * Build 2-letter prefix index bucket map for O(N) efficient candidate lookups
     */
    fun buildBucketMap(songs: List<Song>): Map<String, List<Song>> {
        val map = mutableMapOf<String, MutableList<Song>>()
        for (song in songs) {
            val normTitle = normalizeString(song.title)
            val bucketKey = if (normTitle.length >= 2) normTitle.substring(0, 2) else normTitle
            map.getOrPut(bucketKey) { mutableListOf() }.add(song)
        }
        return map
    }
}
