import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

private val DISCOUNT_RATE = BigDecimal("0.10")
private val DEFAULT_TAX_RATE = BigDecimal("0.19")
private const val DISCOUNT_THRESHOLD = 3

fun money(value: BigDecimal): String = "\$${value.toPlainString()} CLP"

open class Usuario(val nombre: String, val contacto: String) {
    init {
        require(nombre.isNotBlank()) { "El nombre es obligatorio." }
        require(contacto.isNotBlank()) { "El contacto es obligatorio." }
    }

    open fun describir(): String = "$nombre | $contacto"
}

class Cliente(
    nombre: String,
    contacto: String,
    val correo: String,
    val direccion: String,
    val locationKm: Int
) : Usuario(nombre, contacto) {
    init {
        require(direccion.isNotBlank()) { "La dirección es obligatoria." }
        require(locationKm in 0..100) { "La ubicación debe estar entre 0 y 100 km." }
    }

    override fun describir(): String = "Cliente: ${super.describir()} | $correo | $direccion"
}

class Repartidor(nombre: String, contacto: String, locationKm: Int) : Usuario(nombre, contacto) {
    var locationKm: Int = locationKm
        private set
    var available: Boolean = true
        private set

    init {
        require(locationKm in 0..100) { "La ubicación debe estar entre 0 y 100 km." }
    }

    fun distanceTo(client: Cliente): Int = abs(locationKm - client.locationKm)

    fun reserve() {
        check(available) { "El repartidor ya tiene un pedido asignado." }
        available = false
    }

    fun entregarPedido(order: Pedido) {
        require(order.repartidor === this) { "Este pedido pertenece a otro repartidor." }
        order.cambiarEstado(OrderStatus.DELIVERED)
        locationKm = order.cliente.locationKm
        available = true
    }

    override fun describir(): String = "Repartidor: ${super.describir()} | km $locationKm | " +
        if (available) "Disponible" else "Ocupado"
}

class Producto(val nombre: String, precio: BigDecimal, ingredientes: Map<String, Int>) {
    var precio: BigDecimal = precio
        private set
    val ingredientes: Map<String, Int> = ingredientes.toMap()

    init {
        require(nombre.isNotBlank()) { "El nombre de la pizza es obligatorio." }
        validatePrice(precio)
        require(this.ingredientes.isNotEmpty() && this.ingredientes.values.all { it > 0 }) {
            "La receta debe tener ingredientes con cantidades positivas."
        }
    }

    fun actualizarPrecio(newPrice: BigDecimal) {
        validatePrice(newPrice)
        precio = newPrice
    }

    fun detallarProducto(): String = "$nombre | ${money(precio)} | " +
        ingredientes.entries.joinToString { "${it.key}: ${it.value} unidad(es)" }

    // Conserva el precio de la venta aunque luego cambie el catálogo.
    fun snapshot(): Producto = Producto(nombre, precio, ingredientes)

    private fun validatePrice(value: BigDecimal) {
        require(value > BigDecimal.ZERO && value.stripTrailingZeros().scale() <= 0) {
            "El precio debe ser un número entero de pesos mayor que cero."
        }
    }
}

enum class OrderStatus(val label: String) {
    PREPARING("En preparación"), READY("Listo"), ON_THE_WAY("En camino"), DELIVERED("Entregado")
}

data class OrderTotals(
    val subtotal: BigDecimal,
    val discount: BigDecimal,
    val tax: BigDecimal,
    val total: BigDecimal
)

fun calculateTotals(products: List<Producto>, taxRate: BigDecimal = DEFAULT_TAX_RATE): OrderTotals {
    require(products.isNotEmpty()) { "El pedido debe contener al menos una pizza." }
    require(taxRate >= BigDecimal.ZERO && taxRate <= BigDecimal.ONE) { "Tasa de impuesto inválida." }
    val subtotal = products.fold(BigDecimal.ZERO) { sum, product -> sum + product.precio }
    val discount = if (products.size > DISCOUNT_THRESHOLD) {
        (subtotal * DISCOUNT_RATE).setScale(0, RoundingMode.HALF_UP)
    } else {
        BigDecimal.ZERO
    }
    val taxableAmount = subtotal - discount
    val tax = (taxableAmount * taxRate).setScale(0, RoundingMode.HALF_UP)
    return OrderTotals(subtotal, discount, tax, taxableAmount + tax)
}

class Pedido(val id: Int, val cliente: Cliente, productos: List<Producto>, val notas: String?) {
    val productos: List<Producto> = productos.map { it.snapshot() }
    var estado: OrderStatus = OrderStatus.PREPARING
        private set
    var repartidor: Repartidor? = null
        private set

    init {
        require(id > 0) { "El identificador debe ser positivo." }
        require(this.productos.isNotEmpty()) { "El pedido debe contener pizzas." }
    }

    fun calcularTotal(): OrderTotals = calculateTotals(productos)

    fun assignDeliveryPerson(deliveryPerson: Repartidor) {
        check(repartidor == null) { "El pedido ya tiene un repartidor." }
        check(estado == OrderStatus.PREPARING || estado == OrderStatus.READY) {
            "No se puede asignar un repartidor en este estado."
        }
        deliveryPerson.reserve()
        repartidor = deliveryPerson
    }

    fun cambiarEstado(next: OrderStatus) {
        val allowed = when (estado) {
            OrderStatus.PREPARING -> next == OrderStatus.READY
            OrderStatus.READY -> next == OrderStatus.ON_THE_WAY && repartidor != null
            OrderStatus.ON_THE_WAY -> next == OrderStatus.DELIVERED
            OrderStatus.DELIVERED -> false
        }
        require(allowed) { "Cambio de estado no permitido: ${estado.label} → ${next.label}." }
        estado = next
    }
}

fun nearestAvailable(deliveryPeople: List<Repartidor>, client: Cliente): Repartidor? =
    deliveryPeople.sortedBy { it.distanceTo(client) }.find { it.available }
