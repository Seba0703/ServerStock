
public class Common {

    public static int makeTrimesterPrediction(StockVars prevStockVar, int quantityLess ) {

        int quantityLeft = prevStockVar.stockMax - quantityLess;

        int newMax;

        if ( haveToMoreIncreaseStock(prevStockVar,quantityLeft) ) {
            newMax = prevStockVar.stockMax + prevStockVar.multiplierSafetyVar*prevStockVar.safetyVar;

        } else if (haveToIncreaseStock(prevStockVar,quantityLeft) ) {
            newMax = prevStockVar.stockMax + prevStockVar.safetyVar;

        } else if ( haveToDoSame(prevStockVar,quantityLeft)) {
            newMax = prevStockVar.stockMax;

        } else if (haveToDecreaseStock(prevStockVar, quantityLeft)) {
            newMax = prevStockVar.stockMax - prevStockVar.safetyVar;

        } else {
            newMax = prevStockVar.stockMax - prevStockVar.multiplierSafetyVar*prevStockVar.safetyVar;
        }

        boolean upper = newMax - prevStockVar.safetyVar < newMax;
        boolean middle = newMax - prevStockVar.multiplierSafetyVar*prevStockVar.safetyVar < newMax - prevStockVar.safetyVar;
        boolean lower = prevStockVar.stockMin + prevStockVar.safetyVar < newMax - prevStockVar.multiplierSafetyVar*prevStockVar.safetyVar;

        if (upper && middle && lower) {
            return newMax;
        } else {
            return prevStockVar.stockMax;
        }
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
        return quantityLeft > prevStockVar.stockMax - prevStockVar.safetyVar * prevStockVar.multiplierSafetyVar && quantityLeft < prevStockVar.stockMax - prevStockVar.safetyVar;
    }

    private static boolean haveToMoreDecreaseStock(StockVars prevStockVar, int quantityLeft) {
        return quantityLeft >= prevStockVar.stockMax - prevStockVar.safetyVar && quantityLeft <= prevStockVar.stockMax;
    }
}
