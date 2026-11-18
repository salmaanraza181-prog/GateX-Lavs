package com.example.tools

import com.example.engine.netlist.NetlistBuilder
import com.example.model.CircuitPage
import com.example.model.ComponentCatalog
import com.example.model.ComponentCategory
import com.example.model.ComponentType
import com.example.model.DiagnosticSeverity
import com.example.model.GridPoint
import com.example.model.PinDirection
import com.example.model.SimulationDiagnostic
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

data class OhmsLawResult(
    val voltageV: Double,
    val currentA: Double,
    val resistanceOhms: Double,
    val powerW: Double
)

enum class ResistorColorBand(val label: String, val digit: Int?, val multiplier: Double?, val tolerancePct: Double?, val colorHex: Long) {
    BLACK("Black", 0, 1.0, null, 0xFF1C1C1C),
    BROWN("Brown", 1, 10.0, 1.0, 0xFF795548),
    RED("Red", 2, 100.0, 2.0, 0xFFE53935),
    ORANGE("Orange", 3, 1_000.0, null, 0xFFFB8C00),
    YELLOW("Yellow", 4, 10_000.0, null, 0xFFFDD835),
    GREEN("Green", 5, 100_000.0, 0.5, 0xFF43A047),
    BLUE("Blue", 6, 1_000_000.0, 0.25, 0xFF1E88E5),
    VIOLET("Violet", 7, 10_000_000.0, 0.1, 0xFF8E24AA),
    GREY("Grey", 8, null, 0.05, 0xFF757575),
    WHITE("White", 9, null, null, 0xFFFAFAFA),
    GOLD("Gold", null, 0.1, 5.0, 0xFFFFD54F),
    SILVER("Silver", null, 0.01, 10.0, 0xFFB0BEC5)
}

data class LedResistorResult(
    val exactOhms: Double,
    val standardE24Ohms: Double,
    val actualCurrentMa: Double,
    val powerMw: Double,
    val recommendedWattage: String
)

data class RcTimeResult(
    val tauSeconds: Double,
    val fiveTauSeconds: Double,
    val cutoffHz: Double
)

object EngineeringCalculators {

    fun solveOhmsLaw(v: Double?, i: Double?, r: Double?, p: Double?): OhmsLawResult? {
        return when {
            v != null && r != null && r > 0 -> {
                val cur = v / r
                OhmsLawResult(v, cur, r, v * cur)
            }
            v != null && i != null && i != 0.0 -> {
                val res = abs(v / i)
                OhmsLawResult(v, i, res, abs(v * i))
            }
            i != null && r != null && r > 0 -> {
                val volt = i * r
                OhmsLawResult(volt, i, r, volt * i)
            }
            p != null && v != null && v != 0.0 -> {
                val cur = p / v
                OhmsLawResult(v, cur, abs(v / cur), p)
            }
            p != null && r != null && r > 0 && p >= 0 -> {
                val cur = sqrt(p / r)
                OhmsLawResult(cur * r, cur, r, p)
            }
            p != null && i != null && i != 0.0 -> {
                val volt = p / i
                OhmsLawResult(volt, i, abs(volt / i), p)
            }
            else -> null
        }
    }

    fun decode4BandResistor(
        band1: ResistorColorBand,
        band2: ResistorColorBand,
        multiplier: ResistorColorBand,
        tolerance: ResistorColorBand
    ): Pair<Double, Double> {
        val d1 = band1.digit ?: 0
        val d2 = band2.digit ?: 0
        val mult = multiplier.multiplier ?: 1.0
        val tol = tolerance.tolerancePct ?: 5.0
        return ((d1 * 10 + d2) * mult) to tol
    }

    private val e24Values = listOf(
        1.0, 1.1, 1.2, 1.3, 1.5, 1.6, 1.8, 2.0, 2.2, 2.4, 2.7, 3.0,
        3.3, 3.6, 3.9, 4.3, 4.7, 5.1, 5.6, 6.2, 6.8, 7.5, 8.2, 9.1
    )

    fun calculateLedResistor(supplyV: Double, forwardV: Double, targetCurrentMa: Double): LedResistorResult? {
        if (supplyV <= forwardV || targetCurrentMa <= 0.0) return null
        val iAmps = targetCurrentMa / 1000.0
        val exact = (supplyV - forwardV) / iAmps
        var bestE24 = exact
        var minDiff = Double.MAX_VALUE
        for (decade in -1..6) {
            val scale = 10.0.pow(decade)
            for (base in e24Values) {
                val candidate = base * scale
                if (candidate >= exact * 0.92 && abs(candidate - exact) < minDiff) {
                    minDiff = abs(candidate - exact)
                    bestE24 = candidate
                }
            }
        }
        val actualI = (supplyV - forwardV) / bestE24
        val powerW = actualI * actualI * bestE24
        val wattRating = when {
            powerW < 0.125 -> "1/4 W (0.25W)"
            powerW < 0.25 -> "1/2 W (0.5W)"
            powerW < 0.5 -> "1 W"
            else -> "2 W+"
        }
        return LedResistorResult(
            exactOhms = exact,
            standardE24Ohms = bestE24,
            actualCurrentMa = actualI * 1000.0,
            powerMw = powerW * 1000.0,
            recommendedWattage = wattRating
        )
    }

    fun calculateRcTimeConstant(rOhms: Double, cFarads: Double): RcTimeResult? {
        if (rOhms <= 0 || cFarads <= 0) return null
        val tau = rOhms * cFarads
        val fc = 1.0 / (2.0 * PI * tau)
        return RcTimeResult(tau, 5.0 * tau, fc)
    }

    fun convertNumberBase(input: String, fromRadix: Int): Triple<String, String, String>? {
        val clean = input.trim().removePrefix("0x").removePrefix("0X").removePrefix("0b").removePrefix("0B")
        val value = clean.toLongOrNull(fromRadix) ?: return null
        return Triple(
            value.toString(2),
            value.toString(10),
            value.toString(16).uppercase()
        )
    }
}

object CircuitConnectionChecker {
    fun check(page: CircuitPage): List<SimulationDiagnostic> {
        val issues = mutableListOf<SimulationDiagnostic>()
        if (page.components.isEmpty()) {
            issues.add(
                SimulationDiagnostic(
                    DiagnosticSeverity.INFO,
                    "EMPTY_PAGE",
                    "Canvas is empty. Add components from the palette or load a template."
                )
            )
            return issues
        }

        val graph = NetlistBuilder.build(page, includeClosedSwitches = false)
        val hasAnalogPower = page.components.any {
            val cat = ComponentCatalog.get(it.type).category
            cat == ComponentCategory.POWER && it.type != ComponentType.GROUND
        }
        val hasGround = page.components.any { it.type == ComponentType.GROUND }

        if (hasAnalogPower && !hasGround) {
            issues.add(
                SimulationDiagnostic(
                    DiagnosticSeverity.WARNING,
                    "NO_GND_SYMBOL",
                    "Circuit has power sources but no explicit GND symbol."
                )
            )
        }

        // Check unconnected required input/output pins
        for (comp in page.components) {
            val meta = ComponentCatalog.get(comp.type)
            for (pin in meta.pins) {
                val netId = graph.pinToNetId[NetlistBuilder.pinKey(comp.id, pin.id)]
                val net = graph.nets.firstOrNull { it.id == netId }
                val connectedPins = net?.pinKeys?.size ?: 0
                if (connectedPins <= 1 && (pin.direction == PinDirection.INPUT || pin.direction == PinDirection.OUTPUT)) {
                    // For 7400 ICs, allow unused gates to remain floating without error spam; warn on basic gates
                    if (meta.category == ComponentCategory.LOGIC_GATES || meta.category == ComponentCategory.OUTPUT) {
                        issues.add(
                            SimulationDiagnostic(
                                DiagnosticSeverity.WARNING,
                                "UNCONNECTED_PIN",
                                "${comp.label} pin ${pin.label} is unconnected.",
                                comp.id
                            )
                        )
                    }
                }
            }
        }

        if (issues.isEmpty()) {
            issues.add(
                SimulationDiagnostic(
                    DiagnosticSeverity.INFO,
                    "ERC_PASS",
                    "Electrical Rules Check passed: ${page.components.size} components, ${graph.nets.size} nets verified."
                )
            )
        }
        return issues
    }
}

object AutoWiringHelper {
    data class BoxObstacle(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float)

    /** Generates clean orthogonal Manhattan waypoints between two grid coordinates. */
    fun routeManhattan(from: GridPoint, to: GridPoint): List<GridPoint> {
        if (abs(from.x - to.x) < 0.01f || abs(from.y - to.y) < 0.01f) {
            return emptyList()
        }
        val midX = ((from.x + to.x) / 2f).let { kotlin.math.round(it * 2f) / 2f }
        return listOf(
            GridPoint(midX, from.y),
            GridPoint(midX, to.y)
        )
    }

    /** Obstacle-aware 90-degree orthogonal wire router that avoids cutting through component bodies. */
    fun routeSmartOrthogonal(
        from: GridPoint,
        to: GridPoint,
        page: CircuitPage,
        ignoreCompIds: Set<String> = emptySet()
    ): List<GridPoint> {
        val obstacles = page.components
            .filter { it.id !in ignoreCompIds }
            .map { comp ->
                val meta = ComponentCatalog.metadataFor(comp)
                val rotNorm = ((comp.rotationDegrees % 180) + 180) % 180
                val hw = (if (rotNorm == 90) meta.heightGrid else meta.widthGrid) / 2f - 0.15f
                val hh = (if (rotNorm == 90) meta.widthGrid else meta.heightGrid) / 2f - 0.15f
                BoxObstacle(comp.x - hw, comp.x + hw, comp.y - hh, comp.y + hh)
            }

        fun segmentHits(a: GridPoint, b: GridPoint): Int {
            var hits = 0
            val minSx = minOf(a.x, b.x)
            val maxSx = maxOf(a.x, b.x)
            val minSy = minOf(a.y, b.y)
            val maxSy = maxOf(a.y, b.y)
            for (ob in obstacles) {
                if (abs(a.x - b.x) < 0.05f) {
                    // Vertical segment
                    if (a.x > ob.minX && a.x < ob.maxX && maxSy > ob.minY && minSy < ob.maxY) hits++
                } else if (abs(a.y - b.y) < 0.05f) {
                    // Horizontal segment
                    if (a.y > ob.minY && a.y < ob.maxY && maxSx > ob.minX && minSx < ob.maxX) hits++
                }
            }
            return hits
        }

        fun pathScore(wps: List<GridPoint>): Float {
            val pts = listOf(from) + wps + listOf(to)
            var hits = 0
            var len = 0f
            for (i in 0 until pts.lastIndex) {
                hits += segmentHits(pts[i], pts[i + 1])
                len += abs(pts[i + 1].x - pts[i].x) + abs(pts[i + 1].y - pts[i].y)
            }
            return hits * 100f + len + wps.size * 0.35f
        }

        val snap = { v: Float -> kotlin.math.round(v * 2f) / 2f }
        val midX = snap((from.x + to.x) / 2f)
        val midY = snap((from.y + to.y) / 2f)
        val minY = minOf(from.y, to.y)
        val maxY = maxOf(from.y, to.y)
        val minX = minOf(from.x, to.x)
        val maxX = maxOf(from.x, to.x)

        val candidates = mutableListOf<List<GridPoint>>()
        if (abs(from.x - to.x) < 0.01f || abs(from.y - to.y) < 0.01f) {
            candidates.add(emptyList())
        }
        // Standard L and Z channels
        candidates.add(listOf(GridPoint(midX, from.y), GridPoint(midX, to.y)))
        candidates.add(listOf(GridPoint(from.x, midY), GridPoint(to.x, midY)))
        candidates.add(listOf(GridPoint(to.x, from.y)))
        candidates.add(listOf(GridPoint(from.x, to.y)))

        // Detour channels above/below/left/right of obstacle clusters
        for (offset in listOf(1.5f, 3.0f, 5.0f)) {
            val topY = snap(minY - offset)
            val botY = snap(maxY + offset)
            val leftX = snap(minX - offset)
            val rightX = snap(maxX + offset)
            candidates.add(listOf(GridPoint(from.x, topY), GridPoint(to.x, topY)))
            candidates.add(listOf(GridPoint(from.x, botY), GridPoint(to.x, botY)))
            candidates.add(listOf(GridPoint(leftX, from.y), GridPoint(leftX, to.y)))
            candidates.add(listOf(GridPoint(rightX, from.y), GridPoint(rightX, to.y)))
        }

        return candidates.minByOrNull { pathScore(it) } ?: routeManhattan(from, to)
    }
}
