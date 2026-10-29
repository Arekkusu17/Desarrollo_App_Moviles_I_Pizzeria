"""Pruebas de consola: python3 tests/verify_week2.py /tmp/pizzeria-week2.jar."""
import subprocess
import sys
import unittest

JAR = sys.argv[1]
sys.argv = sys.argv[:1]


def run_app(lines):
    return subprocess.run(
        ["java", "-jar", JAR], input="\n".join(lines) + "\n", text=True,
        capture_output=True, check=True, timeout=10,
    ).stdout


def new_order(quantity="1", phone="123456789", email="prueba@example.com", notes="", location="4"):
    return ["1", "Cliente de prueba", "Dirección de prueba", phone, email, location,
            "1", quantity, "0", notes]


class WeekTwoConsoleTests(unittest.TestCase):
    def test_discount_tax_recipe_and_nearest_driver(self):
        output = run_app(new_order("4") + ["3", "0", "0"])
        for expected in ["Subtotal: $40000 CLP", "Descuento: $4000 CLP", "Total final: $42840 CLP",
                         "19 % sobre subtotal con descuento): $6840 CLP", "Repartidor: Repartidor B",
                         "Masa: 6 unidad(es)", "Queso: 12 unidad(es)", "Notas: Sin notas"]:
            self.assertIn(expected, output)

    def test_invalid_contacts_use_defaults_without_notification(self):
        output = run_app(new_order(phone="abc", email="no-correo") + ["0"])
        self.assertIn("(000) 000-0000", output)
        self.assertIn("correo@invalido.com", output)
        self.assertNotIn("[Notificación simulada]", output)

    def test_mixed_products(self):
        lines = new_order()[:-2] + ["2", "2", "0", "Sin aceitunas", "0"]
        output = run_app(lines)
        self.assertIn("Pizza: Napolitana | Precio unitario: $10000 CLP | Cantidad: 1", output)
        self.assertIn("Pizza: Pepperoni | Precio unitario: $12000 CLP | Cantidad: 2", output)
        self.assertIn("Total final: $40460 CLP", output)
        self.assertIn("Notas: Sin aceitunas", output)

    def test_shortage_rejects_whole_order_and_explains_replenishment(self):
        output = run_app(new_order("11") + ["3", "0", "0"])
        for expected in ["Pedido rechazado", "Masa: faltan 1", "Salsa: faltan 1", "Queso: faltan 2",
                         "Para reabastecer: opción 3", "Masa: 10 unidad(es)", "Queso: 20 unidad(es)",
                         "Todavía no hay pedidos aceptados"]:
            self.assertIn(expected, output)
        self.assertNotIn("confirmado", output)

    def test_replenishment_and_invalid_quantity(self):
        output = run_app(["3", "1", "-5", "abc", "3", "3", "0", "0"])
        self.assertIn("Dato inválido", output)
        self.assertIn("Masa = 13 unidad(es)", output)

    def test_price_validation_and_snapshot(self):
        output = run_app(new_order() + ["5", "1", "abc", "-1", "0", "1.5", "20000", "4", "0"])
        self.assertIn("Precio inválido", output)
        self.assertIn("Precio actualizado", output)
        self.assertIn("Total final: $11900 CLP", output)
        self.assertNotIn("Total final: $23800 CLP", output)

    def test_delivered_and_pending_groups(self):
        output = run_app(new_order() + new_order() + ["2", "1", "2", "1", "2", "1", "4", "0"])
        self.assertIn("Estado: Listo", output)
        self.assertIn("Estado: En camino", output)
        self.assertIn("Estado: Entregado", output)
        self.assertIn("Completados: 1 | IDs: 1", output)
        self.assertIn("Pendientes: 1 | IDs: 2", output)
        self.assertIn("Repartidor B | Contacto de prueba B | km 4 | Disponible", output)

    def test_all_drivers_busy_wait_then_reassign_after_delivery(self):
        lines = new_order() * 4 + ["2", "4", "2", "4", "2", "1", "2", "1", "2", "1", "2", "4", "0"]
        output = run_app(lines)
        self.assertIn("Sin repartidores disponibles", output)
        self.assertIn("El pedido permanece Listo", output)
        self.assertIn("Asignado: Repartidor B, a 0 km", output)
        self.assertIn("Estado: En camino", output)

    def test_eof_cancels_incomplete_order(self):
        output = run_app(["1", "Cliente"])
        self.assertIn("La operación incompleta no fue confirmada", output)
        self.assertNotIn("Pedido #1 confirmado", output)

    def test_empty_cart_and_unknown_id(self):
        output = run_app(new_order()[:6] + ["0"] + new_order() + ["2", "999", "0"])
        self.assertIn("Pedido cancelado", output)
        self.assertIn("No existe un pedido con ese ID", output)


if __name__ == "__main__":
    unittest.main()
