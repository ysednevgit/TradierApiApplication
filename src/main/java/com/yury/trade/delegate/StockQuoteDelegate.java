package com.yury.trade.delegate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.yury.trade.entity.StockQuote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class StockQuoteDelegate {

    @Autowired
    private PersistenceDelegate persistenceDelegate;

    @Autowired
    private TradierDelegate tradierDelegate;

    ObjectMapper objectMapper;

    private ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
        return objectMapper;
    }

    public String addStockQuotes() throws IOException {

        List<StockQuote> stockQuotes = getStockQuotes(persistenceDelegate.getStockSymbolRepository().findAllSymbols());

        persistenceDelegate.getStockQuoteRepository().saveAll(stockQuotes);

        System.out.println("Saved " + stockQuotes.size() + " stockQuotes");

        System.out.println("Done.");

        return "Success";
    }

    private List<StockQuote> getStockQuotes(final List<String> stockSymbols) throws IOException {

        List<StockQuote> stockQuotes = new ArrayList<>();

        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 1; i <= stockSymbols.size(); i++) {

            stringBuilder.append(stockSymbols.get(i - 1)).append(",");

            if (i % 500 == 0 || i == stockSymbols.size()) {
                stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
                JsonNode jsonNode = tradierDelegate.quotes(stringBuilder.toString(), "false");

                stringBuilder = new StringBuilder();

                JsonNode quotes = jsonNode.get("quotes");

                ArrayNode arrayNode = (ArrayNode) quotes.get("quote");

                stockQuotes.addAll(getStockQuotes(arrayNode));
                System.out.println("getStockQuotes(). Received " + stockQuotes.size());
            }
        }

        return stockQuotes;
    }

    private List<StockQuote> getStockQuotes(ArrayNode arrayNode) {

        List<StockQuote> stockQuotes = new ArrayList<>();

        for (int i = 0; i < arrayNode.size(); i++) {

            try {
                StockQuote stockQuote = getStockQuote(arrayNode.get(i));

                if (stockQuotes == null) {
                    continue;
                }
                stockQuotes.add(stockQuote);

            } catch (ParseException e) {
                System.out.println("Cannot parse " + stockQuotes.get(i));
            }
        }

        return stockQuotes;
    }

    private StockQuote getStockQuote(JsonNode node) throws ParseException {

        StockQuote stockQuote = getObjectMapper().convertValue(node, StockQuote.class);

        return stockQuote;
    }


}
