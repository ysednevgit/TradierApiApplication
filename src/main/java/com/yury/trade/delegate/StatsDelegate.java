package com.yury.trade.delegate;

import com.yury.trade.entity.OptionV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Component
public class StatsDelegate {

    @Autowired
    private PersistenceDelegate persistenceDelegate;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public void getStats() throws ParseException {

        String stockSymbol = "SPY";
        Date date = sdf.parse("2022-07-22");

        Iterable<OptionV2> options = persistenceDelegate.getOptionRepository().findByUnderlyingAndGreeks_updated_at(stockSymbol, date);

        OptionV2 option1 = getClosest(20d, 300, OptionV2.OptionType.call, options);
        OptionV2 option2 = getClosest(80d, 10, OptionV2.OptionType.call, options);

        //strategy 1
        List<OptionV2> options1 = persistenceDelegate.getOptionRepository().findByOptionV2IdSymbol(option1.getOptionV2Id().getSymbol());
        List<OptionV2> options2 = persistenceDelegate.getOptionRepository().findByOptionV2IdSymbol(option2.getOptionV2Id().getSymbol());

        for (int i = 0; i < options1.size(); i++) {
            double price = options1.get(i).getMid_price() * 6 - options2.get(i).getMid_price() * 1;
            System.out.println(price);
        }


    }

    private OptionV2 getClosest(double delta,
                                final int days_to_expiry,
                                final OptionV2.OptionType optionType,
                                final Iterable<OptionV2> options) {

        if (options == null || !options.iterator().hasNext()) {
            return null;
        }

        delta = delta / 100;

        OptionV2 result = null;

        for (OptionV2 optionV2 : options) {

            boolean isCorrectOptionType = optionType.equals(optionV2.getOption_type());

            if (!isCorrectOptionType) {
                continue;
            }

            if (result == null) {

                result = optionV2;
                continue;
            }

            double resultDeltaDistance = Math.abs(Math.abs(result.getDelta()) - Math.abs(delta));
            double resultDaysDistance = Math.abs(result.getDays_left() - days_to_expiry);

            double deltaDistance = Math.abs(Math.abs(optionV2.getDelta()) - Math.abs(delta));
            double daysDistance = Math.abs(optionV2.getDays_left() - days_to_expiry);

            if (daysDistance < resultDaysDistance) {
                result = optionV2;
            }

            if ((daysDistance == resultDaysDistance) && (deltaDistance < resultDeltaDistance)) {
                result = optionV2;
            }

        }

        return result;
    }

}