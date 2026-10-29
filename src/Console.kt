import java.math.BigDecimal

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

fun validatedPhone(input: String?): String = input?.trim()
    ?.takeIf { it.matches(Regex("\\+?[0-9]{9,15}")) } ?: "(000) 000-0000"

fun validatedEmail(input: String?): String = input?.trim()
    ?.takeIf { it.matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) } ?: "correo@invalido.com"

fun readClient(): Cliente {
    val name = readText("Nombre del cliente")
    val address = readText("Dirección de entrega")
    print("Teléfono (9 a 15 dígitos; + opcional): ")
    val phone = validatedPhone(readlnOrNull() ?: throw InputEnded())
    print("Correo electrónico: ")
    val email = validatedEmail(readlnOrNull() ?: throw InputEnded())
    if (phone == "(000) 000-0000") println("Teléfono inválido o vacío. Se usará: $phone.")
    if (email == "correo@invalido.com") println("Correo inválido o vacío. Se usará: $email.")
    val locationKm = readInt("Ubicación en la ruta de reparto (km 0 a 100)", 0, 100)
    return Cliente(name, phone, email, address, locationKm)
}

fun showSummary(order: Pedido) {
    val totals = order.calcularTotal()
    println("\n========== PEDIDO #${order.id} ==========")
    println(order.cliente.describir())
    for ((product, copies) in order.productos.groupBy { it.nombre to it.precio }) {
        println("Pizza: ${product.first} | Precio unitario: ${money(product.second)} | Cantidad: ${copies.size}")
    }
    println("Subtotal: ${money(totals.subtotal)}")
    println("Descuento: ${money(totals.discount)}")
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
