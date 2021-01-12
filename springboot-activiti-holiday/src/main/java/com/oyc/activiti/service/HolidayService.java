package com.oyc.activiti.service;


import com.oyc.activiti.domain.Holiday;

import java.util.List;

/**
 * (Holiday)表服务接口
 *
 * @author oyc
 * @since 2021-01-12 14:00:34
 */
public interface HolidayService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    Holiday queryById(Integer id);

    /**
     * 查询多条数据
     *
     * @return 对象列表
     */
    List<Holiday> findAll();

    /**
     * 保存数据
     *
     * @param holiday 实例对象
     * @return 实例对象
     */
    Holiday saveOrUpdate(Holiday holiday);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    void deleteById(Integer id);

}
