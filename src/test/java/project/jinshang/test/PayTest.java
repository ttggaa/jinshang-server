package project.jinshang.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import project.jinshang.common.exception.CashException;
import project.jinshang.mod_pay.service.TradeService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PayTest {

    @Autowired
    private TradeService tradeService;

    @Test
    public void test01() throws CashException {
        //tradeService.notify("order-1000001580279710","wxpay","123456");
        //tradeService.test("D201806061713584572","order-1000000388114118","4200000123201806061613185075","66","wxpay","2018-06-06 17:14:49");
        //tradeService.test("D201806061738552426","order-1000000884208488","4200000129201806065012277178","66","wxpay","2018-06-06 17:39:17");
    }
}
