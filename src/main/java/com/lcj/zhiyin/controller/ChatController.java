package com.lcj.zhiyin.controller;

import com.lcj.zhiyin.common.BaseResponse;
import com.lcj.zhiyin.common.ResultUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.zhipuai.ZhipuAiChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/*
  ! 该模块不受security保护， 在SecurityConfig中已加入白名单
 */

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
@Tag(name = "ai")
@Slf4j
public class ChatController {

    private final ZhipuAiChatClient chatClient;

    @GetMapping("/generate")
    public BaseResponse<String> generate(@RequestParam(value = "message", defaultValue = "给我讲个笑话") String message){
        return ResultUtils.success(chatClient.call(message));
    }

    @GetMapping("/stream")
    public BaseResponse<?> stream(@RequestParam(value = "message", defaultValue = "给我讲个笑话") String message){
        return ResultUtils.success(chatClient.stream(message).toStream().toList());
    }

}
