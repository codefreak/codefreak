public class Main {
    private static Calculator calculator = new Calculator();

    public static void main(String[] args) {
        printSumOf(3, 4);
        printSumOf(-2, 3);
        printSumOf(0, 0);
        printSumOf(Integer.MAX_VALUE, 1);
        printSumOf(Integer.MIN_VALUE, -1);
        printSumOf(Integer.MIN_VALUE, Integer.MAX_VALUE);
        printSumOf(null, 1);
        printSumOf(1, null);
        printSumOf(null, null);
    }

    private static void printSumOf(Integer a, Integer b) {
        System.out.printf("%d + %d = ", a, b);
        try {
            System.out.printf("%d\n", calculator.add(a, b));
        } catch(RuntimeException e) {
            System.out.println("ERROR");
        }
    }
}
