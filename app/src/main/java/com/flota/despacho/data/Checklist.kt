package com.flota.despacho.data

/**
 * Inspección diaria de pre-uso CHK-CTR-01 (Control Vial, versión 2.0): gandola (chuto / remolque) y cisterna.
 * La misma lista está en web-oficina/src/checklist.ts: si cambia un punto, cambiarlo en los dos.
 */
enum class Seccion(val titulo: String) {
    CHUTO("Inspección básica del chuto (tractocamión)"),
    CISTERNA("Inspección específica de la cisterna / tanque")
}

data class PuntoChecklist(val codigo: String, val seccion: Seccion, val titulo: String, val detalle: String)

val PUNTOS_CHECKLIST = listOf(
    PuntoChecklist("1.1", Seccion.CHUTO, "Niveles de fluidos", "Aceite de motor, refrigerante, líquido de frenos y dirección."),
    PuntoChecklist("1.2", Seccion.CHUTO, "Frenos y aire", "Presión del tanque de aire, fugas visibles, freno de mano / servicio."),
    PuntoChecklist("1.3", Seccion.CHUTO, "Neumáticos (chuto)", "Presión de aire, labrado aceptable, tuercas ajustadas."),
    PuntoChecklist("1.4", Seccion.CHUTO, "Luces completas", "Altas, bajas, direccionales, freno, reversa y estroboscópicas."),
    PuntoChecklist("1.5", Seccion.CHUTO, "Limpiaparabrisas y vidrios", "Parabrisas sin fisuras, plumas operativas."),
    PuntoChecklist("1.6", Seccion.CHUTO, "Espejos retrovisores", "Ajustados, limpios y sin fisuras (izq / der / ciegos)."),
    PuntoChecklist("1.7", Seccion.CHUTO, "Acople quinta rueda", "Pasador de enganche trabado, grasa adecuada, mangueras de aire."),
    PuntoChecklist("1.8", Seccion.CHUTO, "Cinta de amarre y conexión eléctrica", "Cable siete vías conectado y funcionando."),
    PuntoChecklist("1.9", Seccion.CHUTO, "Equipo de emergencia", "Extintor PQS cargado, triángulos/conos, botiquín y linterna."),
    PuntoChecklist("1.10", Seccion.CHUTO, "Documentación del vehículo", "Carnet de circulación, seguro, revisión técnica al día."),
    PuntoChecklist("2.1", Seccion.CISTERNA, "Estructura del tanque", "Sin abolladuras severas, fisuras, ni fugas de producto."),
    PuntoChecklist("2.2", Seccion.CISTERNA, "Válvulas de descarga y salida", "Válvulas herméticas, sin goteos, tapones colocados."),
    PuntoChecklist("2.3", Seccion.CISTERNA, "Válvulas de alivio / vacío", "Manhole superior cerrado, empaques en buen estado."),
    PuntoChecklist("2.4", Seccion.CISTERNA, "Sistema de puesta a tierra", "Cable de masa, pinza estática y punto de contacto limpio."),
    PuntoChecklist("2.5", Seccion.CISTERNA, "Mangueras y acoples", "Acoples rápidos (Camlock) en buen estado, empacaduras completas."),
    PuntoChecklist("2.6", Seccion.CISTERNA, "Placard HAZMAT / señalización", "Rótulos de peligro (NFPA/UN) según el producto."),
    PuntoChecklist("2.7", Seccion.CISTERNA, "Neumáticos de la cisterna", "Estado de cauchos, frenos de los ejes del remolque."),
    PuntoChecklist("2.8", Seccion.CISTERNA, "Bandeja de contención / derrames", "Kit antiderrame presente (paños absorbentes, arena)."),
)

/** B = bueno / operativo, M = malo / requiere atención, NA = no aplica. */
enum class Resultado(val corto: String, val etiqueta: String) {
    B("B", "Bueno"), M("M", "Malo"), NA("N/A", "No aplica")
}

enum class Gravedad(val etiqueta: String) {
    /** Deja salir: la oficina decide cuándo arreglarla. */
    LEVE("Leve"),
    /** Bloquea la salida de ese chuto o esa cisterna hasta que la oficina la marque resuelta. */
    GRAVE("Grave")
}

enum class Momento(val etiqueta: String) { SALIDA("salida"), LLEGADA("llegada") }

/** Lo que contestó el operador en un punto. [gravedad] y [observacion] solo para M (y la observación se puede dejar en B). */
data class ResultadoPunto(
    val codigo: String,
    /** B, M o NA */
    val resultado: String,
    /** LEVE o GRAVE, solo si es M */
    val gravedad: String? = null,
    val observacion: String = ""
)
