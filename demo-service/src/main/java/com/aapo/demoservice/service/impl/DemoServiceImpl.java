package com.aapo.demoservice.service.impl;

import com.aapo.demoservice.domain.po.Demo;
import com.aapo.demoservice.mapper.DemoMapper;
import com.aapo.demoservice.service.IDemoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class DemoServiceImpl extends ServiceImpl<DemoMapper, Demo> implements IDemoService {

    @Override
    public void updateTxt(Long id, String txt) {
        if (id == null) {
            return;
        }
        lambdaUpdate().set(Demo::getTxt, txt).eq(Demo::getId, id).update();
    }
}