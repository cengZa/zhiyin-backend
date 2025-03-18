package com.lcj.zhiyin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lcj.zhiyin.service.UserTeamService;
import com.lcj.zhiyin.model.domain.UserTeam;
import com.lcj.zhiyin.mapper.UserTeamMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户队伍服务实现类
 */
@Service
@RequiredArgsConstructor
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
        implements UserTeamService {

    private final UserTeamMapper userTeamMapper;

    public Set<Long> getJoinedTeamIds(Long userId, List<Long> teamIdList) {
        return userTeamMapper.selectList(new LambdaQueryWrapper<UserTeam>()
                        .eq(UserTeam::getUserId, userId)
                        .in(UserTeam::getTeamId, teamIdList))
                .stream()
                .map(UserTeam::getTeamId)
                .collect(Collectors.toSet());
    }

    public Map<Long, Long> countByTeamIds(List<Long> teamIdList) {
        return userTeamMapper.selectList(new LambdaQueryWrapper<UserTeam>()
                        .in(UserTeam::getTeamId, teamIdList))
                .stream()
                .collect(Collectors.groupingBy(UserTeam::getTeamId, Collectors.counting()));
    }

}




