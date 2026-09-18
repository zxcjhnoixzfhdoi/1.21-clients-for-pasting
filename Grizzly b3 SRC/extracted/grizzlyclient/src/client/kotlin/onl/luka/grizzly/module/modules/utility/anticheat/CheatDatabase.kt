package onl.luka.grizzly.module.modules.utility.anticheat

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import onl.luka.grizzly.module.modules.utility.CheatDetector
import java.io.File
import java.nio.file.Path
import java.util.UUID
import kotlin.math.max
import kotlin.math.pow

enum class CheatVerdict { CLEAN, SUSPECT, CHEATER }

// Per-check totals kept for a single player, aggregated across every session
data class CheckRecord(
    var hits: Int = 0,
    var strongHits: Int = 0,
    var peakVl: Double = 0.0,
    var lastSeen: Long = 0L,
    var lastDetail: String = "",
)

data class CheatRecord(
    val uuid: String = "",
    var name: String = "",
    var firstSeen: Long = 0L,
    var lastSeen: Long = 0L,
    var lastDetection: Long = 0L,
    var detections: Int = 0,
    var score: Double = 0.0,
    var verdict: CheatVerdict = CheatVerdict.CLEAN,
    // Set when the verdict came from `.cheaters mark`; automatic scoring never lowers it
    var manual: Boolean = false,
    var checks: MutableMap<String, CheckRecord> = LinkedHashMap(),
    var servers: MutableList<String> = ArrayList(),
    var sessions: MutableList<String> = ArrayList(),
) {
    val distinctChecks: Int get() = checks.size
    val strongHits: Int get() = checks.values.sumOf { it.strongHits }
    val sessionCount: Int get() = max(sessions.size, 1)

    // score faded by CheatDatabase.SCORE_HALF_LIFE_DAYS so stale evidence stops convicting
    fun effectiveScore(now: Long = System.currentTimeMillis()): Double {
        if (lastDetection <= 0L) return score
        val days = (now - lastDetection).coerceAtLeast(0L) / 86_400_000.0
        return score * 0.5.pow(days / CheatDatabase.SCORE_HALF_LIFE_DAYS)
    }

    fun topChecks(limit: Int): List<Pair<String, CheckRecord>> =
        checks.entries.sortedByDescending { it.value.hits }.take(limit).map { it.key to it.value }
}

// Result of committing one detection, used to decide whether to announce a new verdict
internal data class DetectionOutcome(val record: CheatRecord, val escalated: Boolean)

// Turns the stream of detections into a sticky verdict, stored in config/medved/cheaters.json.
object CheatDatabase {

    internal const val SCORE_HALF_LIFE_DAYS = 45.0
    private const val SAVE_INTERVAL_MS = 3_000L

    // How much a detection from each check is worth
    private val CHECK_WEIGHTS = mapOf(
        "invalidrot_a" to 1.6,
        "reach_a" to 1.3,
        "velocity_a" to 1.2,
        "flight_a" to 1.2,
        "killaura_a" to 1.15,
        "noslow_a" to 1.1,
        "autoblock_a" to 1.0,
        "scaffold" to 1.0,
        "silentaim_a" to 0.95,
        "speed_a" to 0.9,
        "autoclicker_a" to 0.8,
        "legit_scaffold_a" to 0.55,
    )

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val records = LinkedHashMap<UUID, CheatRecord>()

    private var file: File? = null
    private var dirty = false
    private var lastSaveMs = 0L

    // Bumped on every mutation so cached views know to rebuild
    private var revision = 0
    private var nameIndexRevision = -1
    private var nameIndex: Map<String, CheatRecord> = emptyMap()

    fun init(configDir: Path) {
        file = configDir.resolve("cheaters.json").toFile()
        load()
        ClientLifecycleEvents.CLIENT_STOPPING.register { _ -> save() }
    }

    private fun markDirty() {
        dirty = true
        revision++
    }

    private fun load() {
        records.clear()
        val source = file ?: return
        if (!source.exists()) return
        runCatching {
            val type = object : TypeToken<MutableList<CheatRecord>>() {}.type
            val loaded: MutableList<CheatRecord>? = gson.fromJson(source.readText(), type)
            loaded?.forEach { record ->
                val uuid = runCatching { UUID.fromString(record.uuid) }.getOrNull() ?: return@forEach
                records[uuid] = sanitize(record)
            }
        }
        revision++
        prune()
    }

    // Gson skips constructors, so a field missing from the file comes back null.
    @Suppress("SENSELESS_COMPARISON")
    private fun sanitize(record: CheatRecord): CheatRecord = record.apply {
        if (name == null) name = ""
        if (verdict == null) verdict = CheatVerdict.CLEAN
        if (checks == null) checks = LinkedHashMap()
        if (servers == null) servers = ArrayList()
        if (sessions == null) sessions = ArrayList()
        checks.values.forEach { if (it.lastDetail == null) it.lastDetail = "" }
        if (!score.isFinite()) score = 0.0
    }

    // Writes at most once every SAVE_INTERVAL_MS; called from the engine tick
    fun flush() {
        if (!dirty) return
        val now = System.currentTimeMillis()
        if (now - lastSaveMs < SAVE_INTERVAL_MS) return
        save()
    }

    fun save() {
        if (!dirty) return
        val target = file ?: return
        runCatching {
            target.parentFile?.mkdirs()
            target.writeText(gson.toJson(records.values.toList()))
            dirty = false
            lastSaveMs = System.currentTimeMillis()
        }
    }

    fun lookup(uuid: UUID): CheatRecord? = records[uuid]

    // Exact, case-insensitive name lookup, backed by a cache for per-message use
    fun byName(name: String): CheatRecord? = nameIndex()[name.lowercase()]

    // Every record keyed by lower-cased name
    fun nameIndex(): Map<String, CheatRecord> {
        if (nameIndexRevision != revision) {
            nameIndex = records.values
                .filter { it.name.isNotBlank() }
                .associateBy { it.name.lowercase() }
            nameIndexRevision = revision
        }
        return nameIndex
    }

    fun all(): List<CheatRecord> = records.values.toList()

    fun ranked(): List<CheatRecord> {
        val now = System.currentTimeMillis()
        return records.values.sortedWith(
            compareByDescending<CheatRecord> { it.verdict.ordinal }.thenByDescending { it.effectiveScore(now) },
        )
    }

    fun find(query: String): CheatRecord? {
        val normalized = query.lowercase()
        runCatching { UUID.fromString(query) }.getOrNull()?.let { records[it]?.let { record -> return record } }
        return records.values.firstOrNull { it.name.equals(normalized, ignoreCase = true) }
            ?: records.values.firstOrNull { it.name.lowercase().startsWith(normalized) }
            ?: records.values.firstOrNull { it.name.lowercase().contains(normalized) }
    }

    // Refreshes the stored name and last-seen stamp of an already-known player
    fun touch(uuid: UUID, name: String) {
        val record = records[uuid] ?: return
        if (name.isNotBlank() && record.name != name) record.name = name
        record.lastSeen = System.currentTimeMillis()
        markDirty()
    }

    internal fun recordDetection(
        uuid: UUID,
        name: String,
        checkId: String,
        vl: Double,
        strong: Boolean,
        detail: String,
        server: String,
        session: String,
    ): DetectionOutcome {
        val now = System.currentTimeMillis()
        val record = records.getOrPut(uuid) {
            CheatRecord(uuid = uuid.toString(), name = name, firstSeen = now)
        }
        if (name.isNotBlank()) record.name = name
        if (record.firstSeen == 0L) record.firstSeen = now
        record.lastSeen = now
        record.lastDetection = now
        record.detections++

        val check = record.checks.getOrPut(checkId) { CheckRecord() }
        check.hits++
        if (strong) check.strongHits++
        check.peakVl = max(check.peakVl, vl)
        check.lastSeen = now
        check.lastDetail = detail

        if (server.isNotBlank() && server !in record.servers) record.servers += server
        if (session !in record.sessions) record.sessions += session
        while (record.sessions.size > 40) record.sessions.removeAt(0)

        record.score = (record.score + detectionWeight(checkId, vl, strong)).coerceAtMost(MAX_SCORE)

        val previous = record.verdict
        val verdict = computeVerdict(record, now)
        if (!record.manual && verdict.ordinal > record.verdict.ordinal) record.verdict = verdict
        markDirty()
        return DetectionOutcome(record, record.verdict.ordinal > previous.ordinal)
    }

    private fun detectionWeight(checkId: String, vl: Double, strong: Boolean): Double {
        val threshold = CheatDetector.detectionVl.value.toDouble().coerceAtLeast(1.0)
        val overshoot = ((vl - threshold) / threshold).coerceIn(0.0, 1.5)
        val base = (1.0 + overshoot) * (if (strong) 1.35 else 1.0)
        return base * (CHECK_WEIGHTS[checkId] ?: 1.0)
    }

    // One noisy check is never enough; conviction needs corroboration as well as score.
    private fun computeVerdict(record: CheatRecord, now: Long): CheatVerdict {
        val score = record.effectiveScore(now)
        val conviction = CheatDetector.convictionScore.value.toDouble().coerceAtLeast(1.0)
        val corroborated = record.distinctChecks >= 2 || record.strongHits >= 4 || record.sessionCount >= 3
        return when {
            score >= conviction && corroborated -> CheatVerdict.CHEATER
            score >= conviction * 0.4 -> CheatVerdict.SUSPECT
            else -> CheatVerdict.CLEAN
        }
    }

    fun mark(uuid: UUID, name: String, verdict: CheatVerdict): CheatRecord {
        val now = System.currentTimeMillis()
        val record = records.getOrPut(uuid) {
            CheatRecord(uuid = uuid.toString(), name = name, firstSeen = now)
        }
        if (name.isNotBlank()) record.name = name
        record.verdict = verdict
        record.manual = verdict != CheatVerdict.CLEAN
        record.lastSeen = now
        markDirty()
        save()
        return record
    }

    fun forget(record: CheatRecord): Boolean {
        val uuid = runCatching { UUID.fromString(record.uuid) }.getOrNull() ?: return false
        if (records.remove(uuid) == null) return false
        markDirty()
        save()
        return true
    }

    fun clear(): Int {
        val removed = records.size
        records.clear()
        markDirty()
        save()
        return removed
    }

    // Drops entries that have gone quiet for longer than the configured retention
    fun prune(): Int {
        val retentionDays = CheatDetector.retentionDays.value.toLong()
        val cutoff = System.currentTimeMillis() - retentionDays * 86_400_000L
        val stale = records.filterValues { !it.manual && it.lastSeen in 1 until cutoff }.keys.toList()
        stale.forEach(records::remove)
        if (stale.isNotEmpty()) markDirty()
        return stale.size
    }

    private const val MAX_SCORE = 250.0
}
