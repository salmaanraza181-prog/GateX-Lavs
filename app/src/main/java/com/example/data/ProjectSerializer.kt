package com.example.data

import com.example.model.BreadboardJumper
import com.example.model.BreadboardPlacement
import com.example.model.BreadboardState
import com.example.model.CircuitPage
import com.example.model.CircuitProject
import com.example.model.ComponentInstance
import com.example.model.ComponentType
import com.example.model.GridPoint
import com.example.model.PcbBoardState
import com.example.model.PcbFootprint
import com.example.model.PcbLayer
import com.example.model.PcbPad
import com.example.model.PcbTrace
import com.example.model.PcbVia
import com.example.model.WireEndpoint
import com.example.model.WireJunction
import com.example.model.WireSegment
import org.json.JSONArray
import org.json.JSONObject

object ProjectSerializer {
    const val SCHEMA_VERSION = "salman_digital_project_v1"

    fun toJson(project: CircuitProject): String {
        val root = JSONObject()
        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("id", project.id)
        root.put("name", project.name)
        root.put("description", project.description)
        root.put("createdAt", project.createdAt)
        root.put("updatedAt", project.updatedAt)
        root.put("activePageId", project.activePageId)

        val pagesArr = JSONArray()
        for (page in project.pages) {
            pagesArr.put(pageToJson(page))
        }
        root.put("pages", pagesArr)
        root.put("breadboard", breadboardToJson(project.breadboard))
        root.put("pcb", pcbToJson(project.pcb))
        return root.toString(2)
    }

    fun fromJson(raw: String): CircuitProject? {
        return try {
            val parsed = JSONObject(raw)
            val root = if (parsed.has("project") && parsed.optJSONObject("project") != null) {
                parsed.getJSONObject("project")
            } else {
                parsed
            }
            val id = root.getString("id")
            val name = root.optString("name", "Untitled Circuit")
            val description = root.optString("description", "")
            val createdAt = root.optLong("createdAt", System.currentTimeMillis())
            val updatedAt = root.optLong("updatedAt", System.currentTimeMillis())

            val pagesArr = root.optJSONArray("pages") ?: JSONArray()
            val pages = mutableListOf<CircuitPage>()
            for (i in 0 until pagesArr.length()) {
                pages.add(jsonToPage(pagesArr.getJSONObject(i)))
            }
            if (pages.isEmpty()) {
                pages.add(CircuitPage("p1", "Main"))
            }
            val activePageId = root.optString("activePageId", pages.first().id)

            val bb = root.optJSONObject("breadboard")?.let { jsonToBreadboard(it) } ?: BreadboardState()
            val pcb = root.optJSONObject("pcb")?.let { jsonToPcb(it) } ?: PcbBoardState()

            CircuitProject(
                id = id,
                name = name,
                description = description,
                createdAt = createdAt,
                updatedAt = updatedAt,
                pages = pages,
                activePageId = activePageId,
                breadboard = bb,
                pcb = pcb
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun pageToJson(page: CircuitPage): JSONObject {
        val obj = JSONObject()
        obj.put("id", page.id)
        obj.put("name", page.name)

        val compsArr = JSONArray()
        for (c in page.components) {
            val co = JSONObject()
            co.put("id", c.id)
            co.put("type", c.type.name)
            co.put("label", c.label)
            co.put("x", c.x.toDouble())
            co.put("y", c.y.toDouble())
            co.put("rotationDegrees", c.rotationDegrees)
            co.put("flipped", c.flipped)
            co.put("value", c.value)
            val props = JSONObject()
            c.properties.forEach { (k, v) -> props.put(k, v) }
            co.put("properties", props)
            compsArr.put(co)
        }
        obj.put("components", compsArr)

        val jArr = JSONArray()
        for (j in page.junctions) {
            val jo = JSONObject()
            jo.put("id", j.id)
            jo.put("x", j.x.toDouble())
            jo.put("y", j.y.toDouble())
            jArr.put(jo)
        }
        obj.put("junctions", jArr)

        val wArr = JSONArray()
        for (w in page.wires) {
            val wo = JSONObject()
            wo.put("id", w.id)
            wo.put("from", endpointToJson(w.from))
            wo.put("to", endpointToJson(w.to))
            val wpArr = JSONArray()
            for (pt in w.waypoints) {
                wpArr.put(JSONObject().put("x", pt.x.toDouble()).put("y", pt.y.toDouble()))
            }
            wo.put("waypoints", wpArr)
            w.colorHex?.let { wo.put("colorHex", it) }
            wArr.put(wo)
        }
        obj.put("wires", wArr)
        return obj
    }

    private fun jsonToPage(obj: JSONObject): CircuitPage {
        val id = obj.optString("id", "p1")
        val name = obj.optString("name", "Main")
        val comps = mutableListOf<ComponentInstance>()
        val cArr = obj.optJSONArray("components") ?: JSONArray()
        for (i in 0 until cArr.length()) {
            val co = cArr.getJSONObject(i)
            val typeStr = co.optString("type", "RESISTOR")
            val type = ComponentType.entries.firstOrNull { it.name == typeStr } ?: ComponentType.RESISTOR
            val propsObj = co.optJSONObject("properties") ?: JSONObject()
            val props = HashMap<String, String>()
            propsObj.keys().forEach { k -> props[k] = propsObj.optString(k, "") }
            comps.add(
                ComponentInstance(
                    id = co.getString("id"),
                    type = type,
                    label = co.optString("label", type.name),
                    x = co.optDouble("x", 0.0).toFloat(),
                    y = co.optDouble("y", 0.0).toFloat(),
                    rotationDegrees = co.optInt("rotationDegrees", 0),
                    flipped = co.optBoolean("flipped", false),
                    value = co.optString("value", ""),
                    properties = props
                )
            )
        }

        val juncs = mutableListOf<WireJunction>()
        val jArr = obj.optJSONArray("junctions") ?: JSONArray()
        for (i in 0 until jArr.length()) {
            val jo = jArr.getJSONObject(i)
            juncs.add(
                WireJunction(
                    id = jo.getString("id"),
                    x = jo.optDouble("x", 0.0).toFloat(),
                    y = jo.optDouble("y", 0.0).toFloat()
                )
            )
        }

        val wires = mutableListOf<WireSegment>()
        val wArr = obj.optJSONArray("wires") ?: JSONArray()
        for (i in 0 until wArr.length()) {
            val wo = wArr.getJSONObject(i)
            val wpArr = wo.optJSONArray("waypoints") ?: JSONArray()
            val wps = mutableListOf<GridPoint>()
            for (k in 0 until wpArr.length()) {
                val po = wpArr.getJSONObject(k)
                wps.add(GridPoint(po.optDouble("x", 0.0).toFloat(), po.optDouble("y", 0.0).toFloat()))
            }
            wires.add(
                WireSegment(
                    id = wo.getString("id"),
                    from = jsonToEndpoint(wo.getJSONObject("from")),
                    to = jsonToEndpoint(wo.getJSONObject("to")),
                    waypoints = wps,
                    colorHex = if (wo.has("colorHex") && !wo.isNull("colorHex")) wo.optLong("colorHex") else null
                )
            )
        }
        return CircuitPage(id, name, comps, wires, juncs)
    }

    private fun endpointToJson(ep: WireEndpoint): JSONObject = JSONObject().apply {
        ep.componentId?.let { put("componentId", it) }
        ep.pinId?.let { put("pinId", it) }
        ep.junctionId?.let { put("junctionId", it) }
    }

    private fun jsonToEndpoint(obj: JSONObject): WireEndpoint = WireEndpoint(
        componentId = if (obj.has("componentId")) obj.getString("componentId") else null,
        pinId = if (obj.has("pinId")) obj.getString("pinId") else null,
        junctionId = if (obj.has("junctionId")) obj.getString("junctionId") else null
    )

    private fun breadboardToJson(bb: BreadboardState): JSONObject = JSONObject().apply {
        put("placements", JSONArray().apply {
            bb.placements.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("type", p.componentType.name)
                    put("label", p.label)
                    put("value", p.value)
                    put("startHole", p.startHole)
                    put("endHole", p.endHole)
                })
            }
        })
        put("jumpers", JSONArray().apply {
            bb.jumpers.forEach { j ->
                put(JSONObject().apply {
                    put("id", j.id)
                    put("fromHole", j.fromHole)
                    put("toHole", j.toHole)
                    put("colorHex", j.colorHex)
                })
            }
        })
    }

    private fun jsonToBreadboard(obj: JSONObject): BreadboardState {
        val placements = mutableListOf<BreadboardPlacement>()
        val pArr = obj.optJSONArray("placements") ?: JSONArray()
        for (i in 0 until pArr.length()) {
            val po = pArr.getJSONObject(i)
            val type = ComponentType.entries.firstOrNull { it.name == po.optString("type") } ?: ComponentType.RESISTOR
            placements.add(
                BreadboardPlacement(
                    id = po.getString("id"),
                    componentType = type,
                    label = po.optString("label", "R1"),
                    value = po.optString("value", "1k"),
                    startHole = po.getString("startHole"),
                    endHole = po.getString("endHole")
                )
            )
        }
        val jumpers = mutableListOf<BreadboardJumper>()
        val jArr = obj.optJSONArray("jumpers") ?: JSONArray()
        for (i in 0 until jArr.length()) {
            val jo = jArr.getJSONObject(i)
            jumpers.add(
                BreadboardJumper(
                    id = jo.getString("id"),
                    fromHole = jo.getString("fromHole"),
                    toHole = jo.getString("toHole"),
                    colorHex = jo.optLong("colorHex", 0xFF00E5FF)
                )
            )
        }
        return BreadboardState(placements, jumpers)
    }

    private fun pcbToJson(pcb: PcbBoardState): JSONObject = JSONObject().apply {
        put("widthMm", pcb.boardWidthMm.toDouble())
        put("heightMm", pcb.boardHeightMm.toDouble())
        put("footprints", JSONArray().apply {
            pcb.footprints.forEach { fp ->
                put(JSONObject().apply {
                    put("id", fp.id)
                    put("schematicRef", fp.schematicRef)
                    put("footprintName", fp.footprintName)
                    put("xMm", fp.xMm.toDouble())
                    put("yMm", fp.yMm.toDouble())
                    put("rotationDegrees", fp.rotationDegrees)
                    put("pads", JSONArray().apply {
                        fp.pads.forEach { pad ->
                            put(JSONObject().apply {
                                put("id", pad.id)
                                put("name", pad.name)
                                put("relXMm", pad.relXMm.toDouble())
                                put("relYMm", pad.relYMm.toDouble())
                                put("netName", pad.netName)
                            })
                        }
                    })
                })
            }
        })
        put("traces", JSONArray().apply {
            pcb.traces.forEach { tr ->
                put(JSONObject().apply {
                    put("id", tr.id)
                    put("netName", tr.netName)
                    put("layer", tr.layer.name)
                    put("widthMm", tr.widthMm.toDouble())
                    put("points", JSONArray().apply {
                        tr.points.forEach { (x, y) ->
                            put(JSONObject().put("x", x.toDouble()).put("y", y.toDouble()))
                        }
                    })
                })
            }
        })
        put("vias", JSONArray().apply {
            pcb.vias.forEach { v ->
                put(JSONObject().apply {
                    put("id", v.id)
                    put("netName", v.netName)
                    put("xMm", v.xMm.toDouble())
                    put("yMm", v.yMm.toDouble())
                    put("drillMm", v.drillMm.toDouble())
                })
            }
        })
    }

    private fun jsonToPcb(obj: JSONObject): PcbBoardState {
        val w = obj.optDouble("widthMm", 80.0).toFloat()
        val h = obj.optDouble("heightMm", 60.0).toFloat()
        val fps = mutableListOf<PcbFootprint>()
        val fpArr = obj.optJSONArray("footprints") ?: JSONArray()
        for (i in 0 until fpArr.length()) {
            val fo = fpArr.getJSONObject(i)
            val pads = mutableListOf<PcbPad>()
            val pArr = fo.optJSONArray("pads") ?: JSONArray()
            for (k in 0 until pArr.length()) {
                val po = pArr.getJSONObject(k)
                pads.add(
                    PcbPad(
                        id = po.getString("id"),
                        name = po.optString("name", "1"),
                        relXMm = po.optDouble("relXMm", 0.0).toFloat(),
                        relYMm = po.optDouble("relYMm", 0.0).toFloat(),
                        netName = po.optString("netName", "NET")
                    )
                )
            }
            fps.add(
                PcbFootprint(
                    id = fo.getString("id"),
                    schematicRef = fo.optString("schematicRef", "U1"),
                    footprintName = fo.optString("footprintName", "DIP"),
                    xMm = fo.optDouble("xMm", 10.0).toFloat(),
                    yMm = fo.optDouble("yMm", 10.0).toFloat(),
                    rotationDegrees = fo.optInt("rotationDegrees", 0),
                    pads = pads
                )
            )
        }
        val traces = mutableListOf<PcbTrace>()
        val tArr = obj.optJSONArray("traces") ?: JSONArray()
        for (i in 0 until tArr.length()) {
            val to = tArr.getJSONObject(i)
            val layer = PcbLayer.entries.firstOrNull { it.name == to.optString("layer") } ?: PcbLayer.TOP_COPPER
            val pts = mutableListOf<Pair<Float, Float>>()
            val ptArr = to.optJSONArray("points") ?: JSONArray()
            for (k in 0 until ptArr.length()) {
                val p = ptArr.getJSONObject(k)
                pts.add(p.optDouble("x", 0.0).toFloat() to p.optDouble("y", 0.0).toFloat())
            }
            traces.add(
                PcbTrace(
                    id = to.getString("id"),
                    netName = to.optString("netName", "NET"),
                    layer = layer,
                    widthMm = to.optDouble("widthMm", 0.4).toFloat(),
                    points = pts
                )
            )
        }
        val vias = mutableListOf<PcbVia>()
        val vArr = obj.optJSONArray("vias") ?: JSONArray()
        for (i in 0 until vArr.length()) {
            val vo = vArr.getJSONObject(i)
            vias.add(
                PcbVia(
                    id = vo.getString("id"),
                    netName = vo.optString("netName", "NET"),
                    xMm = vo.optDouble("xMm", 0.0).toFloat(),
                    yMm = vo.optDouble("yMm", 0.0).toFloat(),
                    drillMm = vo.optDouble("drillMm", 0.4).toFloat()
                )
            )
        }
        return PcbBoardState(w, h, fps, traces, vias)
    }
}
