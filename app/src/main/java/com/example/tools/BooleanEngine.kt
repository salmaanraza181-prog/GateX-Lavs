package com.example.tools

import com.example.model.CircuitPage
import com.example.model.ComponentInstance
import com.example.model.ComponentType
import com.example.model.GridPoint
import com.example.model.LogicLevel
import com.example.model.WireEndpoint
import com.example.model.WireSegment
import java.util.UUID

sealed interface BoolAst {
    data class Var(val name: String) : BoolAst
    data class Const(val value: Boolean) : BoolAst
    data class Not(val inner: BoolAst) : BoolAst
    data class And(val left: BoolAst, val right: BoolAst) : BoolAst
    data class Or(val left: BoolAst, val right: BoolAst) : BoolAst
    data class Xor(val left: BoolAst, val right: BoolAst) : BoolAst
}

data class BooleanAnalysisResult(
    val variables: List<String>,
    val simplifiedSop: String,
    val rows: List<Pair<List<Boolean>, Boolean>>,
    val ast: BoolAst
)

object BooleanEngine {

    fun parseAndAnalyze(rawExpression: String): BooleanAnalysisResult? {
        val tokens = tokenize(rawExpression) ?: return null
        if (tokens.isEmpty()) return null
        val parser = Parser(tokens)
        val ast = parser.parseOr() ?: return null
        if (!parser.atEnd()) return null

        val vars = collectVars(ast).sorted().take(4).ifEmpty { listOf("A") }
        val rows = mutableListOf<Pair<List<Boolean>, Boolean>>()
        val minterms = mutableListOf<Int>()

        val count = 1 shl vars.size
        for (mask in 0 until count) {
            val env = HashMap<String, Boolean>()
            val vals = vars.indices.map { i ->
                val b = ((mask shr (vars.size - 1 - i)) and 1) == 1
                env[vars[i]] = b
                b
            }
            val out = eval(ast, env)
            rows.add(vals to out)
            if (out) minterms.add(mask)
        }

        val simplified = simplifyQuineMcCluskey(vars, minterms)
        return BooleanAnalysisResult(
            variables = vars,
            simplifiedSop = simplified,
            rows = rows,
            ast = ast
        )
    }

    fun expressionToCircuitPage(rawExpression: String, pageName: String = "Boolean Circuit"): CircuitPage? {
        val analysis = parseAndAnalyze(rawExpression) ?: return null
        val components = mutableListOf<ComponentInstance>()
        val wires = mutableListOf<WireSegment>()

        val inputMap = HashMap<String, ComponentInstance>()
        analysis.variables.forEachIndexed { idx, vName ->
            val comp = ComponentInstance(
                id = "in_$vName",
                type = ComponentType.LOGIC_INPUT,
                label = vName,
                x = 4f,
                y = 4f + idx * 5f,
                value = "0",
                properties = mapOf("state" to "0")
            )
            components.add(comp)
            inputMap[vName] = comp
        }

        var nextY = 5f
        var gateCounter = 1

        // Helper for binary gates without local recursion capture issues
        val helper = object {
            fun build(node: BoolAst, depth: Int): Pair<String, String> {
                return when (node) {
                    is BoolAst.And -> buildBin(ComponentType.GATE_AND, "AND", node.left, node.right, depth)
                    is BoolAst.Or -> buildBin(ComponentType.GATE_OR, "OR", node.left, node.right, depth)
                    is BoolAst.Xor -> buildBin(ComponentType.GATE_XOR, "XOR", node.left, node.right, depth)
                    is BoolAst.Not -> {
                        val (srcId, srcPin) = build(node.inner, depth + 1)
                        val id = "g_${gateCounter++}"
                        val x = 10f + (4 - depth.coerceAtMost(3)) * 6f
                        val y = nextY
                        nextY += 4f
                        components.add(ComponentInstance(id, ComponentType.GATE_NOT, "NOT${gateCounter}", x, y))
                        wires.add(
                            WireSegment(
                                id = "w_${wires.size + 1}",
                                from = WireEndpoint(srcId, srcPin),
                                to = WireEndpoint(id, "a")
                            )
                        )
                        id to "y"
                    }
                    is BoolAst.Var -> inputMap[node.name]!!.id to "out"
                    is BoolAst.Const -> {
                        val id = "c_${gateCounter++}"
                        val type = if (node.value) ComponentType.VCC else ComponentType.GROUND
                        val pin = if (node.value) "vcc" else "gnd"
                        components.add(ComponentInstance(id, type, if (node.value) "1" else "0", 4f, nextY))
                        nextY += 4f
                        id to pin
                    }
                }
            }

            fun buildBin(
                type: ComponentType,
                prefix: String,
                left: BoolAst,
                right: BoolAst,
                depth: Int
            ): Pair<String, String> {
                val (lId, lPin) = build(left, depth + 1)
                val (rId, rPin) = build(right, depth + 1)
                val id = "g_${gateCounter++}"
                val x = 10f + (4 - depth.coerceAtMost(3)) * 6f
                val y = nextY
                nextY += 4.5f
                components.add(ComponentInstance(id, type, "$prefix${gateCounter}", x, y))
                wires.add(WireSegment("w_${wires.size + 1}", WireEndpoint(lId, lPin), WireEndpoint(id, "a")))
                wires.add(WireSegment("w_${wires.size + 1}", WireEndpoint(rId, rPin), WireEndpoint(id, "b")))
                return id to "y"
            }
        }

        val (rootId, rootPin) = helper.build(analysis.ast, 0)
        val outX = (components.maxOfOrNull { it.x } ?: 16f) + 7f
        val outComp = ComponentInstance(
            id = "out_y",
            type = ComponentType.LOGIC_INDICATOR,
            label = "Y",
            x = outX,
            y = (components.firstOrNull { it.id == rootId }?.y ?: 6f)
        )
        components.add(outComp)
        wires.add(WireSegment("w_${wires.size + 1}", WireEndpoint(rootId, rootPin), WireEndpoint(outComp.id, "in")))

        return CircuitPage(
            id = "page_bool",
            name = pageName,
            components = components,
            wires = wires
        )
    }

    fun simplifyMinterms(vars: List<String>, minterms: List<Int>): String =
        simplifyQuineMcCluskey(vars, minterms)

    fun mintermsToCircuitPage(
        vars: List<String>,
        minterms: List<Int>,
        nandOnly: Boolean = false,
        pageName: String = "Logic Converter"
    ): CircuitPage {
        val simplified = simplifyQuineMcCluskey(vars, minterms)
        val analysis = parseAndAnalyze(simplified)
        val components = mutableListOf<ComponentInstance>()
        val wires = mutableListOf<WireSegment>()

        val inputMap = HashMap<String, ComponentInstance>()
        vars.forEachIndexed { idx, vName ->
            val comp = ComponentInstance(
                id = "lc_in_$vName",
                type = ComponentType.LOGIC_INPUT,
                label = vName,
                x = 4f,
                y = 4f + idx * 4.5f,
                value = "0",
                properties = mapOf("state" to "0")
            )
            components.add(comp)
            inputMap[vName] = comp
        }

        if (analysis == null) {
            val outComp = ComponentInstance("lc_out", ComponentType.PROBE_GREEN, "Y", 14f, 6f)
            components.add(outComp)
            return CircuitPage(id = "page_lc", name = pageName, components = components, wires = wires)
        }

        var nextY = 4f
        var gateCounter = 1
        fun nextWireId(): String = "lc_w_${wires.size + 1}"

        fun addNand(inA: Pair<String, String>, inB: Pair<String, String>, colX: Float): Pair<String, String> {
            val id = "lc_g_${gateCounter++}"
            val y = nextY
            nextY += 3.8f
            components.add(ComponentInstance(id, ComponentType.GATE_NAND, "NAND${gateCounter - 1}", colX, y))
            wires.add(WireSegment(nextWireId(), WireEndpoint(inA.first, inA.second), WireEndpoint(id, "a")))
            wires.add(WireSegment(nextWireId(), WireEndpoint(inB.first, inB.second), WireEndpoint(id, "b")))
            return id to "y"
        }

        val helper = object {
            fun buildStandard(node: BoolAst, depth: Int): Pair<String, String> = when (node) {
                is BoolAst.Var -> (inputMap[node.name] ?: inputMap.values.first()).id to "out"
                is BoolAst.Const -> {
                    val id = "lc_c_${gateCounter++}"
                    val type = if (node.value) ComponentType.VCC else ComponentType.GROUND
                    val pin = if (node.value) "vcc" else "gnd"
                    components.add(ComponentInstance(id, type, if (node.value) "1" else "0", 4f, nextY))
                    nextY += 3.5f
                    id to pin
                }
                is BoolAst.Not -> {
                    val src = buildStandard(node.inner, depth + 1)
                    val id = "lc_g_${gateCounter++}"
                    val x = 11f + (3 - depth.coerceAtMost(3)) * 5.5f
                    val y = nextY
                    nextY += 3.8f
                    components.add(ComponentInstance(id, ComponentType.GATE_NOT, "NOT${gateCounter - 1}", x, y))
                    wires.add(WireSegment(nextWireId(), WireEndpoint(src.first, src.second), WireEndpoint(id, "a")))
                    id to "y"
                }
                is BoolAst.And -> buildBin(ComponentType.GATE_AND, "AND", node.left, node.right, depth)
                is BoolAst.Or -> buildBin(ComponentType.GATE_OR, "OR", node.left, node.right, depth)
                is BoolAst.Xor -> buildBin(ComponentType.GATE_XOR, "XOR", node.left, node.right, depth)
            }

            fun buildBin(
                type: ComponentType,
                prefix: String,
                left: BoolAst,
                right: BoolAst,
                depth: Int
            ): Pair<String, String> {
                val l = buildStandard(left, depth + 1)
                val r = buildStandard(right, depth + 1)
                val id = "lc_g_${gateCounter++}"
                val x = 11f + (3 - depth.coerceAtMost(3)) * 5.5f
                val y = nextY
                nextY += 4.2f
                components.add(ComponentInstance(id, type, "$prefix${gateCounter - 1}", x, y))
                wires.add(WireSegment(nextWireId(), WireEndpoint(l.first, l.second), WireEndpoint(id, "a")))
                wires.add(WireSegment(nextWireId(), WireEndpoint(r.first, r.second), WireEndpoint(id, "b")))
                return id to "y"
            }

            fun buildNandOnly(node: BoolAst, depth: Int): Pair<String, String> {
                val baseX = 11f + (4 - depth.coerceAtMost(4)) * 5.5f
                return when (node) {
                    is BoolAst.Var -> (inputMap[node.name] ?: inputMap.values.first()).id to "out"
                    is BoolAst.Const -> {
                        val id = "lc_c_${gateCounter++}"
                        val type = if (node.value) ComponentType.VCC else ComponentType.GROUND
                        val pin = if (node.value) "vcc" else "gnd"
                        components.add(ComponentInstance(id, type, if (node.value) "1" else "0", 4f, nextY))
                        nextY += 3.5f
                        id to pin
                    }
                    is BoolAst.Not -> {
                        val s = buildNandOnly(node.inner, depth + 1)
                        addNand(s, s, baseX)
                    }
                    is BoolAst.And -> {
                        val l = buildNandOnly(node.left, depth + 1)
                        val r = buildNandOnly(node.right, depth + 1)
                        val n1 = addNand(l, r, baseX)
                        addNand(n1, n1, baseX + 5f)
                    }
                    is BoolAst.Or -> {
                        val l = buildNandOnly(node.left, depth + 1)
                        val r = buildNandOnly(node.right, depth + 1)
                        val nl = addNand(l, l, baseX)
                        val nr = addNand(r, r, baseX)
                        addNand(nl, nr, baseX + 5f)
                    }
                    is BoolAst.Xor -> {
                        val l = buildNandOnly(node.left, depth + 1)
                        val r = buildNandOnly(node.right, depth + 1)
                        val n1 = addNand(l, r, baseX)
                        val n2 = addNand(l, n1, baseX + 4.5f)
                        val n3 = addNand(r, n1, baseX + 4.5f)
                        addNand(n2, n3, baseX + 9f)
                    }
                }
            }
        }

        val root = if (nandOnly) helper.buildNandOnly(analysis.ast, 0) else helper.buildStandard(analysis.ast, 0)
        val outX = (components.maxOfOrNull { it.x } ?: 16f) + 6.5f
        val outY = components.firstOrNull { it.id == root.first }?.y ?: 6f
        val outComp = ComponentInstance("lc_out_y", ComponentType.PROBE_GREEN, "Y", outX, outY)
        components.add(outComp)
        wires.add(WireSegment(nextWireId(), WireEndpoint(root.first, root.second), WireEndpoint(outComp.id, "in")))

        val titleNote = ComponentInstance(
            id = "lc_title",
            type = ComponentType.TEXT_NOTE,
            label = "NOTE1",
            x = (outX * 0.5f).coerceAtLeast(10f),
            y = 1.5f,
            value = "Y = $simplified${if (nandOnly) " (NAND-Only)" else ""}"
        )
        components.add(titleNote)

        return CircuitPage(
            id = "page_lc_${UUID.randomUUID().toString().take(6)}",
            name = pageName,
            components = components,
            wires = wires
        )
    }

    private fun eval(ast: BoolAst, env: Map<String, Boolean>): Boolean = when (ast) {
        is BoolAst.Var -> env[ast.name] ?: false
        is BoolAst.Const -> ast.value
        is BoolAst.Not -> !eval(ast.inner, env)
        is BoolAst.And -> eval(ast.left, env) && eval(ast.right, env)
        is BoolAst.Or -> eval(ast.left, env) || eval(ast.right, env)
        is BoolAst.Xor -> eval(ast.left, env) xor eval(ast.right, env)
    }

    private fun collectVars(ast: BoolAst): Set<String> = when (ast) {
        is BoolAst.Var -> setOf(ast.name)
        is BoolAst.Const -> emptySet()
        is BoolAst.Not -> collectVars(ast.inner)
        is BoolAst.And -> collectVars(ast.left) + collectVars(ast.right)
        is BoolAst.Or -> collectVars(ast.left) + collectVars(ast.right)
        is BoolAst.Xor -> collectVars(ast.left) + collectVars(ast.right)
    }

    private fun simplifyQuineMcCluskey(vars: List<String>, minterms: List<Int>): String {
        val n = vars.size
        if (minterms.isEmpty()) return "0"
        if (minterms.size == (1 shl n)) return "1"

        data class Implicant(val bits: String, val covered: Set<Int>)

        var groups = minterms.distinct().map { m ->
            Implicant(m.toString(2).padStart(n, '0'), setOf(m))
        }
        val primes = mutableListOf<Implicant>()

        while (groups.isNotEmpty()) {
            val nextSet = LinkedHashMap<String, Implicant>()
            val used = HashSet<String>()
            for (i in groups.indices) {
                for (j in i + 1 until groups.size) {
                    val a = groups[i]
                    val b = groups[j]
                    var diffIdx = -1
                    var diffs = 0
                    for (k in 0 until n) {
                        if (a.bits[k] != b.bits[k]) {
                            diffs++
                            diffIdx = k
                        }
                    }
                    if (diffs == 1) {
                        used.add(a.bits)
                        used.add(b.bits)
                        val merged = a.bits.substring(0, diffIdx) + "-" + a.bits.substring(diffIdx + 1)
                        val cov = a.covered + b.covered
                        nextSet[merged] = Implicant(merged, cov)
                    }
                }
            }
            for (imp in groups) {
                if (imp.bits !in used && primes.none { it.bits == imp.bits }) {
                    primes.add(imp)
                }
            }
            groups = nextSet.values.toList()
        }

        // Greedy essential prime implicant cover
        val uncovered = minterms.toMutableSet()
        val chosen = mutableListOf<Implicant>()
        for (m in minterms) {
            val covering = primes.filter { m in it.covered }
            if (covering.size == 1 && covering.first() !in chosen) {
                chosen.add(covering.first())
                uncovered.removeAll(covering.first().covered)
            }
        }
        while (uncovered.isNotEmpty()) {
            val best = primes.maxByOrNull { it.covered.count { c -> c in uncovered } } ?: break
            chosen.add(best)
            uncovered.removeAll(best.covered)
        }

        return chosen.joinToString(" + ") { imp ->
            val literals = mutableListOf<String>()
            for (k in 0 until n) {
                when (imp.bits[k]) {
                    '1' -> literals.add(vars[k])
                    '0' -> literals.add("${vars[k]}'")
                }
            }
            if (literals.isEmpty()) "1" else literals.joinToString("·")
        }
    }

    private enum class TokType { VAR, CONST0, CONST1, AND, OR, XOR, NOT, POST_NOT, LPAREN, RPAREN }
    private data class Tok(val type: TokType, val text: String = "")

    private fun tokenize(input: String): List<Tok>? {
        val raw = mutableListOf<Tok>()
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            when {
                ch.isWhitespace() -> i++
                ch == '+' || ch == '|' -> { raw.add(Tok(TokType.OR)); i++ }
                ch == '.' || ch == '*' || ch == '&' || ch == '·' -> { raw.add(Tok(TokType.AND)); i++ }
                ch == '^' || ch == '⊕' -> { raw.add(Tok(TokType.XOR)); i++ }
                ch == '~' || ch == '!' -> { raw.add(Tok(TokType.NOT)); i++ }
                ch == '\'' -> { raw.add(Tok(TokType.POST_NOT)); i++ }
                ch == '(' -> { raw.add(Tok(TokType.LPAREN)); i++ }
                ch == ')' -> { raw.add(Tok(TokType.RPAREN)); i++ }
                ch == '0' -> { raw.add(Tok(TokType.CONST0)); i++ }
                ch == '1' -> { raw.add(Tok(TokType.CONST1)); i++ }
                ch.isLetter() -> {
                    raw.add(Tok(TokType.VAR, ch.uppercase()))
                    i++
                }
                else -> return null
            }
        }
        // Insert implicit AND where needed (e.g. AB, A(B+C), A'B)
        val out = mutableListOf<Tok>()
        for (idx in raw.indices) {
            val curr = raw[idx]
            if (idx > 0) {
                val prev = raw[idx - 1]
                val prevEndsAtom = prev.type in setOf(TokType.VAR, TokType.CONST0, TokType.CONST1, TokType.POST_NOT, TokType.RPAREN)
                val currStartsAtom = curr.type in setOf(TokType.VAR, TokType.CONST0, TokType.CONST1, TokType.NOT, TokType.LPAREN)
                if (prevEndsAtom && currStartsAtom) {
                    out.add(Tok(TokType.AND))
                }
            }
            out.add(curr)
        }
        return out
    }

    private class Parser(private val tokens: List<Tok>) {
        private var pos = 0
        fun atEnd(): Boolean = pos >= tokens.size
        private fun peek(): Tok? = tokens.getOrNull(pos)

        fun parseOr(): BoolAst? {
            var left = parseXor() ?: return null
            while (peek()?.type == TokType.OR) {
                pos++
                val right = parseXor() ?: return null
                left = BoolAst.Or(left, right)
            }
            return left
        }

        private fun parseXor(): BoolAst? {
            var left = parseAnd() ?: return null
            while (peek()?.type == TokType.XOR) {
                pos++
                val right = parseAnd() ?: return null
                left = BoolAst.Xor(left, right)
            }
            return left
        }

        private fun parseAnd(): BoolAst? {
            var left = parseUnary() ?: return null
            while (peek()?.type == TokType.AND) {
                pos++
                val right = parseUnary() ?: return null
                left = BoolAst.And(left, right)
            }
            return left
        }

        private fun parseUnary(): BoolAst? {
            if (peek()?.type == TokType.NOT) {
                pos++
                val inner = parseUnary() ?: return null
                return BoolAst.Not(inner)
            }
            var atom = parsePrimary() ?: return null
            while (peek()?.type == TokType.POST_NOT) {
                pos++
                atom = BoolAst.Not(atom)
            }
            return atom
        }

        private fun parsePrimary(): BoolAst? {
            val t = peek() ?: return null
            return when (t.type) {
                TokType.VAR -> { pos++; BoolAst.Var(t.text) }
                TokType.CONST0 -> { pos++; BoolAst.Const(false) }
                TokType.CONST1 -> { pos++; BoolAst.Const(true) }
                TokType.LPAREN -> {
                    pos++
                    val inner = parseOr() ?: return null
                    if (peek()?.type != TokType.RPAREN) return null
                    pos++
                    inner
                }
                else -> null
            }
        }
    }
}
