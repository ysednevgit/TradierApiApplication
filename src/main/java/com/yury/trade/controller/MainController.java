package com.yury.trade.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yury.trade.delegate.MarketDelegate;
import com.yury.trade.delegate.StatsDelegate;
import com.yury.trade.delegate.StockHistoryDelegate;
import com.yury.trade.delegate.StockQuoteDelegate;
import com.yury.trade.entity.OptionV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

@RestController
@RequestMapping("/markets")
public class MainController {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MarketDelegate marketsDelegate;

    @Autowired
    private StockHistoryDelegate stockHistoryDelegate;

    @Autowired
    private StockQuoteDelegate stockQuoteDelegate;

    @Autowired
    private StatsDelegate statsDelegate;

    @GetMapping("/get_stats")
    public String getStats(@RequestParam(value = "createHistory") boolean createHistory) throws ParseException {

        statsDelegate.getStats(createHistory);

        return "Success";
    }

    @PostMapping("/refresh")
    public String refresh() throws ParseException {

        return marketsDelegate.refreshIncompleteOptions();
    }

    @PostMapping("/add_stock_quotes")
    public String addStockQuotes() throws IOException {

        return stockQuoteDelegate.addStockQuotes();
    }

    @PostMapping("/add_stock_history")
    public String addStockHistory(@RequestParam(value = "start") String start,
                                  @RequestParam(value = "end") String end) throws IOException {

        return stockHistoryDelegate.addStockHistory(start, end);
    }

    @PostMapping("/add_stock_symbols")
    public String addStockSymbols(@RequestParam(value = "indexes") String indexes) throws IOException {

        return marketsDelegate.addStockSymbols(indexes);
    }

    @PostMapping("/insert_options_data")
    public String insertOptionsData() throws IOException {

        return marketsDelegate.insertOptions();
    }

    @GetMapping("/get_options_data")
    public @ResponseBody List<OptionV2> getOptionsData(@RequestParam(value = "stockSymbol") String stockSymbol) throws IOException {

        return marketsDelegate.getOptions(stockSymbol);
    }

}