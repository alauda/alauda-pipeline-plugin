package io.alauda.jenkins.plugins.pipeline;

import org.junit.Test;
import io.alauda.jenkins.plugins.pipeline.alauda.Alauda;

import static org.junit.Assert.assertEquals;

public class AlaudaTest {

    @Test
    public void testAlaudaPath() throws Exception {
        String endpoint = "https://enterprise.alauda.cn/";
        String buildPath = Alauda.AlaudaPath.getBuildUrl(endpoint, "xxx");
        String servicePath = Alauda.AlaudaPath.getServiceUrl(endpoint, "yyy");
        assertEquals("https://enterprise.alauda.cn/console/build/history/detail/xxx", buildPath);
        assertEquals("https://enterprise.alauda.cn/console/k8s_service/detail/yyy", servicePath);
        endpoint = "https://enterprise.alauda.cn";
        buildPath = Alauda.AlaudaPath.getBuildUrl(endpoint, "xxx");
        assertEquals("https://enterprise.alauda.cn/console/build/history/detail/xxx", buildPath);
    }
}
