
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ConfigParser {


    public static void parseConfig(String configFilename) throws FileNotFoundException {

        String st;
        int configCount = 0;
        try {
            BufferedReader in = new BufferedReader(new FileReader(configFilename));
            while ((st = in.readLine()) != null) {
                String[] tokens = st.split("\\s+");
                switch (tokens[0]) {
                    case "NumberOfPreferredNeighbors":
                        Constants.setNumPrefNeighbors(Integer.parseInt(tokens[1]));
//                        System.out.println("NumberOfPreferredNeighbors " + Constants.NUM_PREF_NEIGHBORS);
//                        configCount++;
                        break;
                    case "UnchokingInterval":
                        Constants.setUnchokeInterval(Integer.parseInt(tokens[1]));
//                        System.out.println("UnchokingInterval " + Constants.UNCHOKE_INTERVAL);
//                        configCount++;
                        break;
                    case "OptimisticUnchokingInterval":
                        Constants.setOptUnchokeInterval(Integer.parseInt(tokens[1]));
//                        System.out.println("OptimisticUnchokingInterval " + Constants.OPT_UNCHOKE_INTERVAL);
//                        configCount++;
                        break;
                    case "FileName":
                        Constants.setFileName(tokens[1]);
//                        System.out.println("FileName " + Constants.FILE_NAME);
//                        configCount++;
                        break;
                    case "FileSize":
                        Constants.setFileSize(Integer.parseInt(tokens[1]));
//                        System.out.println("FileSize " + Constants.FILE_SIZE);
//                        configCount++;
                        break;
                    case "PieceSize":
                        Constants.setPieceSize(Integer.parseInt(tokens[1]));
//                        System.out.println("PieceSize " + Constants.PIECE_SIZE);
//                        configCount++;
                        break;
                }
            }
            in.close();
        } catch (IOException ex) {
            System.out.println(ex.toString());
            ex.printStackTrace();
        }
//        if(configCount < 6) {
//            System.out.println("Too few config assignments in Config Parser");
//            return false;
//        } else {
//            return true;
//        }

    }

}
