package com.lcj.zhiyin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lcj.zhiyin.model.domain.Team;
import com.lcj.zhiyin.model.domain.User;
import com.lcj.zhiyin.model.dto.TeamQuery;
import com.lcj.zhiyin.model.request.TeamCreateRequest;
import com.lcj.zhiyin.model.request.TeamJoinRequest;
import com.lcj.zhiyin.model.request.TeamQuitRequest;
import com.lcj.zhiyin.model.request.TeamUpdateRequest;
import com.lcj.zhiyin.model.vo.TeamUserVO;

import java.util.List;

/**
 * 队伍服务
 */
public interface TeamService extends IService<Team> {

    /**
     * 创建队伍
     */
    long createTeam(TeamCreateRequest teamCreateRequest, User loginUser);

    /**
     * 搜索队伍
     */
    Page<TeamUserVO> listTeams(TeamQuery teamQuery, String currentUserAccount);

    /**
     * 更新队伍
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     * 加入队伍
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    /**
     * 退出队伍
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);


    /**
     * 删除（解散）队伍
     */
    boolean deleteTeam(long id, User loginUser);
}
