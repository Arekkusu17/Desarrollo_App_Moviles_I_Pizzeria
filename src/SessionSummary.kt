import java.io.File
import java.io.IOException

fun showSessionSummary(orders: Array<Pedido>) {
    if (orders.isEmpty()) return

    val ordersByClient = orders.groupBy { it.cliente }
    val productsByIdentity = orders.flatMap { it.productos }.groupingBy { it }.eachCount()
    val summary = buildString {
        appendLine("========== RESUMEN DE LA SESIÓN ==========")
        appendLine("Clientes registrados: ${ordersByClient.size} | Pedidos confirmados: ${orders.size}")
        for (clientOrders in ordersByClient.values) {
            val latest = clientOrders.last()
            val (name, email, phone) = latest.cliente
            appendLine("  $name | $email | $phone | Pedidos: ${clientOrders.size}")
            if (clientOrders.size < 2) {
                appendLine("  Se requieren dos pedidos del cliente para mostrar la combinación.")
                continue
            }
            val previous = clientOrders[clientOrders.lastIndex - 1]
            try {
                val combined = previous + latest
                val (_, products, total) = combined
                val lines = products.groupingBy { it }.eachCount().entries
                    .joinToString { (product, quantity) -> "${product.nombre} x$quantity" }
                appendLine("  Vista previa #${previous.id} + #${latest.id}: $lines")
                appendLine("  Fecha de cálculo: ${combined.date} | Total combinado: ${money(total)}")
            } catch (error: IllegalArgumentException) {
                appendLine("  No se pudieron combinar los pedidos #${previous.id} y #${latest.id}: ${error.message}")
            }
        }
        appendLine()
        appendLine("Productos distintos por nombre y categoría: ${productsByIdentity.size}")
        for ((product, quantity) in productsByIdentity) {
            val promotion = product.promotion()
            val label = promotion?.let { "${it.percentage} % (${it.reason})" } ?: "sin promoción de producto"
            appendLine("  ${product.nombre} [${product.category}] x$quantity | $label")
        }
        appendLine()
        appendLine("Inspección de clases")
        val domainClasses = listOf(Cliente::class, Producto::class, Pedido::class)
        val soldClasses = productsByIdentity.keys.map { it::class }
        (domainClasses + soldClasses).distinct().forEach { appendLine(inspect(it)) }
    }

    println(summary)
    val output = File("resumen_pizzeria.txt")
    try {
        output.writeText(summary)
        println("Resumen guardado en ${output.absolutePath}")
    } catch (error: IOException) {
        println("No se pudo guardar el resumen: ${error.message}")
    }
}
