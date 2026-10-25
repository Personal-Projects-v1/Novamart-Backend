import static org.junit.Assert.assertEquals;
import org.junit.Test;
import commerce.novamart.class1.Calculator;

public final class CalculatorTest {
    private final Calculator calculator = new Calculator();

    @Test
    public void testAddition() {
        int result = calculator.add(2, 3);
        assertEquals(5, result);
    }

    @Test
    public void testSubtraction() {
        int result = calculator.subtract(5, 3);
        assertEquals(2, result);
    }

    @Test
    public void testMultiplication() {
        int result = calculator.multiply(4, 5);
        assertEquals(20, result);
    }

    @Test
    public void testDivision() {
        int result = calculator.divide(10, 2);
        assertEquals(5, result);
    }

    @Test(expected = ArithmeticException.class)
    public void testDivisionByZero() {
        calculator.divide(10, 0);
    }
}