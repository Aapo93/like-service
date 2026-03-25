package com.aapo.demoservice.service;

import com.aapo.demoservice.domain.po.Demo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IDemoService extends IService<Demo> {

    /**
     * 修改txt
     * @param txt
     */
    void updateTxt(Long id, String txt);
}
