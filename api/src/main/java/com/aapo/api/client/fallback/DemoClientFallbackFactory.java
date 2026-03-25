package com.aapo.api.client.fallback;

import com.aapo.api.client.DemoClient;
import com.aapo.api.dto.DemoDTO;
import com.aapo.common.exception.BizIllegalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;


@Slf4j
public class DemoClientFallbackFactory implements FallbackFactory<DemoClient> {
    @Override
    public DemoClient create(Throwable cause) {
        return new DemoClient() {

            @Override
            public DemoDTO demo(String name) {
                log.error("demo失败", cause);
                throw new BizIllegalException(cause);
            }

            @Override
            public DemoDTO demoDelay(String name) {
                log.error("demoDelay失败", cause);
                throw new BizIllegalException(cause);
            }

            @Override
            public DemoDTO demoDelayFallback(String name) {
                log.error("demoDelayFallback失败", cause);
                DemoDTO demoDTO = new DemoDTO();
                demoDTO.setText("快速失败");
                return demoDTO;
            }

            @Override
            public void demoUpdateTxt(Long id, String txt) {
                log.error("demoUpdateTxt失败，参数id:{},txt:{}", id, txt, cause);
            }
        };
    }
}
