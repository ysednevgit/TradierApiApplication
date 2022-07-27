package com.yury.trade.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yury.trade.delegate.MarketDelegateV2;
import com.yury.trade.delegate.StatsDelegate;
import com.yury.trade.delegate.StockHistoryDelegate;
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
    private MarketDelegateV2 marketsDelegate2;

    @Autowired
    private StockHistoryDelegate stockHistoryDelegate;

    @Autowired
    private StatsDelegate statsDelegate;

    @GetMapping("/get_stats")
    public String getStats() throws ParseException {

        statsDelegate.getStats();

        return "Success";
    }

    @PostMapping("/refresh")
    public String refresh() throws ParseException {

        return marketsDelegate2.refreshIncompleteOptions();
    }

    @PostMapping("/add_stock_history")
    public String addStockHistory(@RequestParam(value = "start") String start,
                                  @RequestParam(value = "end") String end) throws IOException {

        return stockHistoryDelegate.addStockHistory(start, end);
    }

    @PostMapping("/add_stock_symbols")
    public String addStockSymbols(@RequestParam(value = "indexes") String indexes) throws IOException {

        return marketsDelegate2.addStockSymbols(indexes);
    }

    @PostMapping("/insert_options_data")
    public String insertOptionsData() throws IOException {

        return marketsDelegate2.insertOptions();
    }

    @GetMapping("/get_options_data")
    public @ResponseBody List<OptionV2> getOptionsData(@RequestParam(value = "stockSymbol") String stockSymbol) throws IOException {

        return marketsDelegate2.getOptions(stockSymbol);
    }

}