package com.lcj.zhiyin.common;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用删除请求
 */
@Data
public class DeleteRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -5860707094194210842L;

    @NotBlank(message = "id不能为空")
    @Min(1)
    private long id;
}
