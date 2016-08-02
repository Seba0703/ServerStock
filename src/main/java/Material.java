
public class Material {
    private int count;
    String nameKey;
    MaterialInfo materialInfo;

    public Material(){
        count = 0;
        materialInfo = new MaterialInfo();
    }

    public void add(String info) {
        if (count > 0) {
            materialInfo.add(info);
        } else {
            nameKey = info;
        }
        count++;
    }

}
