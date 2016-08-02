
public class Common {

    public static int nextTrimestrer(int yearMonth) {
        String yearMonthS = String.valueOf(yearMonth);
        int month = Integer.parseInt(yearMonthS.substring(4));
        month += 3;

        return Integer.parseInt(yearMonthS.substring(0,4) + getRealMonth(month) );
    }

    public static String getRealMonth(int month) {

        String realMonth;

        if ( month > 12 ) {
            realMonth = "0" + String.valueOf( month - 12);
        }else {
            if (month < 10) {
                realMonth = "0" + String.valueOf(month);
            } else {
                realMonth = String.valueOf(month);
            }
        }

        return realMonth;
    }

    public static String getRealDay(int day) {
        if (day >= 10) {
            return String.valueOf(day);
        }else {
            return "0" + String.valueOf(day);
        }
    }

    public static int makeTrimesterPrediction(StockVars prevStockVar, int quantityLess ) {

        int quantityLeft = prevStockVar.stockMax - quantityLess;

        if ( haveToMoreIncreaseStock(prevStockVar,quantityLeft) ) {
            return prevStockVar.stockMax + prevStockVar.multiplierSafetyVar*prevStockVar.safetyVar;

        } else if (haveToIncreaseStock(prevStockVar,quantityLeft) ) {
            return prevStockVar.stockMax + prevStockVar.safetyVar;

        } else if ( haveToDoSame(prevStockVar,quantityLeft)) {
            return prevStockVar.stockMax;

        } else if (haveToDecreaseStock(prevStockVar, quantityLeft)) {
            return prevStockVar.stockMax - prevStockVar.safetyVar;

        } else if (haveToMoreDecreaseStock(prevStockVar,quantityLeft)) {
            return prevStockVar.stockMax - prevStockVar.multiplierSafetyVar*prevStockVar.safetyVar;
        }

        return prevStockVar.stockMax - prevStockVar.multiplierSafetyVar*prevStockVar.safetyVar;
    }

    private static boolean haveToMoreIncreaseStock(StockVars prevStockVar, int quantityLeft) {
        return quantityLeft <= prevStockVar.stockMin;
    }

    private static boolean haveToIncreaseStock(StockVars prevStockVar, int quantityLeft) {
        return quantityLeft > prevStockVar.stockMin && quantityLeft <= prevStockVar.stockMin + prevStockVar.safetyVar;
    }

    private static boolean haveToDoSame(StockVars prevStockVar, int quantityLeft) {
        return quantityLeft > prevStockVar.stockMin + prevStockVar.safetyVar && quantityLeft <= prevStockVar.stockMax - prevStockVar.safetyVar * ( 1 + prevStockVar.multiplierSafetyVar);
    }

    private static boolean haveToDecreaseStock(StockVars prevStockVar, int quantityLeft) {
        return quantityLeft > prevStockVar.stockMax - prevStockVar.safetyVar * ( 1 + prevStockVar.multiplierSafetyVar) && quantityLeft < prevStockVar.stockMax - prevStockVar.safetyVar;
    }

    private static boolean haveToMoreDecreaseStock(StockVars prevStockVar, int quantityLeft) {
        return quantityLeft >= prevStockVar.stockMax - prevStockVar.safetyVar && quantityLeft <= prevStockVar.stockMax;
    }
}
