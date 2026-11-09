# Pizzería en Kotlin

Actividad sumativa individual de Desarrollo App Móviles I, Semana 3 «Creando soluciones con Kotlin». La aplicación de consola Kotlin/JVM amplía la pizzería de Semana 2 con validaciones, promociones, reflexión y un resumen de los pedidos de la sesión.

## Requisitos y ejecución

1. Abrir la carpeta `Pizzeria` en IntelliJ IDEA con soporte Kotlin y JDK 21.
2. Seleccionar un JDK 21 en la configuración del proyecto si IntelliJ no lo detecta. Esperar a que el IDE resuelva las bibliotecas del proyecto `KotlinJavaRuntime` y `KotlinReflection` desde Maven Central.
3. Abrir `src/Main.kt` y ejecutar `main`.
4. Utilizar el menú en la consola; ingresar `0` en el menú principal para terminar.

## Uso

| Opción | Comportamiento |
| --- | --- |
| 1. Nuevo pedido | Registrar cliente, elegir productos y cantidades, terminar la selección con 0 y agregar notas opcionales. Se valida el inventario completo antes de confirmar. |
| 2. Avanzar estado | Elegir el ID de un pedido para pasar al siguiente estado. Al entregar, el repartidor vuelve a estar disponible. |
| 3. Inventario y reabastecimiento | Consultar las existencias de cada ingrediente, seleccionar uno y agregar unidades. Usar 0 para volver sin modificar. |
| 4. Reporte del día | Mostrar una línea por pedido y los grupos de completados y pendientes, sin repetir resúmenes ni datos de usuarios. |
| 5. Actualizar precio | Elegir un producto e ingresar un precio entero positivo. Los pedidos registrados conservan el precio de venta. |
| 0. Salir | Mostrar el reporte y terminar la sesión. Si hay pedidos confirmados, guardar el resumen en `resumen_pizzeria.txt` en el directorio de ejecución. |

El detalle de venta se muestra al confirmar o avanzar el estado de un pedido. La opción 4 presenta el reporte compacto del día. El resumen al salir agrupa clientes y productos por su igualdad, muestra las promociones aplicables a los productos vendidos e inspecciona las clases del dominio. Si un cliente tiene dos o más pedidos, muestra una vista previa de la combinación de los dos últimos. La vista previa no consume inventario y cada sesión con pedidos confirmados reemplaza el archivo anterior.

El fin de la entrada cancela la operación que no se haya confirmado y muestra el reporte de los pedidos aceptados hasta ese momento.

## Recetas e inventario

Las recetas y cantidades son supuestos didácticos: las instrucciones no establecen gramos, recetas ni unidades. Una unidad corresponde a una base de masa o a una porción del ingrediente; dos unidades de queso representan dos porciones.

| Producto | Precio antes de impuestos | Masa | Salsa | Queso | Pepperoni |
| --- | --- | --- | --- | --- | --- |
| Napolitana | $10000 | 1 | 1 | 2 | 0 |
| Pepperoni | $12000 | 1 | 1 | 2 | 1 |
| Combo familiar (Napolitana y Pepperoni) | $22000 | 2 | 2 | 4 | 1 |
| Stock inicial | — | 10 | 10 | 20 | 5 |

Para un pedido se suman las necesidades de todos los productos y se comparan con el inventario. Si falta cualquier ingrediente, se informa su nombre y la cantidad faltante, se explican los pasos de reabastecimiento y se rechaza el pedido completo sin cobro ni reducción parcial del inventario. El usuario puede reabastecer y registrar nuevamente el pedido.

El arreglo del día contiene los pedidos aceptados. Los intentos rechazados o cancelados se informan en consola. Los IDs son consecutivos dentro de la sesión. La cantidad admitida es de 1 a 100 unidades por producto, sumando las selecciones repetidas del mismo producto.

## Cálculo de importes

- Los precios se expresan en pesos enteros mediante `BigDecimal`.
- El subtotal suma los precios de todos los productos a los valores registrados al momento de la venta.
- Primero se aplica el descuento de `@Promocionable` por producto. Los valores de ejemplo son 5 % en la Napolitana y 8 % en el combo familiar.
- Si hay más de tres unidades de productos de categoría pizza, se descuenta un 10 % del importe restante. El combo cuenta como producto de su propia categoría.
- Entre el 2 y el 15 de noviembre de 2026, ambos inclusive, se descuenta otro 10 % sobre el importe después de los descuentos anteriores. El pedido interactivo usa la fecha local.
- Se aplica el impuesto de simulación del 19 % sobre el importe después de descuentos. Cada descuento y el impuesto se redondean por separado al peso más cercano con `HALF_UP`.

Ejemplo dentro de temporada: una Napolitana y un combo familiar suman $32000. El descuento por producto es $2260 y el de temporada es $2974. El impuesto es $5086 y el total $31852. Se consumen 3 masas, 3 porciones de salsa, 6 de queso y 1 de pepperoni.

Al combinar pedidos con `+`, ambos deben corresponder al mismo cliente. Se conservan los precios de cada pedido, se usa la fecha más reciente y se recalculan descuentos e impuesto con el límite de 100 unidades por producto. El resultado se muestra como vista previa.

## Clientes, estados y repartidores

El correo debe seguir un formato como `nombre@dominio.com`. El teléfono se normaliza como `+XX (XXX) XXX-XXXX` cuando trae diez dígitos nacionales. Para conservar los nueve dígitos de los números chilenos, se admite también el formato `+56 (9) 1234-5678`. Se supone el código `56` para entradas locales de nueve o diez dígitos. Los valores inválidos o vacíos se vuelven a solicitar.

Los clientes se deduplican por nombre y correo, y los productos por nombre y categoría. Las comparaciones ignoran mayúsculas y espacios al inicio o al final. Si un cliente ya registrado vuelve a pedir, el nuevo pedido conserva los datos de entrega ingresados en ese momento. Las notas son opcionales y la notificación por correo se simula en consola.

La cercanía se simula en una ruta lineal entre los km 0 y 100. El cliente ingresa su posición; los tres repartidores comienzan en los km 1, 5 y 9. Se ordenan por distancia absoluta al cliente y se usa `find` para seleccionar al primero disponible. Los empates respetan el orden de la lista. No se utiliza GPS ni se calcula una ruta real.

El repartidor queda reservado al confirmar el pedido y no puede recibir otro hasta terminar la entrega. Si todos están ocupados, el pedido se acepta en preparación, pendiente de asignación. Se vuelve a buscar un repartidor al intentar despacharlo.

El flujo es `En preparación` → `Listo` → `En camino` → `Entregado`. Sin repartidor, el pedido permanece `Listo`. La entrega libera al repartidor y actualiza su posición al km del cliente. `Entregado` se agrega como estado terminal para distinguir completados de pendientes; estos últimos son todos los demás estados.

## Organización y correspondencia con la actividad

| Archivo | Responsabilidad y conceptos |
| --- | --- |
| `src/Main.kt` | Menú, registro de clientes y pedidos, catálogo, inventario y reparto. |
| `src/Models.kt` | Dominio, igualdad, desestructuración, combinación de pedidos y cálculo de importes. |
| `src/Inventory.kt` | Inventario con `Map`, cálculo de ingredientes para un lote, verificación con `when` y actualización del stock. |
| `src/Console.kt` | Validaciones de entrada y reportes de consola. |
| `src/Promotions.kt` | Anotación `@Promocionable`, rango de temporada e inspección mediante `kotlin-reflect`. |
| `src/SessionSummary.kt` | Resumen de los pedidos confirmados y guardado en texto. |
| `test/WeekTwoTests.java` y `test/WeekThreeTests.java` | Pruebas de reglas de negocio en Java. |

Se conservan las clases y métodos nombrados explícitamente en la actividad (`detallarProducto`, `calcularTotal`, `cambiarEstado`, `entregarPedido`). Las demás funciones usan nombres en inglés según las reglas del proyecto.

## Verificación

Desde la raíz del proyecto, en macOS con IntelliJ en su ubicación estándar y JDK 21 (`java` y `javac`) en el PATH:

```bash
compiler_dir="/Applications/IntelliJ IDEA.app/Contents/plugins/Kotlin/kotlinc"
reflect_jar="$compiler_dir/lib/kotlin-reflect.jar"
build_dir=$(mktemp -d /tmp/pizzeria-week3.XXXXXX)
"$compiler_dir/bin/kotlinc" src/*.kt -classpath "$reflect_jar" -include-runtime -d "$build_dir/app.jar"
javac --release 21 -encoding UTF-8 -cp "$build_dir/app.jar:$reflect_jar" -d "$build_dir" test/*.java
java -cp "$build_dir/app.jar:$reflect_jar:$build_dir" WeekTwoTests
java -cp "$build_dir/app.jar:$reflect_jar:$build_dir" WeekThreeTests
(cd "$build_dir" && java -cp "$build_dir/app.jar:$reflect_jar" MainKt)
```

Las dos suites también se pueden ejecutar desde sus métodos `main` en IntelliJ. No requieren un framework de pruebas. Para probar el resumen, ingresa dos pedidos del mismo cliente y selecciona 0. El último comando guarda el resumen de esa sesión en el directorio temporal que crea `mktemp`.

Las pruebas comprueban, entre otros casos, los límites de fecha y cantidad, la acumulación de descuentos, los duplicados, los precios históricos al combinar pedidos y la inspección de clases. También cubren el inventario, el redondeo, los estados y la asignación de repartidores de Semana 2.
