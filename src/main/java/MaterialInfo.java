
public class MaterialInfo implements Comparable<MaterialInfo>{
    private int count;
    int quantity;
    int dueDate;
    double price;
    int buyDate;

    public MaterialInfo() {
        count = 0;
    }

    public void add(String info) {

        switch (count) {
            case 0: quantity = Integer.parseInt(info); break;
            case 1: dueDate = Integer.parseInt(info); break;
            case 2: price = Double.parseDouble(info); break;
            case 3: buyDate = Integer.parseInt(info); break;
        }

        count++;
    }

    @Override
    public int compareTo(MaterialInfo o) {
        return dueDate - o.dueDate;
    }
}
