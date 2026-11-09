import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kotlin.jvm.JvmClassMappingKt;

public class WeekThreeTests {
    private static final LocalDate FIRST_DAY = LocalDate.of(2026, 11, 2);
    private static final LocalDate LAST_DAY = LocalDate.of(2026, 11, 15);
    private static final Map<String, Integer> RECIPE = Map.of("Masa", 1);
    private static final Cliente CLIENT = client("Ana Prueba", "ana@example.com");
    private static int passed;

    public static void main(String[] args) {
        test("La temporada incluye ambos extremos", () -> {
            Producto pizza = product("Pepperoni", "1000");
            equalMoney(order(FIRST_DAY.minusDays(1), pizza).calcularTotal().getSeasonalDiscount(), "0");
            equalMoney(order(FIRST_DAY, pizza).calcularTotal().getSeasonalDiscount(), "100");
            equalMoney(order(LAST_DAY, pizza).calcularTotal().getSeasonalDiscount(), "100");
            equalMoney(order(LAST_DAY.plusDays(1), pizza).calcularTotal().getSeasonalDiscount(), "0");
        });
        test("Las anotaciones de pizza y combo se conservan al registrar el pedido", () -> {
            Producto pizza = new PromotionalPizza("Napolitana", new BigDecimal("10000"), RECIPE);
            Producto combo = new PromotionalCombo("Combo familiar", new BigDecimal("22000"), RECIPE);
            Pedido order = order(FIRST_DAY, pizza, combo);
            equalMoney(order.calcularTotal().getProductDiscount(), "2260");
            equalMoney(order.calcularTotal().getSeasonalDiscount(), "2974");
            equalMoney(order.calcularTotal().getTax(), "5086");
            equalMoney(order.calcularTotal().getTotal(), "31852");
        });
        test("Los descuentos por producto, cantidad y temporada se aplican en orden", () -> {
            Producto pizza = new PromotionalPizza("Napolitana", new BigDecimal("10000"), RECIPE);
            OrderTotals totals = new Pedido(1, CLIENT, Collections.nCopies(4, pizza), null, FIRST_DAY).calcularTotal();
            equalMoney(totals.getProductDiscount(), "2000");
            equalMoney(totals.getDiscount(), "9220");
            equalMoney(totals.getSeasonalDiscount(), "3420");
            equalMoney(totals.getTotal(), "36628");
        });
        test("La identidad elimina duplicados y distingue categorías", () -> {
            Cliente duplicateClient = client(" ana prueba ", "ANA@example.com");
            check(CLIENT.equals(duplicateClient) && CLIENT.hashCode() == duplicateClient.hashCode(),
                    "Los clientes equivalentes deben tener el mismo hash");
            Producto pizza = product("Pepperoni", "1000");
            Producto duplicatePizza = product(" PEPPERONI ", "2000");
            Producto ingredient = new Producto("Pepperoni", new BigDecimal("1000"), RECIPE, ProductCategory.INGREDIENT);
            check(List.of(pizza, duplicatePizza, ingredient).stream().distinct().count() == 2,
                    "La colección debe conservar dos identidades de producto");
            check(Set.of(CLIENT).contains(duplicateClient), "El registro debe reconocer al cliente");
        });
        test("La combinación conserva precios de venta y recalcula con la fecha más reciente", () -> {
            Producto pizza = product("Pepperoni", "1000");
            Pedido first = order(FIRST_DAY.minusDays(1), pizza);
            pizza.actualizarPrecio(new BigDecimal("2000"));
            Pedido second = new Pedido(2, client("ANA PRUEBA", "ANA@EXAMPLE.COM"), List.of(pizza), null, LAST_DAY);
            Pedido combined = first.plus(second);
            check(combined.getDate().equals(LAST_DAY), "Fecha de combinación incorrecta");
            equalMoney(combined.getProductos().get(0).getPrecio(), "1000");
            equalMoney(combined.getProductos().get(1).getPrecio(), "2000");
            equalMoney(combined.calcularTotal().getTotal(), "3213");
            equalMoney(first.calcularTotal().getTotal(), "1190");
            equalMoney(second.calcularTotal().getTotal(), "2142");
        });
        test("La combinación rechaza clientes distintos", () -> {
            Producto pizza = product("Pepperoni", "1000");
            Pedido other = new Pedido(2, client("Otro Cliente", "otro@example.com"), List.of(pizza), null, FIRST_DAY);
            expectInvalid(() -> order(FIRST_DAY, pizza).plus(other));
        });
        test("El límite acumulado permite 100 unidades y rechaza 101", () -> {
            Producto pizza = product("Pepperoni", "1000");
            Pedido full = new Pedido(1, CLIENT, Collections.nCopies(100, pizza), null, FIRST_DAY);
            check(full.getProductos().size() == 100, "Debe aceptar el límite superior");
            expectInvalid(() -> new Pedido(1, CLIENT, Collections.nCopies(101, pizza), null, FIRST_DAY));
            expectInvalid(() -> full.plus(order(FIRST_DAY, product(" PEPPERONI ", "2000"))));
            Producto ingredient = new Producto("Pepperoni", new BigDecimal("1000"), RECIPE, ProductCategory.INGREDIENT);
            check(full.plus(order(FIRST_DAY, ingredient)).getProductos().size() == 101,
                    "El límite debe aplicarse por producto y categoría");
        });
        test("Reflection enumera miembros del dominio", () -> {
            String clientInfo = PromotionsKt.inspect(JvmClassMappingKt.getKotlinClass(Cliente.class));
            String orderInfo = PromotionsKt.inspect(JvmClassMappingKt.getKotlinClass(Pedido.class));
            check(clientInfo.contains("contacto") && clientInfo.contains("component3"), "Faltan miembros de Cliente");
            check(orderInfo.contains("productos") && orderInfo.contains("plus"), "Faltan miembros de Pedido");
        });
        System.out.println(passed + " pruebas de Semana 3 aprobadas.");
    }

    private static Cliente client(String name, String email) {
        return new Cliente(name, "+56 (9) 1234-5678", email, "Dirección ficticia", 4);
    }

    private static Producto product(String name, String price) {
        return new Producto(name, new BigDecimal(price), RECIPE);
    }

    private static Pedido order(LocalDate date, Producto... products) {
        return new Pedido(1, CLIENT, List.of(products), null, date);
    }

    private static void equalMoney(BigDecimal actual, String expected) {
        check(actual.compareTo(new BigDecimal(expected)) == 0, "Importe esperado: " + expected + "; obtenido: " + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectInvalid(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Se esperaba el rechazo del pedido");
    }

    private static void test(String name, Runnable action) {
        action.run();
        passed++;
        System.out.println("OK: " + name);
    }
}
