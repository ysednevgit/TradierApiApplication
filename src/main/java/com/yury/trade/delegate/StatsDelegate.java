package com.yury.trade.delegate;

import com.yury.trade.entity.Stats;
import com.yury.trade.entity.Symbol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;

@Component
public class StatsDelegate {

    @Autowired
    private PersistenceDelegate persistenceDelegate;

    public Stats getStats(Collection<Symbol> optionSymbols) {

        persistenceDelegate.getStatsRepository().deleteAll();

        if (optionSymbols == null || optionSymbols.size() < 2) {
            return null;
        }

        Stats stats = new Stats();
        stats.setUpdated(new Date());

        Double lowIvPut = null;
        String lowIvPutSymbol = null;

        Double highIvPut = null;
        String highIvPutSymbol = null;

        Double lowIvCall = null;
        String lowIvCallSymbol = null;

        Double highIvCall = null;
        String highIvCallSymbol = null;

        Double putRatio;
        Double callRatio;

        for (Symbol optionSymbol : optionSymbols) {

            if (stats.getSymbol() == null) {
                stats.setSymbol(optionSymbol.getRoot_symbol());
            }

            if (Symbol.OptionType.call.equals(optionSymbol.getOption_type())) {
                if (lowIvCall == null || lowIvCall > optionSymbol.getGreeks_iv()) {
                    lowIvCall = optionSymbol.getGreeks_iv();
                    lowIvCallSymbol = optionSymbol.getSymbol();
                }
                if (highIvCall == null || highIvCall < optionSymbol.getGreeks_iv()) {
                    highIvCall = optionSymbol.getGreeks_iv();
                    highIvCallSymbol = optionSymbol.getSymbol();
                }
            }

            if (Symbol.OptionType.put.equals(optionSymbol.getOption_type())) {
                if (lowIvPut == null || lowIvPut > optionSymbol.getGreeks_iv()) {
                    lowIvPut = optionSymbol.getGreeks_iv();
                    lowIvPutSymbol = optionSymbol.getSymbol();
                }
                if (highIvPut == null || highIvPut < optionSymbol.getGreeks_iv()) {
                    highIvPut = optionSymbol.getGreeks_iv();
                    highIvPutSymbol = optionSymbol.getSymbol();
                }
            }

        }

        if (lowIvPut != null) {
            putRatio = highIvPut / lowIvPut;
            stats.setPutRatio(putRatio);
        }
        if (lowIvCall != null) {
            callRatio = highIvCall / lowIvCall;
            stats.setCallRatio(Math.round(callRatio * 100.0) / 100.0);
        }

        stats.setLowIvCall(lowIvCall);
        stats.setHighIvCall(highIvCall);
        stats.setLowIvPut(lowIvPut);
        stats.setHighIvPut(highIvPut);

        stats.setLowIvCallSymbol(lowIvCallSymbol);
        stats.setHighIvCallSymbol(highIvCallSymbol);
        stats.setLowIvPutSymbol(lowIvPutSymbol);
        stats.setHighIvPutSymbol(highIvPutSymbol);

        return stats;
    }


}
