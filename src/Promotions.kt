import java.math.BigDecimal
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

val SEASONAL_PROMOTION_DATES: ClosedRange<LocalDate> =
    LocalDate.of(2026, 11, 2)..LocalDate.of(2026, 11, 15)
val SEASONAL_DISCOUNT_RATE = BigDecimal("0.10")

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Promocionable(val percentage: Int, val reason: String)

@Promocionable(percentage = 5, reason = "Selección de la casa")
class PromotionalPizza(nombre: String, precio: BigDecimal, ingredientes: Map<String, Int>) :
    Producto(nombre, precio, ingredientes, ProductCategory.PIZZA) {
    override fun snapshot(): Producto = PromotionalPizza(nombre, precio, ingredientes)
}

@Promocionable(percentage = 8, reason = "Combo familiar")
class PromotionalCombo(nombre: String, precio: BigDecimal, ingredientes: Map<String, Int>) :
    Producto(nombre, precio, ingredientes, ProductCategory.COMBO) {
    override fun snapshot(): Producto = PromotionalCombo(nombre, precio, ingredientes)
}

fun Producto.promotion(): Promocionable? = this::class.findAnnotation<Promocionable>()

fun inspect(type: KClass<*>): String = buildString {
    appendLine("Clase ${type.simpleName}")
    appendLine("  Propiedades: ${type.memberProperties.map { it.name }.sorted().joinToString()}")
    append("  Métodos: ${type.memberFunctions.map { it.name }.sorted().joinToString()}")
}
