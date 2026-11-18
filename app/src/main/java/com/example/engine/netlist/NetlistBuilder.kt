package com.example.engine.netlist

import com.example.model.CircuitPage
import com.example.model.ComponentCatalog
import com.example.model.ComponentInstance
import com.example.model.ComponentType
import com.example.model.WireSegment

data class ElectricalNet(
    val id: String,
    val pinKeys: Set<String>,
    val junctionIds: Set<String>,
    val wireIds: Set<String>
)

data class NetlistGraph(
    val nets: List<ElectricalNet>,
    val pinToNetId: Map<String, String>,
    val wireToNetId: Map<String, String>,
    val junctionToNetId: Map<String, String>,
    val componentsById: Map<String, ComponentInstance>
)

object NetlistBuilder {
    fun pinKey(componentId: String, pinId: String): String = "pin:$componentId:$pinId"

    fun build(
        page: CircuitPage,
        includeClosedSwitches: Boolean = true,
        unionIntactFuses: Boolean = true
    ): NetlistGraph {
        val componentsById = page.components.associateBy { it.id }
        val parent = HashMap<String, String>()

        fun find(x: String): String {
            var root = x
            while (parent[root] != null && parent[root] != root) {
                root = parent[root]!!
            }
            var curr = x
            while (curr != root) {
                val next = parent[curr] ?: break
                parent[curr] = root
                curr = next
            }
            return root
        }

        fun union(a: String, b: String) {
            if (!parent.containsKey(a)) parent[a] = a
            if (!parent.containsKey(b)) parent[b] = b
            val ra = find(a)
            val rb = find(b)
            if (ra != rb) parent[rb] = ra
        }

        for (comp in page.components) {
            val meta = ComponentCatalog.metadataFor(comp)
            for (pin in meta.pins) {
                val key = pinKey(comp.id, pin.id)
                parent[key] = key
            }
            if (includeClosedSwitches) {
                when (comp.type) {
                    ComponentType.TOGGLE_SWITCH -> {
                        if (comp.properties["closed"] == "true") {
                            union(pinKey(comp.id, "p1"), pinKey(comp.id, "p2"))
                        }
                    }
                    ComponentType.PUSH_BUTTON -> {
                        if (comp.properties["pressed"] == "true") {
                            union(pinKey(comp.id, "p1"), pinKey(comp.id, "p2"))
                        }
                    }
                    ComponentType.PUSH_BUTTON_NC -> {
                        if (comp.properties["pressed"] != "true") {
                            union(pinKey(comp.id, "p1"), pinKey(comp.id, "p2"))
                        }
                    }
                    ComponentType.SPDT_SWITCH -> {
                        val pos = if (comp.properties["position"] == "NO") "no" else "nc"
                        union(pinKey(comp.id, "com"), pinKey(comp.id, pos))
                    }
                    ComponentType.DPST_SWITCH -> {
                        if (comp.properties["closed"] == "true") {
                            union(pinKey(comp.id, "a1"), pinKey(comp.id, "a2"))
                            union(pinKey(comp.id, "b1"), pinKey(comp.id, "b2"))
                        }
                    }
                    ComponentType.DPDT_SWITCH -> {
                        val suffix = if (comp.properties["position"] == "NO") "no" else "nc"
                        union(pinKey(comp.id, "com1"), pinKey(comp.id, "${suffix}1"))
                        union(pinKey(comp.id, "com2"), pinKey(comp.id, "${suffix}2"))
                    }
                    ComponentType.ROTARY_SWITCH -> {
                        val pos = (comp.properties["position"]?.toIntOrNull() ?: 1).coerceIn(1, 4)
                        union(pinKey(comp.id, "com"), pinKey(comp.id, "p$pos"))
                    }
                    ComponentType.FUSE -> {
                        if (unionIntactFuses && comp.properties["blown"] != "true") {
                            union(pinKey(comp.id, "p1"), pinKey(comp.id, "p2"))
                        }
                    }
                    else -> Unit
                }
            }
        }

        // Connect wireless NET_LABEL tags that share the same non-blank net name
        val netLabelsByName = HashMap<String, MutableList<String>>()
        for (comp in page.components) {
            if (comp.type == ComponentType.NET_LABEL) {
                val tagName = comp.value.trim().uppercase()
                if (tagName.isNotEmpty()) {
                    netLabelsByName.getOrPut(tagName) { mutableListOf() }.add(pinKey(comp.id, "pin"))
                }
            }
        }
        for (keys in netLabelsByName.values) {
            if (keys.size >= 2) {
                val first = keys.first()
                for (i in 1 until keys.size) {
                    union(first, keys[i])
                }
            }
        }

        for (j in page.junctions) {
            val jKey = "junc:${j.id}"
            parent[jKey] = jKey
        }

        for (wire in page.wires) {
            val fromKey = wire.from.key()
            val toKey = wire.to.key()
            if (fromKey != "invalid" && toKey != "invalid") {
                union(fromKey, toKey)
            }
        }

        val rootToPins = LinkedHashMap<String, MutableSet<String>>()
        val rootToJunctions = LinkedHashMap<String, MutableSet<String>>()
        val rootToWires = LinkedHashMap<String, MutableSet<String>>()

        for (node in parent.keys) {
            val root = find(node)
            when {
                node.startsWith("pin:") -> rootToPins.getOrPut(root) { LinkedHashSet() }.add(node)
                node.startsWith("junc:") -> rootToJunctions.getOrPut(root) { LinkedHashSet() }
                    .add(node.removePrefix("junc:"))
            }
        }

        val wireToRoot = HashMap<String, String>()
        for (wire in page.wires) {
            val fromKey = wire.from.key()
            if (fromKey != "invalid" && parent.containsKey(fromKey)) {
                val root = find(fromKey)
                rootToWires.getOrPut(root) { LinkedHashSet() }.add(wire.id)
                wireToRoot[wire.id] = root
            }
        }

        val allRoots = (rootToPins.keys + rootToJunctions.keys + rootToWires.keys).toList()
        val rootToNetId = HashMap<String, String>()
        val nets = ArrayList<ElectricalNet>(allRoots.size)

        allRoots.forEachIndexed { index, root ->
            val netId = "net_$index"
            rootToNetId[root] = netId
            nets.add(
                ElectricalNet(
                    id = netId,
                    pinKeys = rootToPins[root] ?: emptySet(),
                    junctionIds = rootToJunctions[root] ?: emptySet(),
                    wireIds = rootToWires[root] ?: emptySet()
                )
            )
        }

        val pinToNetId = HashMap<String, String>()
        val junctionToNetId = HashMap<String, String>()
        val wireToNetId = HashMap<String, String>()

        for (net in nets) {
            net.pinKeys.forEach { pinToNetId[it] = net.id }
            net.junctionIds.forEach { junctionToNetId[it] = net.id }
            net.wireIds.forEach { wireToNetId[it] = net.id }
        }

        return NetlistGraph(
            nets = nets,
            pinToNetId = pinToNetId,
            wireToNetId = wireToNetId,
            junctionToNetId = junctionToNetId,
            componentsById = componentsById
        )
    }
}
