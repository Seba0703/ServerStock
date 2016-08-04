import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;


public class HelloWorld {

    //--path vars--
    private final static String MAT_SUB = "mat_sub";
    private final static String MAT_ADD = "mat_add";
    private final static String MAT_STATUS = "mat_status";
    private final static String MAT_TRANSACTIONS_DEST = "mat_transacDest";
    private final static String MAT_TRANSACTIONS_PROD = "mat_transacProd";
    private final static String VARS_CONFIG = "varsConfig";
    private final static String MAT_MATERIALS_ID = "mat_matID";
    private final static String LOGIN = "login";
    private final static String LOGON = "logon";

    //--json keys--
    public final static String NAME_MAT_KEY = "nombre";
    public final static String QUANTITY_MAT_KEY = "cantidad";


    public static void main(String[] args) {

        RocksDBWrapper DB = new RocksDBWrapper();

        DB.saveMaterialStockMax();
        DB.initializeMaterialQuantityTrim();
        DB.savePredictionStockThreeMonths();

        //levantar base de datos de muebles
        //levantar base de datos de usuarios

        put(MAT_SUB, (request, response) -> {

            JSONObject jsonMat = new JSONObject(request.body());

            String materialID = jsonMat.getString(NAME_MAT_KEY);
            String user = jsonMat.getString(Consts.USER);
            String destiny = jsonMat.getString(Consts.DESTINY);
            int quantity = jsonMat.getInt(Consts.QUANTITY);

            DB.updateLessMaterialDBkey(user, destiny, materialID, quantity, response);
            DB.updateLessForTrimester(materialID,quantity);

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
            DB.updateTransaction(material, jsonMat.getInt(Consts.TRANSACTION_DATE));

            return response;
        });

        post(MAT_ADD, (request, response) -> {

            JSONObject jsonMat = new JSONObject(request.body());

            Material material = new Material();

            material.add(jsonMat.getString(NAME_MAT_KEY));
            material.add("" + jsonMat.getInt(Consts.QUANTITY));
            material.add("" + jsonMat.getInt(Consts.DUE_DATE));
            material.add("" + jsonMat.getInt(Consts.PRICE));

            DB.addNewMaterial(material);
            DB.updateTransaction(material, jsonMat.getInt(Consts.TRANSACTION_DATE));

            return "Ok";
        });

        // devuelvo el estado de todos los insumos
        get(MAT_STATUS,(request, response) -> {

            JSONObject jsonO = new JSONObject();
            JSONArray jsonArray = new JSONArray();

            FindIterable<Document> iterable = DB.getMaterialsStockVars();

            iterable.forEach(new Block<Document>() {
                @Override
                public void apply(final Document document) {
                    String materialID = document.getString(Consts.MATERIALS_ID);
                    int stockMin = document.getInteger(Consts.STOCK_MIN);
                    int safetyVar = document.getInteger(Consts.STOCK_SAFE);

                    int materialStock = DB.getMaterialQuantity(materialID);
                    int stockMax = document.getInteger(Consts.STOCK_MAX);

                    int stockBuy = stockMax - materialStock;

                    String result;
                    if (materialStock >= stockMin + safetyVar) {
                        result = Consts.YELLOW;
                    } else {
                        result = Consts.RED;
                    }

                    JSONObject jsonMat = new JSONObject();
                    jsonMat.append(Consts.RESULT, result);
                    jsonMat.append(Consts.MATERIALS_ID, materialID);
                    jsonMat.append(Consts.TO_BUY, stockBuy);
                    jsonMat.append(Consts.STOCK_MAX, stockMax);

                    jsonArray.put(jsonMat);
                }
            });

            jsonO.append(Consts.MATERIALS, jsonArray);

            return jsonO.toString();
        });

        get(MAT_TRANSACTIONS_DEST, (request, response) -> {

            JSONObject jsonReq = new JSONObject(request.body());

            String transTypo = jsonReq.getString(Consts.TRANSACTION_TYPE);
            String destiny = jsonReq.getString(Consts.DESTINY);
            int fromDate = jsonReq.getInt(Consts.FROM_DATE);
            int toDate = jsonReq.getInt(Consts.TO_DATE);

            JSONArray jsonArray = new JSONArray();

            FindIterable<Document> results = DB.getRecords(transTypo, destiny, fromDate, toDate);

            results.forEach(new Block<Document>() {
                @Override
                public void apply(Document document) {

                    JSONObject jsonO = new JSONObject();
                    jsonO.put(Consts.TRANSACTION_DATE, document.getInteger(Consts.TRANSACTION_DATE));
                    jsonO.put(Consts.MATERIALS_ID, document.getString(Consts.MATERIALS_ID));
                    jsonO.put(Consts.QUANTITY, document.getInteger(Consts.QUANTITY));

                    jsonArray.put(jsonO);
                }
            });

            JSONObject jsonResult = new JSONObject();
            jsonResult.put(Consts.RESULT, jsonArray);

            return jsonResult.toString();

        });

        get(MAT_TRANSACTIONS_PROD, (request, response) -> {

            JSONObject jsonReq = new JSONObject(request.body());

            String materialID = jsonReq.getString(Consts.MATERIALS_ID);
            String transTypo = jsonReq.getString(Consts.TRANSACTION_TYPE);
            String destiny = jsonReq.getString(Consts.DESTINY);
            int fromDate = jsonReq.getInt(Consts.FROM_DATE);
            int toDate = jsonReq.getInt(Consts.TO_DATE);

            JSONArray jsonArray = new JSONArray();

            FindIterable<Document> results = DB.getRecords(materialID, transTypo, destiny, fromDate, toDate);

            results.forEach(new Block<Document>() {
                @Override
                public void apply(Document document) {

                    JSONObject jsonO = new JSONObject();
                    jsonO.put(Consts.TRANSACTION_DATE, document.getInteger(Consts.TRANSACTION_DATE));
                    jsonO.put(Consts.PRICE, document.getString(Consts.PRICE));
                    jsonO.put(Consts.QUANTITY, document.getInteger(Consts.QUANTITY));

                    jsonArray.put(jsonO);
                }
            });

            JSONObject jsonResult = new JSONObject();
            jsonResult.put(Consts.RESULT, jsonArray);

            return jsonResult.toString();

        });

        put(VARS_CONFIG, (request, response) -> {

            JSONObject jsonReq = new JSONObject(request.body());
            String materialID = jsonReq.getString(Consts.MATERIALS_ID);
            int multiplier = jsonReq.getInt(Consts.STOCK_MULTIPLY);
            int safe = jsonReq.getInt(Consts.STOCK_SAFE);
            int stockMin = jsonReq.getInt(Consts.STOCK_MIN);

            DB.updateStockVars(materialID, stockMin, safe, multiplier);

            return "Ok";
        });

        get(VARS_CONFIG, (request, response) -> {

            JSONObject jsonReq = new JSONObject(request.body());

            String materialID = jsonReq.getString(Consts.MATERIALS_ID);

            StockVars stockVars = new StockVars();

            DB.getStockVars(materialID, stockVars);

            JSONObject jsonO = new JSONObject();

            jsonO.put(Consts.STOCK_MIN, stockVars.stockMin);
            jsonO.put(Consts.STOCK_MULTIPLY, stockVars.multiplierSafetyVar);
            jsonO.put(Consts.STOCK_SAFE, stockVars.safetyVar);

            return jsonO.toString();
        });

        get(MAT_MATERIALS_ID, (request, response) -> {

            FindIterable<Document> names = DB.getMaterialsID();

            JSONArray jsonArray = new JSONArray();

            names.forEach(new Block<Document>() {
                @Override
                public void apply(Document document) {
                    JSONObject jsonO = new JSONObject();
                    jsonO.put(Consts.MATERIALS_ID, document.getString(Consts.MATERIALS_ID));
                    jsonArray.put(jsonO);
                }
            });

            JSONObject result = new JSONObject();
            result.put(Consts.RESULT, jsonArray);
            return result.toString();
        });

        post(LOGON, (request, response) -> {

            String user = request.headers(Consts.USER);
            String pass = request.headers(Consts.PASS);

            DB.logon(user, pass, response);

            return response.body();

        });

        get(LOGIN, (request, response) -> {

            String user = request.headers(Consts.USER);
            String pass = request.headers(Consts.PASS);

            DB.login(user, pass, response);

            return response.body();

        });


    }
}
