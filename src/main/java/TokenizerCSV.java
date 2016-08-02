import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class TokenizerCSV {

    private final static String INSUMOS_CSV = "insumos.csv";
    private final static String STOCK_VARS_CSV = "stockVars.csv";

    public static ArrayHashMap tokenizeMaterialFile() {

        ArrayHashMap dataMapper = new ArrayHashMap();

        try {

            BufferedReader br = new BufferedReader( new FileReader(INSUMOS_CSV));
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

    public static Map<String, StockVars>  tokenizeStockMaxFile() {

        Map<String, StockVars> map = new HashMap<>();

        try {

            BufferedReader br = new BufferedReader( new FileReader(STOCK_VARS_CSV));
            String strLine;
            StringTokenizer st;

            while( (strLine = br.readLine()) != null) {

                st = new StringTokenizer(strLine, ";");
                StockVars stockVars = new StockVars();

                while(st.hasMoreTokens()) {
                    stockVars.add(st.nextToken());
                }

                map.put(stockVars.nameMaterial, stockVars);
            }
        } catch(Exception e) {
            System.out.println("Exception while reading csv file: " + e);
        }

        return map;
    }
}
