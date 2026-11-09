import java.math.BigDecimal

fun main() {
    val catalog = listOf(
        Producto("Napolitana", BigDecimal("10000"), mapOf("Masa" to 1, "Salsa" to 1, "Queso" to 2)),
        Producto("Pepperoni", BigDecimal("12000"), mapOf("Masa" to 1, "Salsa" to 1, "Queso" to 2, "Pepperoni" to 1))
    )
    val inventory = Inventory(mapOf("Masa" to 10, "Salsa" to 10, "Queso" to 20, "Pepperoni" to 5))
    val ingredientNames: Set<String> = catalog.flatMap { it.ingredientes.keys }.toSet()
    require(inventory.quantities().keys.containsAll(ingredientNames)) {
        "Todas las recetas deben usar ingredientes registrados en el inventario."
    }
    val deliveryPeople = listOf(
        Repartidor("Repartidor A", "Contacto de prueba A", 1),
        Repartidor("Repartidor B", "Contacto de prueba B", 5),
        Repartidor("Repartidor C", "Contacto de prueba C", 9)
    )
    var dailyOrders: Array<Pedido> = emptyArray()
    println("PIZZERÍA — SEMANA 2")
    println("Inventario en unidades de ingrediente por receta. Impuesto de simulación: 19 %.")
    try {
        while (true) {
            println("\n1. Nuevo pedido\n2. Avanzar estado de un pedido\n3. Ver inventario y reabastecer")
            println("4. Reporte del día\n5. Actualizar precio de una pizza\n0. Salir")
            when (readInt("Opción", 0, 5)) {
                0 -> break
                1 -> {
                    val order = createOrder(dailyOrders.size + 1, catalog, inventory, deliveryPeople)
                    order?.let { dailyOrders += it }
                }
                2 -> advanceOrder(dailyOrders, deliveryPeople)
                3 -> replenishInventory(inventory)
                4 -> showDailyReport(dailyOrders)
                5 -> updateCatalogPrice(catalog)
            }
        }
    } catch (error: InputEnded) {
        println("\nEntrada finalizada. La operación incompleta no fue confirmada.")
    }
    showDailyReport(dailyOrders)
    println("Sesión finalizada. Los datos de esta simulación se mantienen solo en memoria.")
}

private fun createOrder(
    id: Int,
    catalog: List<Producto>,
    inventory: Inventory,
    deliveryPeople: List<Repartidor>
): Pedido? {
    val client = readClient()
    val products = mutableListOf<Producto>()
    while (true) {
        for ((index, product) in catalog.withIndex()) println("${index + 1}. ${product.detallarProducto()}")
        println("0. Terminar selección")
        val choice = readInt("Pizza", 0, catalog.size)
        if (choice == 0) break
        val remaining = 1000 - products.size
        if (remaining == 0) {
            println("Límite de 1000 pizzas por pedido alcanzado. Termina la selección con 0.")
            continue
        }
        val quantity = readInt("Cantidad de ${catalog[choice - 1].nombre}", 1, remaining)
        repeat(quantity) { products.add(catalog[choice - 1]) }
    }
    if (products.isEmpty()) {
        println("Pedido cancelado: no se seleccionaron pizzas.")
        return null
    }
    print("Notas opcionales (Enter para omitir): ")
    val notes = (readlnOrNull() ?: throw InputEnded()).trim().takeIf { it.isNotEmpty() }
    val requirements = requiredIngredients(products)
    val missing = inventory.missingIngredients(requirements)
    return when {
        missing.isNotEmpty() -> {
            println("Pedido rechazado por ingredientes insuficientes:")
            for ((ingredient, quantity) in missing) println("- $ingredient: faltan $quantity unidad(es).")
            println("Para reabastecer: opción 3, selecciona cada ingrediente y agrega al menos la cantidad faltante.")
            println("Después registra nuevamente el pedido. El inventario no fue modificado y no se generó cobro.")
            null
        }
        else -> {
            val order = Pedido(id, client, products, notes)
            check(inventory.consume(requirements)) { "No se pudo reservar el inventario." }
            assignNearest(order, deliveryPeople)
            println("Pedido #$id confirmado. Ingredientes descontados según la receta.")
            showSummary(order)
            client.correo.takeUnless { it == "correo@invalido.com" }?.let { email ->
                println("[Notificación simulada] Para $email: pedido #$id en preparación.")
            }
            order
        }
    }
}

private fun assignNearest(order: Pedido, deliveryPeople: List<Repartidor>) {
    nearestAvailable(deliveryPeople, order.cliente)?.let { person ->
        order.assignDeliveryPerson(person)
        println("Asignado: ${person.nombre}, a ${person.distanceTo(order.cliente)} km del cliente.")
    } ?: println("Sin repartidores disponibles. El pedido queda pendiente de asignación hasta su despacho.")
}

private fun advanceOrder(orders: Array<Pedido>, deliveryPeople: List<Repartidor>) {
    if (orders.isEmpty()) {
        println("No hay pedidos para actualizar.")
        return
    }
    showDailyReport(orders)
    val id = readInt("ID del pedido", 1, Int.MAX_VALUE)
    val order = orders.find { it.id == id }
    if (order == null) {
        println("No existe un pedido con ese ID.")
        return
    }
    when (order.estado) {
        OrderStatus.PREPARING -> order.cambiarEstado(OrderStatus.READY)
        OrderStatus.READY -> {
            if (order.repartidor == null) assignNearest(order, deliveryPeople)
            if (order.repartidor == null) {
                println("El pedido permanece Listo hasta que haya un repartidor disponible.")
                return
            }
            order.cambiarEstado(OrderStatus.ON_THE_WAY)
        }
        OrderStatus.ON_THE_WAY -> order.repartidor?.entregarPedido(order)
        OrderStatus.DELIVERED -> println("El pedido ya fue entregado.")
    }
    showSummary(order)
}

private fun replenishInventory(inventory: Inventory) {
    val ingredients = inventory.quantities().keys.toList()
    println("Inventario (cada unidad corresponde a la medida definida en la receta):")
    for ((index, ingredient) in ingredients.withIndex()) {
        println("${index + 1}. $ingredient: ${inventory.quantities().getValue(ingredient)} unidad(es)")
    }
    println("0. Volver")
    val choice = readInt("Ingrediente para reabastecer", 0, ingredients.size)
    if (choice == 0) return
    val ingredient = ingredients[choice - 1]
    val quantity = readInt("Unidades a agregar (no negativas y mayores que cero)", 1, Int.MAX_VALUE)
    try {
        inventory.replenish(ingredient, quantity)
        println("Inventario actualizado: $ingredient = ${inventory.quantities().getValue(ingredient)} unidad(es).")
    } catch (error: ArithmeticException) {
        println("La cantidad excede la capacidad del inventario. No se modificó el stock.")
    } catch (error: IllegalArgumentException) {
        println("No se pudo reabastecer: ${error.message}")
    }
}

private fun updateCatalogPrice(catalog: List<Producto>) {
    for ((index, product) in catalog.withIndex()) println("${index + 1}. ${product.detallarProducto()}")
    println("0. Volver")
    val choice = readInt("Pizza a actualizar", 0, catalog.size)
    if (choice == 0) return
    val newPrice = readPrice()
    catalog[choice - 1].actualizarPrecio(newPrice)
    println("Precio actualizado. Los pedidos ya registrados conservan su precio original.")
}
