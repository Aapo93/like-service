package com.aapo.likeasynservice.service.impl;

import com.aapo.likeasynservice.domain.po.LikeRecord;
import com.aapo.likeasynservice.mapper.LikeRecordMapper;
import com.aapo.likeasynservice.service.ILikeRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LikeRecordServiceImpl extends ServiceImpl<LikeRecordMapper, LikeRecord> implements ILikeRecordService {
}