
public class StockVars {

    String nameMaterial;

    int stockMax;
    int stockMin;
    int safetyVar;
    int multiplierSafetyVar;

    private int count;

    public StockVars() {
        count = 0;
    }

    public void add(String stockVar) {
        switch (count) {
            case 0: nameMaterial = stockVar; break;
            case 1: stockMax = Integer.parseInt(stockVar); break;
            case 2: stockMin = Integer.parseInt(stockVar); break;
            case 3: safetyVar = Integer.parseInt(stockVar); break;
            case 4: multiplierSafetyVar = Integer.parseInt(stockVar); break;
        }

        count++;

    }
}
