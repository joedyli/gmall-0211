package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class BaseAttrValueVo extends SpuAttrValueEntity {

    public void setValueSelected(List<String> valueSelected){
        if (!CollectionUtils.isEmpty(valueSelected)){
            this.setAttrValue(StringUtils.join(valueSelected, ","));
        }
    }
}
