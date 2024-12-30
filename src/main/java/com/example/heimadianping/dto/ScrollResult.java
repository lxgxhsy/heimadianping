package com.example.heimadianping.dto;

import lombok.Data;

import java.util.List;

/**
 * @author 诺诺
 */
@Data
public class ScrollResult {
    private List<?> list;
    private Long minTime;
    private Integer offset;
}
