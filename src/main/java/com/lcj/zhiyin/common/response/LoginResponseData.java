package com.lcj.zhiyin.common.response;

import com.lcj.zhiyin.model.domain.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data

@AllArgsConstructor
public class LoginResponseData {
    User user;
    String token;

    public LoginResponseData() {

    }
}
