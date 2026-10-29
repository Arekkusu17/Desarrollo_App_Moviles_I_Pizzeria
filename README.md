# Pizzería en Kotlin

Actividad individual de Desarrollo App Móviles I, Semana 2: «Aplicando utilidades de Kotlin». Aplicación de consola Kotlin/JVM para gestionar pedidos, recetas, inventario y repartidores durante una sesión.

## Requisitos y ejecución

1. Abrir la carpeta `Pizzeria` en IntelliJ IDEA con soporte Kotlin y JDK 21.
2. Esperar que el IDE resuelva la biblioteca Kotlin.
3. Abrir `src/Main.kt` y ejecutar `main`.
4. Utilizar el menú en la consola; ingresar `0` en el menú principal para terminar.

No requiere Gradle, Maven, base de datos, Android Studio ni conexión con servicios externos. Los datos se mantienen en memoria y se reinician al cerrar la aplicación.

## Uso

| Opción | Comportamiento |
| --- | --- |
| 1. Nuevo pedido | Registrar cliente, elegir pizzas y cantidades, terminar la selección con 0 y agregar notas opcionales. Se valida el inventario completo antes de confirmar. |
| 2. Avanzar estado | Elegir el ID de un pedido para pasar al siguiente estado. Al entregar, el repartidor vuelve a estar disponible. |
| 3. Inventario y reabastecimiento | Consultar las existencias de cada ingrediente, seleccionar uno y agregar unidades. Usar 0 para volver sin modificar. |
| 4. Reporte del día | Mostrar pedidos, resúmenes, grupos de completados y pendientes y descripciones de clientes y repartidores. |
| 5. Actualizar precio | Elegir una pizza e ingresar un precio entero positivo. Los pedidos registrados conservan el precio de venta. |
| 0. Salir | Mostrar el reporte y terminar la sesión. |

El fin de la entrada cancela la operación que no se haya confirmado y muestra el reporte de los pedidos aceptados hasta ese momento.

## Recetas e inventario

Las recetas y cantidades son supuestos didácticos: las instrucciones no establecen gramos, recetas ni unidades. Una unidad corresponde a una base de masa o a una porción del ingrediente; dos unidades de queso representan dos porciones.

| Pizza | Precio antes de impuestos | Masa | Salsa | Queso | Pepperoni |
| --- | --- | --- | --- | --- | --- |
| Napolitana | $10000 | 1 | 1 | 2 | 0 |
| Pepperoni | $12000 | 1 | 1 | 2 | 1 |
| Stock inicial | — | 10 | 10 | 20 | 5 |

Para un pedido se suman las necesidades de todas las pizzas y se comparan con el inventario. Si falta cualquier ingrediente, se informa su nombre y la cantidad faltante, se explican los pasos de reabastecimiento y se rechaza el pedido completo sin cobro ni reducción parcial del inventario. El usuario puede reabastecer y registrar nuevamente el pedido.

El arreglo del día contiene los pedidos aceptados; los intentos rechazados o cancelados se informan en consola y no reciben un ID. Los IDs son consecutivos dentro de la sesión. Se permiten varias pizzas del catálogo por pedido, con un máximo didáctico de 1000 unidades para limitar el tamaño de la selección.

## Cálculo de importes

- Los precios se expresan en pesos enteros mediante `BigDecimal`.
- El subtotal suma los precios de todas las pizzas.
- Se descuenta un 10 % al pedir más de tres pizzas, contando todos los tipos del pedido.
- La pauta no fija una tasa de impuestos ni su base. Se adopta un impuesto de simulación del 19 % sobre el subtotal después del descuento, con precios de catálogo antes de impuestos. Es una decisión del ejercicio, no un sistema de facturación.
- Descuento e impuesto se redondean por separado al peso más cercano con `HALF_UP`.
- El total final es subtotal menos descuento más impuesto.

Ejemplo: cuatro Napolitanas dan subtotal de $40000, descuento de $4000, impuesto de $6840 y total de $42840. Consumen 4 masas, 4 porciones de salsa y 8 de queso; el stock restante es 6, 6 y 12, respectivamente.

La tasa se define en `DEFAULT_TAX_RATE` en `src/Models.kt`. Si se cambia, deben actualizarse también las etiquetas de consola, ejemplos y pruebas correspondientes.

## Clientes, estados y repartidores

El teléfono admite entre 9 y 15 dígitos, con `+` inicial opcional. El correo requiere usuario, `@` y dominio con punto, sin espacios. Son validaciones básicas de formato; no comprueban la existencia de los contactos. Los valores inválidos o vacíos se reemplazan por `(000) 000-0000` y `correo@invalido.com`.

Las notas son opcionales y se muestran como «Sin notas» cuando están ausentes. Solo con un correo de formato válido se imprime una notificación claramente identificada como simulada; no se envían mensajes reales.

La cercanía se simula en una ruta lineal entre los km 0 y 100. El cliente ingresa su posición; los tres repartidores comienzan en los km 1, 5 y 9. Se ordenan por distancia absoluta al cliente y se usa `find` para seleccionar al primero disponible. Los empates respetan el orden de la lista. No se utiliza GPS ni se calcula una ruta real.

El repartidor queda reservado al confirmar el pedido y no puede recibir otro hasta terminar la entrega. Si todos están ocupados, el pedido se acepta en preparación, pendiente de asignación. Se vuelve a buscar un repartidor al intentar despacharlo.

El flujo es `En preparación` → `Listo` → `En camino` → `Entregado`. Sin repartidor, el pedido permanece `Listo`. La entrega libera al repartidor y actualiza su posición al km del cliente. `Entregado` se agrega como estado terminal para distinguir completados de pendientes; estos últimos son todos los demás estados.

## Organización y correspondencia con la actividad

| Archivo | Responsabilidad y conceptos |
| --- | --- |
| `src/Main.kt` | Menú, pedidos del día en `Array<Pedido>`, catálogo y repartidores con `listOf`, ingredientes únicos con `Set`, coordinación de altas y estados. |
| `src/Models.kt` | Clases `Producto` y `Pedido`; herencia de `Usuario` en `Cliente` y `Repartidor`; `override`, encapsulación, totales, asignación y entrega. |
| `src/Inventory.kt` | Inventario con `Map`, cálculo de ingredientes para un lote, verificación con `when` y actualización del stock. |
| `src/Console.kt` | Entrada con `try-catch`, validación de strings, operadores `?.` y `?:`, reportes con `for` y `filter` y plantillas de strings. |
| `test/WeekTwoTests.kt` | Pruebas de reglas de negocio en Kotlin, sin dependencias de pruebas externas. |
| `tests/verify_week2.py` | Pruebas funcionales de la consola, mediante biblioteca estándar de Python. |

Se conservan las clases y métodos nombrados explícitamente en la actividad (`detallarProducto`, `calcularTotal`, `cambiarEstado`, `entregarPedido`). Las demás funciones usan nombres en inglés según las reglas del proyecto.

## Verificación

Desde la raíz del proyecto, en macOS con IntelliJ en su ubicación estándar y `java` en el PATH:

```bash
"/Applications/IntelliJ IDEA.app/Contents/plugins/Kotlin/kotlinc/bin/kotlinc" src/*.kt -include-runtime -d /tmp/pizzeria-week2.jar
python3 tests/verify_week2.py /tmp/pizzeria-week2.jar
"/Applications/IntelliJ IDEA.app/Contents/plugins/Kotlin/kotlinc/bin/kotlinc" src/*.kt test/WeekTwoTests.kt -include-runtime -d /tmp/pizzeria-week2-tests.jar
java -cp /tmp/pizzeria-week2-tests.jar WeekTwoTestsKt
```

Las pruebas de dominio también pueden ejecutarse desde `test/WeekTwoTests.kt` en IntelliJ. Python 3 se necesita únicamente para las pruebas de consola. Los JAR se generan en `/tmp`, fuera del repositorio.

Se comprueban descuentos, impuestos y redondeo con valores esperados, recetas mixtas, consumos exactos, rechazo sin modificaciones parciales, reabastecimiento, negativos y desbordamientos, contactos inválidos, precios de venta, estados, disponibilidad de repartidores, herencia y polimorfismo. No se afirma envío real de notificaciones, persistencia ni geolocalización.

## Versiones y entrega

El trabajo de Semana 2 se desarrolla en `feature/week-2-kotlin-utilities`. Flujo de integración: `feature/*` → `develop` → `main`, sin coautoría en los commits.

Semana 1 permanece disponible en la etiqueta `semana-1` y en su ZIP independiente. Sus pruebas originales pertenecen a esa versión y no al nuevo flujo del menú. El ZIP de Semana 2 incluye el proyecto, README y pruebas; excluye `.git`, compilados y archivos temporales. No debe confundirse la rama de trabajo con una versión ya integrada en `main`.
