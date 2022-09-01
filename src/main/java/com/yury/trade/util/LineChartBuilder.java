package com.yury.trade.util;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class LineChartBuilder extends JFrame {

    private Color[] colors = new Color[]{Color.RED, Color.black, Color.BLUE, Color.CYAN, Color.GRAY, Color.GREEN, Color.MAGENTA,
            Color.ORANGE, Color.YELLOW, Color.PINK};

    public LineChartBuilder(final String symbol, List<LineChartDataset> datasets) {

        initUI(symbol, datasets);
    }

    private void initUI(String symbol, List<LineChartDataset> datasets) {

        XYDataset dataset = createDataset(datasets);
        JFreeChart chart = createChart(dataset);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        chartPanel.setBackground(Color.white);

        add(chartPanel);

        pack();
        setTitle(symbol + " Options Chart");
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private XYDataset createDataset(List<LineChartDataset> datasets) {

        XYSeriesCollection collection = new XYSeriesCollection();

        for (LineChartDataset dataset : datasets) {

            var series = new XYSeries(dataset.getName());

            for (Map.Entry<Integer, Integer> entry : dataset.getData().entrySet()) {
                series.add(entry.getKey(), entry.getValue());
            }
            collection.addSeries(series);
        }

        return collection;
    }

    private JFreeChart createChart(final XYDataset dataset) {

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Performance",
                "Days",
                "Account change ($)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();

        var renderer = new XYLineAndShapeRenderer();

        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            renderer.setSeriesPaint(i, getColor(i));
            renderer.setSeriesStroke(i, new BasicStroke(2.0f));
        }

        XYLineAnnotation line = new XYLineAnnotation(
                0, 0, plot.getDomainAxis().getRange().getLength(), 0, new BasicStroke(2.0f), Color.black);
        plot.addAnnotation(line);

        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.white);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);

        chart.getLegend().setFrame(BlockBorder.NONE);

        chart.setTitle(new TextTitle("Options Chart",
                        new Font("Serif", Font.BOLD, 18)
                )
        );

        return chart;
    }

    private Color getColor(int index) {

        return index < colors.length ? colors[index] : getRandomColor();
    }

    private Color getRandomColor() {
        Random rand = new Random();
        // Java 'Color' class takes 3 floats, from 0 to 1.
        float r = rand.nextFloat();
        float g = rand.nextFloat();
        float b = rand.nextFloat();
        return new Color(r, g, b);
    }


}