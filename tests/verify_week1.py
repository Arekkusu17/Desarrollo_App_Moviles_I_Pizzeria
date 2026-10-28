"""Pruebas funcionales de consola. Uso: python3 tests/verify_week1.py ruta/al/programa.jar"""
import subprocess
import sys
import unittest

JAR = sys.argv[1]
sys.argv = sys.argv[:1]


def run_app(lines):
    result = subprocess.run(
        ["java", "-jar", JAR], input="\n".join(lines) + "\n",
        text=True, capture_output=True, timeout=10, check=True,
    )
    return result.stdout


def order(price="1000", stock="10", quantity="3", phone="+56912345678"):
    return run_app(["Cliente de prueba", "Dirección de prueba", phone, "Pepperoni", price, stock, quantity])


class WeekOneTests(unittest.TestCase):
    def test_three_pizzas_have_no_discount(self):
        output = order()
        for expected in ["Total a pagar: $3000 CLP", "Descuento aplicado: $0 CLP",
                         "Stock restante: 7 porciones", "Estado: Preparación",
                         "Repartidor: Repartidor de turno", "Pizza: Pepperoni",
                         "Cliente: Cliente de prueba", "Teléfono: +56912345678"]:
            self.assertIn(expected, output)

    def test_four_pizzas_receive_ten_percent_discount(self):
        output = order(quantity="4")
        self.assertIn("Descuento aplicado: $400 CLP", output)
        self.assertIn("Total a pagar: $3600 CLP", output)
        self.assertIn("Stock restante: 6 porciones", output)

    def test_exact_stock_confirms_order(self):
        output = order(stock="3")
        self.assertIn("Pedido confirmado", output)
        self.assertIn("Stock restante: 0 porciones", output)

    def test_insufficient_stock_preserves_inventory_and_does_not_charge(self):
        output = order(stock="2", quantity="4")
        for expected in ["Faltan 2 porciones", "Stock restante: 2 porciones",
                         "Estado: Rechazado", "Total a pagar: $0 CLP",
                         "Descuento aplicado: $0 CLP", "Repartidor: Sin asignar"]:
            self.assertIn(expected, output)
        self.assertNotIn("Pedido confirmado", output)

    def test_zero_stock_rejects_order(self):
        self.assertIn("Estado: Rechazado", order(stock="0"))

    def test_invalid_and_empty_phone_use_default(self):
        for phone in ["abc", "", "123", "1234567890123456"]:
            with self.subTest(phone=phone):
                self.assertIn("Teléfono: Sin teléfono válido", order(phone=phone))

    def test_invalid_numbers_and_required_fields_can_be_corrected(self):
        output = run_app(["", " Cliente de prueba ", "", "Dirección", "123456789",
                          "Pepperoni", "abc", "0", "-1", "1.5", "1000",
                          "-1", "muchos", "3", "0", "-2", "2147483648", "3"])
        self.assertIn("Este campo es obligatorio", output)
        self.assertIn("Ingresa un precio entero mayor que cero", output)
        self.assertIn("Ingresa un número entero mayor o igual a 0", output)
        self.assertIn("Ingresa un número entero mayor o igual a 1", output)
        self.assertIn("Total a pagar: $3000 CLP", output)

    def test_discount_rounds_to_nearest_peso(self):
        output = order(price="1001", quantity="5")
        self.assertIn("Descuento aplicado: $501 CLP", output)
        self.assertIn("Total a pagar: $4504 CLP", output)

    def test_end_of_input_cancels_without_confirming(self):
        for lines in [[], ["Cliente", "Dirección"],
                      ["Cliente", "Dirección", "123456789", "Pepperoni", "1000", "10"]]:
            with self.subTest(lines=lines):
                output = run_app(lines)
                self.assertIn("Registro cancelado", output)
                self.assertNotIn("Pedido confirmado", output)
                self.assertNotIn("RESUMEN DEL PEDIDO", output)


if __name__ == "__main__":
    unittest.main()
