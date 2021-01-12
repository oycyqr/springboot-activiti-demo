package com.oyc.activiti.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * @ClassName: holiday
 * @Description: (Holiday)假期实体类
 * @Author oyc
 * @Date 2021/1/12 11:46
 * @Version 1.0
 */
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"})
@Data
@Entity
public class Holiday implements Serializable {
    //该注解可以在Id字段上，可以在Id字段的getter方法上
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    /**
     * 请假标题
     */
    private String title;
    /**
     * 请假人
     */
    private String userName;
    /**
     * 请假理由
     */
    private String reason;
    /**
     * 开始时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date startTime;
    /**
     * 结束时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date endTime;
    /**
     * 请假类型
     */
    private int type;
    /**
     * 状态
     */
    private int status;

    /**
     * 备注信息
     */
    private String remark;

    /**
     * 扩展字段
     */
    private String ext1;
    private String ext2;
    private String ext3;
    private String ext4;
    private String ext5;

    @Transient
    private String instanceId;
    @Transient
    private String processDefinitionId;

    @Transient
    private Object processInstanceInfo;
}
