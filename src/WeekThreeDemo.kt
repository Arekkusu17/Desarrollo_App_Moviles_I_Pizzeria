import java.io.File
import java.io.IOException
import java.math.BigDecimal

fun showWeekThreeDemo(catalog: List<Producto>) {
    val summary = buildWeekThreeSummary(catalog)
    println(summary)
    val output = File("resumen_pizzeria.txt")
    try {
        output.writeText(summary)
        println("Resumen guardado en ${output.absolutePath}")
    } catch (error: IOException) {
        println("No se pudo guardar el resumen: ${error.message}")
    }
}

private fun buildWeekThreeSummary(catalog: List<Producto>): String {
    val pizza = catalog.first { it.category == ProductCategory.PIZZA && it.promotion() != null }
    val combo = catalog.first { it.category == ProductCategory.COMBO && it.promotion() != null }
    val client = Cliente("Ana Ejemplo", "+56 (9) 1234-5678", "ana@ejemplo.com", "Dirección de prueba", 4)
    val duplicateClient = Cliente(" ana ejemplo ", "+56 (9) 8765-4321", "ANA@EJEMPLO.COM", "Otra dirección", 5)
    val duplicatePizza = Producto(" ${pizza.nombre.uppercase()} ", pizza.precio + BigDecimal.ONE, pizza.ingredientes)
    val sameNameIngredient = Producto(pizza.nombre, pizza.precio, pizza.ingredientes, ProductCategory.INGREDIENT)
    val first = Pedido(1, client, listOf(pizza, pizza), null, SEASONAL_PROMOTION_DATES.start.minusDays(1))
    val second = Pedido(2, duplicateClient, listOf(pizza, combo), null, SEASONAL_PROMOTION_DATES.start)
    val combined = first + second
    val (orderClient, products, total) = combined
    val (name, email, phone) = orderClient
    val productLines = products.groupingBy { it }.eachCount().entries
        .joinToString { (product, quantity) -> "${product.nombre} x$quantity" }

    return buildString {
        appendLine("========== DEMOSTRACIÓN PIZZERÍA SEMANA 3 ==========")
        appendLine("Validaciones")
        appendLine("  ana@ejemplo: ${if (validatedEmail("ana@ejemplo") == null) "correo inválido" else "correo válido"}")
        appendLine("  Teléfono 222-555-0147 → ${validatedPhone("222-555-0147")}")
        appendLine("  100 unidades válidas: ${100 in ORDER_QUANTITY_RANGE} | 101 unidades válidas: ${101 in ORDER_QUANTITY_RANGE}")
        appendLine()
        appendLine("Pedidos de ejemplo combinados con +")
        appendLine("  Temporada: ${SEASONAL_PROMOTION_DATES.start} al ${SEASONAL_PROMOTION_DATES.endInclusive}")
        appendLine("  Pedido A (${first.date}): ${money(first.calcularTotal().total)} | fuera de temporada")
        appendLine("  Pedido B (${second.date}): ${money(second.calcularTotal().total)} | en temporada")
        appendLine("  Pedido combinado (${combined.date}): $productLines")
        appendLine("  Descuento total: ${money(combined.calcularTotal().discount)} | Total recalculado: ${money(total)}")
        appendLine()
        appendLine("Cliente desestructurado")
        appendLine("  Nombre: $name | Correo: $email | Teléfono: $phone")
        appendLine()
        appendLine("Productos promocionables detectados con reflection")
        for (product in catalog) {
            val promotion = product.promotion() ?: continue
            appendLine("  ${product.nombre}: ${promotion.percentage} % (${promotion.reason})")
        }
        appendLine()
        appendLine("Igualdad y duplicados")
        appendLine("  Clientes con mismo nombre y correo: ${client == duplicateClient}; registrados: ${setOf(client, duplicateClient).size}")
        appendLine("  Productos con mismo nombre y categoría: ${pizza == duplicatePizza}; registrados: ${setOf(pizza, duplicatePizza).size}")
        appendLine("  Productos con el mismo nombre en categorías distintas: ${pizza == sameNameIngredient}")
        appendLine()
        appendLine("Inspección de clases")
        listOf(Cliente::class, Producto::class, Pedido::class, pizza::class, combo::class)
            .forEach { appendLine(inspect(it)) }
    }
}
