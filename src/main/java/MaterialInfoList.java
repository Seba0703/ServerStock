import java.util.ArrayList;
import java.util.List;

public class MaterialInfoList {

    private List<MaterialInfo> materialInfoList;
    private int quantity;

    public MaterialInfoList() {
        materialInfoList = new ArrayList<>();
        quantity = 0;
    }

    public List<MaterialInfo> getList() {
        return materialInfoList;
    }

    public int getQuantity() {
        return quantity;
    }


    public void add(MaterialInfo value) {
        materialInfoList.add(value);
        quantity += value.quantity;
    }
}
