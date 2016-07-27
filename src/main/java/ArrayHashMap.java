import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.*;


public class ArrayHashMap {

    private Map<String, List<MaterialInfo>> dataMapper = null;



    public ArrayHashMap() {
        dataMapper = new HashMap<>();
    }

    public void put(String key, MaterialInfo value) {

        List<MaterialInfo> dataList;

        if (dataMapper.containsKey(key)) {
            dataList = dataMapper.get(key);
        } else {
            dataList = new ArrayList<>();
        }

        dataList.add(value);

        if (!dataMapper.containsKey(key)) {
            dataMapper.put(key,dataList);
        }

    }

    public List<MaterialInfo> get(String key) {
        return dataMapper.get(key);
    }

    public void sortAllLists() {
        for (Map.Entry<String,  List<MaterialInfo>> entry : dataMapper.entrySet()) {
            Collections.sort(entry.getValue());
        }
    }

    public void saveIntoDB(MongoDatabase db) {

        for (Map.Entry<String,  List<MaterialInfo>> entry : dataMapper.entrySet()) {

            List<Document> docList = toDocumentList(entry.getValue());

            db.getCollection(RocksDBWrapper.MaterialsCollection).insertOne(new Document(Consts.INFO, docList)
                    .append(Consts.MATERIALS_ID,entry.getKey()));

        }
    }

    private List<Document> toDocumentList(List<MaterialInfo> list) {

        List<Document> infoList = new ArrayList<>();

        for (MaterialInfo info : list) {

            Document infoDoc = new Document().append(Consts.DUE_DATE, info.dueDate)
                                            .append(Consts.PRICE, info.price)
                                            .append(Consts.QUANTITY, info.quantity);
            infoList.add(infoDoc);
        }

        return infoList;

    }

}
