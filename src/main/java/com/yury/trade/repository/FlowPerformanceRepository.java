package com.yury.trade.repository;

import com.yury.trade.entity.FlowPerformance;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.List;

public interface FlowPerformanceRepository extends CrudRepository<FlowPerformance, String> {

    List<FlowPerformance> findByStartDate(Date startDate);

}