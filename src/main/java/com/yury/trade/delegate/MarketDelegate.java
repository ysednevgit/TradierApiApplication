package com.yury.trade.delegate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.yury.trade.entity.OptionV2;
import com.yury.trade.entity.OptionV2Id;
import com.yury.trade.entity.StockSymbol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class MarketDelegate {

    @Autowired
    private PersistenceDelegate persistenceDelegate;

    @Autowired
    StatsDelegate statsDelegate;

    @Autowired
    private TradierDelegate tradierDelegate;

    private SimpleDateFormat simpleDateFormatFull = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat simpleDateFormatShort = new SimpleDateFormat("yyMMdd");

    ObjectMapper objectMapper;

    private static DecimalFormat df2 = new DecimalFormat("###.##");


    public String refreshIncompleteOptions() throws ParseException {

        Iterable<OptionV2> options = persistenceDelegate.getOptionRepository().findAll();

        int size = 0;

        for (OptionV2 option : options) {

            Date updated = simpleDateFormat.parse(simpleDateFormat.format(option.getGreeks_updated_at()));

            option.setGreeks_updated_at(updated);
            size++;
        }

        persistenceDelegate.getOptionRepository().saveAll(options);

        System.out.println("Refreshed " + size);

        return "Success";
    }


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

    public List<String> getAllStockSymbols() {

        List<String> symbolNames = persistenceDelegate.getStockSymbolRepository().findAllSymbols();

        System.out.println("Found " + symbolNames.size() + " symbol names");

        return symbolNames;
    }

    public List<String> getEnabledStockSymbols() {

        List<String> symbolNames = persistenceDelegate.getStockSymbolRepository().findEnabledSymbols();

        System.out.println("Found " + symbolNames.size() + " symbol names");

        return symbolNames;
    }

    public String insertOptions() {

        try {
            List<String> stockSymbols = getEnabledStockSymbols();

            for (String stockSymbol : stockSymbols) {
                System.out.println("Getting options data for " + stockSymbol);

                List<String> stockSymbolsList = new ArrayList<>();
                stockSymbolsList.add(stockSymbol);

                List<String> optionSymbols = getOptionSymbols(stockSymbolsList);

                List<OptionV2> options = getOptionsData(optionSymbols, "true");

                System.out.println("Received " + optionSymbols.size() + " options. Inserting options to the database... ");
                persistenceDelegate.getOptionRepository().saveAll(options);
            }

            System.out.println("Success. Inserted");

        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }

        return "Done";
    }

    public List<OptionV2> getOptions(String stockSymbol) throws IOException {

        List<String> stockSymbols = new ArrayList<>();
        stockSymbols.add(stockSymbol);

        List<String> optionSymbols = getOptionSymbols(stockSymbols);

        List<OptionV2> options = getOptionsData(optionSymbols, "true");

        return options;
    }

    private List<String> getOptionSymbols(Collection<String> stockSymbols) throws IOException {
        List<String> optionSymbols = new ArrayList<>();

        int itemId = 0;
        for (String stockSymbol : stockSymbols) {

            itemId++;
            JsonNode jsonNode = tradierDelegate.optionsLookup(stockSymbol);

            if (jsonNode.get("symbols").get(0) == null) {
                continue;
            }

            ArrayNode arrayNode = (ArrayNode) jsonNode.get("symbols").get(0).get("options");

            for (int i = 0; i < arrayNode.size(); i++) {

                optionSymbols.add(arrayNode.get(i).textValue());
            }

            System.out.println("Got " + optionSymbols.size() + " optionSymbols for " + stockSymbol + " " + itemId + "/" + stockSymbols.size());

        }

        return optionSymbols;
    }

    private List<OptionV2> getOptionsData(List<String> symbolNames, String greeks) throws IOException {

        List<OptionV2> options = new ArrayList<>();

        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 1; i <= symbolNames.size(); i++) {

            stringBuilder.append(symbolNames.get(i - 1)).append(",");

            if (i % 250 == 0 || i == symbolNames.size()) {
                stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
                JsonNode jsonNode = tradierDelegate.quotes(stringBuilder.toString(), greeks);

                stringBuilder = new StringBuilder();

                JsonNode quotes = jsonNode.get("quotes");

                ArrayNode optionsArrayNode = (ArrayNode) quotes.get("quote");

                options.addAll(getOptions(optionsArrayNode));
                System.out.println("getOptionsData(). Received " + options.size());
            }
        }

        return options;
    }

    private List<OptionV2> getOptions(ArrayNode symbolsNode) {

        List<OptionV2> options = new ArrayList<>();

        for (int i = 0; i < symbolsNode.size(); i++) {

            try {
                OptionV2 optionV2 = getOption(symbolsNode.get(i));

                if (optionV2 == null) {
                    continue;
                }
                options.add(optionV2);

            } catch (ParseException e) {
                System.out.println("Cannot parse date for " + symbolsNode.get(i));
            }

        }

        return options;
    }

    private OptionV2 getOption(JsonNode node) throws ParseException {

        JsonNode greeksNode = node.get("greeks");

        if (greeksNode == null || greeksNode instanceof NullNode) {
            return null;
        }

        OptionV2 option = getObjectMapper().convertValue(node, OptionV2.class);

        if (option.getAsk() != null && option.getBid() != null) {
            option.setMid_price(Double.parseDouble(df2.format(option.getBid() + (option.getAsk() - option.getBid()) / 2)));
        }

        OptionV2Id id = new OptionV2Id();

        id.setSymbol(node.get("symbol").textValue());
        id.setUpdated(simpleDateFormatFull.parse(greeksNode.get("updated_at").textValue()));

        option.setOptionV2Id(id);

        option.setDays_left(getDaysToExpiration(option));

        option.setDelta(getDouble(greeksNode.get("delta")));
        option.setGamma(getDouble(greeksNode.get("gamma")));
        option.setTheta(getDouble(greeksNode.get("theta")));
        option.setVega(getDouble(greeksNode.get("vega")));
        option.setRho(getDouble(greeksNode.get("rho")));
        option.setBid_iv(getDouble(greeksNode.get("bid_iv")));
        option.setMid_iv(getDouble(greeksNode.get("mid_iv")));
        option.setAsk_iv(getDouble(greeksNode.get("ask_iv")));

        option.setGreeks_updated_at(simpleDateFormat.parse(greeksNode.get("updated_at").textValue()));

        return option;
    }

    private Double getDouble(JsonNode node) {
        if (node == null) {
            return null;
        }
        return Double.parseDouble(node.toString());
    }

    private ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
        return objectMapper;
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
        persistenceDelegate.getStockSymbolRepository().saveAll(symbols);

        return symbols.size();
    }

    private StockSymbol getSymbol(JsonNode node) {
        StockSymbol symbol = getObjectMapper().convertValue(node, StockSymbol.class);

        return symbol;
    }

    private int getDaysToExpiration(OptionV2 option) {
        Date expirationDate = null;
        Date currentDate = option.getOptionV2Id().getUpdated();

        String optionSymbol = option.getOptionV2Id().getSymbol();

        String rootSymbol = optionSymbol.substring(0, optionSymbol.length() - 15);

        optionSymbol = optionSymbol.replace(rootSymbol, "");

        try {
            expirationDate = simpleDateFormatShort.parse(optionSymbol.substring(0, 6));
            currentDate = simpleDateFormatShort.parse(simpleDateFormatShort.format(currentDate));
        } catch (ParseException e) {
            System.out.println("Unable to get expirationDate from " + optionSymbol.substring(0, 6) + " for " + optionSymbol);
        }

        long diffInMillies = Math.abs(currentDate.getTime() - expirationDate.getTime());
        long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

        return (int) diff + 1;
    }

}
