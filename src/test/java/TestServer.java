import Wrappers.CalendarWrapper;
import org.bson.Document;
import org.json.JSONArray;
import org.junit.Test;
import spark.Response;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestServer {

    @Test
    public void ArrayHashMapTest() {
        ArrayHashMap dataMapper = TokenizerCSV.tokenizeMaterialFile();

        List<MaterialInfo> list = new ArrayList<>(dataMapper.get("JERINGA"));

        assertTrue(list.get(0).dueDate == 20161208);
        assertTrue(list.get(1).dueDate == 20171008);
        assertTrue(list.get(2).dueDate == 20180708);
        assertTrue(list.get(3).dueDate == 20181208);
        assertTrue(list.get(4).dueDate == 20201008);
    }

    @Test
    public void stockVarsAddNew_update_OK() {
        MongoDBWrapper db = new MongoDBWrapper(new CalendarWrapper());
        db.DELETE();
        Material calcu = new Material();
        String materialID = "CALCULADORA";
        calcu.add(materialID);
        calcu.add("100"); calcu.add("20201231"); calcu.add("2.9931"); calcu.add("20160802");
        db.addNewMaterial(calcu);
        db.addProductoStockVars(materialID, 300, 100, 30, 2);
        Document doc = db.getStockVars(materialID);

        assertTrue(doc.getInteger(Consts.STOCK_MIN) == 100);
        assertTrue(doc.getInteger(Consts.STOCK_MAX) == 300);
        assertTrue(doc.getInteger(Consts.STOCK_SAFE) == 30);
        assertTrue(doc.getInteger(Consts.STOCK_MULTIPLY) == 2);

        db.updateStockVars(materialID, 50, 15, 3);

        Document docUpdate = db.getStockVars(materialID);

        assertTrue(docUpdate.getInteger(Consts.STOCK_MIN) == 50);
        assertTrue(docUpdate.getInteger(Consts.STOCK_SAFE) == 15);
        assertTrue(docUpdate.getInteger(Consts.STOCK_MULTIPLY) == 3);

    }

    @Test
    public void lessStock() {
        MongoDBWrapper db = new MongoDBWrapper(new CalendarWrapper());
        db.DELETE();
        Response response = new MockResponse();
        Material calcu = new Material();
        String materialID = "CALCULADORA";
        calcu.add(materialID);
        calcu.add("100"); calcu.add("20201231"); calcu.add("2.9931"); calcu.add("20160802");
        db.addNewMaterial(calcu);
        int before = db.getMaterialQuantity(materialID);
        int quantityLess = 30;
        db.updateLessMaterialDBkey("GUELY", "SUCURSAL2", materialID, quantityLess, response);
        int quantity = db.getMaterialQuantity(materialID);
        assertTrue(quantity == before - quantityLess);
    }

    void realTimeInit(String materialID, int stockActual, int maxStock, int minStock, int safeStock, int multiply) {
        MongoDBWrapper db = new MongoDBWrapper(new CalendarWrapper());
        db.DELETE();

        Material calcu = new Material();
        calcu.add(materialID);
        calcu.add("" +stockActual); calcu.add("20201231"); calcu.add("2.9931"); calcu.add("20160802");
        db.addNewMaterial(calcu);
        db.addProductoStockVars(materialID, maxStock, minStock, safeStock, multiply);
    }

    @Test // no se vendio nada maxStock = 300 tengo = 100
    public void bajarMuchoStockMax() {
        String materialID = "CALCULADORA";
        int stockMax = 300;
        int stockMin = 100;
        int actualStock = 100;
        int safeStock = 50;
        int multiply = 2;

        realTimeInit(materialID, actualStock, stockMax, stockMin, safeStock, multiply);

        CalendarMock calendar = new CalendarMock();
        calendar.set(31, 12, 2016);

        MongoDBWrapper db = new MongoDBWrapper(calendar);

        //ANTES de actualizar los stocks maximos
        int preStockMax = db.getStockVars(materialID).getInteger(Consts.STOCK_MAX);
        db.savePredictionStockThreeMonths();
        //DESPUES de actualizar los stocks maximos
        int postStockMax = db.getStockVars(materialID).getInteger(Consts.STOCK_MAX);

        assertTrue(postStockMax == preStockMax - safeStock * multiply);

    }

    @Test
    public void subirUnPocoStockMax() {
        String materialID = "CALCULADORA";

        int stockMax = 300;
        int stockMin = 50;
        int actualStock = 240;
        int safeStock = 50;
        int multiply = 2;

        realTimeInit(materialID, actualStock, stockMax, stockMin, safeStock, multiply);
        int quantityLess = 220;
        lessInRealTime(quantityLess, materialID);

        CalendarMock calendar = new CalendarMock();
        calendar.set(31, 12, 2016);

        MongoDBWrapper db = new MongoDBWrapper(calendar);

        //ANTES de actualizar los stocks maximos
        int preStockMax = db.getStockVars(materialID).getInteger(Consts.STOCK_MAX);
        db.savePredictionStockThreeMonths();
        //DESPUES de actualizar los stocks maximos
        int postStockMax = db.getStockVars(materialID).getInteger(Consts.STOCK_MAX);

        assertTrue(postStockMax == preStockMax + safeStock);

    }

    @Test
    public void bajaUnpocoStockMax() {
        String materialID = "CALCULADORA";

        int stockMax = 300;
        int stockMin = 50;
        int actualStock = 100;
        int safeStock = 50;
        int multiply = 2;

        realTimeInit(materialID, actualStock, stockMax, stockMin, safeStock, multiply);
        int quantityLess = 100;
        lessInRealTime(quantityLess, materialID);

        CalendarMock calendar = new CalendarMock();
        calendar.set(31, 12, 2016);

        MongoDBWrapper db = new MongoDBWrapper(calendar);

        //ANTES de actualizar los stocks maximos
        int preStockMax = db.getStockVars(materialID).getInteger(Consts.STOCK_MAX);
        db.savePredictionStockThreeMonths();
        //DESPUES de actualizar los stocks maximos
        int postStockMax = db.getStockVars(materialID).getInteger(Consts.STOCK_MAX);

        assertTrue(postStockMax == preStockMax - safeStock);

    }

    void lessInRealTime(int quantityLess, String materialID) {

        MongoDBWrapper db = new MongoDBWrapper(new CalendarWrapper());
        Response response = new MockResponse();
        db.updateLessMaterialDBkey("GUELY", "SUCURSAL2", materialID, quantityLess, response);
        db.updateLessForTrimester(materialID, quantityLess);
    }


    @Test
    public void subirMuchoStockMax() {
        String materialID = "CALCULADORA";

        int stockMax = 300;
        int stockMin = 50;
        int actualStock = 280;
        int safeStock = 50;
        int multiply = 2;

        realTimeInit(materialID, actualStock, stockMax, stockMin, safeStock, multiply);

        int quantityLess = 270;
        lessInRealTime(quantityLess, materialID);

        CalendarMock calendar = new CalendarMock();
        calendar.set(31, 12, 2016);

        MongoDBWrapper db = new MongoDBWrapper(calendar);

        //ANTES de actualizar los stocks maximos
        int preStockMax = db.getStockVars(materialID).getInteger(Consts.STOCK_MAX);
        System.out.println(preStockMax);
        db.savePredictionStockThreeMonths();
        //DESPUES de actualizar los stocks maximos
        int postStockMax = db.getStockVars(materialID).getInteger(Consts.STOCK_MAX);
        System.out.println(postStockMax);

        assertTrue(postStockMax == preStockMax + safeStock * multiply);

    }

    @Test
    public void quedaIgualStockMax() {
        String materialID = "CALCULADORA";

        int stockMax = 300;
        int stockMin = 50;
        int actualStock = 240;
        int safeStock = 50;
        int multiply = 2;

        realTimeInit(materialID, actualStock, stockMax, stockMin, safeStock, multiply);
        int quantityLess = 170;
        lessInRealTime(quantityLess, materialID);

        CalendarMock calendar = new CalendarMock();
        calendar.set(31, 12, 2016);

        MongoDBWrapper db = new MongoDBWrapper(calendar);

        //ANTES de actualizar los stocks maximos
        int preStockMax = db.getStockVars(materialID).getInteger(Consts.STOCK_MAX);
        db.savePredictionStockThreeMonths();
        //DESPUES de actualizar los stocks maximos
        int postStockMax = db.getStockVars(materialID).getInteger(Consts.STOCK_MAX);

        assertTrue(postStockMax == preStockMax);
    }

    @Test
    public void amarilloLista() {
        String materialID = "CALCULADORA";

        int stockMax = 300;
        int stockMin = 50;
        int actualStock = 240;
        int safeStock = 50;
        int multiply = 2;

        realTimeInit(materialID, actualStock, stockMax, stockMin, safeStock, multiply);
        int quantityLess = 170;
        lessInRealTime(quantityLess, materialID);

        MongoDBWrapper db = new MongoDBWrapper(new CalendarWrapper());

        Document doc = db.getMaterialsStockVars().get(0);

        int stockMinInDB = doc.getInteger(Consts.STOCK_MIN);
        int safetyVarinDB = doc.getInteger(Consts.STOCK_SAFE);

        int materialStock = db.getMaterialQuantity(materialID);

        int result;
        if (materialStock >= stockMinInDB + safetyVarinDB) {
            result = Consts.WHITE;
        } else if ( materialStock <  stockMinInDB + safetyVarinDB && materialStock >= stockMinInDB) {
            result = Consts.YELLOW;
        } else {
            result = Consts.RED;
        }

        assertTrue(result == Consts.YELLOW);
    }

    @Test
    public void updateTransaction() {
        String materialID = "CALCULADORA";

        int stockMax = 300;
        int stockMin = 50;
        int actualStock = 240;
        int safeStock = 50;
        int multiply = 2;

        MongoDBWrapper db = new MongoDBWrapper(new CalendarWrapper());
        db.DELETE();

        Material calcu = new Material();
        calcu.add(materialID);
        calcu.add("" + actualStock); calcu.add("20201231"); calcu.add("2.9931"); calcu.add("20160802");
        db.addNewMaterial(calcu);
        db.addProductoStockVars(materialID, stockMax, stockMin, safeStock, multiply);

        String user = "GUELY";

        db.updateTransaction(user, calcu);

        Document doc = db.getRecords(new Document(Consts.TRANSACTION_TYPE, Consts.TRANSACTION_TYPE_IN)).first();

        assertTrue(doc.getString(Consts.MATERIALS_ID).equals(materialID));
        assertTrue(doc.getString(Consts.DESTINY).equals(Consts.DESTINY_IN));
        assertTrue(doc.getString(Consts.USER).equals(user));
    }

    @Test
    public void testNoneUpdated() {
        MongoDBWrapper db = new MongoDBWrapper(new CalendarWrapper());
        db.DELETE();

        Document doc = new Document(Consts.N_SUC, 1)
                .append(Consts.N_MEMBER, 121)
                .append(Consts.STATE, Consts.STATE_GOOD)
                .append(Consts.BUY_DATE, 20160202)
                .append(Consts.LAST_UPDATE, 20160203)
                .append(Consts.FINAL_PRICE, 25);
        Document doc2 = new Document(Consts.N_SUC, 1)
                .append(Consts.N_MEMBER, 1)
                .append(Consts.STATE, Consts.STATE_GOOD)
                .append(Consts.BUY_DATE, 20160202)
                .append(Consts.LAST_UPDATE, 20160203)
                .append(Consts.FINAL_PRICE, 25);
        Document doc3 = new Document(Consts.N_SUC, 1)
                .append(Consts.N_MEMBER, 2)
                .append(Consts.STATE, Consts.STATE_BAD)
                .append(Consts.BUY_DATE, 20160202)
                .append(Consts.LAST_UPDATE, 20160203)
                .append(Consts.FINAL_PRICE, 25);
        db.addNewFurniture(doc);
        db.addNewFurniture(doc2);
        db.addNewFurniture(doc3);

        JSONArray array = db.getFunitureNotUpdated();
        db.DELETE();

        assertTrue(array.length() == 3);
    }

    @Test
    public void testTwoNotUpdatedAndOneBadUpdated() {
        CalendarMock s = new CalendarMock();
        MongoDBWrapper db = new MongoDBWrapper(s);
        db.DELETE();

        Document doc = new Document(Consts.N_SUC, 1)
                .append(Consts.N_MEMBER, 121)
                .append(Consts.STATE, Consts.STATE_GOOD)
                .append(Consts.BUY_DATE, 20160202)
                .append(Consts.LAST_UPDATE, 20160203)
                .append(Consts.FINAL_PRICE, 25);
        Document doc2 = new Document(Consts.N_SUC, 1)
                .append(Consts.N_MEMBER, 1)
                .append(Consts.STATE, Consts.STATE_GOOD)
                .append(Consts.BUY_DATE, 2016020)
                .append(Consts.LAST_UPDATE, 20160203)
                .append(Consts.FINAL_PRICE, 25);
        Document doc3 = new Document(Consts.N_SUC, 1)
                .append(Consts.N_MEMBER, 2)
                .append(Consts.STATE, Consts.STATE_BAD)
                .append(Consts.BUY_DATE, 20160202)
                .append(Consts.LAST_UPDATE, 20161103)
                .append(Consts.FINAL_PRICE, 25);
        db.addNewFurniture(doc);
        db.addNewFurniture(doc2);
        db.addNewFurniture(doc3);

        JSONArray array = db.getFunitureNotUpdated();
        db.DELETE();

        assertTrue(array.length() == 2);
    }

    @Test
    public void testOneUpdateOneOutOneNotUpdate() {
        CalendarMock s = new CalendarMock();
        MongoDBWrapper db = new MongoDBWrapper(s);
        db.DELETE();

        Document doc = new Document(Consts.N_SUC, 1)
                .append(Consts.N_MEMBER, 121)
                .append(Consts.STATE, Consts.STATE_GOOD)
                .append(Consts.BUY_DATE, 20160202)
                .append(Consts.LAST_UPDATE, 20160203)
                .append(Consts.FINAL_PRICE, 25);
        Document doc2 = new Document(Consts.N_SUC, 1)
                .append(Consts.N_MEMBER, 1)
                .append(Consts.STATE, Consts.STATE_GOOD)
                .append(Consts.BUY_DATE, 20160202)
                .append(Consts.LAST_UPDATE, 20161103)
                .append(Consts.FINAL_PRICE, 25);
        Document doc3 = new Document(Consts.N_SUC, 1)
                .append(Consts.N_MEMBER, 2)
                .append(Consts.STATE, Consts.STATE_OUT)
                .append(Consts.BUY_DATE, 20160202)
                .append(Consts.LAST_UPDATE, 20160203)
                .append(Consts.FINAL_PRICE, 25);
        db.addNewFurniture(doc);
        db.addNewFurniture(doc2);
        db.addNewFurniture(doc3);

        JSONArray array = db.getFunitureNotUpdated();
        db.DELETE();

        assertTrue(array.length() == 1);
    }

    @Test
    public void testTwoUpdateAndOneOUT() {
        CalendarMock s = new CalendarMock();
        MongoDBWrapper db = new MongoDBWrapper(s);
        db.DELETE();

        Document doc = new Document(Consts.N_SUC, 1)
                .append(Consts.N_MEMBER, 121)
                .append(Consts.STATE, Consts.STATE_GOOD)
                .append(Consts.BUY_DATE, 20160202)
                .append(Consts.LAST_UPDATE, 20160203)
                .append(Consts.FINAL_PRICE, 25);
        Document doc2 = new Document(Consts.N_SUC, 1)
                .append(Consts.N_MEMBER, 1)
                .append(Consts.STATE, Consts.STATE_GOOD)
                .append(Consts.BUY_DATE, 20160202)
                .append(Consts.LAST_UPDATE, 20161103)
                .append(Consts.FINAL_PRICE, 25);
        Document doc3 = new Document(Consts.N_SUC, 1)
                .append(Consts.N_MEMBER, 2)
                .append(Consts.STATE, Consts.STATE_OUT)
                .append(Consts.BUY_DATE, 20160202)
                .append(Consts.LAST_UPDATE, 20160203)
                .append(Consts.FINAL_PRICE, 25);
        db.addNewFurniture(doc);
        db.addNewFurniture(doc2);
        db.addNewFurniture(doc3);
        Document doc4Up = new Document(Consts.N_SUC, 1)
                .append(Consts.N_MEMBER, 121)
                .append(Consts.STATE, Consts.STATE_REGULAR)
                .append(Consts.LAST_UPDATE, 20161103);
        db.addUpdateFurniture(doc4Up);

        JSONArray array = db.getFunitureNotUpdated();
        db.DELETE();

        assertTrue(array.length() == 0);
    }


}
