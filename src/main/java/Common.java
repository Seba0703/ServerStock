
public class Common {

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
