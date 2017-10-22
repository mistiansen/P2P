
import java.io.BufferedReader;
import java.io.FileReader;

public class ConfigParser {


    public static void parseConfig(String configFilename) {

        String st;
        try {
            BufferedReader in = new BufferedReader(new FileReader(configFilename));
            while ((st = in.readLine()) != null) {
                String[] tokens = st.split("\\s+");
                switch (tokens[0]) {
                    case "NumberOfPreferredNeighbors":
                        Constants.setNumPrefNeighbors(Integer.parseInt(tokens[1]));
                        break;
                    case "UnchokingInterval":
                        Constants.setUnchokeInterval(Integer.parseInt(tokens[1]));
                        break;
                    case "OptimisticUnchokingInterval":
                        Constants.setOptUnchokeInterval(Integer.parseInt(tokens[1]));
                        break;
                    case "FileName":
                        Constants.setFileName(tokens[1]);
                        break;
                    case "FileSize":
                        Constants.setFileSize(Integer.parseInt(tokens[1]));
                        break;
                    case "PieceSize":
                        Constants.setPieceSize(Integer.parseInt(tokens[1]));
                        break;
                }
            }
            in.close();
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }

    }


}
