package com.lcj.zhiyin.common;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用分页请求参数
 */
@Data
public class PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -5860707094194210842L;

    private static final int MAX_PAGE_SIZE = 100; // 限制最大分页大小

    protected int pageSize = 10;

    protected int pageNum = 1;

    public void setPageSize(int pageSize) {
        this.pageSize = Math.min(pageSize, MAX_PAGE_SIZE);
    }

}
