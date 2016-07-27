
public class MaterialInfo implements Comparable<MaterialInfo>{
    private int count;
    int quantity;
    int dueDate;
    int price;

    public MaterialInfo() {
        count = 0;
    }

    public void add(String info) {

        int value = Integer.parseInt(info);

        switch (count) {
            case 0: quantity = value; break;
            case 1: dueDate = value; break;
            case 2: price = value; break;

        }

        count++;
    }

    @Override
    public int compareTo(MaterialInfo o) {
        return dueDate - o.dueDate;
    }
}
