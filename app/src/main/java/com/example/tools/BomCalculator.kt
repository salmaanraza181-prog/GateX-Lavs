package com.example.tools

import com.example.model.AnalogSimulationResult
import com.example.model.CircuitPage
import com.example.model.ComponentCatalog
import com.example.model.ComponentCategory
import com.example.model.ComponentType
import com.example.model.DigitalSimulationResult
import kotlin.math.abs

data class BomItem(
    val partName: String,
    val value: String,
    val category: String,
    val designators: List<String>,
    val quantity: Int,
    val unitCostUsd: Double,
    val totalCostUsd: Double,
    val powerMw: Double
)

data class BomReport(
    val items: List<BomItem>,
    val totalPartsCount: Int,
    val totalEstimatedUsd: Double,
    val totalEstimatedPkr: Double,
    val totalPowerMw: Double,
    val recommendedSupply: String
)

object BomCalculator {
    private const val USD_TO_PKR = 278.0

    fun unitPriceUsd(type: ComponentType): Double = when (type) {
        ComponentType.RESISTOR, ComponentType.CAPACITOR, ComponentType.DIODE -> 0.05
        ComponentType.POLARIZED_CAPACITOR, ComponentType.ZENER_DIODE, ComponentType.LED,
        ComponentType.PHOTODIODE, ComponentType.FUSE -> 0.12
        ComponentType.RGB_LED, ComponentType.VARIABLE_RESISTOR, ComponentType.POTENTIOMETER,
        ComponentType.INDUCTOR, ComponentType.TRANSISTOR_NPN, ComponentType.TRANSISTOR_PNP,
        ComponentType.MOSFET -> 0.35
        ComponentType.TOGGLE_SWITCH, ComponentType.PUSH_BUTTON, ComponentType.PUSH_BUTTON_NC,
        ComponentType.SPDT_SWITCH, ComponentType.DPST_SWITCH, ComponentType.DPDT_SWITCH -> 0.45
        ComponentType.DIP_SWITCH, ComponentType.ROTARY_SWITCH, ComponentType.BUZZER,
        ComponentType.PILOT_LAMP, ComponentType.RELAY, ComponentType.RELAY_SPDT -> 0.85
        ComponentType.OP_AMP_741, ComponentType.IC_555, ComponentType.VOLTAGE_REGULATOR -> 0.60
        ComponentType.IC_7400, ComponentType.IC_7402, ComponentType.IC_7404, ComponentType.IC_7408,
        ComponentType.IC_7410, ComponentType.IC_7411, ComponentType.IC_7420, ComponentType.IC_7421,
        ComponentType.IC_7427, ComponentType.IC_7430, ComponentType.IC_7432, ComponentType.IC_7486,
        ComponentType.IC_74125, ComponentType.IC_74266 -> 0.55
        ComponentType.IC_7472, ComponentType.IC_7473, ComponentType.IC_7474, ComponentType.IC_7476,
        ComponentType.IC_7490, ComponentType.IC_7493, ComponentType.IC_74138, ComponentType.IC_74151,
        ComponentType.IC_7447, ComponentType.IC_7483, ComponentType.IC_74194, ComponentType.IC_7485,
        ComponentType.IC_74595, ComponentType.IC_74161, ComponentType.IC_74154 -> 0.95
        ComponentType.SEVEN_SEGMENT, ComponentType.HEX_DISPLAY, ComponentType.BAR_GRAPH,
        ComponentType.DECODED_BAR_GRAPH, ComponentType.TRAFFIC_LIGHT -> 1.25
        ComponentType.DOT_MATRIX_8X8 -> 2.10
        ComponentType.LCD_16X2 -> 3.50
        ComponentType.KEYPAD -> 1.80
        ComponentType.MOTOR, ComponentType.TRANSFORMER -> 2.80
        ComponentType.BATTERY, ComponentType.DC_SOURCE, ComponentType.POWER_SUPPLY -> 1.50
        ComponentType.CUSTOM_IC -> 1.20
        else -> 0.0
    }

    private fun typicalQuiescentPowerMw(type: ComponentType): Double = when (type) {
        ComponentType.IC_7400, ComponentType.IC_7402, ComponentType.IC_7404, ComponentType.IC_7408,
        ComponentType.IC_7410, ComponentType.IC_7411, ComponentType.IC_7420, ComponentType.IC_7421,
        ComponentType.IC_7427, ComponentType.IC_7430, ComponentType.IC_7432, ComponentType.IC_7486,
        ComponentType.IC_74125, ComponentType.IC_74266 -> 12.0
        ComponentType.IC_7472, ComponentType.IC_7473, ComponentType.IC_7474, ComponentType.IC_7476,
        ComponentType.IC_7490, ComponentType.IC_7493, ComponentType.IC_74138, ComponentType.IC_74151,
        ComponentType.IC_7447, ComponentType.IC_7483, ComponentType.IC_74194, ComponentType.IC_7485,
        ComponentType.IC_74595, ComponentType.IC_74161, ComponentType.IC_74154, ComponentType.CUSTOM_IC -> 28.0
        ComponentType.IC_555, ComponentType.OP_AMP_741 -> 35.0
        ComponentType.SEVEN_SEGMENT, ComponentType.HEX_DISPLAY, ComponentType.BAR_GRAPH,
        ComponentType.DECODED_BAR_GRAPH, ComponentType.TRAFFIC_LIGHT -> 45.0
        ComponentType.LCD_16X2 -> 95.0
        ComponentType.DOT_MATRIX_8X8 -> 120.0
        ComponentType.BUZZER -> 50.0
        ComponentType.MOTOR -> 250.0
        ComponentType.GATE_AND, ComponentType.GATE_OR, ComponentType.GATE_NOT, ComponentType.GATE_NAND,
        ComponentType.GATE_NOR, ComponentType.GATE_XOR, ComponentType.GATE_XNOR, ComponentType.GATE_BUFFER,
        ComponentType.GATE_TRISTATE -> 4.0
        ComponentType.LED, ComponentType.RGB_LED, ComponentType.PILOT_LAMP,
        ComponentType.PROBE_RED, ComponentType.PROBE_GREEN, ComponentType.PROBE_YELLOW,
        ComponentType.PROBE_BLUE, ComponentType.PROBE_ORANGE, ComponentType.PROBE_CYAN -> 15.0
        else -> 0.0
    }

    fun generate(
        page: CircuitPage,
        analogResult: AnalogSimulationResult? = null,
        digitalResult: DigitalSimulationResult? = null
    ): BomReport {
        val ignored = setOf(ComponentType.NET_LABEL, ComponentType.TEXT_NOTE, ComponentType.GROUND, ComponentType.VCC)
        val physical = page.components.filter { it.type !in ignored }

        val grouped = physical.groupBy { comp ->
            val meta = ComponentCatalog.get(comp.type)
            val valStr = comp.value.ifBlank { meta.defaultValue }
            Triple(comp.type, meta.displayName, valStr)
        }

        val items = grouped.entries.map { (key, comps) ->
            val (type, displayName, valStr) = key
            val meta = ComponentCatalog.get(type)
            val unit = unitPriceUsd(type)
            val powerMw = comps.sumOf { comp ->
                val directPowerW = analogResult?.componentPowers?.get(comp.id)
                val analogI = analogResult?.componentCurrents?.get(comp.id)
                val vDrop = analogResult?.let { res ->
                    val pinVs = meta.pins.mapNotNull { pin ->
                        res.pinVoltages["${comp.id}:${pin.id}"]
                    }
                    if (pinVs.size >= 2) abs(pinVs.max() - pinVs.min()) else null
                }
                val analogPowerMw = when {
                    directPowerW != null && abs(directPowerW) > 1e-5 -> abs(directPowerW) * 1000.0
                    analogI != null && vDrop != null -> abs(analogI * vDrop) * 1000.0
                    else -> null
                }
                if (analogPowerMw != null && analogPowerMw > 0.01) {
                    analogPowerMw
                } else {
                    val activeState = digitalResult?.componentStates?.get(comp.id)
                    val isLit = activeState?.get("state") == "1" || activeState?.get("active") == "1"
                    if (meta.category == ComponentCategory.OUTPUT && !isLit && digitalResult != null && digitalResult.tickCount > 0) {
                        typicalQuiescentPowerMw(type) * 0.2
                    } else {
                        typicalQuiescentPowerMw(type)
                    }
                }
            }
            BomItem(
                partName = displayName,
                value = valStr,
                category = meta.category.title,
                designators = comps.map { it.label }.sorted(),
                quantity = comps.size,
                unitCostUsd = unit,
                totalCostUsd = unit * comps.size,
                powerMw = powerMw
            )
        }.sortedWith(compareBy<BomItem> { it.category }.thenBy { it.partName })

        val totalCost = items.sumOf { it.totalCostUsd }
        val totalPower = items.sumOf { it.powerMw }
        val supplyRec = when {
            totalPower <= 50.0 -> "5V USB / 100mA Lab Supply"
            totalPower <= 350.0 -> "5V Regulated 500mA Supply (7805)"
            else -> "5V / 12V Dual Bench Supply (1A+)"
        }

        return BomReport(
            items = items,
            totalPartsCount = physical.size,
            totalEstimatedUsd = totalCost,
            totalEstimatedPkr = totalCost * USD_TO_PKR,
            totalPowerMw = totalPower,
            recommendedSupply = supplyRec
        )
    }

    fun toCsv(projectName: String, report: BomReport): String {
        val sb = StringBuilder()
        sb.appendLine("GateX Labs Bill of Materials - ${projectName.replace(",", " ")}")
        sb.appendLine("Designators,Part Name,Value,Category,Qty,Unit USD,Total USD,Est Power (mW)")
        report.items.forEach { item ->
            val des = item.designators.joinToString(" ")
            sb.appendLine(
                "\"$des\",\"${item.partName}\",\"${item.value}\",\"${item.category}\",${item.quantity},${"%.2f".format(item.unitCostUsd)},${"%.2f".format(item.totalCostUsd)},${"%.1f".format(item.powerMw)}"
            )
        }
        sb.appendLine(",,,TOTAL,${report.totalPartsCount},,${"%.2f".format(report.totalEstimatedUsd)},${"%.1f".format(report.totalPowerMw)}")
        return sb.toString()
    }
}
