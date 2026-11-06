public class MemoryTest {
    public static void main(String[] args) {
        System.out.println("Starting memory test...");

        java.util.List<byte[]> blocks = new java.util.ArrayList<>();

        try {
            while (true) {
                blocks.add(new byte[1024 * 1024]);
                System.out.println("Allocated approximately " + blocks.size() + " MB");
            }
        } catch (OutOfMemoryError e) {
            System.out.println("JAVA_HEAP_LIMIT_REACHED");
        }
    }
}
