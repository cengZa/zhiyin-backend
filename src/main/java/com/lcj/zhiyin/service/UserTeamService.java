package com.lcj.zhiyin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lcj.zhiyin.model.domain.UserTeam;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户队伍服务
 */
public interface UserTeamService extends IService<UserTeam> {
    Set<Long> getJoinedTeamIds(Long userId, List<Long> teamIdList);
    Map<Long, Long> countByTeamIds(List<Long> teamIdList);
}
