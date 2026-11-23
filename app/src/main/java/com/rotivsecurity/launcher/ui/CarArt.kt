package com.rotivsecurity.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.rotivsecurity.launcher.CarPhoto
import com.rotivsecurity.launcher.Prefs
import kotlin.math.min

/** Carrocerias disponíveis no desenho. */
val CarBodies = listOf("Hatch", "Sedã", "SUV", "Picape")

/** Cores prontas (nome, ARGB). A última opção nos Ajustes é a cor livre. */
val CarColors = listOf(
    "Preto" to 0xFF111318L,
    "Branco" to 0xFFE9EDF2L,
    "Prata" to 0xFFA7AFB9L,
    "Grafite" to 0xFF4A505AL,
    "Vermelho" to 0xFFC0262DL,
    "Azul Rotiv" to 0xFF3B82F6L,
    "Marinho" to 0xFF1E3A8AL,
    "Verde" to 0xFF2F7D4FL,
    "Amarelo" to 0xFFE7B416L,
    "Laranja" to 0xFFE26A1CL,
    "Vinho" to 0xFF6E1A2BL,
    "Bege" to 0xFFC9B48AL,
    "Marrom" to 0xFF5B3A29L,
)

/** O carro do usuário: a foto, se houver, ou o desenho na cor escolhida. */
@Composable
fun MyCar(modifier: Modifier = Modifier) {
    val usePhoto by Prefs.carPhoto.collectAsState()
    val photo by CarPhoto.image.collectAsState()
    val body by Prefs.carBody.collectAsState()
    val color by Prefs.carColor.collectAsState()
    val p = photo
    if (usePhoto && p != null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Image(
                p,
                contentDescription = "Meu carro",
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Fit,
            )
        }
    } else {
        CarDrawing(body, Color(color), modifier)
    }
}

/**
 * Carro de perfil desenhado por código (sem imagens de montadoras).
 * Só a lataria muda de cor; vidros, rodas e faróis ficam fixos.
 */
@Composable
fun CarDrawing(body: Int, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = min(size.width / VIEW_W, size.height / VIEW_H)
        val ox = (size.width - VIEW_W * s) / 2f
        val oy = (size.height - VIEW_H * s) / 2f
        withTransform({
            translate(ox, oy)
            scale(s, s, pivot = Offset.Zero)
        }) {
            drawCar(body.coerceIn(0, 3), color)
        }
    }
}

private const val VIEW_W = 400f
private const val VIEW_H = 170f
private const val WHEEL_Y = 128f
private const val REAR_WHEEL_X = 95f
private const val FRONT_WHEEL_X = 305f

private fun o(x: Int, y: Int) = Offset(x.toFloat(), y.toFloat())

/** Contorno superior da lataria, da traseira (esquerda) para a frente (direita). */
private val outlines = listOf(
    // Hatch
    listOf(o(26, 136), o(20, 128), o(18, 100), o(22, 86), o(34, 66), o(70, 50), o(200, 46), o(250, 80), o(340, 90), o(370, 98), o(378, 116), o(372, 134), o(370, 136)),
    // Sedã
    listOf(o(26, 136), o(18, 128), o(18, 104), o(26, 94), o(70, 88), o(98, 62), o(140, 48), o(215, 48), o(258, 82), o(345, 92), o(372, 100), o(380, 116), o(374, 134), o(370, 136)),
    // SUV
    listOf(o(26, 136), o(16, 128), o(14, 90), o(20, 58), o(36, 40), o(200, 38), o(250, 74), o(345, 84), o(374, 92), o(382, 116), o(376, 134), o(370, 136)),
    // Picape
    listOf(o(26, 136), o(16, 128), o(16, 84), o(120, 84), o(124, 48), o(200, 44), o(248, 80), o(345, 88), o(374, 96), o(382, 116), o(376, 134), o(370, 136)),
)

/** Vidros de cada carroceria. */
private val windows = listOf(
    listOf(
        listOf(o(245, 80), o(199, 50), o(158, 50), o(158, 80)),
        listOf(o(152, 50), o(78, 52), o(48, 66), o(40, 80), o(152, 80)),
    ),
    listOf(
        listOf(o(252, 82), o(212, 52), o(168, 52), o(168, 82)),
        listOf(o(160, 52), o(142, 52), o(106, 64), o(92, 82), o(160, 82)),
    ),
    listOf(
        listOf(o(244, 74), o(198, 42), o(150, 42), o(150, 74)),
        listOf(o(144, 42), o(80, 42), o(80, 74), o(144, 74)),
        listOf(o(74, 42), o(38, 44), o(28, 60), o(28, 74), o(74, 74)),
    ),
    listOf(
        listOf(o(243, 78), o(198, 48), o(160, 48), o(160, 78)),
        listOf(o(154, 48), o(130, 50), o(128, 78), o(154, 78)),
    ),
)

private fun bodyPath(body: Int): Path {
    val pts = outlines[body]
    val p = Path()
    p.moveTo(pts[0].x, pts[0].y)
    // Cantos arredondados: curva até o meio de cada segmento.
    for (i in 1 until pts.size - 1) {
        val mid = (pts[i] + pts[i + 1]) / 2f
        p.quadraticTo(pts[i].x, pts[i].y, mid.x, mid.y)
    }
    p.lineTo(pts.last().x, pts.last().y)
    // Parte de baixo com os recortes das rodas (frente → traseira).
    val r = 34f
    val archY = 134f
    p.lineTo(FRONT_WHEEL_X + r, 136f)
    p.arcTo(Rect(Offset(FRONT_WHEEL_X, archY), r), 0f, -180f, false)
    p.lineTo(REAR_WHEEL_X + r, 136f)
    p.arcTo(Rect(Offset(REAR_WHEEL_X, archY), r), 0f, -180f, false)
    p.close()
    return p
}

private fun polygon(pts: List<Offset>) = Path().apply {
    moveTo(pts[0].x, pts[0].y)
    pts.drop(1).forEach { lineTo(it.x, it.y) }
    close()
}

private fun DrawScope.drawCar(body: Int, color: Color) {
    // Sombra no chão
    drawOval(Color.Black.copy(alpha = 0.45f), topLeft = Offset(30f, 148f), size = Size(345f, 16f))

    val shape = bodyPath(body)
    drawPath(shape, color)
    clipPath(shape) {
        // Brilho em cima e saia escura embaixo, para dar volume
        drawRect(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.18f),
                1f to Color.Transparent,
                startY = 30f,
                endY = 100f,
            ),
            topLeft = Offset(0f, 30f),
            size = Size(VIEW_W, 70f),
        )
        drawRect(Color.Black.copy(alpha = 0.30f), topLeft = Offset(0f, 118f), size = Size(VIEW_W, 30f))
        // Vinco lateral
        drawLine(Color.White.copy(alpha = 0.16f), Offset(30f, 104f), Offset(372f, 108f), strokeWidth = 1.5f)
    }

    // Vidros
    windows[body].forEach { w ->
        val glass = polygon(w)
        drawPath(glass, Color(0xFF16202F))
        clipPath(glass) {
            drawLine(Color.White.copy(alpha = 0.10f), Offset(w[0].x - 30f, 90f), Offset(w[0].x + 10f, 30f), strokeWidth = 10f)
        }
    }

    // Porta, maçaneta e retrovisor
    val doorX = if (body == 3) 157f else 164f
    drawLine(Color.Black.copy(alpha = 0.35f), Offset(doorX, 86f), Offset(doorX, 128f), strokeWidth = 1.5f)
    drawRoundRect(Color.Black.copy(alpha = 0.35f), topLeft = Offset(doorX + 14f, 94f), size = Size(16f, 4f))
    val mirrorX = windows[body][0][0].x - 6f
    drawRoundRect(color.copy(alpha = 1f), topLeft = Offset(mirrorX - 4f, windows[body][0][0].y - 6f), size = Size(14f, 9f))

    // Faróis e lanternas (fixos)
    drawRoundRect(Color(0xFFE6F0FF), topLeft = Offset(360f, 99f), size = Size(18f, 8f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f))
    drawRoundRect(Color(0xFFEF4444), topLeft = Offset(16f, 100f), size = Size(8f, 14f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f))

    // Rodas
    for (x in listOf(REAR_WHEEL_X, FRONT_WHEEL_X)) {
        val c = Offset(x, WHEEL_Y)
        drawCircle(Color(0xFF0B0E13), radius = 27f, center = c)
        drawCircle(Color(0xFF8B95A3), radius = 17f, center = c)
        drawCircle(Color(0xFF5B6470), radius = 17f, center = c, style = Stroke(width = 2f))
        for (k in 0 until 5) {
            val a = Math.toRadians(k * 72.0 - 90.0)
            drawLine(
                Color(0xFF5B6470),
                c,
                Offset(c.x + (14 * Math.cos(a)).toFloat(), c.y + (14 * Math.sin(a)).toFloat()),
                strokeWidth = 3f,
            )
        }
        drawCircle(Color(0xFF2A303A), radius = 5f, center = c)
    }
}
