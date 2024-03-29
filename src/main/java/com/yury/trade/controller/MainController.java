package com.yury.trade.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yury.trade.delegate.*;
import com.yury.trade.entity.OptionV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @Autowired
    private CustomStatsDelegate customStatsDelegate;

    @Autowired
    private ChartDelegate chartDelegate;

    @Autowired
    private FlowDelegate flowDelegate;

    @Autowired
    private VolatilityDelegate volatilityDelegate;

    @GetMapping("/get_flow")
    public ResponseEntity<String> getFlow(@RequestParam(value = "symbol", required = false) String symbol,
                          @RequestParam(value = "startDate", required = false) String startDate,
                          @RequestParam(value = "endDate", required = false) String endDate,
                          @RequestParam(value = "debug", required = false) boolean debug,
                          @RequestParam(value = "drawChart", required = false) boolean drawChart) throws Exception {

        flowDelegate.getFlow(symbol, startDate, endDate, debug, drawChart);

        return new ResponseEntity("Success", HttpStatus.OK);
    }

    @GetMapping("/draw_chart")
    public String drawChart(@RequestParam(value = "symbol", required = true) String symbol,
                            @RequestParam(value = "startDate", required = false) String startDate,
                            @RequestParam(value = "test", required = false) boolean test) throws Exception {

        chartDelegate.drawChart(symbol, startDate, test);

        return "Success";
    }

    @GetMapping("/draw_flow_chart")
    public String drawFlowChart(@RequestParam(value = "symbol", required = false) String symbol,
                                @RequestParam(value = "startDate") String startDate,
                                @RequestParam(value = "endDate", required = false) String endDate,
                                @RequestParam(value = "combo", required = false) boolean combo) throws Exception {

        chartDelegate.drawFlowChart(symbol, startDate, endDate, combo);

        return "Success";
    }

    @GetMapping("/draw_volatility_weekday_chart")
    public String drawWeekdayChart(@RequestParam(value = "symbol") String symbol,
                                   @RequestParam(value = "startDate") String startDate) throws Exception {

        volatilityDelegate.showWeekdayChart(symbol, startDate);

        return "Success";
    }

    @GetMapping("/get_stats")
    public String getStats(@RequestParam(value = "symbol", required = false) String symbol,
                           @RequestParam(value = "startDate", required = false) String startDate,
                           @RequestParam(value = "debug", required = false) boolean debug,
                           @RequestParam(value = "test", required = false) boolean test,
                           @RequestParam(value = "drawChart", required = false) boolean drawChart) throws Exception {

        statsDelegate.getStats(symbol, startDate, debug, test);

        if (drawChart) {
            chartDelegate.drawChart(symbol, startDate, test);
        }

        return "Success";
    }

    @GetMapping("/get_custom_stats")
    public String getCustomStats(@RequestParam(value = "symbol", required = false) String symbol,
                                 @RequestParam(value = "startDate", required = false) String startDate) throws ParseException {

        customStatsDelegate.getStats(symbol, startDate);

        return "Success";
    }

    @GetMapping("/get_volatility")
    public String getStats(@RequestParam(value = "symbol", required = false) String symbol,
                           @RequestParam(value = "startDate", required = false) String startDate,
                           @RequestParam(value = "endDate", required = false) String endDate) throws Exception {

        volatilityDelegate.calcVolatility(symbol, startDate, endDate);

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
                                  @RequestParam(value = "end") String end,
                                  @RequestParam(value = "all", defaultValue = "false") boolean all) throws IOException {

        return stockHistoryDelegate.addStockHistory(start, end, all);
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