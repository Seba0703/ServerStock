import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class TestServer {

    @Test
    public void ArrayHashMapTest() {
        ArrayHashMap dataMapper = TokenizerCSV.tokenizeMaterialFile();

        List<MaterialInfo> list = new ArrayList<>(dataMapper.get("jeringa"));

        assertTrue(list.get(0).dueDate == 20140708);
        assertTrue(list.get(1).dueDate == 20141008);
        assertTrue(list.get(2).dueDate == 20141008);
        assertTrue(list.get(3).dueDate == 20151208);
        assertTrue(list.get(4).dueDate == 20160708);


    }

}
