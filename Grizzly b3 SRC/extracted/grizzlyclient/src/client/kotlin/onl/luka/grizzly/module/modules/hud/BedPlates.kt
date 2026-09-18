package onl.luka.grizzly.module.modules.hud

import onl.luka.grizzly.module.HudLayer
import onl.luka.grizzly.module.HudModule
import onl.luka.grizzly.config.entry.Color
import onl.luka.grizzly.config.entry.ColorEntry
import onl.luka.grizzly.module.modules.other.Colour
import onl.luka.grizzly.module.modules.other.Font
import onl.luka.grizzly.util.Text
import onl.luka.grizzly.util.HudBackgroundMode
import onl.luka.grizzly.util.HudBlur
import onl.luka.grizzly.util.radius
import onl.luka.grizzly.util.roundedFill
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.tags.BlockTags
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.BedBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sqrt

object BedPlates : HudModule(
    "Bed Plates",
    "Highlights defended beds and shows their unique shell blocks",
) {
    override val hudLayer: HudLayer get() = renderLayer.value

    private val renderLayer  = enum("render layer", HudLayer.UNDERLAY)
    private val searchRange  = float("search range",   48f,  8f,  96f)
    private val shellRadius  = int("shell radius",       6,   2,   16)
    private val scanInterval = int("scan interval ms", 500, 100, 2000)
    private val backgroundMode = enum("background mode", HudBackgroundMode.SOLID)
    private val blurStrength = int("blur strength", 6, 1, HudBlur.MAX_STRENGTH).also {
        it.visibleWhen = { backgroundMode.value == HudBackgroundMode.BLUR }
    }
    private val bgColor = color("bg color", Color(13, 13, 22, 208), allowAlpha = true)
    private val panelShadow = hudDropShadow()
    private val showDistance = boolean("show distance", true)
    private val useBedColor  = boolean("use bed color", false).also {
        it.visibleWhen = { showDistance.value }
    }
    private val textColor = color("text color", Color(255, 255, 255, 255), allowAlpha = true).also {
        it.pickerMode = ColorEntry.PickerMode.THEME
        it.visibleWhen = { showDistance.value && !useBedColor.value }
    }

    private data class BedEntry(
        val pos: BlockPos,
        val stacks: List<ItemStack>,
        val bedColor: DyeColor?,
    )

    private data class ScanJob(
        val level: ClientLevel,
        val origin: Vec3,
        val rangeSq: Double,
        val shellRadius: Int,
        val chunks: List<ChunkPos>,
        var chunkIndex: Int = 0,
        val rawBeds: MutableSet<BlockPos> = linkedSetOf(),
        var beds: List<BlockPos>? = null,
        var bedIndex: Int = 0,
        var shell: ShellScan? = null,
        val entries: MutableList<BedEntry> = mutableListOf(),
    )

    private class ShellScan(
        val origin: BlockPos,
        val maxDistance: Int,
    ) {
        val queue = ArrayDeque<BlockPos>().apply { add(origin) }
        val visited = mutableSetOf(origin)
        val stacksByItem = linkedMapOf<Item, ItemStack>()
    }

    private var trackedBeds: List<BedEntry> = emptyList()
    private var trackedLevel: ClientLevel? = null
    private var lastScanTime = 0L
    private var scanJob: ScanJob? = null

    override fun onDisabled() {
        trackedBeds  = emptyList()
        trackedLevel = null
        lastScanTime = 0L
        scanJob = null
    }

    override fun onTick(client: Minecraft) {
        val player = client.player
        val level = client.level
        if (player == null || level == null) {
            clear()
            return
        }

        if (trackedLevel !== level) {
            trackedBeds = emptyList()
            scanJob = null
            lastScanTime = 0L
            trackedLevel = level
        }

        val now = System.currentTimeMillis()
        if (scanJob == null) {
            if (lastScanTime != 0L && now - lastScanTime < scanInterval.value) return
            scanJob = createScanJob(level, player.position())
        }

        advanceScan(level)
    }

    override fun renderHudElement(g: GuiGraphicsExtractor) {
        val mc     = Minecraft.getInstance()
        val playerPos = mc.player?.position() ?: return
        val accent = Colour.accent.liveColor(Colour.accent.value).argb

        for (entry in trackedBeds) {
            if (!showDistance.value && entry.stacks.isEmpty()) continue
            val bedTop    = Vec3(entry.pos.x + 0.5, entry.pos.y + 1.2, entry.pos.z + 0.5)
            val projected = projectToScreen(bedTop, mc) ?: continue

            val distance = sqrt(playerPos.distanceToSqr(
                entry.pos.x + 0.5,
                entry.pos.y + 0.5,
                entry.pos.z + 0.5,
            )).toFloat()
            val distanceColor = if (useBedColor.value) {
                entry.bedColor?.let(::dyeColorArgb) ?: accent
            } else {
                textColor.liveColor(Colour.accent.liveColor(Colour.accent.value)).argb
            }
            renderIconStrip(g, projected, distanceColor, entry.stacks, distance, showDistance.value)
        }
    }

    override fun hudWidth(): Int {
        val iconSize = 16
        val gap = 2
        val padding = 3
        val visible = trackedBeds.filter { showDistance.value || it.stacks.isNotEmpty() }
        if (visible.isEmpty()) return 1
        val font = Font.getFont()
        return visible.maxOf { entry ->
            val iconsW = if (entry.stacks.isEmpty()) 0
                else entry.stacks.size * iconSize + (entry.stacks.size - 1) * gap
            val distanceW = if (showDistance.value) font.width(Font.styledText("%.1fm".format(0f))) else 0
            padding * 2 + maxOf(iconsW, distanceW)
        }
    }

    override fun hudHeight(): Int {
        val padding  = 3
        val iconSize = 16
        val hasIcons = trackedBeds.any { it.stacks.isNotEmpty() }
        if (!showDistance.value && !hasIcons) return 1
        val distanceH = if (showDistance.value) Font.getFont().lineHeight else 0
        val iconH = if (hasIcons) iconSize else 0
        val contentGap = if (showDistance.value && hasIcons) 2 else 0
        return padding * 2 + distanceH + contentGap + iconH
    }

    private fun renderIconStrip(
        extractor: GuiGraphicsExtractor,
        projected: Pair<Float, Float>,
        textColor: Int,
        stacks:    List<ItemStack>,
        distance:  Float,
        showDistance: Boolean,
    ) {

        val iconSize  = 16
        val gap       = 2
        val padding   = 3
        val hasIcons  = stacks.isNotEmpty()
        if (!showDistance && !hasIcons) return
        val distLabel = if (showDistance) "%.1fm".format(distance) else ""
        val font = Font.getFont()
        val comp = Font.styledText(distLabel)
        val distanceH = if (showDistance) font.lineHeight else 0
        val contentGap = if (showDistance && hasIcons) 2 else 0
        val iconsW    = if (!hasIcons) 0
        else stacks.size * iconSize + (stacks.size - 1) * gap
        val distW     = if (showDistance) font.width(comp) else 0
        val width     = padding * 2 + maxOf(iconsW, distW)
        val height    = padding * 2 + distanceH + contentGap + if (hasIcons) iconSize else 0
        val x = (projected.first  - width  / 2f).toInt()
        val y = (projected.second - height - 4f).toInt()

        val background = bgColor.liveColor(bgColor.value).argb
        panelShadow.draw(extractor, x, y, width, height, radius)
        if (backgroundMode.value == HudBackgroundMode.BLUR) {
            HudBlur.draw(extractor, x, y, width, height, radius, background, blurStrength.value)
        } else {
            extractor.roundedFill(x, y, width, height, radius, background)
        }

        // Distance text
        if (showDistance) {
            val distX = x + (width - distW) / 2
            extractor.Text(font, comp, distX, y + padding, textColor)
        }

        // Icon strip
        val iconY = y + padding + distanceH + contentGap
        var iconX = x + (width - iconsW) / 2
        for (stack in stacks) {
            extractor.item(stack, iconX, iconY)
            extractor.itemDecorations(font, stack, iconX, iconY)
            iconX += iconSize + gap
        }
    }

    private fun projectToScreen(worldPos: Vec3, mc: Minecraft): Pair<Float, Float>? {
        val camera = mc.gameRenderer.mainCamera()
        val camPos = camera.position()

        val dx = (worldPos.x - camPos.x).toFloat()
        val dy = (worldPos.y - camPos.y).toFloat()
        val dz = (worldPos.z - camPos.z).toFloat()

        val mat = camera.getViewRotationProjectionMatrix(Matrix4f())

        val x = mat.m00() * dx + mat.m10() * dy + mat.m20() * dz + mat.m30()
        val y = mat.m01() * dx + mat.m11() * dy + mat.m21() * dz + mat.m31()
        val w = mat.m03() * dx + mat.m13() * dy + mat.m23() * dz + mat.m33()

        if (w <= 0f) return null

        val ndcX =  x / w
        val ndcY =  y / w

        val sw = mc.window.guiScaledWidth.toFloat()
        val sh = mc.window.guiScaledHeight.toFloat()

        val sx = (ndcX + 1f) * 0.5f * sw
        val sy = (1f - ndcY) * 0.5f * sh

        return sx to sy
    }

    private fun clear() {
        trackedBeds = emptyList()
        trackedLevel = null
        scanJob = null
        lastScanTime = 0L
    }

    private fun createScanJob(level: ClientLevel, origin: Vec3): ScanJob {
        val center = ChunkPos.containing(BlockPos.containing(origin.x, origin.y, origin.z))
        val chunkRadius = ceil(searchRange.value / 16f).toInt() + 1
        val chunks = buildList {
            for (x in center.x() - chunkRadius..center.x() + chunkRadius) {
                for (z in center.z() - chunkRadius..center.z() + chunkRadius) {
                    add(ChunkPos(x, z))
                }
            }
        }.sortedBy { chunk ->
            val dx = chunk.getMiddleBlockX() + 0.5 - origin.x
            val dz = chunk.getMiddleBlockZ() + 0.5 - origin.z
            dx * dx + dz * dz
        }
        return ScanJob(
            level = level,
            origin = origin,
            rangeSq = (searchRange.value * searchRange.value).toDouble(),
            shellRadius = shellRadius.value,
            chunks = chunks,
        )
    }

    private fun advanceScan(level: ClientLevel) {
        val job = scanJob ?: return

        var chunksProcessed = 0
        while (job.chunkIndex < job.chunks.size && chunksProcessed < CHUNKS_PER_TICK) {
            val chunkPos = job.chunks[job.chunkIndex++]
            chunksProcessed++
            val chunk = level.chunkSource.getChunk(chunkPos.x(), chunkPos.z(), ChunkStatus.FULL, false) ?: continue
            chunk.findBlocks({ state -> state.`is`(BlockTags.BEDS) }) { pos, _ ->
                if (job.origin.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= job.rangeSq) {
                    job.rawBeds.add(pos.immutable())
                }
            }
        }

        if (job.chunkIndex < job.chunks.size) return
        if (job.beds == null) job.beds = collapseBeds(job.rawBeds)

        var shellBudget = SHELL_NODES_PER_TICK
        while (shellBudget > 0) {
            var shell = job.shell
            if (shell == null) {
                val beds = job.beds.orEmpty()
                if (job.bedIndex >= beds.size) {
                    trackedBeds = job.entries.toList()
                    scanJob = null
                    lastScanTime = System.currentTimeMillis()
                    return
                }
                shell = ShellScan(beds[job.bedIndex++], job.shellRadius)
                job.shell = shell
            }

            shellBudget -= advanceShellScan(level, shell, shellBudget)
            if (shell.queue.isEmpty()) {
                val stacks = shell.stacksByItem.values
                    .sortedBy { it.hoverName.string.lowercase() }
                val bedColor = (level.getBlockState(shell.origin).block as? BedBlock)?.color
                job.entries.add(BedEntry(shell.origin, stacks, bedColor))
                job.shell = null
            }
        }
    }

    private fun collapseBeds(rawBeds: Collection<BlockPos>): List<BlockPos> {
        val rawBedSet = rawBeds.toHashSet()
        val visited = mutableSetOf<BlockPos>()
        val groups  = mutableListOf<List<BlockPos>>()
        for (p in rawBedSet) {
            if (p in visited) continue
            val queue = ArrayDeque<BlockPos>()
            val group = mutableListOf<BlockPos>()
            queue.add(p)
            visited.add(p)
            while (queue.isNotEmpty()) {
                val cur = queue.removeFirst()
                group.add(cur)
                for (dir in BED_NEIGHBORS) {
                    val n = cur.offset(dir.x, dir.y, dir.z)
                    if (n in rawBedSet && n !in visited) {
                        visited.add(n)
                        queue.add(n)
                    }
                }
            }
            groups.add(group)
        }

        return groups.map { grp ->
            val avgX = grp.map { it.x }.average()
            val avgY = grp.map { it.y }.average()
            val avgZ = grp.map { it.z }.average()
            BlockPos.containing(avgX, avgY, avgZ)
        }
    }

    private fun advanceShellScan(level: ClientLevel, scan: ShellScan, budget: Int): Int {
        var processed = 0
        val maxSq = scan.maxDistance * scan.maxDistance
        while (scan.queue.isNotEmpty() && processed < budget) {
            val pos = scan.queue.removeFirst()
            processed++
            for (dir in SHELL_DIRECTIONS) {
                val next = pos.offset(dir.x, dir.y, dir.z)
                if (!scan.visited.add(next)) continue
                val dx = next.x - scan.origin.x
                val dy = next.y - scan.origin.y
                val dz = next.z - scan.origin.z
                if (abs(dx) + abs(dy) + abs(dz) > scan.maxDistance * 2) continue
                if (dx*dx + dy*dy + dz*dz > maxSq) continue
                val state = level.getBlockState(next)
                if (!isValidDefenseBlock(state)) continue
                stackFor(state)?.let { scan.stacksByItem.putIfAbsent(it.item, it) }
                scan.queue.add(next)
            }
        }
        return processed.coerceAtLeast(1)
    }

    private fun isValidDefenseBlock(state: BlockState): Boolean {
        val block = state.block
        val name  = block.descriptionId.lowercase()
        return when {
            state.`is`(BlockTags.WOOL)       -> true
            state.`is`(BlockTags.PLANKS)     -> true
            state.`is`(BlockTags.LOGS)       -> true
            state.`is`(BlockTags.LEAVES)     -> true
            state.`is`(BlockTags.STAIRS)     -> true
            state.`is`(BlockTags.SLABS)      -> true
            state.`is`(BlockTags.TERRACOTTA) -> true
            name.contains("glass")           -> true
            name.contains("carpet")          -> true
            block == Blocks.LADDER           -> true
            block == Blocks.OBSIDIAN         -> true
            block == Blocks.CRYING_OBSIDIAN  -> true
            block == Blocks.END_STONE        -> true
            block == Blocks.WATER            -> true
            block == Blocks.TNT              -> true
            else                             -> false
        }
    }

    private fun stackFor(state: BlockState): ItemStack? {
        val item = state.block.asItem()
        if (item === Items.AIR) return null
        return ItemStack(item)
    }

    private fun dyeColorArgb(color: DyeColor): Int =
        0xFF000000.toInt() or (color.textureDiffuseColor and 0x00FFFFFF)

    private const val CHUNKS_PER_TICK = 6
    private const val SHELL_NODES_PER_TICK = 384

    private val BED_NEIGHBORS = arrayOf(
        BlockPos(1, 0, 0), BlockPos(-1, 0, 0),
        BlockPos(0, 0, 1), BlockPos(0, 0, -1),
        BlockPos(0, 1, 0), BlockPos(0, -1, 0),
    )

    private val SHELL_DIRECTIONS = arrayOf(
        BlockPos(1, 0, 0), BlockPos(-1, 0, 0),
        BlockPos(0, 0, 1), BlockPos(0, 0, -1),
        BlockPos(0, 1, 0),
    )
}
