package com.yury.trade.util;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;

import javax.swing.*;

public class BarChartBuilder extends JFrame {

    private static final long serialVersionUID = 1L;

    public BarChartBuilder(String appTitle, CategoryDataset dataset,
                           String horizontalTitle, String verticalTitle, String categoriesTitle) {
        super(appTitle);

        //Create chart
        JFreeChart chart=ChartFactory.createBarChart(
                horizontalTitle, //Chart Title
                categoriesTitle, // Category axis
                verticalTitle, // Value axis
                dataset,
                PlotOrientation.VERTICAL,
                true,true,false
        );

        ChartPanel panel= new ChartPanel(chart);
        setContentPane(panel);
    }

}