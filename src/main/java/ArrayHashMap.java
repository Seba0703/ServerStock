import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.*;


public class ArrayHashMap {

    private Map<String, MaterialInfoList> dataMapper = null;

    public ArrayHashMap() {
        dataMapper = new HashMap<>();
    }

    public void put(String key, MaterialInfo value) {

        MaterialInfoList materialInfoList;

        if (dataMapper.containsKey(key)) {
            materialInfoList = dataMapper.get(key);
        } else {
            materialInfoList = new MaterialInfoList();
        }

        materialInfoList.add(value);

        if (!dataMapper.containsKey(key)) {
            dataMapper.put(key,materialInfoList);
        }

    }

    public List<MaterialInfo> get(String key) {
        return dataMapper.get(key).getList();
    }

    public void sortAllLists() {
        for (Map.Entry<String,  MaterialInfoList> entry : dataMapper.entrySet()) {
            Collections.sort(entry.getValue().getList());
        }
    }

    public void saveIntoDB(MongoDatabase db) {

        Calendar date = Calendar.getInstance();
        String yearMonth = date.get(Calendar.YEAR) + Common.getRealMonth(date.get(Calendar.MONTH));

        for (Map.Entry<String,  MaterialInfoList> entry : dataMapper.entrySet()) {

            List<Document> docList = toDocumentList(entry.getValue().getList());

            db.getCollection(RocksDBWrapper.MaterialsCollection).insertOne(new Document(Consts.INFO, docList)
                    .append(Consts.MATERIALS_ID, entry.getKey())
                    .append(Consts.QUANTITY, entry.getValue().getQuantity()));

            //inicializa las salidas de los productos por AñoMes
            db.getCollection(RocksDBWrapper.MaterialOutQuantityTrim).insertOne(new Document(Consts.MATERIALS_ID, entry.getKey())
                    .append(Consts.YEAR_MONTH_ID, yearMonth)
                    .append(Consts.QUANTITY, 0));

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
