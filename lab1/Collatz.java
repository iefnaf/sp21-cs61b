/** Class that prints the Collatz sequence starting from a given number.
 *  @author gaofanfei
 */
public class Collatz {

    /** Return the next number of n in Collatz sequence */
    public static int nextNumber(int n) {
        if (n == 1) {
            return 1;
        }
        if ((n & 0x1) == 0) {
            return n / 2;
        } else {
            return n * 3 + 1;
        }
    }

    public static void main(String[] args) {
        int n = 5;
        System.out.print(n + " ");
        while (n != 1) {
            n = nextNumber(n);
            System.out.print(n + " ");
        }
        System.out.println();
    }
}

