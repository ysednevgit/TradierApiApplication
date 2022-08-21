package com.yury.trade.repository;

import com.yury.trade.entity.OptionV2;
import com.yury.trade.entity.OptionV2Id;
import com.yury.trade.util.SymbolWithDate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.List;

public interface OptionRepository extends CrudRepository<OptionV2, OptionV2Id> {

    @Query("SELECT distinct root_symbol FROM OptionV2 where greeks_updated_at = ?1")
    List<String> findRootSymbols(Date greeks_updated_at);

    @Query("SELECT s FROM OptionV2 s WHERE days_left = null")
    List<OptionV2> findIncompleteOptions();

    List<OptionV2> findByOptionV2IdSymbol(String symbol);

    @Query("SELECT s FROM OptionV2 s WHERE optionV2Id.symbol = ?1 and greeks_updated_at > ?2")
    List<OptionV2> findByOptionV2IdSymbolWithGreaterUpdated(String symbol, Date updated);

    @Query("SELECT s FROM OptionV2 s WHERE optionV2Id.symbol = ?1 and greeks_updated_at >= ?2")
    List<OptionV2> findByOptionV2IdSymbolWithGreaterOrSameUpdated(String symbol, Date updated);

    List<OptionV2> findByUnderlying(String Underlying);

    @Query("SELECT s FROM OptionV2 s WHERE underlying = ?1 and greeks_updated_at = ?2 order by days_left, option_type, strike")
    List<OptionV2> findByUnderlyingAndGreeks_updated_at(String underlying, Date greeks_updated_at);

    @Query("SELECT s FROM OptionV2 s WHERE underlying = ?1 and strike = ?2 and greeks_updated_at = ?3 and expiration_date = ?4")
    List<OptionV2> findByNextByStrike(String underlying, Double strike, Date minGreeks_updated_at, Date expirationDate);

    @Query("SELECT s FROM OptionV2 s WHERE underlying = ?1 and greeks_updated_at = ?2 and expiration_date = ?3")
    List<OptionV2> findNext(String underlying, Date greeks_updated_at, Date expirationDate);

    @Query(value = "SELECT MIN(greeks_updated_at) as date, root_symbol as symbol  FROM OPTIONV2 GROUP BY root_symbol ORDER BY root_symbol", nativeQuery = true)
    List<SymbolWithDate> findRootSymbolsWithMinDates();

    //    @Query("SELECT s FROM OptionV2 s WHERE underlying = ?1 and delta = ?2 and greeks_updated_at = ?3")
//    List<OptionV2> findByUnderlyingAndDeltaAndGreeks_updated_at(String underlying, Double delta, Date minGreeks_updated_at);

}
