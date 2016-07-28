import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import spark.Response;

import java.util.List;

import static com.mongodb.client.model.Filters.*;

class RocksDBWrapper {

    final static String COPA_DB = "copa_db";

    final static String MaterialsCollection = "insumos";
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
            ArrayHashMap dataMapper = TokenizerCSV.tokenize();
            dataMapper.saveIntoDB(copaDB);
        }
    }

    public void updateLessMaterialDBkey(String materialID, int quantitySub, Response response) {

        FindIterable<Document> iterable = copaDB.getCollection(MaterialsCollection).find(eq(Consts.MATERIALS_ID, materialID));

        //obtengo toda la "info": [{"cantidad": 4,"fechavto":20140505,"precio": 21}, {...}, ..,]
        //unica entrada en el Document, busco por ID
        Document info = iterable.first();

        List<Document> docArray =  (List<Document>) info.get(Consts.INFO);

        int i = 0;
        boolean done = false;
        int newQuantityStock = 0;

        while ( i < docArray.size() && !done) {

            Document firstDueDate = docArray.get(i);

            newQuantityStock = firstDueDate.getInteger(Consts.QUANTITY) - quantitySub;

            if ( newQuantityStock <= 0) {
                docArray.remove(i);
                if (newQuantityStock == 0) {
                    done = true;
                } else {
                    quantitySub = -newQuantityStock;
                    i--;
                }
            } else {
                firstDueDate.put(Consts.QUANTITY, newQuantityStock);
                done = true;
            }

            i++;

        }

        UpdateResult result = null;

        if (newQuantityStock >= 0) {

            result = copaDB.getCollection(MaterialsCollection).updateOne(new Document(Consts.MATERIALS_ID, materialID),
                    new Document("$set", new Document(Consts.INFO, docArray)));
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
                new Document("$set", new Document(Consts.INFO, docArray)));

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
}
