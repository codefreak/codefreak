import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {

    private Calculator calculator = new Calculator();

    @Test
    @DisplayName("Addition with regular numbers")
    void addRegular() {
        assertEquals(Integer.valueOf(4), calculator.add(-3, 7), "Sum of -3 and 7 is not 4");
    }

    @Test
    @DisplayName("Valid additions with extreme values")
    void addWithoutOverflow() {
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), calculator.add(Integer.MAX_VALUE, 0), "Sum of Integer.MAX_VALUE and 0 is not Integer.MAX_VALUE");
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), calculator.add(Integer.MIN_VALUE, 0), "Sum of Integer.MIN_VALUE and 0 is not Integer.MIN_VALUE");
        assertEquals(Integer.valueOf(0), calculator.add(Integer.MAX_VALUE, -Integer.MAX_VALUE), "Sum of Integer.MAX_VALUE and -Integer.MAX_VALUE is not 0");
    }

    @Test
    @DisplayName("Invalid additions that would result in an integer overflow")
    void addWithOverflow() {
        assertThrows(IllegalArgumentException.class, () -> {
            calculator.add(Integer.MAX_VALUE, 1);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            calculator.add(Integer.MAX_VALUE - 10, 11);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            calculator.add(Integer.MIN_VALUE, -1);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            calculator.add(Integer.MIN_VALUE + 10, -11);
        });
    }

    @Test
    @DisplayName("Additions with null values")
    void addWithNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            calculator.add(null, null);
        });
    }

    @Test
    @Disabled
    void skipMe() {}

    @Test
    void alwaysSucceed() {}
}
