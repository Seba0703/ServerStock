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

        //vencimiento mas proximo
        Document firstDueDate = docArray.get(0);

        int newQuantityStock = firstDueDate.getInteger(Consts.QUANTITY) - quantitySub;

        if (newQuantityStock > 0) {
            firstDueDate.put(Consts.QUANTITY, newQuantityStock);
        }else {
            docArray.remove(0);
        }

        UpdateResult result = copaDB.getCollection(MaterialsCollection).updateOne(new Document(Consts.MATERIALS_ID, materialID),
                new Document("$set", new Document(Consts.INFO, docArray)));


        if ( result.getMatchedCount() == 0  || !result.isModifiedCountAvailable()) {
            response.status(404);
            response.body("Bad request");
        } else {
            response.status(200);
            response.body("Ok");
        }

    }

    public void updateAddMaterialDBkey(Material material, Response response) {

        FindIterable<Document> iterable = copaDB.getCollection(MaterialsCollection).find(eq(Consts.MATERIALS_ID, material.nameKey));

        //obtengo toda la "info": [{"cantidad": 4,"fechavto":20140505,"precio": 21}, {...}, ..,]
        //unica entrada en el Document, busco por ID
        Document info = iterable.first();
        List<Document> docArray =  (List<Document>) info.get(Consts.INFO);




    }

}
