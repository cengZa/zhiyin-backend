package com.lcj.zhiyin.mapper;


import com.lcj.zhiyin.model.domain.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 Mapper
 */
public interface UserMapper extends BaseMapper<User> {

//    List<User> searchUsersByTags(@Param("tagNameList") List<String> tagNameList);
}




