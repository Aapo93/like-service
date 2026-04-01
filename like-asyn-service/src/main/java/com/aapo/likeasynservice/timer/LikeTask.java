package com.aapo.likeasynservice.timer;

import com.aapo.likeasynservice.service.ILikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LikeTask {

    private final ILikeService likeService;

    @Scheduled(cron = "${aapo.timer.liketask.statistics}")
    public void statistics(){
        likeService.statistics();
    }
}
