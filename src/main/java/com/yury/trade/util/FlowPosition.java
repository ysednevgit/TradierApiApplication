package com.yury.trade.util;

import com.yury.trade.entity.OptionV2;
import lombok.Data;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.*;

@Data
public class FlowPosition implements Serializable {

    private static DecimalFormat df2 = new DecimalFormat("###.##");

    private Map<String, Item> itemsMap = new LinkedHashMap<>();

    private Map<String, List<OptionV2>> optionsByOptionSymbolMap = new HashMap<>();

    private List<Integer> coeffsHistory = new LinkedList<>();

    private int contractSize = 100;

    private Double shortStrike;

    private OptionV2 lastOption;

    private double positionDelta = 0;
    private double positionTheta = 0;
    private double positionGamma = 0;
    private double positionPrice = 0;
    private double adjustments = 0;

    public void add(OptionV2 optionV2, int coeff) {
        itemsMap.put(optionV2.getOptionV2Id().getSymbol(), new Item(optionV2, coeff));

        if (coeff < 0) {
            shortStrike = optionV2.getStrike();
        }

        lastOption = optionV2;
    }

    public int getOptionsCount() {
        int count = 0;
        for (Item item : itemsMap.values()) {
            count = count + Math.abs(item.getCoeff());
        }
        return count;
    }

    @Override
    public String toString() {
        return "delta=" + df2.format(contractSize * positionDelta) +
                ", theta=" + df2.format(contractSize * positionTheta) +
                ", gamma=" + df2.format(contractSize * positionGamma) +
                ", $" + df2.format(contractSize * positionPrice) +
                ", adj=" + df2.format(adjustments) +
                ", " + itemsMap.values() +
                "} ";
    }

    public void calc() {
        positionDelta = 0;
        positionTheta = 0;
        positionGamma = 0;
        positionPrice = 0;

        for (Map.Entry<String, Item> entry : itemsMap.entrySet()) {
            OptionV2 option = entry.getValue().getOptionV2();
            int coeff = entry.getValue().getCoeff();

            positionPrice += option.getMid_price() * coeff;
            positionTheta += option.getTheta() * coeff;
            positionGamma += option.getGamma() * coeff;
            positionDelta += option.getDelta() * coeff;
        }
    }

    @Data
    public class Item {
        private OptionV2 optionV2;
        private int coeff = 1;
        private int index = 0;

        public Item(OptionV2 optionV2, int coeff) {
            this.optionV2 = optionV2;
            this.coeff = coeff;
        }

        @Override
        public String toString() {
            return coeff + " " + optionV2;
        }
    }

}
