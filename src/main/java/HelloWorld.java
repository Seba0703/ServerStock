import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.print.Doc;

import static spark.Spark.*;


public class HelloWorld {

    //--path vars--
    private final static String MAT_SUB = "mat_sub";
    private final static String MAT_ADD = "mat_add";
    private final static String MAT_STATUS = "mat_status";
    private final static String MAT_TRANSACTIONS = "mat_transacDest";
    private final static String MAT_TRANSACTIONS_CHANGE = "mat_transacChange";
    private final static String EDIT_PROD = "mat_edit_prod";
    private final static String VARS_CONFIG = "varsConfig";
    private final static String MAT_MATERIALS_ID = "mat_matID";
    private final static String LOGIN = "login";
    private final static String SET_USERS = "setUsers";
    private final static String USERS_ID = "userID";
    private static final String DELETE_PROD = "deleteProd";
    private static final String HAS_PERMISSION = "tienePermiso";
    private static final String PROD_STATUS = "prod_status";
    private static RocksDBWrapper DB;

    public static void main(String[] args) {

        DB = new RocksDBWrapper();

        DB.saveMaterialStockMax();
        DB.initializeMaterialQuantityTrim();
        DB.savePredictionStockThreeMonths();

        //levantar base de datos de muebles
        //levantar base de datos de usuarios

        put(MAT_SUB, (request, response) -> {
            JSONObject jsonMat = new JSONObject(request.body());

            String materialID = jsonMat.getString(Consts.MATERIALS_ID).toUpperCase();
            String user = jsonMat.getString(Consts.USER).toUpperCase();
            String destiny = jsonMat.getString(Consts.DESTINY).toUpperCase();
            int quantity = jsonMat.getInt(Consts.QUANTITY);

            DB.updateLessMaterialDBkey(user, destiny, materialID, quantity, response);
            DB.updateLessForTrimester(materialID, quantity);

            return response.body();
        });

        put(MAT_ADD, (request, response) -> {

            JSONObject jsonMat = new JSONObject(request.body());

            Material material = new Material();

            material.add(jsonMat.getString(Consts.MATERIALS_ID).toUpperCase());
            material.add("" + jsonMat.getInt(Consts.QUANTITY));
            material.add("" + jsonMat.getInt(Consts.DUE_DATE));
            material.add("" + jsonMat.getDouble(Consts.PRICE));
            material.add("" + jsonMat.getInt(Consts.TRANSACTION_DATE));
            String user = jsonMat.getString(Consts.USER).toUpperCase();

            DB.updateAddMaterialDBkey(material, response);
            DB.updateTransaction(user, material);

            return response.body();
        });

        post(MAT_ADD, (request, response) -> {

            System.out.println("post prod");

            JSONObject jsonMat = new JSONObject(request.body());
            Material material = new Material();
            material.add(jsonMat.getString(Consts.MATERIALS_ID).toUpperCase());
            material.add("" + jsonMat.getInt(Consts.QUANTITY));
            material.add("" + jsonMat.getInt(Consts.DUE_DATE));
            material.add("" + jsonMat.getDouble(Consts.PRICE));
            material.add("" + jsonMat.getInt(Consts.TRANSACTION_DATE));
            String user = jsonMat.getString(Consts.USER).toUpperCase();

            if (DB.hasMaterial(material.nameKey)) {
                System.out.println("tiene");
                DB.updateAddMaterialDBkey(material, response);
            } else {
                System.out.println("nuevo");
                DB.addNewMaterial(material);
            }
            DB.updateTransaction(user, material);

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
                    System.out.println(document);
                    String materialID = document.getString(Consts.MATERIALS_ID);
                    int stockMin = document.getInteger(Consts.STOCK_MIN);
                    int safetyVar = document.getInteger(Consts.STOCK_SAFE);
                    int stockMax = document.getInteger(Consts.STOCK_MAX);

                    int materialStock = DB.getMaterialQuantity(materialID);

                    int stockBuy = stockMax - materialStock;

                    int result;
                    if (materialStock >= stockMin + safetyVar) {
                        result = Consts.WHITE;
                    } else if ( materialStock <  stockMin + safetyVar && materialStock >= stockMin) {
                        result = Consts.YELLOW;
                    } else {
                        result = Consts.RED;
                    }

                    JSONObject jsonMat = new JSONObject();
                    jsonMat.put(Consts.RESULT, result);
                    jsonMat.put(Consts.MATERIALS_ID, materialID);
                    jsonMat.put(Consts.TO_BUY, stockBuy);
                    jsonMat.put(Consts.STOCK_MAX, stockMax);
                    jsonMat.put(Consts.QUANTITY, materialStock);

                    jsonArray.put(jsonMat);
                }
            });

            jsonO.put(Consts.MATERIALS, jsonArray);
            System.out.println(jsonArray.length());

            return jsonO.toString();
        });

        put(MAT_TRANSACTIONS, (request, response) -> {

            System.out.println(request.body());

            Document docRequest = Document.parse(request.body());

            JSONArray jsonArray = new JSONArray();

            FindIterable<Document> results = DB.getRecords(docRequest);

            results.forEach(new Block<Document>() {
                @Override
                public void apply(Document document) {

                    JSONObject jsonO = new JSONObject(document.toJson());

                    jsonArray.put(jsonO);
                }
            });

            JSONObject jsonResult = new JSONObject();
            jsonResult.put(Consts.RESULT, jsonArray);

            return jsonResult.toString();

        });

        put(VARS_CONFIG, (request, response) -> {

            JSONObject jsonReq = new JSONObject(request.body());
            String materialID = jsonReq.getString(Consts.MATERIALS_ID).toUpperCase();
            int multiplier = jsonReq.getInt(Consts.STOCK_MULTIPLY);
            int safe = jsonReq.getInt(Consts.STOCK_SAFE);
            int stockMin = jsonReq.getInt(Consts.STOCK_MIN);

            DB.updateStockVars(materialID, stockMin, safe, multiplier);

            return "Ok";
        });

        post(VARS_CONFIG, (request, response) -> {

            System.out.println("post vars");

            JSONObject jsonReq = new JSONObject(request.body());
            String materialID = jsonReq.getString(Consts.MATERIALS_ID).toUpperCase();
            int multiplier = jsonReq.getInt(Consts.STOCK_MULTIPLY);
            int safe = jsonReq.getInt(Consts.STOCK_SAFE);
            int stockMin = jsonReq.getInt(Consts.STOCK_MIN);
            int stockMax = jsonReq.getInt(Consts.STOCK_MAX);
            System.out.println("paso json");

            DB.addProductoStockVars(materialID, stockMax, stockMin, safe, multiplier);
            System.out.println("termino");

            return "Ok";
        });

        get(VARS_CONFIG, (request, response) -> {

            Document doc = DB.getStockVars(request.headers(Consts.MATERIALS_ID).toUpperCase());

            return doc.toJson();
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

        post(SET_USERS, (request, response) -> {
            System.out.println("entra set");
            Document doc = Document.parse(request.body());
            System.out.println(doc);
            String name = ((String) doc.remove(Consts.USER)).toUpperCase();
            doc.append(Consts.USER, name);
            System.out.println(doc);
            DB.setUser(doc, response);

            return response.body();
        });

        get(LOGIN, (request, response) -> {

            System.out.println("llega al login");

            String user = request.headers(Consts.USER).toUpperCase();
            String pass = request.headers(Consts.PASS);

            DB.login(user, pass, response);
            System.out.println("Response login");

            return response.body();

        });

        get(USERS_ID, (request, response) -> {

            FindIterable<Document> names = DB.getUserID();

            JSONArray jsonArray = new JSONArray();

            names.forEach(new Block<Document>() {
                @Override
                public void apply(Document document) {
                    document.remove(Consts.PASS);
                    JSONObject jsonO = new JSONObject(document.toJson());
                    jsonArray.put(jsonO);
                }
            });

            JSONObject result = new JSONObject();
            result.put(Consts.RESULT, jsonArray);
            return result.toString();
        });

        put(HAS_PERMISSION, ((request, response) -> {
            System.out.println(request.body());
            DB.hasPermission(Document.parse(request.body()), response);
            return response.body();
        }));

        put(EDIT_PROD, (request, response) -> {

            JSONObject jsonObject = new JSONObject(request.body());
            String name = jsonObject.getString(Consts.MATERIALS_ID).toUpperCase();
            int dueDate = jsonObject.getInt(Consts.DUE_DATE);
            int buyDate = jsonObject.getInt(Consts.TRANSACTION_DATE);
            double price = jsonObject.getDouble(Consts.PRICE);

            DB.getMaterialData(name, price, dueDate, buyDate, response);

            System.out.println(response.body());

            return response.body();
        });

        put(DELETE_PROD, (request, response) -> {

            System.out.println("entra PUT");

            JSONObject jsonOld = new JSONObject(request.body());

            Material oldMat = new Material();
            oldMat.add(jsonOld.getString(Consts.MATERIALS_ID).toUpperCase());
            oldMat.add("" + jsonOld.getInt(Consts.QUANTITY));
            oldMat.add("" + jsonOld.getInt(Consts.DUE_DATE));
            oldMat.add("" + jsonOld.getDouble(Consts.PRICE));
            oldMat.add("" + jsonOld.getInt(Consts.TRANSACTION_DATE));

            JSONObject jsonNew = jsonOld.getJSONObject(Consts.NEW_VALUE);

            Material newMat = new Material();
            newMat.add(jsonNew.getString(Consts.MATERIALS_ID).toUpperCase());
            newMat.add("" + jsonNew.getInt(Consts.QUANTITY));
            newMat.add("" + jsonNew.getInt(Consts.DUE_DATE));
            newMat.add("" + jsonNew.getDouble(Consts.PRICE));
            newMat.add("" + jsonNew.getInt(Consts.TRANSACTION_DATE));

            DB.deleteMaterialData(oldMat, response);
            DB.updateAddMaterialDBkey(newMat, response);
            DB.updateChanges(oldMat, newMat, jsonOld.getString(Consts.USER), jsonNew.getString(Consts.USER));

            System.out.println(response.body());

            return response.body();
        });

        post(DELETE_PROD, (request, response) -> {

            System.out.println("entra POST");

            JSONObject jsonOld = new JSONObject(request.body());

            Material oldMat = new Material();
            oldMat.add(jsonOld.getString(Consts.MATERIALS_ID).toUpperCase());
            oldMat.add("" + jsonOld.getInt(Consts.QUANTITY));
            oldMat.add("" + jsonOld.getInt(Consts.DUE_DATE));
            oldMat.add("" + jsonOld.getDouble(Consts.PRICE));
            oldMat.add("" + jsonOld.getInt(Consts.TRANSACTION_DATE));

            JSONObject jsonNew = jsonOld.getJSONObject(Consts.NEW_VALUE);

            Material newMat = new Material();
            newMat.add(jsonNew.getString(Consts.MATERIALS_ID).toUpperCase());
            newMat.add("" + jsonNew.getInt(Consts.QUANTITY));
            newMat.add("" + jsonNew.getInt(Consts.DUE_DATE));
            newMat.add("" + jsonNew.getDouble(Consts.PRICE));
            newMat.add("" + jsonNew.getInt(Consts.TRANSACTION_DATE));

            DB.deleteMaterialData(oldMat, response);
            DB.addNewMaterial(newMat);
            DB.updateChanges(oldMat, newMat, jsonOld.getString(Consts.USER), jsonNew.getString(Consts.USER));

            System.out.println(response.body());

            return response.body();
        });

        put(MAT_TRANSACTIONS_CHANGE, (request, response) -> {

            System.out.println("entra CHANGE");

            Document document = Document.parse(request.body());

            String transType = document.getString(Consts.TRANSACTION_TYPE);

            if (transType.equals(Consts.CHANGEnew)) {
                Document auxDoc = new Document();

                if (document.containsKey(Consts.MATERIALS_ID)) {
                    auxDoc.append(Consts.MATERIALS_IDnew, document.getString(Consts.MATERIALS_ID));
                }

                if (document.containsKey(Consts.TRANSACTION_DATE)) {
                    auxDoc.append(Consts.TRANS_DATEnew, document.get(Consts.TRANSACTION_DATE));
                }

                if (document.containsKey(Consts.DUE_DATE)) {
                    auxDoc.append(Consts.DUE_DATEnew, document.getInteger(Consts.DUE_DATE));
                }

                if (document.containsKey(Consts.USER)) {
                    auxDoc.append(Consts.USERnew, document.getString(Consts.USER));
                }

                document = auxDoc;
            } else {
                document.remove(Consts.TRANSACTION_TYPE);
            }

            System.out.println(document);

            FindIterable<Document> result = DB.getChangeRecords(document);

            JSONArray jsonArray = new JSONArray();

            result.forEach(new Block<Document>() {
                @Override
                public void apply(Document doc) {
                    JSONObject jsonO = new JSONObject(doc.toJson());
                    jsonArray.put(jsonO);
                }
            });

            JSONObject jsonResult = new JSONObject();
            jsonResult.put(Consts.RESULT, jsonArray);
            System.out.println(jsonResult.toString());

            return jsonResult.toString();
        });

        put(PROD_STATUS, (request, response) -> {
            Document doc = Document.parse(request.body());
            String materialID = doc.getString(Consts.MATERIALS_ID);
            int materialStock = DB.getMaterialQuantity(materialID);
            Document matStock = DB.getStockVars(materialID);

            int stockMin = matStock.getInteger(Consts.STOCK_MIN);
            int safetyVar = matStock.getInteger(Consts.STOCK_SAFE);

            int result;
            if (materialStock >= stockMin + safetyVar) {
                result = Consts.WHITE;
            } else if ( materialStock <  stockMin + safetyVar && materialStock >= stockMin) {
                result = Consts.YELLOW;
            } else {
                result = Consts.RED;
            }

            JSONObject jsonMat = new JSONObject();
            jsonMat.put(Consts.RESULT, result);
            jsonMat.put(Consts.QUANTITY, materialStock);

            return jsonMat.toString();
        });

    }
}
