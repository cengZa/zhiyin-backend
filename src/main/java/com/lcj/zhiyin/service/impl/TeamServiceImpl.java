package com.lcj.zhiyin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lcj.zhiyin.exception.BusinessException;
import com.lcj.zhiyin.common.ErrorCode;
import com.lcj.zhiyin.model.domain.User;
import com.lcj.zhiyin.model.domain.UserTeam;
import com.lcj.zhiyin.model.dto.TeamQuery;
import com.lcj.zhiyin.model.enums.TeamStatusEnum;
import com.lcj.zhiyin.model.request.TeamCreateRequest;
import com.lcj.zhiyin.model.request.TeamJoinRequest;
import com.lcj.zhiyin.model.request.TeamQuitRequest;
import com.lcj.zhiyin.model.request.TeamUpdateRequest;
import com.lcj.zhiyin.model.vo.TeamUserVO;
import com.lcj.zhiyin.service.TeamService;
import com.lcj.zhiyin.model.domain.Team;
import com.lcj.zhiyin.mapper.TeamMapper;
import com.lcj.zhiyin.service.UserService;
import com.lcj.zhiyin.service.UserTeamService;
import com.mysql.cj.log.Log;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 队伍服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    private final UserTeamService userTeamService;
    private final UserService userService;
    private final RedissonClient redissonClient;
    private final TeamMapper teamMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createTeam(TeamCreateRequest teamCreateRequest, User loginUser) {
        validateTeam(teamCreateRequest);

        long userId = loginUser.getId();

        // 校验用户最多创建 5 个队伍
        LambdaQueryWrapper<Team> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Team::getUserId, userId);
        long hasTeamNum = this.count(queryWrapper);
        if (hasTeamNum >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多创建 5 个队伍");
        }
        // 插入队伍信息
        Team team = new Team();
        BeanUtils.copyProperties(teamCreateRequest, team);
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);

        log.info("创建的队伍为 => {}", team);

        Long teamId = team.getId();
        if (!result || teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }

        // 插入用户与队伍关联关系
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(LocalDateTime.now());
        result = userTeamService.save(userTeam);

        log.info("新增用户-队伍关系 => {}", userTeam);

        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        return teamId;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(rollbackFor = BusinessException.class)
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        validateTeam(teamUpdateRequest);

        Team oldTeam = teamMapper.selectById(teamUpdateRequest.getId());
        if (oldTeam == null) throw new BusinessException(ErrorCode.NULL_ERROR, "目标队伍不存在");

        // 只有管理员或者队伍的创建者可以修改
        if (Objects.equals(oldTeam.getUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, updateTeam);
        return this.updateById(updateTeam);
    }

    private void validateTeam(TeamCreateRequest request) {
        if (TeamStatusEnum.SECRET.getValue() == (request.getStatus()) &&
                StringUtils.isBlank(request.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密队伍必须设置密码");
        }

        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(request.getStatus());
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不正确");
        }
    }

    private void validateTeam(TeamUpdateRequest request) {
        if (TeamStatusEnum.SECRET.getValue() == (request.getStatus()) &&
                StringUtils.isBlank(request.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密队伍必须设置密码");
        }

        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(request.getStatus());
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不正确");
        }
    }

    @Override
    public Page<TeamUserVO> listTeams(TeamQuery teamQuery, String currentUserAccount) {
        // 构造查询条件
        LambdaQueryWrapper<Team> queryWrapper = buildQueryWrapper(teamQuery);

        // 查询队伍列表
        Page<Team> page = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        Page<Team> teamPage = teamMapper.selectPage(page, queryWrapper);
        if (teamPage.getRecords().isEmpty()) {
            return new Page<>();
        }

        log.info("输出的Page.Records => {}", teamPage.getRecords());
        log.info("输出的Page => {}", teamPage);

        // 转换 Team -> TeamUserVO
        List<TeamUserVO> teamUserVOList = teamPage.getRecords().stream()
                .map(this::convertToTeamUserVO)
                .collect(Collectors.toList());

        log.info("输出的List<TeamUserVO> => {}", teamUserVOList);

        // 填充创建者信息
        enrichCreatorInfo(teamUserVOList);

        log.info("填充创建者信息以后 => {}", teamUserVOList);

        // 调用辅助方法：统计当前用户是否已加入队伍和各队伍的加入人数
        augmentTeamUserVOs(teamUserVOList, currentUserAccount);

        log.info("填充以后 => {}", teamUserVOList);

        return new Page<TeamUserVO>(teamPage.getCurrent(), teamPage.getSize(), teamPage.getTotal())
                .setRecords(teamUserVOList);
    }

    private LambdaQueryWrapper<Team> buildQueryWrapper(TeamQuery teamQuery) {
        LambdaQueryWrapper<Team> queryWrapper = new LambdaQueryWrapper<>();
        Optional.ofNullable(teamQuery.getId()).ifPresent(id -> queryWrapper.eq(Team::getId, id));
        Optional.ofNullable(teamQuery.getIdList()).filter(CollectionUtils::isNotEmpty).ifPresent(ids -> queryWrapper.in(Team::getId, ids));
        Optional.ofNullable(teamQuery.getSearchText()).filter(StringUtils::isNotBlank).ifPresent(text ->
                queryWrapper.and(qw -> qw.like(Team::getName, text).or().like(Team::getDescription, text))
        );
        Optional.ofNullable(teamQuery.getName()).filter(StringUtils::isNotBlank).ifPresent(name -> queryWrapper.like(Team::getName, name));
        Optional.ofNullable(teamQuery.getDescription()).filter(StringUtils::isNotBlank).ifPresent(desc -> queryWrapper.like(Team::getDescription, desc));
        Optional.ofNullable(teamQuery.getMaxNum()).filter(num -> num > 0).ifPresent(num -> queryWrapper.eq(Team::getMaxNum, num));
        Optional.ofNullable(teamQuery.getUserId()).filter(id -> id > 0).ifPresent(id -> queryWrapper.eq(Team::getUserId, id));

        // 处理队伍状态，默认返回公开队伍
        TeamStatusEnum statusEnum = Optional.ofNullable(TeamStatusEnum.getEnumByValue(teamQuery.getStatus()))
                .orElse(TeamStatusEnum.PUBLIC);
        if (TeamStatusEnum.PRIVATE.equals(statusEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        queryWrapper.eq(Team::getStatus, statusEnum.getValue());

        // 只显示未过期的队伍
        queryWrapper.and(qw -> qw.gt(Team::getExpireTime, LocalDateTime.now()).or().isNull(Team::getExpireTime));

        return queryWrapper;
    }

    private TeamUserVO convertToTeamUserVO(Team team) {
        TeamUserVO vo = new TeamUserVO();
        BeanUtils.copyProperties(team, vo);
        return vo;
    }

    private void enrichCreatorInfo(List<TeamUserVO> teamUserVOList) {
        Set<Long> creatorIds = teamUserVOList.stream()
                .map(TeamUserVO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (creatorIds.isEmpty()) {
            return;
        }
        Map<Long, User> creatorMap = userService.getUsersByIds(creatorIds);
        teamUserVOList.forEach(vo -> {
            if (vo.getUserId() != null && creatorMap.containsKey(vo.getUserId())) {
                vo.setCreateUser(userService.getSafetyUser(creatorMap.get(vo.getUserId())));
            }
        });
    }

    // 单管用户是否加入队伍 & 加入人数统计
    private void augmentTeamUserVOs(List<TeamUserVO> teamUserVOList, String currentUserAccount) {
        List<Long> teamIdList = teamUserVOList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(teamIdList)) {
            return;
        }
        User currentUser = userService.getUserByUserAccount(currentUserAccount);
        if (currentUser == null) {
            return;
        }
        Long currentUserId = currentUser.getId();

        // 查询当前用户加入的队伍
        Set<Long> joinedTeamIds = userTeamService.getJoinedTeamIds(currentUserId, teamIdList);
        teamUserVOList.forEach(vo -> vo.setHasJoin(joinedTeamIds.contains(vo.getId())));

        // 统计每个队伍的加入人数
        Map<Long, Long> joinCountMap = userTeamService.countByTeamIds(teamIdList);
        teamUserVOList.forEach(vo -> vo.setHasJoinNum(joinCountMap.getOrDefault(vo.getId(), 0L).intValue()));
    }


    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {

        Team team = getTeamById(teamJoinRequest.getTeamId());

        LocalDateTime expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }

        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(team.getStatus());

        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有队伍");
        }

        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            if (teamJoinRequest.getPassword() == null ||
                    !teamJoinRequest.getPassword().equals(team.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }

        long userId = loginUser.getId();
        long teamId = team.getId();

        // 只有一个线程能获取到锁
        RLock lock = redissonClient.getLock("zhiyin:join_team:" + teamId);
        try {
            if (!lock.tryLock(0, 10, TimeUnit.SECONDS)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加入队伍超时");
            }
            // 校验该用户已加入队伍数量
            long joinCount = userTeamService.count(new LambdaQueryWrapper<UserTeam>()
                    .eq(UserTeam::getUserId, userId));
            if(joinCount >= 5){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加入队伍数量过多");
            }

            // 校验用户是否已加入该队伍
            long alreadyJoined = userTeamService.count(new LambdaQueryWrapper<UserTeam>()
                    .eq(UserTeam::getUserId, userId)
                    .eq(UserTeam::getTeamId, teamId));
            if(alreadyJoined > 0){
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "已加入该队伍");
            }

            // 校验队伍是否已满
            long teamJoinCount = countTeamUserByTeamId(teamId);
            if (teamJoinCount >= team.getMaxNum()) throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");

            // 构造并保存用户-队伍关联记录
            UserTeam userTeam = new UserTeam();
            userTeam.setUserId(userId);
            userTeam.setTeamId(teamId);
            userTeam.setJoinTime(LocalDateTime.now());
            return userTeamService.save(userTeam);

        } catch (InterruptedException e) {
            log.error("joinTeam error", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加入队伍失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamQuitRequest.getTeamId();

        Team team = this.getTeamById(teamId);
        long loginUserId = loginUser.getId();

        UserTeam queryUserTeam = new UserTeam();
        queryUserTeam.setTeamId(teamId);
        queryUserTeam.setUserId(loginUserId);

        LambdaQueryWrapper<UserTeam> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserTeam::getTeamId, teamId)
                .eq(UserTeam::getUserId, loginUserId);

        long count = userTeamService.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未加入队伍");
        }

        // 获取该队伍当前加入人数
        long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
        // 队伍只剩一人，解散
        if (teamHasJoinNum == 1) {
            // 删除队伍
            this.removeById(teamId);
        } else {
            // 如果当前用户为队长， 则需要转移队长身份
            if (team.getUserId() == loginUserId) {
                // 把队伍转移给最早加入的用户
                // 1. 查询已加入队伍的所有用户和加入时间
                LambdaQueryWrapper<UserTeam> teamJoinWrapper = new LambdaQueryWrapper<>();
                teamJoinWrapper.eq(UserTeam::getTeamId, teamId)
                        .orderByAsc(UserTeam::getJoinTime)
                        .last("limit 2");

                List<UserTeam> userTeamList = userTeamService.list(teamJoinWrapper);
                if (CollectionUtils.isEmpty(userTeamList) || userTeamList.size() <= 1) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队伍数据异常");
                }
                // 更新当前队伍的队长 第二个加入的用户
                UserTeam nextUserTeam = userTeamList.get(1);
                Long nextTeamLeaderId = nextUserTeam.getUserId();

                Team updateTeam = new Team();
                updateTeam.setId(teamId);
                updateTeam.setUserId(nextTeamLeaderId);
                boolean result = this.updateById(updateTeam);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍队长失败");
                }
            }
        }
        // 删除当前用户与队伍的关联记录
        return userTeamService.remove(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(long id, User loginUser) {
        // 校验队伍是否存在
        Team team = getTeamById(id);
        long teamId = team.getId();
        // 校验当前用户是不是队伍的队长
        if (!team.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无访问权限");
        }
        // 移除所有加入队伍的关联信息
        LambdaQueryWrapper<UserTeam> userTeamQueryWrapper = new LambdaQueryWrapper<>();
        userTeamQueryWrapper.eq(UserTeam::getTeamId, teamId);

        boolean result = userTeamService.remove(userTeamQueryWrapper);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍关联信息失败");
        }
        // 删除队伍
        return this.removeById(teamId);
    }

    /**
     * 根据 id 获取队伍信息
     */
    private Team getTeamById(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        return team;
    }

    /**
     * 获取某队伍当前人数
     */
    private long countTeamUserByTeamId(long teamId) {
        LambdaQueryWrapper<UserTeam> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTeam::getTeamId, teamId);
        return userTeamService.count(wrapper);
    }

}




