class Inventory(initialStock: Map<String, Int>) {
    private val stock: MutableMap<String, Int> = initialStock.toMutableMap()

    init {
        require(stock.isNotEmpty() && stock.keys.all { it.isNotBlank() } && stock.values.all { it >= 0 }) {
            "El inventario debe tener ingredientes válidos y cantidades no negativas."
        }
    }

    fun quantities(): Map<String, Int> = stock.toMap()

    fun replenish(ingredient: String, quantity: Int) {
        require(ingredient in stock) { "Ingrediente desconocido." }
        require(quantity > 0) { "La cantidad a reabastecer debe ser positiva." }
        val updated = Math.addExact(stock.getValue(ingredient), quantity)
        stock[ingredient] = updated
    }

    fun missingIngredients(requirements: Map<String, Int>): Map<String, Int> {
        require(requirements.isNotEmpty() && requirements.values.all { it > 0 }) {
            "Las cantidades de la receta deben ser positivas."
        }
        return requirements.mapValues { (ingredient, needed) -> needed - (stock[ingredient] ?: 0) }
            .filterValues { it > 0 }
    }

    // Revisa el lote completo antes de descontar: un rechazo conserva todo el inventario.
    fun consume(requirements: Map<String, Int>): Boolean = when {
        missingIngredients(requirements).isNotEmpty() -> false
        else -> {
            for ((ingredient, needed) in requirements) {
                stock[ingredient] = stock.getValue(ingredient) - needed
            }
            true
        }
    }
}

fun requiredIngredients(products: List<Producto>): Map<String, Int> {
    require(products.isNotEmpty()) { "El lote debe contener al menos una pizza." }
    val requirements = mutableMapOf<String, Int>()
    for (product in products) {
        for ((ingredient, quantity) in product.ingredientes) {
            requirements[ingredient] = Math.addExact(requirements[ingredient] ?: 0, quantity)
        }
    }
    return requirements.toMap()
}
