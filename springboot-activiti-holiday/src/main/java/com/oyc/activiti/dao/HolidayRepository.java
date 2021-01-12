package com.oyc.activiti.dao;

import com.oyc.activiti.domain.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;


/**
 * (Holiday)表数据库访问层
 *
 * @author oyc
 * @since 2021-01-12 13:51:10
 */
public interface HolidayRepository extends JpaRepository<Holiday,Integer> {
}
