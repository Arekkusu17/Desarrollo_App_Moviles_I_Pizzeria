import java.math.BigDecimal

private val EMAIL_PATTERN = Regex("[A-Za-z0-9_%+-]+(?:\\.[A-Za-z0-9_%+-]+)*@" +
    "(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z]{2,}")
private val PHONE_INPUT_PATTERN = Regex("\\+?[0-9 ()-]+")
private val PHONE_SEPARATORS = Regex("[ ()-]")
private val STANDARD_PHONE_PATTERN = Regex("([0-9]{2})([0-9]{3})([0-9]{3})([0-9]{4})")
private val CHILEAN_PHONE_PATTERN = Regex("(56)([0-9])([0-9]{4})([0-9]{4})")

class InputEnded : RuntimeException()

fun readText(prompt: String): String {
    while (true) {
        print("$prompt: ")
        val value = readlnOrNull()?.trim() ?: throw InputEnded()
        if (value.isNotEmpty()) return value
        println("Este campo es obligatorio.")
    }
}

fun readInt(prompt: String, minimum: Int, maximum: Int): Int {
    while (true) {
        try {
            val value = readText(prompt).toInt()
            require(value in minimum..maximum)
            return value
        } catch (error: IllegalArgumentException) {
            println("Dato inválido. Ingresa un entero entre $minimum y $maximum.")
        }
    }
}

fun readPrice(): BigDecimal {
    while (true) {
        try {
            val input = readText("Nuevo precio en pesos enteros, sin separadores")
            require(input.matches(Regex("[0-9]+")))
            val value = input.toBigDecimal()
            require(value > BigDecimal.ZERO)
            return value.setScale(0)
        } catch (error: IllegalArgumentException) {
            println("Precio inválido. Ingresa un entero mayor que cero.")
        }
    }
}

fun validatedPhone(input: String?): String? {
    val text = input?.trim()?.takeIf(PHONE_INPUT_PATTERN::matches) ?: return null
    val compact = text.replace(PHONE_SEPARATORS, "")
    val digits = when {
        compact.startsWith("+") -> compact.drop(1)
        compact.length in 9..10 -> "56$compact"
        else -> compact
    }
    val match = STANDARD_PHONE_PATTERN.matchEntire(digits)
        ?: CHILEAN_PHONE_PATTERN.matchEntire(digits)
        ?: return null
    val (country, area, first, last) = match.destructured
    return "+$country ($area) $first-$last"
}

fun validatedEmail(input: String?): String? = input?.trim()?.takeIf(EMAIL_PATTERN::matches)

fun readClient(): Cliente {
    val name = readText("Nombre del cliente")
    val address = readText("Dirección de entrega")
    val phone = readPhone()
    val email = readEmail()
    val locationKm = readInt("Ubicación en la ruta de reparto (km 0 a 100)", 0, 100)
    return Cliente(name, phone, email, address, locationKm)
}

private fun readPhone(): String {
    while (true) {
        val phone = validatedPhone(readText("Teléfono"))
        if (phone != null) return phone
        println("Teléfono inválido. Usa, por ejemplo, +56 (9) 1234-5678 o +56 (222) 555-0147.")
    }
}

private fun readEmail(): String {
    while (true) {
        val email = validatedEmail(readText("Correo electrónico"))
        if (email != null) return email
        println("Correo inválido. Usa el formato nombre@dominio.com.")
    }
}

fun showSummary(order: Pedido) {
    val totals = order.calcularTotal()
    println("\n========== PEDIDO #${order.id} ==========")
    println(order.cliente.describir())
    for ((product, copies) in order.productos.groupBy { it.nombre to it.precio }) {
        println("Producto: ${product.first} | Precio unitario: ${money(product.second)} | Cantidad: ${copies.size}")
    }
    println("Subtotal: ${money(totals.subtotal)}")
    println("Descuento por producto: ${money(totals.productDiscount)}")
    println("Descuento por cantidad: ${money(totals.discount - totals.productDiscount - totals.seasonalDiscount)}")
    println("Descuento de temporada: ${money(totals.seasonalDiscount)}")
    println("Impuesto (19 % sobre subtotal con descuento): ${money(totals.tax)}")
    println("Total final: ${money(totals.total)}")
    println("Estado: ${order.estado.label}")
    println("Repartidor: ${order.repartidor?.nombre ?: "Pendiente de asignación"}")
    println("Notas: ${order.notas?.takeIf { it.isNotBlank() } ?: "Sin notas"}")
}

fun showDailyReport(orders: Array<Pedido>) {
    println("\n========== REPORTE DEL DÍA ==========")
    if (orders.isEmpty()) println("Todavía no hay pedidos aceptados.")
    for (order in orders) {
        val products = order.productos.groupingBy { it.nombre }.eachCount()
            .entries.joinToString { "${it.key} x${it.value}" }
        println("#${order.id} | ${order.cliente.nombre} | $products | " +
            "${money(order.calcularTotal().total)} | ${order.estado.label}")
    }
    val completed = orders.filter { it.estado == OrderStatus.DELIVERED }
    val pending = orders.filter { it.estado != OrderStatus.DELIVERED }
    println("Completados: ${completed.size} | IDs: ${completed.joinToString { it.id.toString() }.ifEmpty { "ninguno" }}")
    println("Pendientes: ${pending.size} | IDs: ${pending.joinToString { it.id.toString() }.ifEmpty { "ninguno" }}")
}
