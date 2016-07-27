import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class TokenizerCSV {

    private final static String CSVFile = "insumos.csv";

    public static ArrayHashMap tokenize() {

        ArrayHashMap dataMapper = new ArrayHashMap();

        try {

            BufferedReader br = new BufferedReader( new FileReader(CSVFile));
            String strLine = "";
            StringTokenizer st = null;

            while( (strLine = br.readLine()) != null) {

                st = new StringTokenizer(strLine, ";");
                Material material = new Material();

                while(st.hasMoreTokens()) {
                    material.add(st.nextToken());
                }

                dataMapper.put(material.nameKey, material.materialInfo);

            }

            dataMapper.sortAllLists();

        } catch(Exception e) {
            System.out.println("Exception while reading csv file: " + e);
        }

        return dataMapper;

    }


}
