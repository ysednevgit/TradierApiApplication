package com.yury.trade.delegate;

import com.yury.trade.entity.Option;
import com.yury.trade.entity.Symbol;
import com.yury.trade.repository.OptionRepository;
import com.yury.trade.repository.StatsRepository;
import com.yury.trade.repository.SymbolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class PersistenceDelegate {

    @Autowired
    private SymbolRepository symbolRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private StatsRepository statsRepository;

    public SymbolRepository getSymbolRepository() {
        return symbolRepository;
    }

    public OptionRepository getOptionRepository() {
        return optionRepository;
    }

    public StatsRepository getStatsRepository() {
        return statsRepository;
    }

    public void saveSymbols(List<Symbol> symbols) {

        for (Symbol symbol : symbols) {
            symbol.setUpdated(new Date());
        }

        symbolRepository.saveAll(symbols);
    }

    public void saveOptions(List<Option> options) {

        for (Option option : options) {
            option.setUpdated(new Date());
        }

        optionRepository.saveAll(options);
    }

}
