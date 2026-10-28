# Pizzería en Kotlin

Actividad formativa individual de Desarrollo App Móviles I, Semana 1: «Mi primera app en Kotlin». Programa de consola Kotlin/JVM que registra un cliente y un pedido, calcula descuentos y controla el stock.

## Requisitos y ejecución

- IntelliJ IDEA con soporte Kotlin y JDK 21.
- Abrir la carpeta `Pizzeria` como proyecto en IntelliJ.
- Seleccionar el JDK 21 del proyecto y esperar la resolución de la biblioteca Kotlin.
- Abrir `src/Main.kt` y ejecutar `main` con el botón de ejecución.
- Ingresar los datos solicitados en la consola. Los precios se ingresan en pesos chilenos enteros, sin separadores.

Ejemplo: precio `1000`, stock `10` y cantidad `4` producen subtotal de $4000, descuento de $400, total de $3600 y stock restante de 6 porciones.

## Reglas y decisiones de Semana 1

- Se registra un pedido por ejecución, con un identificador UUID. Los datos se mantienen únicamente durante la ejecución.
- Se conservan los nombres de variables indicados por la actividad para cliente, pizza y pedido. Las funciones auxiliares y otras variables usan inglés.
- Se aplica un 10 % de descuento únicamente al pedir más de tres pizzas.
- Los importes utilizan `BigDecimal`. El descuento se redondea al peso más cercano con `HALF_UP`; por ejemplo, $500,50 se redondea a $501. Esta semana no se agregan impuestos.
- El nombre, dirección y nombre de pizza son obligatorios. Se vuelve a solicitar un campo vacío. El precio y la cantidad deben ser positivos; el stock puede ser cero. Las entradas numéricas incorrectas se solicitan nuevamente.
- El teléfono se conserva como texto, con validación básica de 9 a 15 dígitos y un signo `+` opcional. Si está vacío, es nulo o no cumple el formato, se usa «Sin teléfono válido». No se comprueba su existencia ni un formato nacional específico.
- La pauta no define recetas: se asume una porción de ingredientes por pizza. El inventario se reduce solo si alcanza para todo el pedido.
- Si falta stock, se informa la cantidad faltante, se conserva el inventario y el pedido queda rechazado, sin cobro, descuento aplicado ni repartidor.
- Las instrucciones saltan del paso 4 al 6 y piden un repartidor en el resumen sin definir su asignación. Se usa «Repartidor de turno» para pedidos confirmados; la coordinación avanzada queda para semanas posteriores.
- Se utiliza `readlnOrNull()` para leer la consola; es el equivalente actual de `readLine()`, usado en la guía y marcado como obsoleto por el compilador instalado.
- El fin de la entrada antes de completar el registro cancela el proceso sin confirmar un pedido.

## Verificación funcional

La aplicación usa el sistema de compilación IntelliJ; no requiere Gradle ni Maven. Para verificarla desde una terminal en macOS con IntelliJ instalado en la ubicación estándar:

```bash
"/Applications/IntelliJ IDEA.app/Contents/plugins/Kotlin/kotlinc/bin/kotlinc" src/Main.kt -include-runtime -d /tmp/pizzeria-week1.jar
python3 tests/verify_week1.py /tmp/pizzeria-week1.jar
```

Las pruebas requieren Python 3, usan solamente su biblioteca estándar y ejecutan el JAR mediante `java`. Si IntelliJ está en otra ubicación, ajustar la ruta al compilador. También se puede compilar con `kotlinc` si está disponible en el PATH.

Se verifican nueve casos: tres pizzas sin descuento, cuatro con descuento, stock exacto, stock insuficiente, stock cero, teléfono inválido o vacío, corrección de campos y números inválidos, redondeo del descuento y fin de entrada. Las pruebas no utilizan datos personales reales ni generan archivos en el proyecto.

## Entrega y evolución

Flujo de ramas: `feature/*` → `develop` → `main`. Los commits no incluyen coautoría.

La versión entregable de Semana 1 se identifica con la etiqueta `semana-1`. El ZIP debe contener el proyecto y sus archivos necesarios para abrirlo, sin `.git`, compilados ni archivos temporales. Las siguientes semanas ampliarán el sistema según sus pautas, conservando esta versión inicial.
