import org.json.JSONObject;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;


public class HelloWorld {

    //--path vars--
    public final static String MAT_SUB = "mat_sub";
    public final static String MAT_ADD = "mat_add";

    //--json keys--
    public final static String NAME_MAT_KEY = "nombre";
    public final static String QUANTITY_MAT_KEY = "cantidad";


    public static void main(String[] args) {

        RocksDBWrapper DB = new RocksDBWrapper();

        //levantar base de datos de muebles
        //levantar base de datos de usuarios

        put(MAT_SUB, (request, response) -> {

            JSONObject jsonMat = new JSONObject(request.body());

            String materialID = jsonMat.getString(NAME_MAT_KEY);
            String user = jsonMat.getString(Consts.USER);
            String destiny = jsonMat.getString(Consts.DESTINY);
            int quantity = jsonMat.getInt(Consts.QUANTITY);

            DB.updateLessMaterialDBkey(materialID,quantity,response);

            return response;
        });

        put(MAT_ADD, (request, response) -> {

            JSONObject jsonMat = new JSONObject(request.body());

            Material material = new Material();

            material.add(jsonMat.getString(NAME_MAT_KEY));
            material.add("" + jsonMat.getInt(Consts.QUANTITY));
            material.add("" + jsonMat.getInt(Consts.DUE_DATE));
            material.add("" + jsonMat.getInt(Consts.PRICE));

            DB.updateAddMaterialDBkey(material, response);

            return response;
        });
    }
}
