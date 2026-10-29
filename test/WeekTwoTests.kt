import java.math.BigDecimal

private fun client(location: Int = 4) = Cliente("Prueba", "123456789", "prueba@example.com", "Dirección de prueba", location)
private fun pizza(price: String = "1000") = Producto("Prueba", BigDecimal(price), mapOf("Masa" to 1, "Queso" to 2))

private inline fun <reified T : Throwable> expectFailure(action: () -> Unit) {
    try {
        action()
    } catch (error: Throwable) {
        check(error is T) { "Se esperaba ${T::class.simpleName}, se obtuvo $error" }
        return
    }
    error("Se esperaba ${T::class.simpleName}")
}

fun main() {
    val cases: List<Pair<String, () -> Unit>> = listOf(
        "Tres pizzas sin descuento" to {
            val totals = calculateTotals(List(3) { pizza() })
            check(totals.subtotal == BigDecimal("3000"))
            check(totals.discount == BigDecimal.ZERO)
            check(totals.tax == BigDecimal("570") && totals.total == BigDecimal("3570"))
        },
        "Cuatro pizzas con descuento e impuesto posterior" to {
            val totals = calculateTotals(List(4) { pizza() })
            check(totals.discount == BigDecimal("400"))
            check(totals.tax == BigDecimal("684") && totals.total == BigDecimal("4284"))
        },
        "Redondeo al peso" to {
            val totals = calculateTotals(List(5) { pizza("1001") })
            check(totals.discount == BigDecimal("501"))
            check(totals.tax == BigDecimal("856") && totals.total == BigDecimal("5360"))
        },
        "Pedido mixto" to {
            val totals = calculateTotals(listOf(pizza(), pizza("2000")))
            check(totals.total == BigDecimal("3570"))
        },
        "Cálculos rechazan lotes vacíos y tasas inválidas" to {
            expectFailure<IllegalArgumentException> { calculateTotals(emptyList()) }
            expectFailure<IllegalArgumentException> { calculateTotals(listOf(pizza()), BigDecimal("-1")) }
        },
        "Receta para varias pizzas" to {
            check(requiredIngredients(List(3) { pizza() }) == mapOf("Masa" to 3, "Queso" to 6))
        },
        "Consumo exacto" to {
            val inventory = Inventory(mapOf("Masa" to 2, "Queso" to 4))
            check(inventory.consume(requiredIngredients(List(2) { pizza() })))
            check(inventory.quantities() == mapOf("Masa" to 0, "Queso" to 0))
        },
        "Rechazo atómico con faltantes específicos" to {
            val original = mapOf("Masa" to 2, "Queso" to 1)
            val inventory = Inventory(original)
            val needed = requiredIngredients(List(2) { pizza() })
            check(inventory.missingIngredients(needed) == mapOf("Queso" to 3))
            check(!inventory.consume(needed) && inventory.quantities() == original)
        },
        "Ingrediente desconocido se informa como faltante" to {
            val inventory = Inventory(mapOf("Masa" to 1))
            check(!inventory.consume(mapOf("Salsa" to 2)))
            check(inventory.quantities() == mapOf("Masa" to 1))
        },
        "Reabastecimiento y cantidades negativas" to {
            val inventory = Inventory(mapOf("Masa" to 0))
            expectFailure<IllegalArgumentException> { inventory.replenish("Masa", -1) }
            expectFailure<IllegalArgumentException> { Inventory(mapOf("Masa" to -1)) }
            expectFailure<IllegalArgumentException> { inventory.consume(mapOf("Masa" to -1)) }
            inventory.replenish("Masa", 3)
            check(inventory.quantities()["Masa"] == 3)
        },
        "Desbordamiento de stock conserva inventario" to {
            val inventory = Inventory(mapOf("Masa" to Int.MAX_VALUE))
            expectFailure<ArithmeticException> { inventory.replenish("Masa", 1) }
            check(inventory.quantities()["Masa"] == Int.MAX_VALUE)
        },
        "Precios inválidos" to {
            for (price in listOf("0", "-10", "1.5")) {
                expectFailure<IllegalArgumentException> { pizza(price) }
            }
        },
        "Cambio de catálogo conserva precio vendido" to {
            val product = pizza()
            val order = Pedido(1, client(), listOf(product), null)
            product.actualizarPrecio(BigDecimal("2000"))
            check(order.calcularTotal().total == BigDecimal("1190"))
        },
        "Validación de teléfono y correo" to {
            check(validatedPhone(" +56912345678 ") == "+56912345678")
            check(validatedPhone("abc") == "(000) 000-0000")
            check(validatedPhone(null) == "(000) 000-0000")
            check(validatedEmail(" prueba@example.com ") == "prueba@example.com")
            check(validatedEmail("sin@dominio") == "correo@invalido.com")
            check(validatedEmail(null) == "correo@invalido.com")
        },
        "Repartidor más cercano disponible" to {
            val far = Repartidor("Lejos", "Prueba", 1)
            val near = Repartidor("Cerca", "Prueba", 5)
            check(nearestAvailable(listOf(far, near), client()) === near)
            near.reserve()
            check(nearestAvailable(listOf(far, near), client()) === far)
            far.reserve()
            check(nearestAvailable(listOf(far, near), client()) == null)
        },
        "Entrega libera repartidor y actualiza posición" to {
            val driver = Repartidor("Reparto", "Prueba", 1)
            val order = Pedido(1, client(), listOf(pizza()), null)
            order.assignDeliveryPerson(driver)
            check(!driver.available)
            expectFailure<IllegalArgumentException> { driver.entregarPedido(order) }
            check(!driver.available && order.estado == OrderStatus.PREPARING)
            order.cambiarEstado(OrderStatus.READY)
            order.cambiarEstado(OrderStatus.ON_THE_WAY)
            driver.entregarPedido(order)
            check(driver.available && driver.locationKm == 4)
            check(order.estado == OrderStatus.DELIVERED)
            expectFailure<IllegalArgumentException> { driver.entregarPedido(order) }
        },
        "Sin repartidor no puede despacharse" to {
            val order = Pedido(1, client(), listOf(pizza()), null)
            expectFailure<IllegalArgumentException> { order.cambiarEstado(OrderStatus.DELIVERED) }
            order.cambiarEstado(OrderStatus.READY)
            expectFailure<IllegalArgumentException> { order.cambiarEstado(OrderStatus.ON_THE_WAY) }
            check(order.estado == OrderStatus.READY)
        },
        "No asigna dos pedidos al mismo repartidor" to {
            val driver = Repartidor("Reparto", "Prueba", 1)
            Pedido(1, client(), listOf(pizza()), null).assignDeliveryPerson(driver)
            val other = Pedido(2, client(), listOf(pizza()), null)
            expectFailure<IllegalStateException> { other.assignDeliveryPerson(driver) }
            check(other.repartidor == null)
        },
        "Polimorfismo de usuarios" to {
            val users: List<Usuario> = listOf(client(), Repartidor("Reparto", "Prueba", 1))
            check(users[0].describir().startsWith("Cliente:"))
            check(users[1].describir().startsWith("Repartidor:"))
        }
    )
    for ((name, action) in cases) {
        action()
        println("OK: $name")
    }
    println("${cases.size} pruebas de dominio aprobadas.")
}
