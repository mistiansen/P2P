public class Util {

    public static int bytesToInt(byte[] b) {
        return b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public static boolean strToBool(String boolStr) {
        switch (boolStr) {
            case "0":
                return false;
            case "1":
                return true;
            case "true":
                return true;
            case "false":
                return false;
            default:
                return false;
        }
    }
}


