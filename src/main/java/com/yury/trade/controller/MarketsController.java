package com.yury.trade.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yury.trade.delegate.MarketsDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;

@RestController
@RequestMapping("/markets")
public class MarketsController {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MarketsDelegate marketsDelegate;

    @PostMapping("/add_stock_symbols")
    public @ResponseBody
    String addStockSymbols(@RequestParam(value = "indexes", defaultValue = "false") String indexes) throws IOException {

        return marketsDelegate.addStockSymbols(indexes);
    }

    @PostMapping("/refresh_stock_options")
    public @ResponseBody
    String refreshStockOptions() throws IOException {

        return marketsDelegate.refreshStockOptions();
    }

    @PostMapping("/update_stock_symbols")
    public @ResponseBody
    String updateStockSymbols(@RequestParam(value = "indexes", defaultValue = "false") String indexes) throws IOException {

        return marketsDelegate.updateStockSymbols();
    }

    @PostMapping("/add_stock_options")
    public @ResponseBody
    String addStockOptions(@RequestParam(value = "letters", defaultValue = "A") String letters) throws IOException, ParseException {

        return marketsDelegate.addStockOptionSymbols(letters);
    }

    @PostMapping("/update_stock_options")
    public @ResponseBody
    String updateStockOptions() throws IOException, ParseException {

        return marketsDelegate.updateStockOptions();
    }





}