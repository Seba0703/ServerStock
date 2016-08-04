import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import spark.Response;

import javax.print.Doc;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.*;

class RocksDBWrapper {

    final static String COPA_DB = "copa_db";

    final static String MaterialsCollection = "insumos";
    final static String MaterialStock_Vars = "stockMaxMat";
    final static String MaterialOutQuantityTrim = "salidasMatTrim";
    final static String TransactionRecords = "transacciones";
    final static String LastUpdate = "lastUpdate";

    final static String USERS = "usuarios";

    final static String FurnituresDBname = "muebles";

    private boolean existDB = false;
    private  MongoDatabase copaDB;

    RocksDBWrapper() {
        MongoClient mongoClient = new MongoClient();

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

        if (!existDB) {
            System.out.println("nueva base de datos");
            ArrayHashMap dataMapper = TokenizerCSV.tokenizeMaterialFile();
            dataMapper.saveIntoDB(copaDB);
        }
    }

    public void updateLessMaterialDBkey(String user, String destiny, String materialID, int quantitySub, Response response) {

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
            int price = firstDueDate.getInteger(Consts.PRICE);

            if ( newQuantityStock <= 0) {               // si es mas chico que cero ese lote quedo vacio y se va al siguiente
                updateTransaction(user,destiny,materialID,quantityLote,price);
                docArray.remove(i);
                if (newQuantityStock == 0) {
                    done = true;
                } else {
                    quantitySub = -newQuantityStock;
                    i--;
                }
            } else {
                updateTransaction(user,destiny,materialID,quantitySub,price);
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

        FindIterable<Document> iterableShow = copaDB.getCollection(MaterialsCollection).find(eq(Consts.MATERIALS_ID, materialID));
        iterableShow.forEach(new Block<Document>() {
            @Override
            public void apply(final Document document) {
                System.out.println(document);
            }
        });
    }

    public void updateAddMaterialDBkey(Material material, Response response) {

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
                addToDocList(docArray, index, materialInfo );
                found = true;
            }

            index++;
        }

        if (docArray.isEmpty()) {
            docArray.add(new Document().append(Consts.DUE_DATE, materialInfo.dueDate)
                    .append(Consts.PRICE, materialInfo.price)
                    .append(Consts.QUANTITY, materialInfo.quantity));
        }

        UpdateResult result = copaDB.getCollection(MaterialsCollection).updateOne(new Document(Consts.MATERIALS_ID, material.nameKey),
                new Document("$set", new Document(Consts.INFO, docArray)
                        .append(Consts.QUANTITY, totalQuantity)));

        if ( result.getMatchedCount() == 0  || !result.isModifiedCountAvailable()) {
            response.status(404);
            response.body("Bad request");
        } else {
            response.status(200);
            response.body("Ok");
        }

        FindIterable<Document> iterableShow = copaDB.getCollection(MaterialsCollection).find(eq(Consts.MATERIALS_ID, material.nameKey));
        iterableShow.forEach(new Block<Document>() {
            @Override
            public void apply(final Document document) {
                System.out.println(document);
            }
        });
    }

    private void addToDocList(List<Document> docArray, int index, MaterialInfo materialInfo) {
        docArray.add(index + 1, new Document().append(Consts.DUE_DATE, materialInfo.dueDate)
                .append(Consts.PRICE, materialInfo.price)
                .append(Consts.QUANTITY, materialInfo.quantity));
    }

    private boolean lastSameDate(List<Document> docArray, int i, MaterialInfo info) {
        return docArray.get(i).getInteger(Consts.DUE_DATE) == info.dueDate && docArray.get(i + 1).getInteger(Consts.DUE_DATE) != info.dueDate;
    }


    public void savePredictionStockThreeMonths() {

        Calendar date = Calendar.getInstance();
        int newYearMonth = Integer.parseInt( date.get(Calendar.YEAR) + Common.getRealMonth(date.get(Calendar.MONTH)));

        FindIterable<Document> iterable = copaDB.getCollection(LastUpdate).find();
        Document first = iterable.first();
        int lastUpdate = first.getInteger(Consts.LAST_UPDATE);

        if ( newYearMonth >= Common.nextTrimestrer(lastUpdate) ) {

            // actualiza la fecha de ultima actualizacion de cantidad de insumos en el ultimo trimestre.
            copaDB.getCollection(LastUpdate).updateOne(new Document(Consts.UPDATE_ID, Consts.UP_ID),
                    new Document("$set", new Document(Consts.LAST_UPDATE, newYearMonth)));

            FindIterable<Document> iterableMat = copaDB.getCollection(MaterialOutQuantityTrim).find(new Document(Consts.YEAR_MONTH_ID, lastUpdate));

            // se itera por cada insumo, y se guarda en la base de datos DataStockMonthly el stock que se tiene
            // en ese mes para ese insumo.
            iterableMat.forEach(new Block<Document>() {
                @Override
                public void apply(final Document document) {

                    String materialID = document.getString(Consts.MATERIALS_ID);
                    int quantityOut = document.getInteger(Consts.QUANTITY);

                    FindIterable<Document> it = copaDB.getCollection(MaterialStock_Vars).find(new Document(Consts.MATERIALS_ID, materialID)
                            .append(Consts.YEAR_MONTH_ID, lastUpdate));

                    Document materialStockVar = it.first();
                    StockVars stockVars = new StockVars();
                    stockVars.safetyVar = materialStockVar.getInteger(Consts.STOCK_SAFE);
                    stockVars.multiplierSafetyVar = materialStockVar.getInteger(Consts.STOCK_MULTIPLY);
                    stockVars.stockMax = materialStockVar.getInteger(Consts.STOCK_MAX);
                    stockVars.stockMin = materialStockVar.getInteger(Consts.STOCK_MIN);

                    int predictionMaxStock = Common.makeTrimesterPrediction(stockVars,quantityOut);

                    copaDB.getCollection(MaterialStock_Vars).insertOne(new Document(Consts.STOCK_MAX, predictionMaxStock)
                            .append(Consts.MATERIALS_ID, materialID)
                            .append(Consts.YEAR_MONTH_ID, newYearMonth)
                            .append(Consts.STOCK_MIN, stockVars.stockMin)
                            .append(Consts.STOCK_SAFE, stockVars.safetyVar)
                            .append(Consts.STOCK_MULTIPLY, stockVars.multiplierSafetyVar));
                }
            });
        }
    }

    public void saveMaterialStockMax() {

        if (!existDB) {
            Map<String, StockVars> mapStockVar = TokenizerCSV.tokenizeStockMaxFile();

            Calendar date = Calendar.getInstance();
            int newYearMonth = Integer.parseInt( date.get(Calendar.YEAR) + Common.getRealMonth(date.get(Calendar.MONTH)));

            //inicializa la fecha de ultima actualizacion
            copaDB.getCollection(LastUpdate).insertOne( new Document(Consts.UPDATE_ID,Consts.UP_ID)
                    .append(Consts.LAST_UPDATE,newYearMonth));

            //inicializa las variables de stock por producto
            for (Map.Entry<String, StockVars> entry : mapStockVar.entrySet()) {
                copaDB.getCollection(MaterialStock_Vars).insertOne(new Document(Consts.STOCK_MAX, entry.getValue().stockMax)
                        .append(Consts.MATERIALS_ID, entry.getKey())
                        .append(Consts.YEAR_MONTH_ID, newYearMonth)
                        .append(Consts.STOCK_MIN, entry.getValue().stockMin)
                        .append(Consts.STOCK_SAFE, entry.getValue().safetyVar)
                        .append(Consts.STOCK_MULTIPLY, entry.getValue().multiplierSafetyVar));
            }
        }
    }

    //por trimestre se van sumando las salidas por producto
    public void updateLessForTrimester(String materialID, int quantityLess) {

        FindIterable<Document> iterable = copaDB.getCollection(LastUpdate).find();
        Document first = iterable.first();
        int lastUpdate = first.getInteger(Consts.LAST_UPDATE);

        FindIterable<Document> iterableMat = copaDB.getCollection(MaterialOutQuantityTrim).find(new Document(Consts.MATERIALS_ID,materialID)
                .append(Consts.YEAR_MONTH_ID, lastUpdate));
        Document materialQ = iterableMat.first();
        int newQuantity = materialQ.getInteger(Consts.QUANTITY) + quantityLess;
        copaDB.getCollection(MaterialOutQuantityTrim).updateOne(new Document(Consts.MATERIALS_ID,materialID)
                .append(Consts.YEAR_MONTH_ID, lastUpdate), new Document("$set", new Document(Consts.QUANTITY,newQuantity)));

    }

    public void initializeMaterialQuantityTrim() {

        Calendar date = Calendar.getInstance();
        int newYearMonth = Integer.parseInt( date.get(Calendar.YEAR) + Common.getRealMonth(date.get(Calendar.MONTH)));

        FindIterable<Document> iterable = copaDB.getCollection(LastUpdate).find();
        Document first = iterable.first();
        int lastUpdate = first.getInteger(Consts.LAST_UPDATE);

        if ( !((lastUpdate <= newYearMonth) && (newYearMonth <= Common.nextTrimestrer(lastUpdate))) ) {
            FindIterable<Document> iterableMat = copaDB.getCollection(MaterialsCollection).find();

            iterableMat.forEach(new Block<Document>() {
                @Override
                public void apply(final Document document) {

                    String materialID = document.getString(Consts.MATERIALS_ID);

                    copaDB.getCollection(MaterialOutQuantityTrim).insertOne(new Document(Consts.MATERIALS_ID, materialID)
                            .append(Consts.YEAR_MONTH_ID, newYearMonth)
                            .append(Consts.QUANTITY, 0));
                }
            });
        }
    }

    public FindIterable<Document> getMaterialsStockVars() {
        FindIterable<Document> iterable = copaDB.getCollection(LastUpdate).find();
        Document first = iterable.first();
        int lastUpdate = first.getInteger(Consts.LAST_UPDATE);
        return copaDB.getCollection(MaterialStock_Vars).find(new Document(Consts.YEAR_MONTH_ID, lastUpdate));
    }

    public int getMaterialQuantity(String materialID) {
        FindIterable<Document> iterable = copaDB.getCollection(MaterialsCollection).find(new Document(Consts.MATERIALS_ID, materialID));
        Document matInfo = iterable.first();
        return matInfo.getInteger(Consts.QUANTITY);
    }

    private void recordTransaction(String matName, int transDate, String destiny, String transType, int price, int quantity ) {
        copaDB.getCollection(TransactionRecords).insertOne(new Document(Consts.TRANSACTION_DATE, transDate)
                .append(Consts.MATERIALS_ID,matName)
                .append(Consts.DESTINY, destiny)
                .append(Consts.TRANSACTION_TYPE, transType)
                .append(Consts.PRICE, price)
                .append(Consts.QUANTITY, quantity));

    }

    //entradas
    public void updateTransaction(Material material, int transactionDate) {
        recordTransaction(material.nameKey,transactionDate,Consts.DESTINY_IN,Consts.TRANSACTION_TYPE_IN,material.materialInfo.price,material.materialInfo.quantity);
    }

    //salidas
    private void updateTransaction(String user, String destiny, String materialID, int quantity, int price) {
        Calendar calendar = Calendar.getInstance();
        String date = calendar.get(Calendar.YEAR) + Common.getRealMonth(calendar.get(Calendar.MONTH)) + Common.getRealDay(calendar.get(Calendar.DAY_OF_MONTH));
        recordTransaction(materialID,Integer.parseInt(date),destiny,Consts.TRANSACTION_TYPE_OUT,price,quantity);
    }

    public FindIterable<Document> getRecords(String materialID, String transType, String destiny, int fromDate, int toDate) {

        return copaDB.getCollection(TransactionRecords).find(new Document(Consts.MATERIALS_ID,materialID)
                .append(Consts.TRANSACTION_TYPE, transType)
                .append(Consts.DESTINY, destiny)
                .append(Consts.TRANSACTION_DATE, new Document("$lte", toDate))
                .append(Consts.TRANSACTION_DATE, new Document("$gte", fromDate)))
                .sort(new Document(Consts.TRANSACTION_DATE, -1));

    }

    public FindIterable<Document> getRecords(String transType, String destiny, int fromDate, int toDate) {
        return copaDB.getCollection(TransactionRecords).find(new Document(Consts.TRANSACTION_TYPE, transType)
                .append(Consts.DESTINY, destiny)
                .append(Consts.TRANSACTION_DATE, new Document("$lte", toDate))
                .append(Consts.TRANSACTION_DATE, new Document("$gte", fromDate)))
                .sort(new Document(Consts.TRANSACTION_DATE, -1) );
    }

    public void updateStockVars(String materialID, int stockMin, int safe, int multiplier) {

        FindIterable<Document> iterable = copaDB.getCollection(LastUpdate).find();
        Document first = iterable.first();
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

    public void getStockVars(String materialID, StockVars stockVars) {

        Document stockVarDoc = copaDB.getCollection(MaterialStock_Vars).find(new Document(Consts.MATERIALS_ID, materialID)).first();

        stockVars.stockMin = stockVarDoc.getInteger(Consts.STOCK_MIN);
        stockVars.multiplierSafetyVar = stockVarDoc.getInteger(Consts.STOCK_MULTIPLY);
        stockVars.safetyVar = stockVarDoc.getInteger(Consts.STOCK_SAFE);
    }

    public FindIterable<Document> getMaterialsID() {
        return copaDB.getCollection(MaterialsCollection).find();
    }

    public void addNewMaterial(Material material) {
        copaDB.getCollection(MaterialsCollection).insertOne(new Document(Consts.MATERIALS_ID, material.nameKey)
                .append(Consts.QUANTITY, material.materialInfo.quantity)
                .append(Consts.PRICE, material.materialInfo.price)
                .append(Consts.DUE_DATE, material.materialInfo.dueDate));
    }

    public void logon(String user, String pass, Response response) {

        Document userDoc = copaDB.getCollection(USERS).find(new Document(Consts.USER, user)).first();

        if ( !userDoc.isEmpty() ) {
            response.status(404);
            response.body("Usuario existente.");
        } else {
            copaDB.getCollection(USERS).insertOne(new Document(Consts.USER,user)
                    .append(Consts.PASS, pass));
            response.body("Ok.");
        }
    }

    public void login(String user, String pass, Response response) {

        Document userDoc = copaDB.getCollection(USERS).find(new Document(Consts.USER, user)
                .append(Consts.PASS, pass))
                .first();

        if ( !userDoc.isEmpty() ) {
            response.body("Ok.");
        } else {
            response.status(404);
            response.body("Usuario no existe o contraseña erronea.");
        }


    }
}
