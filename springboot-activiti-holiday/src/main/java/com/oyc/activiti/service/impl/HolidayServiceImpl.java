package com.oyc.activiti.service.impl;

import com.oyc.activiti.dao.HolidayRepository;
import com.oyc.activiti.domain.Holiday;
import com.oyc.activiti.service.HolidayService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * (Holiday)表服务实现类
 *
 * @author oyc
 * @since 2021-01-12 13:51:12
 */
@Service("holidayService")
public class HolidayServiceImpl implements HolidayService {
    @Resource
    private HolidayRepository holidayRepository;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public Holiday queryById(Integer id) {
        return holidayRepository.getOne(id);
    }

    /**
     * 查询多条数据
     *
     * @return 对象列表
     */
    @Override
    public List<Holiday> findAll() {
        return holidayRepository.findAll();
    }

    /**
     * 新增数据
     *
     * @param holiday 实例对象
     * @return 实例对象
     */
    @Override
    public Holiday saveOrUpdate(Holiday holiday) {
        holidayRepository.save(holiday);
        return holiday;
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public void deleteById(Integer id) {
        holidayRepository.deleteById(id);
    }
}
