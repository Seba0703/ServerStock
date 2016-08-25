import Wrappers.CalendarWrapper;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.descending;

class MongoDBWrapper {

    private final static String COPA_DB = "copa_db";

    final static String MaterialsCollection = "insumos";
    private final static String MaterialStock_Vars = "stockMaxMat";
    final static String MaterialOutQuantityTrim = "salidasMatTrim";
    private final static String TransactionRecords = "transacciones";
    private final static String Changes = "cambios";

    //base de datos de usuarios
    private final static String USERS = "usuarios";
    //---------------------------------------------

    final static String FurnituresDBname = "muebles";
    final static String NUM_MUEBLES = "nameMuebles";

    private boolean existDB = false;
    private  MongoDatabase copaDB;

    private int yearMonthDay;
    private int yearMonth;

    private MongoClient mongoClient;

    MongoDBWrapper(CalendarWrapper calendar) {

        yearMonthDay = calendar.getYYYYMMDD();
        yearMonth =  calendar.getYYYYMM();
        mongoClient = new MongoClient();

        MongoIterable<String> dbList = mongoClient.listDatabaseNames();

        dbList.forEach(new Block<String>() {
            @Override
            public void apply(final String dbName) {
                if (dbName.equals(COPA_DB)) {
                    existDB = true;
                }
            }
        });

        copaDB = mongoClient.getDatabase(COPA_DB);
    }

    void saveProductsFromCSV() {
        if (!existDB) {
            System.out.println("nueva base de datos");
            ArrayHashMap dataMapper = TokenizerCSV.tokenizeMaterialFile();
            dataMapper.saveIntoDB(copaDB, yearMonth);
        }
    }

    void updateLessMaterialDBkey(String user, String destiny, String materialID, int quantitySub, Response response) {

        FindIterable<Document> iterable = copaDB.getCollection(MaterialsCollection).find(eq(Consts.MATERIALS_ID, materialID));
        //obtengo toda la "info": [{"cantidad": 4,"fechavto":20140505,"precio": 21}, {...}, ..,]
        //unica entrada en el Document, busco por ID
        Document info = iterable.first();
        List<Document> docArray =  (List<Document>) info.get(Consts.INFO);
        int totalQuantity = info.getInteger(Consts.QUANTITY) - quantitySub; //para actualizar total

        int i = 0;
        boolean done = false;
        int newQuantityStock = 0;
        while ( i < docArray.size() && !done) {

            Document firstDueDate = docArray.get(i);    //primer lote que se vence

            int quantityLote = firstDueDate.getInteger(Consts.QUANTITY);
            newQuantityStock = quantityLote - quantitySub;
            double price = firstDueDate.getDouble(Consts.PRICE);
            int dueDate = firstDueDate.getInteger(Consts.DUE_DATE);

            if ( newQuantityStock <= 0) {               // si es mas chico que cero ese lote quedo vacio y se va al siguiente
                updateTransaction(user, destiny, materialID, quantityLote, dueDate, price);
                docArray.remove(i);
                if (newQuantityStock == 0) {
                    done = true;
                } else {
                    quantitySub = -newQuantityStock;
                    i--;
                }
            } else {
                updateTransaction(user,destiny,materialID,quantitySub, dueDate, price);
                firstDueDate.put(Consts.QUANTITY, newQuantityStock);
                done = true;
            }

            i++;
        }

        UpdateResult result = null;

        if (newQuantityStock >= 0) {

            result = copaDB.getCollection(MaterialsCollection).updateOne(new Document(Consts.MATERIALS_ID, materialID),
                    new Document("$set", new Document(Consts.INFO, docArray)
                            .append(Consts.QUANTITY, totalQuantity)));
        }

        if ( result == null || result.getMatchedCount() == 0  || !result.isModifiedCountAvailable()) {
            response.status(404);
            response.body("Bad request");
        } else {
            response.status(200);
            response.body("Ok");
        }
    }

    void updateAddMaterialDBkey(Material material, Response response) {

        FindIterable<Document> iterable = copaDB.getCollection(MaterialsCollection).find(eq(Consts.MATERIALS_ID, material.nameKey));

        //obtengo toda la "info": [{"cantidad": 4,"fechavto":20140505,"precio": 21}, {...}, ...,]
        //unica entrada en el Document, busco por ID
        Document info = iterable.first();
        List<Document> docArray =  (List<Document>) info.get(Consts.INFO);
        int totalQuantity = info.getInteger(Consts.QUANTITY) + material.materialInfo.quantity;

        int index = 0;
        boolean found = false;
        MaterialInfo materialInfo = material.materialInfo;

        while ( index < docArray.size() && !found ) {

            if ( (index != docArray.size() - 1) && lastSameDate(docArray,index,materialInfo) ) {
                addToDocList(docArray, index + 1, materialInfo );
                found = true;
            } else if (materialInfo.dueDate < docArray.get(index).getInteger(Consts.DUE_DATE)) {
                addToDocList(docArray, index, materialInfo );
                found = true;
            } else if (index == docArray.size() - 1 ) {
                addToDocList(docArray, materialInfo );
                found = true;
            }

            index++;
        }

        if (docArray.isEmpty()) {
            addToDocList(docArray, materialInfo );
        }

        UpdateResult result = copaDB.getCollection(MaterialsCollection).updateOne(eq(Consts.MATERIALS_ID, material.nameKey),
                new Document("$set", new Document(Consts.INFO, docArray)
                        .append(Consts.QUANTITY, totalQuantity)));

        if ( result.getMatchedCount() == 0  || !result.isModifiedCountAvailable()) {
            response.status(404);
            response.body("Bad request");
        } else {
            response.status(200);
            response.body("Ok");
        }
    }

    private void addToDocList(List<Document> docArray, int index, MaterialInfo materialInfo) {
        docArray.add(index, new Document().append(Consts.DUE_DATE, materialInfo.dueDate)
                .append(Consts.PRICE, materialInfo.price)
                .append(Consts.QUANTITY, materialInfo.quantity)
                .append(Consts.TRANSACTION_DATE, materialInfo.buyDate));
    }

    private void addToDocList(List<Document> docArray, MaterialInfo materialInfo) {
        docArray.add( new Document().append(Consts.DUE_DATE, materialInfo.dueDate)
                .append(Consts.PRICE, materialInfo.price)
                .append(Consts.QUANTITY, materialInfo.quantity)
                .append(Consts.TRANSACTION_DATE, materialInfo.buyDate));
    }

    private boolean lastSameDate(List<Document> docArray, int i, MaterialInfo info) {
        return docArray.get(i).getInteger(Consts.DUE_DATE) == info.dueDate && docArray.get(i + 1).getInteger(Consts.DUE_DATE) != info.dueDate;
    }


    void savePredictionStockThreeMonths() {

        FindIterable<Document> iterable = copaDB.getCollection(MaterialsCollection).find();

        iterable.forEach(new Block<Document>() {
            @Override
            public void apply(final Document document) {

                int lastUpdate = document.getInteger(Consts.LAST_UPDATE);
                String materialID = document.getString(Consts.MATERIALS_ID);

                if ( yearMonth >= CalendarWrapper.nextTrimester(lastUpdate) ) {

                    //update la fecha de actualizacion
                    copaDB.getCollection(MaterialsCollection).updateOne(eq(Consts.MATERIALS_ID, materialID),
                            new Document("$set", new Document(Consts.LAST_UPDATE, yearMonth)));

                    Document quantityOutDoc = copaDB.getCollection(MaterialOutQuantityTrim).find(
                            new Document(Consts.YEAR_MONTH_ID, lastUpdate)
                                    .append(Consts.MATERIALS_ID, materialID)).first();

                    int quantityOut = quantityOutDoc.getInteger(Consts.QUANTITY);

                    Document materialStockVar = copaDB.getCollection(MaterialStock_Vars).find(new Document(Consts.MATERIALS_ID, materialID)
                            .append(Consts.YEAR_MONTH_ID, lastUpdate)).first();

                    StockVars stockVars = new StockVars();
                    stockVars.safetyVar = materialStockVar.getInteger(Consts.STOCK_SAFE);
                    stockVars.multiplierSafetyVar = materialStockVar.getInteger(Consts.STOCK_MULTIPLY);
                    stockVars.stockMax = materialStockVar.getInteger(Consts.STOCK_MAX);
                    stockVars.stockMin = materialStockVar.getInteger(Consts.STOCK_MIN);

                    int predictionMaxStock = Common.makeTrimesterPrediction(stockVars, quantityOut);

                    copaDB.getCollection(MaterialStock_Vars).insertOne(new Document(Consts.STOCK_MAX, predictionMaxStock)
                            .append(Consts.MATERIALS_ID, materialID)
                            .append(Consts.YEAR_MONTH_ID, yearMonth)
                            .append(Consts.STOCK_MIN, stockVars.stockMin)
                            .append(Consts.STOCK_SAFE, stockVars.safetyVar)
                            .append(Consts.STOCK_MULTIPLY, stockVars.multiplierSafetyVar));
                }
            }
        });
    }

    void saveMaterialStockMaxCSV() {

        if (!existDB) {
            Map<String, StockVars> mapStockVar = TokenizerCSV.tokenizeStockMaxFile();

            //inicializa las variables de stock por producto
            for (Map.Entry<String, StockVars> entry : mapStockVar.entrySet()) {
                copaDB.getCollection(MaterialStock_Vars).insertOne(new Document(Consts.STOCK_MAX, entry.getValue().stockMax)
                        .append(Consts.MATERIALS_ID, entry.getKey())
                        .append(Consts.YEAR_MONTH_ID, yearMonth)
                        .append(Consts.STOCK_MIN, entry.getValue().stockMin)
                        .append(Consts.STOCK_SAFE, entry.getValue().safetyVar)
                        .append(Consts.STOCK_MULTIPLY, entry.getValue().multiplierSafetyVar));
            }
        }
    }

    //por trimestre se van sumando las salidas por producto
    void updateLessForTrimester(String materialID, int quantityLess) {

        Document materialDoc = copaDB.getCollection(MaterialsCollection).find(eq(Consts.MATERIALS_ID, materialID)).first();
        int lastUpdate = materialDoc.getInteger(Consts.LAST_UPDATE);

        Document materialQ = copaDB.getCollection(MaterialOutQuantityTrim).find(new Document(Consts.MATERIALS_ID,materialID)
                .append(Consts.YEAR_MONTH_ID, lastUpdate)).first();

        int newQuantity = materialQ.getInteger(Consts.QUANTITY) + quantityLess;
        copaDB.getCollection(MaterialOutQuantityTrim).updateOne(new Document(Consts.MATERIALS_ID,materialID)
                .append(Consts.YEAR_MONTH_ID, lastUpdate), new Document("$set", new Document(Consts.QUANTITY, newQuantity)));

    }

    List<Document> getMaterialsStockVars() {
        FindIterable<Document> iterable = copaDB.getCollection(MaterialsCollection).find();

        List<Document> result = new ArrayList<>();

        iterable.forEach(new Block<Document>() {
            @Override
            public void apply(final Document document) {
                String materialID = document.getString(Consts.MATERIALS_ID);
                int yearMonth = document.getInteger(Consts.LAST_UPDATE);
                Document matStockVar = copaDB.getCollection(MaterialStock_Vars).find(
                        new Document(Consts.MATERIALS_ID, materialID)
                                .append(Consts.YEAR_MONTH_ID, yearMonth)).first();
                result.add(matStockVar);
            }
        });

        return result;
    }

    int getMaterialQuantity(String materialID) {
        FindIterable<Document> iterable = copaDB.getCollection(MaterialsCollection).find(eq(Consts.MATERIALS_ID, materialID));
        Document matInfo = iterable.first();
        return matInfo.getInteger(Consts.QUANTITY);
    }

    private void recordTransaction(String user, String matName, int transDate, String destiny, String transType, double price, int quantity, int dueDate ) {
        copaDB.getCollection(TransactionRecords).insertOne(new Document(Consts.TRANSACTION_DATE, transDate)
                .append(Consts.MATERIALS_ID, matName)
                .append(Consts.DESTINY, destiny)
                .append(Consts.TRANSACTION_TYPE, transType)
                .append(Consts.PRICE, price)
                .append(Consts.QUANTITY, quantity)
                .append(Consts.USER, user)
                .append(Consts.DUE_DATE, dueDate));

    }

    //entradas
    void updateTransaction(String user, Material material) {
        recordTransaction(user, material.nameKey, material.materialInfo.buyDate, Consts.DESTINY_IN, Consts.TRANSACTION_TYPE_IN,
                material.materialInfo.price, material.materialInfo.quantity, material.materialInfo.dueDate);
    }

    //salidas
    private void updateTransaction(String user, String destiny, String materialID, int quantity, int dueDate, double price) {
        recordTransaction(user, materialID, yearMonthDay, destiny, Consts.TRANSACTION_TYPE_OUT, price, quantity, dueDate);
    }

    FindIterable<Document> getRecords(Document document) {

        return copaDB.getCollection(TransactionRecords).find(document)
                .sort(new Document(Consts.TRANSACTION_DATE, -1) );
    }

    void updateStockVars(String materialID, int stockMin, int safe, int multiplier) {

        Document first = copaDB.getCollection(MaterialsCollection).find(eq(Consts.MATERIALS_ID, materialID)).first();
        int lastUpdate = first.getInteger(Consts.LAST_UPDATE);

        if (stockMin != 0 && safe != 0 && multiplier != 0) {
            Document doc = new Document(Consts.STOCK_MIN, stockMin)
                    .append(Consts.STOCK_SAFE, safe)
                    .append(Consts.STOCK_MULTIPLY, multiplier);
            updateMaterialStock(materialID, lastUpdate, doc);

        } else if (stockMin != 0 && safe != 0) {
            Document doc = new Document(Consts.STOCK_MIN, stockMin)
                    .append(Consts.STOCK_SAFE, safe);
            updateMaterialStock(materialID, lastUpdate, doc);

        } else if (stockMin != 0 && multiplier != 0) {
            Document doc = new Document(Consts.STOCK_MIN, stockMin)
                    .append(Consts.STOCK_MULTIPLY, multiplier);
            updateMaterialStock(materialID, lastUpdate, doc);

        } else if (stockMin == 0 && safe != 0 && multiplier != 0) {
            Document doc = new Document(Consts.STOCK_SAFE, safe)
                    .append(Consts.STOCK_MULTIPLY, multiplier);
            updateMaterialStock(materialID, lastUpdate, doc);
        } else if (stockMin != 0 ) {
            Document doc = new Document(Consts.STOCK_MIN, stockMin);
            updateMaterialStock(materialID, lastUpdate, doc);

        } else if (safe != 0 ) {
            Document doc = new Document(Consts.STOCK_SAFE, safe);
            updateMaterialStock(materialID, lastUpdate, doc);

        } else if (multiplier != 0) {
            Document doc = new Document(Consts.STOCK_MULTIPLY, multiplier);
            updateMaterialStock(materialID, lastUpdate, doc);
        }
    }

    private void updateMaterialStock(String materialID, int lastUpdate, Document document) {
        copaDB.getCollection(MaterialStock_Vars).updateOne(new Document(Consts.MATERIALS_ID, materialID)
                .append(Consts.YEAR_MONTH_ID, lastUpdate), new Document("$set", document));
    }

    Document getStockVars(String materialID) {
        Document first = copaDB.getCollection(MaterialsCollection).find(new Document(Consts.MATERIALS_ID, materialID)).first();
        int lastUpdate = first.getInteger(Consts.LAST_UPDATE);

        return copaDB.getCollection(MaterialStock_Vars).find(new Document(Consts.MATERIALS_ID, materialID)
                .append(Consts.YEAR_MONTH_ID, lastUpdate)).first();
    }

    FindIterable<Document> getMaterialsID() {
        return copaDB.getCollection(MaterialsCollection).find();
    }

    void addNewMaterial(Material material) {
        List<Document> listDoc = new ArrayList<>();
        Document docInfo = new Document().append(Consts.QUANTITY, material.materialInfo.quantity)
                .append(Consts.PRICE, material.materialInfo.price)
                .append(Consts.DUE_DATE, material.materialInfo.dueDate)
                .append(Consts.TRANSACTION_DATE, material.materialInfo.buyDate);

        listDoc.add(docInfo);

        copaDB.getCollection(MaterialsCollection).insertOne(new Document(Consts.MATERIALS_ID, material.nameKey)
                .append(Consts.INFO, listDoc)
                .append(Consts.QUANTITY, material.materialInfo.quantity)
                .append(Consts.LAST_UPDATE, yearMonth));

        copaDB.getCollection(MaterialOutQuantityTrim).insertOne(new Document(Consts.MATERIALS_ID,  material.nameKey)
                .append(Consts.YEAR_MONTH_ID, yearMonth)
                .append(Consts.QUANTITY, 0));
    }

    void setUser(Document userDoc, Response response) {

        String userName = userDoc.getString(Consts.USER);

        Document toFind = new Document(Consts.USER, userName);
        Document userFind = copaDB.getCollection(USERS).find(toFind).first();

        boolean checkPass = (Boolean) userDoc.remove(Consts.CHECK_PASS);

        if( userFind != null && checkPass) {
            if (userDoc.getString(Consts.PASS).equals(userFind.getString(Consts.PASS))) {
                String pasNew = (String)userDoc.remove(Consts.PASS_NEW);
                userDoc.replace(Consts.PASS, pasNew);
                if (userName.equals(Consts.ADMIN)) {
                    copaDB.getCollection(USERS).updateOne(toFind, new Document("$set", new Document(Consts.PASS, pasNew)));
                } else {
                    copaDB.getCollection(USERS).replaceOne(toFind, userDoc);
                }
                response.status(200);
                response.body("Ok");
            } else {
                response.body("Error password");
                response.status(404);
            }
        } else if (userFind != null && !userName.equals(Consts.ADMIN) ) {
            userDoc.append(Consts.PASS, userFind.getString(Consts.PASS));
            copaDB.getCollection(USERS).replaceOne( toFind, userDoc);
            response.status(200);
            response.body("Ok");
        } else if (!userName.equals(Consts.ADMIN)) {
            copaDB.getCollection(USERS).insertOne(userDoc);
            response.status(200);
            response.body("Ok");
        } else {
            response.body("Master user cannot be setted");
            response.status(404);
        }
    }

    void login(String user, String pass, Response response) {

        Document userDoc = copaDB.getCollection(USERS).find(new Document(Consts.USER, user)
                .append(Consts.PASS, pass))
                .first();

        if ( userDoc != null ) {
            response.body("Ok.");
        } else {
            response.status(404);
            response.body("Usuario no existe o contraseña erronea.");
        }
    }

    void addProductoStockVars(String materialID, int stockMax, int stockMin, int safe, int multiplier) {

        copaDB.getCollection(MaterialStock_Vars).insertOne(new Document(Consts.STOCK_MAX, stockMax)
                .append(Consts.MATERIALS_ID, materialID)
                .append(Consts.YEAR_MONTH_ID, yearMonth)
                .append(Consts.STOCK_MIN, stockMin)
                .append(Consts.STOCK_SAFE, safe)
                .append(Consts.STOCK_MULTIPLY, multiplier));
    }

    FindIterable<Document> getUserID() {
        return copaDB.getCollection(USERS).find();
    }

    void getMaterialData(String name, double price, int dueDate, int buyDate, Response response) {

        Document matInfo = copaDB.getCollection(MaterialsCollection).find(new Document(Consts.MATERIALS_ID, name)).first();
        List<Document> docArray =  (List<Document>) matInfo.get(Consts.INFO);

        int i = 0;
        boolean end = false;
        Document docInfo = null;
        int maxCant = 0;

        while ( i < docArray.size() && !end ) {
            Document info = docArray.get(i);
            int dueDateProd = info.getInteger(Consts.DUE_DATE);
            double priceProd = info.getDouble(Consts.PRICE);
            int cant = info.getInteger(Consts.QUANTITY);
            int buyDateProd = info.getInteger(Consts.TRANSACTION_DATE);
            if (dueDateProd == dueDate && priceProd == price && buyDateProd == buyDate && cant > maxCant) {
                maxCant = cant;
                docInfo = info;
            } else if (dueDate < dueDateProd) {
                end = true;
            }

            i++;
        }

        if (docInfo != null) {
            JSONObject jsonO = new JSONObject();
            jsonO.put(Consts.MATERIALS_ID, name);
            jsonO.put(Consts.QUANTITY, docInfo.getInteger(Consts.QUANTITY));
            jsonO.put(Consts.PRICE, price);
            jsonO.put(Consts.DUE_DATE, dueDate);
            jsonO.put(Consts.TRANSACTION_DATE, buyDate);

            response.body(jsonO.toString());
        } else {
            response.status(404);
            response.body("Bad request");
        }

    }

    void deleteMaterialData(Material oldMat, Response response, boolean deletePerm) {

        Document matInfo = copaDB.getCollection(MaterialsCollection).find(new Document(Consts.MATERIALS_ID, oldMat.nameKey)).first();
        List<Document> docArray =  (List<Document>) matInfo.get(Consts.INFO);

        int i = 0;
        boolean end = false;
        Document docInfo = null;

        MaterialInfo oldMatInfo = oldMat.materialInfo;

        while ( i < docArray.size() && !end ) {
            Document info = docArray.get(i);
            int dueDateProd = info.getInteger(Consts.DUE_DATE);
            double priceProd = info.getDouble(Consts.PRICE);
            int cant = info.getInteger(Consts.QUANTITY);
            int buyDateProd = info.getInteger(Consts.TRANSACTION_DATE);
            if (dueDateProd == oldMatInfo.dueDate && priceProd == oldMatInfo.price && buyDateProd == oldMatInfo.buyDate && cant == oldMatInfo.quantity) {
                docInfo = info;
                end = true;
            } else if (oldMatInfo.dueDate < dueDateProd) {
                end = true;
            }

            i++;
        }

        docArray.remove(docInfo);

        if(docArray.size() != 0 || !deletePerm) {
            int totalQuantity = matInfo.getInteger(Consts.QUANTITY) - docInfo.getInteger(Consts.QUANTITY);

            UpdateResult result = copaDB.getCollection(MaterialsCollection).updateOne(new Document(Consts.MATERIALS_ID, oldMat.nameKey),
                    new Document("$set", new Document(Consts.INFO, docArray)
                            .append(Consts.QUANTITY, totalQuantity)));

            if (result.getMatchedCount() == 0 || !result.isModifiedCountAvailable()) {
                response.status(404);
                response.body("Bad request");
            } else {
                response.status(200);
                response.body("Ok");
            }
        } else {
            DeleteResult result = copaDB.getCollection(MaterialsCollection).deleteOne(eq(Consts.MATERIALS_ID, oldMat.nameKey));

            if (!result.wasAcknowledged()) {
                response.status(404);
                response.body("Bad request");
            } else {
                response.status(200);
                response.body("Ok");
            }
        }
    }

    void updateChanges(Material oldMat, Material newMat, String oldUser, String newUser) {

        MaterialInfo oldInfo = oldMat.materialInfo;
        MaterialInfo newInfo = newMat.materialInfo;

        copaDB.getCollection(Changes).insertOne(new Document(Consts.MATERIALS_ID, oldMat.nameKey)
                .append(Consts.QUANTITY,oldInfo.quantity)
                .append(Consts.TRANSACTION_DATE, oldInfo.buyDate)
                .append(Consts.DUE_DATE, oldInfo.dueDate)
                .append(Consts.PRICE, oldInfo.price)
                .append(Consts.USER, oldUser)

                .append(Consts.DESTINY, Consts.DESTINY_IN)                          //comun entre nuevo y viejo
                .append(Consts.TRANSACTION_TYPE, Consts.TRANSACTION_TYPE_CHANGE)    //comun entre nuevo y viejo

                .append(Consts.MATERIALS_IDnew, newMat.nameKey)
                .append(Consts.QUANTITYnew, newInfo.quantity)
                .append(Consts.TRANS_DATEnew, newInfo.buyDate)
                .append(Consts.DUE_DATEnew, newInfo.dueDate)
                .append(Consts.PRICEnew, newInfo.price)
                .append(Consts.USERnew, newUser));
    }

    FindIterable<Document> getChangeRecords(Document document) {
        return copaDB.getCollection(Changes).find(document);
    }

    boolean hasMaterial(String nameKey) {
        Document find = copaDB.getCollection(MaterialsCollection).find(new Document(Consts.MATERIALS_ID, nameKey)).first();
        return find != null;
    }

    void hasPermission(Document toFind, Response response) {
        Document found = copaDB.getCollection(USERS).find(toFind).first();
        if (found != null) {
            response.status(200);
            response.body("Aceptado");
        } else {
            response.status(411);
            response.body("rechazado");
        }
    }

    void DELETE() {
        copaDB.drop();
    }

    public void saveMasterUser() {

        if (!existDB) {
            Document masterUser = new Document(Consts.USER, Consts.ADMIN)
                    .append(Consts.PASS, Consts.PASS_ADMIN)
                    .append( Consts.PROP_PC_IN, true)
                    .append(Consts.PROP_EXTRACT, true)
                    .append(Consts.PROP_CONFIG_STOCK_VARS, true)
                    .append(Consts.PROP_ADD_LOGON, true)
                    .append(Consts.PROP_EDIT, true);
            copaDB.getCollection(USERS).insertOne(masterUser);
        }
    }

    public void addNewFurniture(Document newFernature) {
        Document info = new Document(Consts.N_MEMBER, newFernature.getInteger(Consts.N_MEMBER))
                .append(Consts.FINAL_PRICE, newFernature.remove(Consts.FINAL_PRICE))
                .append(Consts.BUY_DATE, newFernature.remove(Consts.BUY_DATE));
        copaDB.getCollection(NUM_MUEBLES).insertOne(info);
        copaDB.getCollection(FurnituresDBname).insertOne(newFernature);
    }


    public Document hasFurniture(Document furnitureSucNumber) {
        return copaDB.getCollection(FurnituresDBname).find(furnitureSucNumber).first();
    }

    public void addUpdateFurniture(Document parse) {
        copaDB.getCollection(FurnituresDBname).insertOne(parse);
    }

    public JSONArray getAllFurniture() {
        FindIterable<Document> furnitures = copaDB.getCollection(FurnituresDBname).find();
        JSONArray jsonArray = new JSONArray();

        furnitures.forEach(new Block<Document>() {
            @Override
            public void apply(Document doc) {
                Document info = getMemberInfo(doc.getInteger(Consts.N_MEMBER));
                doc.put(Consts.FINAL_PRICE, info.getDouble(Consts.FINAL_PRICE));
                doc.put(Consts.BUY_DATE, info.getInteger(Consts.BUY_DATE));
                JSONObject jsonObject = new JSONObject(doc.toJson());
                jsonArray.put(jsonObject);
            }
        });

        return jsonArray;
    }

    public Document getMemberInfo(int member) {
        return copaDB.getCollection(NUM_MUEBLES).find(eq(Consts.N_MEMBER, member)).first();
    }

    public JSONArray getFunitureNotUpdated() {

        int floorDate = CalendarWrapper.beforeTwo(yearMonthDay);

        FindIterable<Document> members = copaDB.getCollection(NUM_MUEBLES).find();
        JSONArray furnituresNotUpdated = new JSONArray();

        MongoCollection<Document> furnitures = copaDB.getCollection(FurnituresDBname);

        members.forEach(new Block<Document>() {
            @Override
            public void apply(Document doc) {
                int nMember = doc.getInteger(Consts.N_MEMBER);

                Document result = furnitures.find(eq(Consts.N_MEMBER, nMember)).sort(descending(Consts.LAST_UPDATE)).limit(1).first();

                if (result.getInteger(Consts.LAST_UPDATE) <= floorDate && result.getInteger(Consts.STATE) != Consts.STATE_OUT) {
                    furnituresNotUpdated.put(nMember);
                }
            }
        });
        return furnituresNotUpdated;
    }
}
