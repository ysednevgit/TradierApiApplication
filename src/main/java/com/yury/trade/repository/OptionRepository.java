package com.yury.trade.repository;

import com.yury.trade.entity.OptionV2;
import com.yury.trade.entity.OptionV2Id;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.List;

public interface OptionRepository  extends CrudRepository<OptionV2, OptionV2Id> {

    @Query("SELECT s FROM OptionV2 s WHERE days_left = null")
    List<OptionV2> findIncompleteOptions();

    List<OptionV2> findByOptionV2IdSymbol(String symbol);

    List<OptionV2> findByUnderlying(String Underlying);

    @Query("SELECT s FROM OptionV2 s WHERE underlying = ?1 and greeks_updated_at = ?2")
    List<OptionV2> findByUnderlyingAndGreeks_updated_at(String underlying, Date greeks_updated_at);

}
