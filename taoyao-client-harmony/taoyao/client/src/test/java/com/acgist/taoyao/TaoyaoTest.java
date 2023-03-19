package com.acgist.taoyao;

import com.acgist.taoyao.model.Message;
import com.acgist.taoyao.signal.Taoyao;
import ohos.hiviewdfx.HiLog;
import org.junit.Test;

public class TaoyaoTest {

    @Test
    public void test() throws InterruptedException {
        final Taoyao taoyao = new Taoyao(9999, "localhost", "DES", "2SPWy+TF1zM=");
        taoyao.connect();
        Thread.sleep(Long.MAX_VALUE);
    }

}
