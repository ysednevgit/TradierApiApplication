package com.yury.trade.delegate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.yury.trade.entity.Stats;
import com.yury.trade.entity.StockSymbol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class MarketsDelegate {

    @Autowired
    private PersistenceDelegate persistenceDelegate;

    @Autowired
    StatsDelegate statsDelegate;

    @Autowired
    private TradierDelegate tradierDelegate;

    ObjectMapper objectMapper;

    private static DecimalFormat df2 = new DecimalFormat("###.##");

    private ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
        return objectMapper;
    }

/**
    public String addStockSymbols(String indexes) throws IOException {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        for (char ch : letters.toCharArray()) {
            JsonNode jsonNode = tradierDelegate.search(Character.toString(ch), indexes);

            ArrayNode symbolsArrayNode = (ArrayNode) jsonNode.get("securities").get("security");

            insertSymbols(symbolsArrayNode);
            System.out.println("Inserted " + symbolsArrayNode.size() + " for letter " + ch);
        }

        return "Success";
    }

    public String updateStockSymbols() throws IOException {

        List<String> symbolNames = persistenceDelegate.getSymbolRepository().findAllSymbols();

        System.out.println("Found " + symbolNames.size() + " symbol names");

        insertSymbols(symbolNames, "false");

        return "Success";
    }

    public String refreshStockOptions() throws IOException {

        System.out.println("Updating Stock symbols............");

        updateStockSymbols();

        System.out.println("Updating Stock symbols.......DONE ");

        System.out.println("Adding Stock options symbols............");

        addStockOptionSymbols("ABCDEFGHIJKLMNOPQRSTUVWXYZ");

        System.out.println("Adding Stock options symbols.......DONE ");

        System.out.println("Updating Stats............");

        updateStockOptions();

        System.out.println("Updating Stats.......DONE ");

        return "Success";
    }

    public String addOptionsV2() throws IOException {

        System.out.println("Updating Stock symbols............");

        updateStockSymbols();

        System.out.println("Updating Stock symbols.......DONE ");

        System.out.println("Adding Stock options symbols............");

        addStockOptionSymbols("ABCDEFGHIJKLMNOPQRSTUVWXYZ");

        System.out.println("Adding Stock options symbols.......DONE ");

        System.out.println("Updating Stats............");

        updateStockOptions();

        System.out.println("Updating Stats.......DONE ");

        return "Success";
    }

    public String addStockOptionSymbols(String letters) throws IOException {

        Collection<StockSymbol> symbols = persistenceDelegate.getSymbolRepository().findGoodSymbols();

        filterOptionSymbols(symbols, letters);

        List<String> optionSymbols = getOptionSymbols(symbols);

        System.out.println("Found " + optionSymbols.size() + " optionSymbols");

//        insertSymbols(optionSymbols, "true");

        return "Success";
    }

    public String updateStockOptions() {

        Collection<StockSymbol> optionSymbols = persistenceDelegate.getSymbolRepository().findAllOptions();

        System.out.println("Found " + optionSymbols.size() + " optionSymbols");

        Map<String, List<StockSymbol>> optionSymbolsMap = getOptionSymbolsMap(optionSymbols);

        List<Stats> stats = new ArrayList<>();

        for (String symbol : optionSymbolsMap.keySet()) {
            Stats statsObj = statsDelegate.getStats(optionSymbolsMap.get(symbol));
            if (statsObj != null) {
                stats.add(statsObj);
            }
        }

        persistenceDelegate.getStatsRepository().saveAll(stats);

        return "Success";
    }

    Map<String, List<StockSymbol>> getOptionSymbolsMap(Collection<StockSymbol> optionSymbols) {
        Map<String, List<StockSymbol>> optionSymbolsMap = new HashMap<>();

        for (StockSymbol symbol : optionSymbols) {
            String key = symbol.getRoot_symbol();

            if (optionSymbolsMap.containsKey(key)) {
                optionSymbolsMap.get(key).add(symbol);
            } else {
                List<StockSymbol> symbols = new ArrayList<>();
                symbols.add(symbol);
                optionSymbolsMap.put(key, symbols);
            }
        }
        return optionSymbolsMap;
    }

    private StockSymbol insertOptionSymbol(String optionSymbolName) throws IOException {

        JsonNode jsonNode = tradierDelegate.quotes(optionSymbolName, "true");

        StockSymbol symbol = getSymbol(jsonNode.get("quotes").get("quote"));

        if (symbol != null) {
            symbol.setUpdated(new Date());
            persistenceDelegate.getSymbolRepository().save(symbol);
            System.out.println("Inserted " + optionSymbolName + " iv=" + symbol.getGreeks_iv());
        }
        return symbol;
    }

    private void insertSymbols(List<String> symbolNames, String greeks) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        int inserted = 0;
        for (int i = 1; i <= symbolNames.size(); i++) {

            stringBuilder.append(symbolNames.get(i - 1)).append(",");

            if (i % 100 == 0 || i == symbolNames.size()) {
                stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
                JsonNode jsonNode = tradierDelegate.quotes(stringBuilder.toString(), greeks);
                ArrayNode symbolsArrayNode = (ArrayNode) jsonNode.get("quotes").get("quote");

                inserted += insertSymbols(symbolsArrayNode);
                System.out.println("Inserted " + inserted + " left= " + (symbolNames.size() - inserted) + " i=" + i);
                stringBuilder = new StringBuilder();
            }
        }
    }

    private StockSymbol getSymbol(JsonNode node) {
        StockSymbol symbol = getObjectMapper().convertValue(node, StockSymbol.class);

        if (StockSymbol.Type.option.equals(symbol.getType())) {

            JsonNode greeksNode = node.get("greeks");

            if (greeksNode == null || greeksNode instanceof NullNode) {
                return null;
            }
            JsonNode bidIVNode = greeksNode.get("bid_iv");
            JsonNode askIVNode = greeksNode.get("ask_iv");
            JsonNode midIVNode = greeksNode.get("mid_iv");

            if (bidIVNode == null || askIVNode == null || midIVNode == null) {
                return null;
            }

            double bidAskIVRatio = (bidIVNode.doubleValue()) / (askIVNode.doubleValue());

            if (bidAskIVRatio < 0.87) {
                return null;
            }

            double midIv = midIVNode.asDouble();

            symbol.setGreeks_iv(Math.round(midIv * 100.0) / 100.0);
        }
        return symbol;
    }

    private int insertSymbols(ArrayNode symbolsNode) {

        List<StockSymbol> symbols = new ArrayList<>();

        for (int i = 0; i < symbolsNode.size(); i++) {
            StockSymbol symbol = getSymbol(symbolsNode.get(i));

            if (symbol == null) {
                continue;
            }
            symbols.add(symbol);
        }
        persistenceDelegate.saveSymbols(symbols);

        return symbols.size();
    }

    private void filterOptionSymbols(Collection<StockSymbol> symbols, String letters) {
        Iterator<StockSymbol> iterator = symbols.iterator();

        List<Character> chars = new ArrayList<>();
        for (char ch : letters.toCharArray()) {
            chars.add(ch);
        }

        while (iterator.hasNext()) {
            StockSymbol symbol = iterator.next();

            Character prefix = symbol.getSymbol().charAt(0);

            if (!chars.contains(prefix)) {
                iterator.remove();
            }
        }

    }

    private List<String> getOptionSymbols(Collection<StockSymbol> symbols) throws IOException {
        List<String> optionSymbols = new ArrayList<>();
        List<String> tempOptionSymbols = new ArrayList<>();

        int itemId = 0;
        for (StockSymbol symbol : symbols) {

            itemId++;
            Double last = symbol.getLast();
            JsonNode jsonNode = tradierDelegate.optionsLookup(symbol.getSymbol());

            if (jsonNode.get("symbols").get(0) == null) {
                continue;
            }

            ArrayNode arrayNode = (ArrayNode) jsonNode.get("symbols").get(0).get("options");

            for (int i = 0; i < arrayNode.size(); i++) {

                tempOptionSymbols.add(arrayNode.get(i).textValue());
            }

            List<String> filteredOptionSymbols = getFilteredOptionSymbols(tempOptionSymbols, last);
            System.out.println("Got " + filteredOptionSymbols.size() + " optionSymbols for " + symbol.getSymbol() + " " + itemId + "/" + symbols.size());

            if (filteredOptionSymbols.size() > 0) {
                StockSymbol symbol1 = insertOptionSymbol(filteredOptionSymbols.get(0));

                if (symbol1 != null) {
                    insertOptionSymbol(filteredOptionSymbols.get(1));
                }
                optionSymbols.addAll(filteredOptionSymbols);
            }

            tempOptionSymbols.clear();
        }

        return optionSymbols;
    }

    private Double getStrike(String optionSymbol) {

        String value = optionSymbol.substring(optionSymbol.length() - 8, optionSymbol.length() - 3) + "." + optionSymbol.substring(optionSymbol.length() - 3);
        return Double.parseDouble(value);
    }

    private List<String> getFilteredOptionSymbols(List<String> optionSymbols, Double last) {

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyMMdd");

        LocalDateTime now = LocalDateTime.now();

        LocalDate minLocalDate = null;
        LocalDate maxLocalDate = null;

        String minOptionSymbol = null;
        String maxOptionSymbol = null;

        for (String optionSymbol : optionSymbols) {

            String optionType = optionSymbol.substring(optionSymbol.length() - 9, optionSymbol.length() - 8);
            if (!optionType.equals("C")) {
                continue;
            }

            Double strike = getStrike(optionSymbol);

            if (strike < last) {
                continue;
            }

            String expirationDateString = optionSymbol.substring(optionSymbol.length() - 15, optionSymbol.length() - 9);

            LocalDate localDate = LocalDate.from(dtf.parse(expirationDateString));

            if (localDate.minusDays(21l).isBefore(now.toLocalDate())) {
                continue;
            }

            if (minLocalDate == null || minLocalDate.isAfter(localDate)) {
                minLocalDate = localDate;
                minOptionSymbol = optionSymbol;
            }

            if (maxLocalDate == null || maxLocalDate.isBefore(localDate)) {
                maxLocalDate = localDate;
                maxOptionSymbol = optionSymbol;
            }

        }

        List<String> result = new ArrayList<>();
        if (minOptionSymbol != null) {
            result.add(minOptionSymbol);
        }
        if (maxOptionSymbol != null) {
            result.add(maxOptionSymbol);
        }

        return result;
    }

 **/

}
