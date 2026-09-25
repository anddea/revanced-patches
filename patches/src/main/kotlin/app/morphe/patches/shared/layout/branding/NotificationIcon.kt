package app.morphe.patches.shared.layout.branding

import org.w3c.dom.Document
import org.w3c.dom.Element
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Fits existing monochrome artwork into a 22dp area on a 24dp notification canvas.
 *
 * The paths remain vectors. Transparent padding paths do not affect the fit, while strokes and
 * Bezier control points are included to avoid clipping. Explicit notification artwork is not
 * passed here. Unsupported custom drawables are left intact rather than partially transformed.
 * This uses only XML and arithmetic so patching works on both Android and desktop JVMs.
 */
internal fun normalizeNotificationIcon(document: Document): Boolean {
    val vector = document.documentElement
    if (vector.tagName != "vector") return false
    val paths = (0 until vector.childNodes.length).mapNotNull {
        vector.childNodes.item(it) as? Element
    }
    // Current bundled monochrome icons are flat paths. Preserve custom groups/clips as supplied.
    if (paths.isEmpty() || paths.any { it.tagName != "path" }) return false

    val bounds = IconBounds()
    val colors = mutableListOf<Pair<Element, String>>()
    for (path in paths) {
        var visible = false
        var strokeWidth = 0.0
        for (role in listOf("fill", "stroke")) {
            val value = path.getAttribute("android:${role}Color")
            if (value.isEmpty()) continue
            val alpha = colorAlpha(value) ?: return false
            if (alpha == 0 || path.getAttribute("android:${role}Alpha").toDoubleOrNull() == 0.0) continue
            if (role == "stroke") {
                strokeWidth = path.getAttribute("android:strokeWidth").toDoubleOrNull() ?: 0.0
                if (strokeWidth <= 0.0) continue
            }
            visible = true
            colors += path to role
        }
        if (!visible) continue
        val pathBounds = pathBounds(path.getAttribute("android:pathData")) ?: return false
        // A miter join can extend beyond half the stroke width.
        val join = path.getAttribute("android:strokeLineJoin")
        val miter = if (join.isEmpty() || join == "miter") {
            path.getAttribute("android:strokeMiterLimit").toDoubleOrNull() ?: 4.0
        } else 1.0
        bounds.include(pathBounds, strokeWidth * miter / 2.0)
    }
    val size = max(bounds.right - bounds.left, bounds.bottom - bounds.top)
    if (!size.isFinite() || size <= 0.0) return false

    val scale = 22.0 / size
    val group = document.createElement("group").apply {
        setAttribute("android:scaleX", scale.toString())
        setAttribute("android:scaleY", scale.toString())
        setAttribute("android:translateX", (12 - scale * (bounds.left + bounds.right) / 2).toString())
        setAttribute("android:translateY", (12 - scale * (bounds.top + bounds.bottom) / 2).toString())
    }
    // White works on notification surfaces which render the drawable without applying a tint.
    // Preserve color alpha, fillAlpha/strokeAlpha, holes, and transparent padding paths.
    colors.forEach { (path, role) ->
        val alpha = colorAlpha(path.getAttribute("android:${role}Color"))!!
        path.setAttribute("android:${role}Color", "#${alpha.toString(16).padStart(2, '0')}ffffff")
    }
    vector.removeAttribute("android:tint")
    vector.setAttribute("android:width", "24dp")
    vector.setAttribute("android:height", "24dp")
    vector.setAttribute("android:viewportWidth", "24")
    vector.setAttribute("android:viewportHeight", "24")
    while (vector.hasChildNodes()) group.appendChild(vector.firstChild)
    vector.appendChild(group)
    return true
}

private fun colorAlpha(color: String): Int? {
    if (!color.startsWith('#') || color.drop(1).toLongOrNull(16) == null) return null
    return when (color.length) {
        4, 7 -> 255
        5 -> color.substring(1, 2).toInt(16) * 17
        9 -> color.substring(1, 3).toInt(16)
        else -> null
    }
}

private class IconBounds {
    var left = Double.POSITIVE_INFINITY
    var top = Double.POSITIVE_INFINITY
    var right = Double.NEGATIVE_INFINITY
    var bottom = Double.NEGATIVE_INFINITY

    fun include(x: Double, y: Double) {
        left = min(left, x)
        top = min(top, y)
        right = max(right, x)
        bottom = max(bottom, y)
    }

    fun include(other: IconBounds, padding: Double) {
        include(other.left - padding, other.top - padding)
        include(other.right + padding, other.bottom + padding)
    }
}

private val pathToken = Regex("[MmLlHhVvCcSsQqTtAaZz]|[-+]?(?:[0-9]*\\.[0-9]+|[0-9]+\\.?[0-9]*)(?:[eE][-+]?[0-9]+)?")

/** Conservative SVG path bounds, including relative commands, curve controls and arc ellipses. */
private fun pathBounds(data: String): IconBounds? = runCatching {
    require(data.replace(pathToken, "").all { it.isWhitespace() || it == ',' })
    val tokens = pathToken.findAll(data).map { it.value }.toList()
    val bounds = IconBounds()
    var index = 0
    var command = ' '
    var previous = ' '
    var x = 0.0
    var y = 0.0
    var startX = 0.0
    var startY = 0.0
    var controlX = 0.0
    var controlY = 0.0
    fun number() = tokens[index++].toDouble().also { require(it.isFinite()) }
    while (index < tokens.size) {
        if (tokens[index].singleOrNull()?.isLetter() == true) command = tokens[index++][0]
        val relative = command.isLowerCase()
        val operation = command.uppercaseChar()
        fun point(): Pair<Double, Double> =
            (number() + if (relative) x else 0.0) to (number() + if (relative) y else 0.0)
        if (operation != 'M') bounds.include(x, y)
        val end = when (operation) {
            'M', 'L', 'T' -> {
                if (operation == 'T' && previous in "QT") bounds.include(2 * x - controlX, 2 * y - controlY)
                if (operation == 'T') {
                    controlX = if (previous in "QT") 2 * x - controlX else x
                    controlY = if (previous in "QT") 2 * y - controlY else y
                }
                point()
            }
            'H' -> (number() + if (relative) x else 0.0) to y
            'V' -> x to (number() + if (relative) y else 0.0)
            'C', 'S', 'Q' -> {
                if (operation == 'C') point().also { bounds.include(it.first, it.second) }
                if (operation == 'S' && previous in "CS") bounds.include(2 * x - controlX, 2 * y - controlY)
                point().also {
                    controlX = it.first
                    controlY = it.second
                    bounds.include(controlX, controlY)
                }
                point()
            }
            'A' -> {
                var rx = abs(number())
                var ry = abs(number())
                val angle = Math.toRadians(number())
                val large = number().also { require(it == 0.0 || it == 1.0) } == 1.0
                val sweep = number().also { require(it == 0.0 || it == 1.0) } == 1.0
                val end = point()
                if (rx > 0 && ry > 0 && (x != end.first || y != end.second)) {
                    val c = cos(angle)
                    val s = sin(angle)
                    val dx = (x - end.first) / 2
                    val dy = (y - end.second) / 2
                    val px = c * dx + s * dy
                    val py = -s * dx + c * dy
                    val correction = max(1.0, hypot(px / rx, py / ry))
                    rx *= correction
                    ry *= correction
                    val distance = (px / rx) * (px / rx) + (py / ry) * (py / ry)
                    val factor = (if (large == sweep) -1 else 1) * sqrt(max(0.0, (1 - distance) / distance))
                    val cx = factor * rx * py / ry
                    val cy = -factor * ry * px / rx
                    val centerX = c * cx - s * cy + (x + end.first) / 2
                    val centerY = s * cx + c * cy + (y + end.second) / 2
                    val width = hypot(rx * c, ry * s)
                    val height = hypot(rx * s, ry * c)
                    bounds.include(centerX - width, centerY - height)
                    bounds.include(centerX + width, centerY + height)
                }
                end
            }
            'Z' -> {
                command = ' '
                startX to startY
            }
            else -> error("Unsupported path command")
        }
        x = end.first
        y = end.second
        // Move-only subpaths have no painted area.
        if (operation == 'M') {
            startX = x
            startY = y
            command = if (relative) 'l' else 'L'
        } else bounds.include(x, y)
        previous = operation
    }
    bounds
}.getOrNull()
