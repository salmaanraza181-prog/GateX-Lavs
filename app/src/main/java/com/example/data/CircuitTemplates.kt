package com.example.data

import com.example.model.BreadboardState
import com.example.model.CircuitPage
import com.example.model.CircuitProject
import com.example.model.ComponentInstance
import com.example.model.ComponentType
import com.example.model.PcbBoardState
import com.example.model.WireEndpoint
import com.example.model.WireJunction
import com.example.model.WireSegment

enum class TemplateDomain { ANALOG, DIGITAL, MIXED }

data class CircuitTemplate(
    val id: String,
    val title: String,
    val domain: TemplateDomain,
    val summary: String,
    val explanation: String,
    val buildPage: () -> CircuitPage
) {
    fun toNewProject(now: Long = System.currentTimeMillis()): CircuitProject {
        val page = buildPage()
        return CircuitProject(
            id = "proj_${id}_$now",
            name = title,
            description = summary,
            createdAt = now,
            updatedAt = now,
            pages = listOf(page),
            activePageId = page.id,
            breadboard = BreadboardState(),
            pcb = PcbBoardState()
        )
    }
}

object CircuitTemplates {
    val all: List<CircuitTemplate> by lazy {
        analogTemplates() + digitalTemplates()
    }

    fun byId(id: String): CircuitTemplate? = all.firstOrNull { it.id == id }
}

internal fun w(id: String, c1: String, p1: String, c2: String, p2: String) = WireSegment(
    id = id,
    from = WireEndpoint(componentId = c1, pinId = p1),
    to = WireEndpoint(componentId = c2, pinId = p2)
)

internal fun wJ(id: String, c1: String, p1: String, jId: String) = WireSegment(
    id = id,
    from = WireEndpoint(componentId = c1, pinId = p1),
    to = WireEndpoint(junctionId = jId)
)

internal fun analogTemplates(): List<CircuitTemplate> = listOf(
    CircuitTemplate(
        id = "led_resistor",
        title = "1. LED with Resistor",
        domain = TemplateDomain.ANALOG,
        summary = "5V DC supply driving a Red LED through a 220Ω current-limiting resistor.",
        explanation = "Ohm's Law limits LED current: I = (5V - 2.0V) / 220Ω ≈ 13.6 mA, protecting the LED junction.",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "Main",
                components = listOf(
                    ComponentInstance("v1", ComponentType.DC_SOURCE, "V1", 4f, 8f, value = "5V", properties = mapOf("voltage" to "5.0")),
                    ComponentInstance("r1", ComponentType.RESISTOR, "R1", 10f, 5f, value = "220"),
                    ComponentInstance("led1", ComponentType.LED, "D1", 16f, 5f, value = "Red 2.0V", properties = mapOf("vf" to "2.0")),
                    ComponentInstance("gnd", ComponentType.GROUND, "GND", 4f, 12f)
                ),
                wires = listOf(
                    w("w1", "v1", "pos", "r1", "p1"),
                    w("w2", "r1", "p2", "led1", "anode"),
                    w("w3", "led1", "cathode", "v1", "neg"),
                    w("w4", "v1", "neg", "gnd", "gnd")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "series_circuit",
        title = "2. Series Circuit",
        domain = TemplateDomain.ANALOG,
        summary = "Two resistors (1kΩ + 2kΩ) in series across a 9V battery.",
        explanation = "Total resistance R_eq = R1 + R2 = 3kΩ. Same current I = 9V / 3kΩ = 3 mA flows through both resistors.",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "Main",
                components = listOf(
                    ComponentInstance("v1", ComponentType.DC_SOURCE, "V1", 4f, 8f, value = "9V", properties = mapOf("voltage" to "9.0")),
                    ComponentInstance("r1", ComponentType.RESISTOR, "R1", 10f, 5f, value = "1k"),
                    ComponentInstance("r2", ComponentType.RESISTOR, "R2", 16f, 5f, value = "2k"),
                    ComponentInstance("gnd", ComponentType.GROUND, "GND", 4f, 12f)
                ),
                wires = listOf(
                    w("w1", "v1", "pos", "r1", "p1"),
                    w("w2", "r1", "p2", "r2", "p1"),
                    w("w3", "r2", "p2", "v1", "neg"),
                    w("w4", "v1", "neg", "gnd", "gnd")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "parallel_circuit",
        title = "3. Parallel Circuit",
        domain = TemplateDomain.ANALOG,
        summary = "Two parallel branches (1kΩ || 1kΩ) across a 10V DC supply.",
        explanation = "Equivalent resistance R_eq = 500Ω. Total supply current splits equally (10mA + 10mA = 20mA).",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "Main",
                components = listOf(
                    ComponentInstance("v1", ComponentType.DC_SOURCE, "V1", 4f, 8f, value = "10V", properties = mapOf("voltage" to "10.0")),
                    ComponentInstance("r1", ComponentType.RESISTOR, "R1", 12f, 5f, value = "1k"),
                    ComponentInstance("r2", ComponentType.RESISTOR, "R2", 12f, 10f, value = "1k"),
                    ComponentInstance("gnd", ComponentType.GROUND, "GND", 4f, 13f)
                ),
                junctions = listOf(
                    WireJunction("j1", 8f, 6f),
                    WireJunction("j2", 16f, 10f)
                ),
                wires = listOf(
                    wJ("w1", "v1", "pos", "j1"),
                    wJ("w2", "r1", "p1", "j1"),
                    wJ("w3", "r2", "p1", "j1"),
                    wJ("w4", "r1", "p2", "j2"),
                    wJ("w5", "r2", "p2", "j2"),
                    wJ("w6", "v1", "neg", "j2"),
                    w("w7", "v1", "neg", "gnd", "gnd")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "voltage_divider",
        title = "4. Voltage Divider",
        domain = TemplateDomain.ANALOG,
        summary = "10V input divided by R1=1kΩ and R2=1kΩ to produce Vout = 5.0V.",
        explanation = "Vout = Vin * R2 / (R1 + R2) = 10V * 1k / (1k + 1k) = 5.00V measured by the attached voltmeter.",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "Main",
                components = listOf(
                    ComponentInstance("v1", ComponentType.DC_SOURCE, "Vin", 4f, 8f, value = "10V", properties = mapOf("voltage" to "10.0")),
                    ComponentInstance("r1", ComponentType.RESISTOR, "R1", 10f, 5f, value = "1k"),
                    ComponentInstance("r2", ComponentType.RESISTOR, "R2", 10f, 10f, rotationDegrees = 90, value = "1k"),
                    ComponentInstance("vm", ComponentType.VOLTMETER, "Vout", 17f, 9f),
                    ComponentInstance("gnd", ComponentType.GROUND, "GND", 4f, 13f)
                ),
                junctions = listOf(
                    WireJunction("jmid", 13f, 7f),
                    WireJunction("jgnd", 10f, 12f)
                ),
                wires = listOf(
                    w("w1", "v1", "pos", "r1", "p1"),
                    wJ("w2", "r1", "p2", "jmid"),
                    wJ("w3", "r2", "p1", "jmid"),
                    wJ("w4", "vm", "pos", "jmid"),
                    wJ("w5", "r2", "p2", "jgnd"),
                    wJ("w6", "vm", "neg", "jgnd"),
                    wJ("w7", "v1", "neg", "jgnd"),
                    w("w8", "v1", "neg", "gnd", "gnd")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "rectifier",
        title = "16. Half-Wave Rectifier",
        domain = TemplateDomain.ANALOG,
        summary = "5V 50Hz AC source rectified through a 1N4148 diode into a 1kΩ load with filter cap.",
        explanation = "Diode conducts only on positive AC half-cycles above 0.7V; smoothing capacitor reduces output ripple.",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "Main",
                components = listOf(
                    ComponentInstance("ac1", ComponentType.AC_SOURCE, "AC1", 4f, 8f, value = "5V 50Hz", properties = mapOf("amplitude" to "5.0", "frequency" to "50.0")),
                    ComponentInstance("d1", ComponentType.DIODE, "D1", 10f, 5f, value = "1N4148", properties = mapOf("vf" to "0.7")),
                    ComponentInstance("c1", ComponentType.CAPACITOR, "C1", 15f, 8f, rotationDegrees = 90, value = "47u"),
                    ComponentInstance("r1", ComponentType.RESISTOR, "RL", 20f, 8f, rotationDegrees = 90, value = "1k"),
                    ComponentInstance("osc", ComponentType.OSCILLOSCOPE, "OSC1", 25f, 6f),
                    ComponentInstance("gnd", ComponentType.GROUND, "GND", 4f, 13f)
                ),
                junctions = listOf(
                    WireJunction("jout", 15f, 5f),
                    WireJunction("jg", 15f, 12f)
                ),
                wires = listOf(
                    w("w1", "ac1", "pos", "d1", "anode"),
                    w("w2", "ac1", "pos", "osc", "ch1"),
                    wJ("w3", "d1", "cathode", "jout"),
                    wJ("w4", "c1", "p1", "jout"),
                    wJ("w5", "r1", "p1", "jout"),
                    wJ("w6", "osc", "ch2", "jout"),
                    wJ("w7", "c1", "p2", "jg"),
                    wJ("w8", "r1", "p2", "jg"),
                    wJ("w9", "ac1", "neg", "jg"),
                    wJ("w10", "osc", "gnd", "jg"),
                    w("w11", "ac1", "neg", "gnd", "gnd")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "rc_charging",
        title = "17. RC Charging Circuit",
        domain = TemplateDomain.ANALOG,
        summary = "1kΩ + 10µF RC network (τ = 10ms) driven by a 5V DC step source.",
        explanation = "Capacitor voltage rises exponentially Vc(t) = 5V * (1 - e^(-t/τ)), reaching 63.2% at t = 10ms.",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "Main",
                components = listOf(
                    ComponentInstance("v1", ComponentType.DC_SOURCE, "V1", 4f, 8f, value = "5V", properties = mapOf("voltage" to "5.0")),
                    ComponentInstance("r1", ComponentType.RESISTOR, "R1", 10f, 5f, value = "1k"),
                    ComponentInstance("c1", ComponentType.CAPACITOR, "C1", 16f, 8f, rotationDegrees = 90, value = "10u"),
                    ComponentInstance("osc", ComponentType.OSCILLOSCOPE, "OSC1", 22f, 7f),
                    ComponentInstance("gnd", ComponentType.GROUND, "GND", 4f, 13f)
                ),
                junctions = listOf(
                    WireJunction("jvc", 16f, 5f),
                    WireJunction("jg", 16f, 12f)
                ),
                wires = listOf(
                    w("w1", "v1", "pos", "r1", "p1"),
                    w("w2", "v1", "pos", "osc", "ch1"),
                    wJ("w3", "r1", "p2", "jvc"),
                    wJ("w4", "c1", "p1", "jvc"),
                    wJ("w5", "osc", "ch2", "jvc"),
                    wJ("w6", "c1", "p2", "jg"),
                    wJ("w7", "v1", "neg", "jg"),
                    wJ("w8", "osc", "gnd", "jg"),
                    w("w9", "v1", "neg", "gnd", "gnd")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "transistor_switch",
        title = "18. Transistor Switching Circuit",
        domain = TemplateDomain.ANALOG,
        summary = "NPN 2N2222 low-side driver switching a collector LED load from a base control input.",
        explanation = "When base current flows via Rb (V_BE >= 0.7V), the NPN transistor saturates and lights the collector LED.",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "Main",
                components = listOf(
                    ComponentInstance("vcc", ComponentType.DC_SOURCE, "VCC", 16f, 4f, value = "5V", properties = mapOf("voltage" to "5.0")),
                    ComponentInstance("vin", ComponentType.VARIABLE_INPUT, "Vctrl", 4f, 9f, value = "5V", properties = mapOf("voltage" to "5.0")),
                    ComponentInstance("rb", ComponentType.RESISTOR, "Rb", 9f, 9f, value = "10k"),
                    ComponentInstance("rc", ComponentType.RESISTOR, "Rc", 16f, 7f, rotationDegrees = 90, value = "330"),
                    ComponentInstance("q1", ComponentType.TRANSISTOR_NPN, "Q1", 15f, 11f, value = "2N2222"),
                    ComponentInstance("led1", ComponentType.LED, "D1", 16f, 9f, rotationDegrees = 90, value = "Red 2.0V", properties = mapOf("vf" to "2.0")),
                    ComponentInstance("gnd", ComponentType.GROUND, "GND", 16f, 15f)
                ),
                junctions = listOf(WireJunction("jg", 16f, 14f)),
                wires = listOf(
                    w("w1", "vin", "out", "rb", "p1"),
                    w("w2", "rb", "p2", "q1", "base"),
                    w("w3", "vcc", "pos", "rc", "p1"),
                    w("w4", "rc", "p2", "led1", "anode"),
                    w("w5", "led1", "cathode", "q1", "collector"),
                    wJ("w6", "q1", "emitter", "jg"),
                    wJ("w7", "vin", "ref", "jg"),
                    wJ("w8", "vcc", "neg", "jg"),
                    wJ("w9", "gnd", "gnd", "jg")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "spdt_changeover_lab",
        title = "19. SPDT Switch Changeover",
        domain = TemplateDomain.ANALOG,
        summary = "5V DC source routed via interactive SPDT switch between two parallel LED indicators.",
        explanation = "Tap the SPDT switch on canvas to toggle COM between NC (bottom branch) and NO (top branch).",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "Main",
                components = listOf(
                    ComponentInstance("v1", ComponentType.DC_SOURCE, "V1", 4f, 8f, value = "5V", properties = mapOf("voltage" to "5.0")),
                    ComponentInstance("sw1", ComponentType.SPDT_SWITCH, "SW1", 10f, 8f, value = "NC", properties = mapOf("position" to "NC")),
                    ComponentInstance("r1", ComponentType.RESISTOR, "R_NO", 16f, 5f, value = "220"),
                    ComponentInstance("led1", ComponentType.LED, "D_NO", 22f, 5f, value = "Green 2V", properties = mapOf("vf" to "2.0")),
                    ComponentInstance("r2", ComponentType.RESISTOR, "R_NC", 16f, 11f, value = "220"),
                    ComponentInstance("led2", ComponentType.LED, "D_NC", 22f, 11f, value = "Red 2V", properties = mapOf("vf" to "2.0")),
                    ComponentInstance("gnd", ComponentType.GROUND, "GND", 4f, 14f)
                ),
                junctions = listOf(WireJunction("jg", 22f, 13f)),
                wires = listOf(
                    w("w1", "v1", "pos", "sw1", "com"),
                    w("w2", "sw1", "no", "r1", "p1"),
                    w("w3", "r1", "p2", "led1", "anode"),
                    wJ("w4", "led1", "cathode", "jg"),
                    w("w5", "sw1", "nc", "r2", "p1"),
                    w("w6", "r2", "p2", "led2", "anode"),
                    wJ("w7", "led2", "cathode", "jg"),
                    wJ("w8", "v1", "neg", "jg"),
                    wJ("w9", "gnd", "gnd", "jg")
                )
            )
        }
    ),
    CircuitTemplate(
        id = "relay_motor_driver",
        title = "20. Relay Motor & Buzzer Driver",
        domain = TemplateDomain.ANALOG,
        summary = "SPST switch energizes a 5V SPDT relay coil to switch a 12V DC motor & warning buzzer.",
        explanation = "Low-voltage 5V switch controls the relay coil; when closed, the relay switches COM from buzzer (NC) to motor (NO).",
        buildPage = {
            CircuitPage(
                id = "p1",
                name = "Main",
                components = listOf(
                    ComponentInstance("v5", ComponentType.DC_SOURCE, "Vcoil", 4f, 5f, value = "5V", properties = mapOf("voltage" to "5.0")),
                    ComponentInstance("sw1", ComponentType.TOGGLE_SWITCH, "SW1", 9f, 5f, value = "ON", properties = mapOf("closed" to "true")),
                    ComponentInstance("k1", ComponentType.RELAY_SPDT, "K1", 15f, 7f, value = "5V SPDT", properties = mapOf("coilVoltage" to "5.0", "coilResistance" to "100")),
                    ComponentInstance("v12", ComponentType.DC_SOURCE, "V12", 4f, 11f, value = "12V", properties = mapOf("voltage" to "12.0")),
                    ComponentInstance("m1", ComponentType.MOTOR, "MTR1", 22f, 5f, value = "12V (24Ω)"),
                    ComponentInstance("bz1", ComponentType.BUZZER, "BUZ1", 22f, 10f, value = "1 kHz"),
                    ComponentInstance("gnd", ComponentType.GROUND, "GND", 4f, 15f)
                ),
                junctions = listOf(WireJunction("jg", 15f, 14f)),
                wires = listOf(
                    w("w1", "v5", "pos", "sw1", "p1"),
                    w("w2", "sw1", "p2", "k1", "coil1"),
                    wJ("w3", "k1", "coil2", "jg"),
                    wJ("w4", "v5", "neg", "jg"),
                    w("w5", "v12", "pos", "k1", "com"),
                    wJ("w6", "v12", "neg", "jg"),
                    w("w7", "k1", "no", "m1", "pos"),
                    wJ("w8", "m1", "neg", "jg"),
                    w("w9", "k1", "nc", "bz1", "pos"),
                    wJ("w10", "bz1", "neg", "jg"),
                    wJ("w11", "gnd", "gnd", "jg")
                )
            )
        }
    )
)
