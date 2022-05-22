package com.yury.trade.repository;

import com.yury.trade.entity.Symbol;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface SymbolRepository  extends CrudRepository<Symbol, String> {

    @Query("SELECT symbol FROM Symbol")
    List<String> findAllSymbols();

    @Query("SELECT s FROM Symbol s WHERE last > 12 AND average_volume > 1000 AND type = 'stock'")
    Collection<Symbol> findGoodSymbols();

    @Query("SELECT s FROM Symbol s WHERE type = 'option'")
    Collection<Symbol> findAllOptions();

    @Transactional
    @Modifying
    @Query("DELETE FROM Symbol WHERE type = 'option'")
    void deleteAllOptions();
}
