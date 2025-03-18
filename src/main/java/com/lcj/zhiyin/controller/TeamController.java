package com.lcj.zhiyin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lcj.zhiyin.exception.BusinessException;
import com.lcj.zhiyin.common.BaseResponse;
import com.lcj.zhiyin.common.DeleteRequest;
import com.lcj.zhiyin.common.ErrorCode;
import com.lcj.zhiyin.common.ResultUtils;
import com.lcj.zhiyin.model.domain.Team;
import com.lcj.zhiyin.model.domain.User;
import com.lcj.zhiyin.model.domain.UserTeam;
import com.lcj.zhiyin.model.dto.TeamQuery;
import com.lcj.zhiyin.model.request.TeamCreateRequest;
import com.lcj.zhiyin.model.request.TeamJoinRequest;
import com.lcj.zhiyin.model.request.TeamQuitRequest;
import com.lcj.zhiyin.model.request.TeamUpdateRequest;
import com.lcj.zhiyin.model.vo.TeamUserVO;
import com.lcj.zhiyin.service.TeamService;
import com.lcj.zhiyin.service.UserService;
import com.lcj.zhiyin.service.UserTeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 队伍接口
 */
@RestController
@RequestMapping("/team")
@CrossOrigin(origins = {"http://localhost:3000"})
@Slf4j
@RequiredArgsConstructor
@Validated
@Tag(name="Team控制器")
public class TeamController {

    //? : 与使用@AllArgsConstructor有什么区别？
    private final TeamService teamService;
    private final UserService userService;
    private final UserTeamService userTeamService;

    // TODO: 是否允许队伍重名？
    @Operation(summary = "创建队伍")
    @PostMapping("/create")
    public BaseResponse<Long> createTeam(@Valid @RequestBody TeamCreateRequest teamCreateRequest) {
//        User loginUser = userService.getUserByUserAccount(userDetails.getUsername());
        User loginUser = userService.getLoginUser();
        return ResultUtils.success(teamService.createTeam(teamCreateRequest, loginUser));
    }

    @Operation(summary = "更新队伍", description = "队长 or 管理员")
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@Valid @RequestBody TeamUpdateRequest teamUpdateRequest) {
        User loginUser = userService.getLoginUser();
        return ResultUtils.success(teamService.updateTeam(teamUpdateRequest, loginUser));
    }

    @Operation(summary = "查询队伍")
    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(@RequestParam @Min(1) long id) {
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(team);
    }

    @Operation(summary = "队伍 列表查询", description = "条件, 分页")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public BaseResponse<Page<TeamUserVO>> listTeams(TeamQuery teamQuery) {
        Page<TeamUserVO> teamList = teamService.listTeams(teamQuery, userService.getLoginUser().getUserAccount());
        return ResultUtils.success(teamList);
    }

    @Operation(summary = "加入队伍")
    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest) {
        User loginUser = userService.getLoginUser();
        return ResultUtils.success(teamService.joinTeam(teamJoinRequest, loginUser));
    }

    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@Valid @RequestBody TeamQuitRequest teamQuitRequest) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser();
        boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
        return ResultUtils.success(result);
    }

    @DeleteMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest) {
        return ResultUtils.success(teamService.deleteTeam(deleteRequest.getId(), userService.getLoginUser()));
    }


    /**
     * 获取我创建的队伍
     */
    @GetMapping("/list/my/create")
    public BaseResponse<Page<TeamUserVO>> listMyCreateTeams(TeamQuery teamQuery) {
        User loginUser = userService.getLoginUser();
        teamQuery.setUserId(loginUser.getId());
        Page<TeamUserVO> teamList = teamService.listTeams(teamQuery, loginUser.getUserAccount());
        return ResultUtils.success(teamList);
    }


    /**
     * 获取我加入的队伍
     */
    @GetMapping("/list/my/join")
    public BaseResponse<Page<TeamUserVO>> listMyJoinTeams(@Valid TeamQuery teamQuery) {
        User loginUser = userService.getLoginUser();

        // 获取当前用户已加入的队伍ID
        List<UserTeam> userTeamList = userTeamService.list(
                new LambdaQueryWrapper<UserTeam>()
                        .eq(UserTeam::getUserId, loginUser.getId())
        );

        // 直接提取唯一的队伍ID
        List<Long> idList = userTeamList.stream()
                .map(UserTeam::getTeamId)
                .distinct()
                .collect(Collectors.toList());
        teamQuery.setIdList(idList);

        Page<TeamUserVO> teamList = teamService.listTeams(teamQuery, loginUser.getUserAccount());
        return ResultUtils.success(teamList);
    }

}



























