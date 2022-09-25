package com.yury.trade.util;

import com.yury.trade.entity.OptionV2;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Position implements Serializable {

    private static DecimalFormat df2 = new DecimalFormat("###.##");

    public List<OptionV2> options = new ArrayList<>();
    public final List<Integer> coeffs = new ArrayList<>();
    //how many rolls happened for every leg
    public List<Integer> rolls = new ArrayList<>();

    public int contractSize = 100;

    public double positionDelta = 0;
    public double positionTheta = 0;
    public double positionGamma = 0;
    public double positionPrice = 0;
    public double adjustments = 0;

    public int daysRun = 0;

    public void roll(int legIndex) {

        rolls.set(legIndex, rolls.get(legIndex) + 1);
    }

    public void addCoeff(int coeff) {
        coeffs.add(coeff);
        rolls.add(0);
    }

    public boolean add(final List<List<OptionV2>> legOptionsList, int index) {

        Date updated = legOptionsList.get(0).get(index).getGreeks_updated_at();

        if (legOptionsList.size() != coeffs.size()) {
            return false;
        }

        for (List<OptionV2> legOptions : legOptionsList) {

            if (legOptions.size() <= index || !updated.equals(legOptions.get(index).getGreeks_updated_at())) {
                return false;
            }
        }

        options.clear();

        for (List<OptionV2> legOptions : legOptionsList) {
            OptionV2 option = legOptions.get(index);

            if (option.getMid_price() == null || option.getMid_price() == 0) {
                return false;
            }
            options.add(option);
        }
        calc();

        return true;
    }

    @Override
    public String toString() {
        return "delta=" + df2.format(contractSize * positionDelta) +
                ", theta=" + df2.format(contractSize * positionTheta) +
                ", gamma=" + df2.format(contractSize * positionGamma) +
                ", $" + df2.format(contractSize * positionPrice) +
                ", adj=" + df2.format(adjustments) +
                ", " + coeffs +
                ", " + options +
                "} ";
    }

    private void calc() {
        positionDelta = 0;
        positionTheta = 0;
        positionGamma = 0;
        positionPrice = 0;

        for (int i = 0; i < options.size(); i++) {
            OptionV2 option = options.get(i);
            int coeff = coeffs.get(i);

            positionPrice += option.getMid_price() * coeff;
            positionTheta += option.getTheta() * coeff;
            positionGamma += option.getGamma() * coeff;
            positionDelta += option.getDelta() * coeff;
        }
    }
}

