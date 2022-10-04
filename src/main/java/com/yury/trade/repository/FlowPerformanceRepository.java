package com.yury.trade.repository;

import com.yury.trade.entity.FlowPerformance;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.List;

public interface FlowPerformanceRepository extends CrudRepository<FlowPerformance, String> {

    @Query("SELECT s FROM FlowPerformance s WHERE startDate = :startDate ORDER BY changeValue DESC")
    List<FlowPerformance> findByStartDate(Date startDate);

    @Query("SELECT s FROM FlowPerformance s WHERE symbol = :symbol AND startDate = :startDate ORDER BY changeValue DESC")
    List<FlowPerformance> findByParams(String symbol, Date startDate);

    @Query("SELECT s FROM FlowPerformance s WHERE symbol = :symbol AND startDate = :startDate AND endDate = :endDate ORDER BY changeValue DESC")
    List<FlowPerformance> findByParams(String symbol, Date startDate, Date endDate);

}