import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class WeekTwoTests {
    private static int passed;

    public static void main(String[] args) {
        test("Tres pizzas sin descuento", () -> {
            OrderTotals totals = totals(Collections.nCopies(3, pizza("1000")));
            equalMoney(totals.getSubtotal(), "3000");
            equalMoney(totals.getDiscount(), "0");
            equalMoney(totals.getTax(), "570");
            equalMoney(totals.getTotal(), "3570");
        });
        test("Cuatro pizzas con descuento e impuesto posterior", () -> {
            OrderTotals totals = totals(Collections.nCopies(4, pizza("1000")));
            equalMoney(totals.getDiscount(), "400");
            equalMoney(totals.getTax(), "684");
            equalMoney(totals.getTotal(), "4284");
        });
        test("Redondeo al peso", () -> {
            OrderTotals totals = totals(Collections.nCopies(5, pizza("1001")));
            equalMoney(totals.getDiscount(), "501");
            equalMoney(totals.getTax(), "856");
            equalMoney(totals.getTotal(), "5360");
        });
        test("Pedido mixto", () -> equalMoney(totals(List.of(pizza("1000"), pizza("2000"))).getTotal(), "3570"));
        test("Cálculos rechazan lotes vacíos y tasas inválidas", () -> {
            expectFailure(IllegalArgumentException.class, () -> totals(List.of()));
            expectFailure(IllegalArgumentException.class,
                    () -> ModelsKt.calculateTotals(List.of(pizza("1000")), new BigDecimal("-1")));
        });
        test("Receta para varias pizzas", () -> {
            Map<String, Integer> needed = InventoryKt.requiredIngredients(Collections.nCopies(3, pizza("1000")));
            check(needed.equals(Map.of("Masa", 3, "Queso", 6)), "Receta incorrecta");
        });
        test("Consumo exacto", () -> {
            Inventory inventory = new Inventory(Map.of("Masa", 2, "Queso", 4));
            check(inventory.consume(Map.of("Masa", 2, "Queso", 4)), "El lote debería aceptarse");
            check(inventory.quantities().equals(Map.of("Masa", 0, "Queso", 0)), "Stock incorrecto");
        });
        test("Rechazo atómico con faltantes específicos", () -> {
            Map<String, Integer> original = Map.of("Masa", 2, "Queso", 1);
            Inventory inventory = new Inventory(original);
            Map<String, Integer> needed = Map.of("Masa", 2, "Queso", 4);
            check(inventory.missingIngredients(needed).equals(Map.of("Queso", 3)), "Faltantes incorrectos");
            check(!inventory.consume(needed), "El lote debería rechazarse");
            check(inventory.quantities().equals(original), "Un rechazo debe conservar el stock");
        });
        test("Ingrediente desconocido se informa como faltante", () -> {
            Inventory inventory = new Inventory(Map.of("Masa", 1));
            check(!inventory.consume(Map.of("Salsa", 2)), "El ingrediente no existe");
            check(inventory.quantities().equals(Map.of("Masa", 1)), "El stock debe conservarse");
        });
        test("Reabastecimiento y cantidades negativas", () -> {
            Inventory inventory = new Inventory(Map.of("Masa", 0));
            expectFailure(IllegalArgumentException.class, () -> inventory.replenish("Masa", -1));
            expectFailure(IllegalArgumentException.class, () -> new Inventory(Map.of("Masa", -1)));
            expectFailure(IllegalArgumentException.class, () -> inventory.consume(Map.of("Masa", -1)));
            inventory.replenish("Masa", 3);
            check(inventory.quantities().get("Masa") == 3, "Reabastecimiento incorrecto");
        });
        test("Desbordamiento de stock conserva inventario", () -> {
            Inventory inventory = new Inventory(Map.of("Masa", Integer.MAX_VALUE));
            expectFailure(ArithmeticException.class, () -> inventory.replenish("Masa", 1));
            check(inventory.quantities().get("Masa") == Integer.MAX_VALUE, "El stock debe conservarse");
        });
        test("Precios inválidos", () -> {
            for (String price : List.of("0", "-10", "1.5")) {
                expectFailure(IllegalArgumentException.class, () -> pizza(price));
            }
        });
        test("Cambio de catálogo conserva precio vendido", () -> {
            Producto product = pizza("1000");
            Pedido order = new Pedido(1, client(), List.of(product), null);
            product.actualizarPrecio(new BigDecimal("2000"));
            equalMoney(order.calcularTotal().getTotal(), "1190");
        });
        test("Validación de teléfono y correo", () -> {
            check(ConsoleKt.validatedPhone(" +56912345678 ").equals("+56912345678"), "Teléfono válido rechazado");
            check(ConsoleKt.validatedPhone("abc").equals("(000) 000-0000"), "Falta valor predeterminado");
            check(ConsoleKt.validatedPhone(null).equals("(000) 000-0000"), "Teléfono nulo no controlado");
            check(ConsoleKt.validatedEmail(" prueba@example.com ").equals("prueba@example.com"), "Correo válido rechazado");
            check(ConsoleKt.validatedEmail("sin@dominio").equals("correo@invalido.com"), "Correo inválido aceptado");
            check(ConsoleKt.validatedEmail(null).equals("correo@invalido.com"), "Correo nulo no controlado");
        });
        test("Repartidor más cercano disponible", () -> {
            Repartidor far = new Repartidor("Lejos", "Prueba", 1);
            Repartidor near = new Repartidor("Cerca", "Prueba", 5);
            List<Repartidor> drivers = List.of(far, near);
            check(ModelsKt.nearestAvailable(drivers, client()) == near, "No seleccionó al más cercano");
            near.reserve();
            check(ModelsKt.nearestAvailable(drivers, client()) == far, "Seleccionó un repartidor ocupado");
            far.reserve();
            check(ModelsKt.nearestAvailable(drivers, client()) == null, "No hay repartidores disponibles");
        });
        test("Entrega libera repartidor y actualiza posición", () -> {
            Repartidor driver = new Repartidor("Reparto", "Prueba", 1);
            Pedido order = new Pedido(1, client(), List.of(pizza("1000")), null);
            order.assignDeliveryPerson(driver);
            check(!driver.getAvailable(), "El repartidor debe quedar reservado");
            expectFailure(IllegalArgumentException.class, () -> driver.entregarPedido(order));
            check(!driver.getAvailable() && order.getEstado() == OrderStatus.PREPARING, "Entrega anticipada alteró el estado");
            order.cambiarEstado(OrderStatus.READY);
            order.cambiarEstado(OrderStatus.ON_THE_WAY);
            driver.entregarPedido(order);
            check(driver.getAvailable() && driver.getLocationKm() == 4, "El repartidor no fue liberado o reubicado");
            check(order.getEstado() == OrderStatus.DELIVERED, "El pedido debe estar entregado");
            expectFailure(IllegalArgumentException.class, () -> driver.entregarPedido(order));
        });
        test("Sin repartidor no puede despacharse", () -> {
            Pedido order = new Pedido(1, client(), List.of(pizza("1000")), null);
            expectFailure(IllegalArgumentException.class, () -> order.cambiarEstado(OrderStatus.DELIVERED));
            order.cambiarEstado(OrderStatus.READY);
            expectFailure(IllegalArgumentException.class, () -> order.cambiarEstado(OrderStatus.ON_THE_WAY));
            check(order.getEstado() == OrderStatus.READY, "El pedido debe permanecer listo");
        });
        test("No asigna dos pedidos al mismo repartidor", () -> {
            Repartidor driver = new Repartidor("Reparto", "Prueba", 1);
            new Pedido(1, client(), List.of(pizza("1000")), null).assignDeliveryPerson(driver);
            Pedido other = new Pedido(2, client(), List.of(pizza("1000")), null);
            expectFailure(IllegalStateException.class, () -> other.assignDeliveryPerson(driver));
            check(other.getRepartidor() == null, "El pedido no debe conservar una asignación fallida");
        });
        test("Polimorfismo de usuarios", () -> {
            List<Usuario> users = List.of(client(), new Repartidor("Reparto", "Prueba", 1));
            check(users.get(0).describir().startsWith("Cliente:"), "Descripción de cliente incorrecta");
            check(users.get(1).describir().startsWith("Repartidor:"), "Descripción de repartidor incorrecta");
        });
        System.out.println(passed + " pruebas de dominio en Java aprobadas.");
    }

    private static Cliente client() {
        return new Cliente("Prueba", "123456789", "prueba@example.com", "Dirección de prueba", 4);
    }

    private static Producto pizza(String price) {
        return new Producto("Prueba", new BigDecimal(price), Map.of("Masa", 1, "Queso", 2));
    }

    private static OrderTotals totals(List<Producto> products) {
        return ModelsKt.calculateTotals(products, new BigDecimal("0.19"));
    }

    private static void equalMoney(BigDecimal actual, String expected) {
        check(actual.compareTo(new BigDecimal(expected)) == 0, "Importe esperado: " + expected + "; obtenido: " + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectFailure(Class<? extends Throwable> expected, Runnable action) {
        try {
            action.run();
        } catch (Throwable error) {
            if (expected.isInstance(error)) return;
            throw new AssertionError("Se esperaba " + expected.getSimpleName() + "; se obtuvo " + error, error);
        }
        throw new AssertionError("Se esperaba " + expected.getSimpleName());
    }

    private static void test(String name, Runnable action) {
        action.run();
        passed++;
        System.out.println("OK: " + name);
    }
}
