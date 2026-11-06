public class NetworkTest {
    public static void main(String[] args) {
        try {
            System.out.println(
                java.net.InetAddress.getByName("example.com")
            );
            System.out.println("NETWORK_ACCESS_AVAILABLE");
        } catch (Exception e) {
            System.out.println("NETWORK_BLOCKED");
            System.out.println(e.getClass().getSimpleName());
        }
    }
}
