import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

private val DISCOUNT_RATE = BigDecimal("0.10")
private const val DISCOUNT_THRESHOLD = 3
private const val DEFAULT_PHONE = "Sin teléfono válido"
private const val DEFAULT_DELIVERY_PERSON = "Repartidor de turno"

fun main() {
    println("PIZZERÍA — REGISTRO DE PEDIDO")

    val nombre: String = readRequiredText("Nombre del cliente") ?: return
    val direccion: String = readRequiredText("Dirección de entrega") ?: return
    print("Teléfono (entre 9 y 15 dígitos, con + opcional): ")
    val phoneInput: String = readlnOrNull()?.trim().orEmpty()
    val telefono: String = if (phoneInput.matches(Regex("\\+?[0-9]{9,15}"))) {
        phoneInput
    } else {
        println("Teléfono nulo o incorrecto. Se usará: $DEFAULT_PHONE.")
        DEFAULT_PHONE
    }
    println("Cliente registrado: $nombre | $direccion | $telefono")

    val nombrePizza: String = readRequiredText("Nombre de la pizza") ?: return
    val precioUnitario: BigDecimal = readPrice() ?: return
    var ingredientesDisponibles: Int = readQuantity("Porciones de ingredientes disponibles", 0) ?: return
    val cantidadPizzas: Int = readQuantity("Cantidad de pizzas", 1) ?: return
    val idPedido: String = UUID.randomUUID().toString()
    var totalPedido: BigDecimal = BigDecimal.ZERO
    var estadoPedido: String = "Pendiente"

    val subtotal: BigDecimal = precioUnitario.multiply(cantidadPizzas.toBigDecimal())
    val discount: BigDecimal = if (cantidadPizzas > DISCOUNT_THRESHOLD) {
        subtotal.multiply(DISCOUNT_RATE).setScale(0, RoundingMode.HALF_UP)
    } else {
        BigDecimal.ZERO
    }

    // Para esta actividad, cada pizza consume una porción de ingredientes.
    val confirmed: Boolean = if (ingredientesDisponibles >= cantidadPizzas) {
        ingredientesDisponibles -= cantidadPizzas
        totalPedido = subtotal.subtract(discount)
        estadoPedido = "Preparación"
        println("Pedido confirmado. Stock restante: $ingredientesDisponibles porciones.")
        true
    } else {
        estadoPedido = "Rechazado por falta de ingredientes"
        println("El pedido no puede ser completado. Faltan ${cantidadPizzas - ingredientesDisponibles} porciones.")
        false
    }
    val deliveryPerson: String = if (confirmed) DEFAULT_DELIVERY_PERSON else "Sin asignar"

    println("\n========== RESUMEN DEL PEDIDO ==========")
    println("Pedido: $idPedido")
    println("Cliente: $nombre")
    println("Dirección: $direccion")
    println("Teléfono: $telefono")
    println("Pizza: $nombrePizza")
    println("Precio unitario: \$${precioUnitario.toPlainString()} CLP")
    println("Cantidad: $cantidadPizzas")
    println("Subtotal solicitado: \$${subtotal.toPlainString()} CLP")
    println("Descuento aplicado: \$${if (confirmed) discount.toPlainString() else "0"} CLP")
    println("Total a pagar: \$${totalPedido.toPlainString()} CLP")
    println("Estado: $estadoPedido")
    println("Repartidor: $deliveryPerson")
    println("Stock restante: $ingredientesDisponibles porciones")
}

private fun readRequiredText(prompt: String): String? {
    while (true) {
        print("$prompt: ")
        val input = readlnOrNull()
        if (input == null) {
            println("Entrada finalizada. Registro cancelado sin confirmar un pedido.")
            return null
        }
        val value = input.trim()
        if (value.isNotEmpty()) return value
        println("Este campo es obligatorio. Inténtalo nuevamente.")
    }
}

private fun readQuantity(prompt: String, minimum: Int): Int? {
    while (true) {
        val input = readRequiredText(prompt) ?: return null
        val value = input.toIntOrNull()
        if (value != null && value >= minimum) return value
        println("Ingresa un número entero mayor o igual a $minimum.")
    }
}

private fun readPrice(): BigDecimal? {
    while (true) {
        val input = readRequiredText("Precio unitario en pesos chilenos enteros") ?: return null
        if (input.matches(Regex("[0-9]+"))) {
            val price = input.toBigDecimalOrNull()
            if (price != null && price > BigDecimal.ZERO) return price.setScale(0)
        }
        println("Ingresa un precio entero mayor que cero, sin puntos ni comas.")
    }
}
