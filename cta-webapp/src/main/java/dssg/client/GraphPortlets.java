package dssg.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import com.extjs.gxt.charts.client.Chart;
import com.extjs.gxt.charts.client.model.ChartModel;
import com.extjs.gxt.charts.client.model.Legend;
import com.extjs.gxt.charts.client.model.Legend.Position;
import com.extjs.gxt.charts.client.model.axis.XAxis;
import com.extjs.gxt.charts.client.model.axis.YAxis;
import com.extjs.gxt.charts.client.model.charts.FilledBarChart;
import com.extjs.gxt.charts.client.model.charts.LineChart;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.IconButtonEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.event.SliderEvent;
import com.extjs.gxt.ui.client.fx.Resizable;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.Padding;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.Slider;
import com.extjs.gxt.ui.client.widget.button.ToolButton;
import com.extjs.gxt.ui.client.widget.custom.Portlet;
import com.extjs.gxt.ui.client.widget.form.SliderField;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.grid.CellEditor;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayoutData;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout.VBoxLayoutAlign;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Random;

public class GraphPortlets extends Portlet {
  // Global variables
  private ContentPanel panel;
  private Command updateChart1Cmd;
  private Command updateChart2Cmd;
  private Command updateChart3Cmd;
  private Command updateSliderCmd;
  private Command updateSlider2Cmd;
  private String dataType;
  private Integer timeSliderPosition;
  private String route;
  private String direction;
  private Integer startT;
  private Integer stopT;
  private Integer numStops;
  private Integer stopTageoid;
  private Integer[][] sim_dataTimeWindow;
  private Integer[][] sim_dataTimeStop;
  private Resizable r;
  private String title;

  /*
   * Constructor for LOAD and FLOW
   */
  public GraphPortlets(String dataType, String route, String direction,
      Integer startT, Integer stopT, Date date,
      Map<String, Integer[]> sim_data) {

    // Global variable initializers
    timeSliderPosition = startT*2;
    stopTageoid = 0;
    this.dataType = dataType;
    this.route = route;
    if (direction.equals(""))
      MessageBox.alert("Error with direction", "Direction does not exist.", null);
    else
      this.direction = direction;

    this.startT = startT;
    this.stopT = stopT;
    

    // Create the simulated data arrays
    sim_dataTimeWindow = new Integer[2][48];
    
    if (dataType.equals("Load")) {
      this.numStops = sim_data.get("load_timestop_N_stop1").length;
      sim_dataTimeWindow[0] = sim_data.get("max_load_N");
      sim_dataTimeWindow[1] = sim_data.get("max_load_S");
      sim_dataTimeStop = new Integer[48][numStops];
      for(int i=0;i<numStops;i++)
        sim_dataTimeStop[i]=sim_data.get("load_timestop_N_stop"+i);
    }

    if (dataType.equals("Flow")) {
      this.numStops = sim_data.get("flow_timestop_N_stop1").length;
      sim_dataTimeWindow[0] = sim_data.get("max_flow_N");
      sim_dataTimeWindow[1] = sim_data.get("max_flow_S");
      sim_dataTimeStop = new Integer[48][numStops];
      for(int i=0;i<numStops;i++)
        sim_dataTimeStop[i]=sim_data.get("flow_timestop_N_stop"+i);
    }
    

    
    
    // Box Layout for the charts and sliders
    final VBoxLayout portletLayout = new VBoxLayout();
    portletLayout.setPadding(new Padding(15));
    portletLayout.setVBoxLayoutAlign(VBoxLayoutAlign.STRETCH);

    // Layout preferences
    this.setHeadingHtml("Route " + route + ": " + dataType + "  (" + date+")");
    configPanel(this);
    this.setHeight(750);
    this.setLayout(portletLayout);
    r = new Resizable(this);
    r.setDynamic(true);
    final VBoxLayoutData vBoxData = new VBoxLayoutData(10, 15, 10, 25);
    final VBoxLayoutData vBoxData2 = new VBoxLayoutData(10, 10, 10, 25);

    this.add(panelChartTimeWindow());
    this.add(hourControls(), vBoxData);
    this.add(panelChartAllStops());
    this.add(stopControls(), vBoxData2);

  }

  /*
   * Create All Elements Left Potlet (Double Chart)
   */
  // Panel for Time Window chart
  private ContentPanel panelChartTimeWindow() {
    // Local variables
    String url;
    final Chart chart;
    // Content panel for chart
    panel = new ContentPanel();
    panel.setFrame(false);
    panel.setBodyBorder(false);
    panel.setHeaderVisible(false);
    // Chart info
    url = "chart/open-flash-chart.swf";
    chart = new Chart(url);
    chart.setBorders(true);
    chart.setHeight(310);
    panel.add(chart);
    updateChart1Cmd = new Command() {
      @Override
      public void execute() {
        chart.setChartModel(createChartTimeWindow());
      }
    };
    updateChart1Cmd.execute();

    return panel;
  }

  // Line chart Time Window
  private ChartModel createChartTimeWindow() {
    // Choose title
    if (dataType.equals("Load"))
      title = "   Max Load by half hour, all stops.";
    if (dataType.equals("Flow"))
      title = "   Max Flow by half hour, all stops.";

    // Create a ChartModel with the Chart Title and some style attributes
    ChartModel cm = new ChartModel(title,
        "font-size: 14px; font-family:      Verdana; text-align: center;");
    // Code to add legends and paddings
    Legend lg = new Legend(Position.TOP, true);
    lg.setPadding(5);
    lg.setShadow(false);
    cm.setLegend(lg);
    cm.setBackgroundColour("#fffff0");  
    // Create the X axis
    XAxis xa = new XAxis();
    // set the labels for the axis
    for (double i = startT; i <= stopT; i = i + 0.5) {
      if (i % 1 == 0) {
        xa.addLabels(Integer.toString((int) i));
      } else {
        xa.addLabels("");
      }
    }
    cm.setXAxis(xa);

    // Create the Y axis
    YAxis ya = new YAxis();
    // Add the labels to the Y axis
    int maxY = Math.max(getMax(sim_dataTimeWindow[0]),
        getMax(sim_dataTimeWindow[1]));
    ya.setRange(0, 1.1 * maxY, 10);
    cm.setYAxis(ya);

    // Create a Line Chart object NORTH DATA
    LineChart lchart = new LineChart();
    lchart.setAnimateOnShow(true);
    lchart.setColour("#00aa00");
    lchart.setTooltip("#val#");
    lchart.setText("North");
    int i = 0;
    for (double n = startT; n <= stopT; n = n + .5) {
      lchart.addValues(sim_dataTimeWindow[0][i]);
      i++;
    }

    if ((direction.equals("N") || direction.equals("B")) && route != null) {
      cm.addChartConfig(lchart);
    }

    // Create a Line Chart object SOUTH DATA
    lchart = new LineChart();
    lchart.setAnimateOnShow(true);
    lchart.setColour("#ff0000");
    lchart.setTooltip("#val#");
    lchart.setText("South");
    i = 0;
    for (double n = startT; n <= stopT; n = n + .5) {
      lchart.addValues(sim_dataTimeWindow[1][i]);
      i++;
    }
    if ((direction.equals("S") || direction.equals("B")) && route != null) {
      cm.addChartConfig(lchart);
    }

    // Creates the line for max Suggested load
    LineChart lchartD = new LineChart();
    lchartD.setColour("#0099FF");
    lchartD.setText("Max 30ft bus");
    for (double n = startT; n <= stopT; n = n + .5) {
      lchartD.addValues(40);
    }
    if (dataType.equals("Load")&& maxY>=40)
      cm.addChartConfig(lchartD);

    LineChart lchartM = new LineChart();
    lchartM.setColour("#0066FF");
    lchartM.setText("Max 40ft bus");
    for (double n = startT; n <= stopT; n = n + .5) {
      lchartM.addValues(70);
    }
    if (dataType.equals("Load")&& maxY>=70)
      cm.addChartConfig(lchartM);

    LineChart lchartU = new LineChart();
    lchartU.setColour("#0033FF");
    lchartU.setText("Max 60ft bus");
    for (double n = startT; n <= stopT; n = n + .5) {
      lchartU.addValues(95);
    }
    if (dataType.equals("Load")&& maxY>=95)
      cm.addChartConfig(lchartU);

    // Returns the Chart Model
    return cm;
  }

  // Center Slider
  private SliderField hourControls() {
    // Time Window slider
    final Slider slider = new Slider();
    slider.setIncrement(1);
    // Update Execution
    updateSliderCmd = new Command() {
      @Override
      public void execute() {
        slider.setMaxValue(stopT * 2);
        slider.setMinValue(startT * 2);
      }
    };
    updateSliderCmd.execute();

    final SliderField sf = new SliderField(slider);
    sf.setFieldLabel("Time:");

    // Executed when slider position changes
    slider.addListener(Events.Change, new Listener<SliderEvent>() {
      @Override
      public void handleEvent(SliderEvent be) {
        slider.setMessage(((slider.getValue())) * .5 + " hrs");
        timeSliderPosition = (int) be.getNewValue();
        updateChart2Cmd.execute();
      }
    });

    return sf;
  }

  // Panel for All Stops chart
  private ContentPanel panelChartAllStops() {
    // Local variables
    String url;
    final Chart chart;
    // Content panel for chart
    panel = new ContentPanel();
    panel.setFrame(false);
    panel.setBodyBorder(false);
    panel.setHeaderVisible(false);
    // Chart info
    url = "chart/open-flash-chart.swf";
    chart = new Chart(url);
    chart.setBorders(true);
    chart.setHeight(300);
    panel.add(chart);
    updateChart2Cmd = new Command() {
      @Override
      public void execute() {
        chart.setChartModel(createChartAllStops());
        panel.expand();
      }
    };
    updateChart2Cmd.execute();
    return panel;
  }

  // Line chart for All Stops chart
  private ChartModel createChartAllStops() {
    // Choose title
    if (dataType.equals("Load"))
      title = " hrs.   Load by stop.";
    if (dataType.equals("Flow"))
      title = " hrs.   Flow by stop.";
    

    // Create a ChartModel with the Chart Title and some style attributes
    ChartModel cm = new ChartModel("Time: " + (double) timeSliderPosition/2 + title,
        "font-size: 14px; font-family:      Verdana; text-align: center;");
    // Code to add legends and paddings
    Legend lg = new Legend(Position.TOP, true);
    lg.setPadding(5);
    lg.setShadow(false);
    cm.setLegend(lg);
    cm.setBackgroundColour("#fffff0");  
    // Create the X axis
    XAxis xa = new XAxis();
    xa.setOffset(true);
    // set the labels for the axis
    for (int i = 0; i < numStops; i++) {
      if (i % 5 == 0) {
        xa.addLabels(Integer.toString(i));
      } else {
        xa.addLabels("");
      }

    }
    cm.setXAxis(xa);
    // Create the Y axis
    YAxis ya = new YAxis();
    // Add the labels to the Y axis
    int maxY= getMax(sim_dataTimeStop[timeSliderPosition]);
    ya.setRange(0, 1.1*maxY+1, 10);
    cm.setYAxis(ya);

    // Create a Line Chart object NORTH DATA
    LineChart lchart = new LineChart();
    lchart.setAnimateOnShow(true);
    lchart.setColour("#00aa00");
    lchart.setTooltip("#val#");
    lchart.setText("North");
    
    for (int n = 0; n < numStops; n++) {
      lchart.addValues(sim_dataTimeStop[timeSliderPosition][n]);
    }
    if ((direction.equals("N") || direction.equals("B")) && route != null) {
      cm.addChartConfig(lchart);
    }

    // Create a Line Chart object SOUTH DATA
    lchart = new LineChart();
    lchart.setAnimateOnShow(true);
    lchart.setColour("#ff0000");
    lchart.setTooltip("#val#");
    lchart.setText("South");
    for (int n = 0; n < numStops; n++) {
      lchart.addValues(Math.abs(Math.sin(timeSliderPosition * (Math.PI / 24)
          + Math.PI / 24))
          * Math.abs(Math.floor(Math.cos(Random.nextDouble()) * 220
              * Math.sin(n * Math.PI / 70)) + 20));
    }
    if ((direction.equals("S") || direction.equals("B")) && route != null) {
      cm.addChartConfig(lchart);
    }

    // Creates the line for max Suggested load
    LineChart lchartD = new LineChart();
    lchartD.setColour("#0099FF");
    lchartD.setText("Max 30ft bus");
    for (int n = 0; n <= numStops; n++) {
      lchartD.addValues(40);
    }
    if (dataType.equals("Load")&& maxY>=40)
      cm.addChartConfig(lchartD);

    LineChart lchartM = new LineChart();
    lchartM.setColour("#0066FF");
    lchartM.setText("Max 40ft bus");
    for (int n = 0; n <= numStops; n++) {
      lchartM.addValues(70);
    }
    if (dataType.equals("Load")&& maxY>=70)
      cm.addChartConfig(lchartM);

    LineChart lchartU = new LineChart();
    lchartU.setColour("#0033FF");
    lchartU.setText("Max 60ft bus");
    for (int n = 0; n <= numStops; n++) {
      lchartU.addValues(95);
    }
    if (dataType.equals("Load")&& maxY>=95)
      cm.addChartConfig(lchartU);

    // Returns the Chart Model
    return cm;
  }

  // Bottom Slider
  private SliderField stopControls() {
    // Stop slider
    final Slider slider = new Slider();
    slider.setIncrement(1);
    slider.addStyleName("sliderStyle");
    // Update Execution
    updateSlider2Cmd = new Command() {
      @Override
      public void execute() {
        slider.setMinValue(0);
        slider.setMaxValue(numStops);
      }
    };
    updateSlider2Cmd.execute();

    final SliderField sf = new SliderField(slider);
    sf.setFieldLabel("Time:");
    // Executed when slider position changes
    slider.addListener(Events.Change, new Listener<SliderEvent>() {
      @Override
      public void handleEvent(SliderEvent be) {
        slider.setMessage("Stop:" + slider.getValue());
        stopTageoid = be.getNewValue();
        updateChart3Cmd.execute();
      }
    });

    return sf;
  }

  /*
   * Create All Elements Right Top Porlet (One Stop Chart)
   */
  // Portlet One Stop
  public Portlet getPortletOneStop() {
    // Portlet One stop, time period
    final Portlet stopPortlet = new Portlet();
    // Layout preferences
    stopPortlet.setHeadingHtml("Route: " + route + "\t" + dataType
        + " by half hour for one stop.");
    configPanel(stopPortlet);
    stopPortlet.setHeight(370);
    r = new Resizable(stopPortlet);
    r.setDynamic(true);
    stopPortlet.add(pannelChartOneStop());

    return stopPortlet;
  }

  // Pannel for One Stop chart
  private ContentPanel pannelChartOneStop() {

    String url;
    final Chart chart;

    // Content panel for chart
    panel = new ContentPanel();
    panel.setFrame(false);
    panel.setBodyBorder(false);
    panel.setHeaderVisible(false);

    // Chart info
    url = "chart/open-flash-chart.swf";
    chart = new Chart(url);
    chart.setBorders(true);
    updateChart3Cmd = new Command() {
      @Override
      public void execute() {
        chart.setChartModel(createChartOneStop(stopTageoid));
      }
    };
    updateChart3Cmd.execute();
    chart.setHeight(320);
    panel.add(chart);

    return panel;

  }

  // Line and bar chart for One Stop
  private ChartModel createChartOneStop(int s) {
    // Chart model initialization
    ChartModel cm = new ChartModel("Stop: " + s + "\t" + dataType
        + " by half hour.",
        "font-size: 14px; font-family: Verdana; text-align: center;");
    cm.setBackgroundColour("#fffff5");
    // Code to add legends and paddings
    Legend lg = new Legend(Position.TOP, true);
    lg.setPadding(5);
    lg.setShadow(false);
    cm.setLegend(lg);

    // Create the X axis
    XAxis xa = new XAxis();
    // set the labels for the axis
    for (double i = startT; i <= stopT; i = i + 0.5) {
      if (i % 1 == 0) {
        xa.addLabels(Integer.toString((int) i));
      } else {
        xa.addLabels("");
      }
    }
    cm.setXAxis(xa);
    

    Integer[] dataN = new Integer[(stopT - startT) * 2];
    Integer[] dataNM = new Integer[(stopT - startT) * 2];
    Double[] dataS = new Double[(stopT - startT) * 2];
    Double[] dataSM = new Double[(stopT - startT) * 2];
    for (int n = 0; n < (stopT - startT) * 2; n++) {
      dataN[n] = sim_dataTimeStop[n][stopTageoid];
      dataNM[n] = dataN[n] * 3 / 4;
      dataS[n] = Math.floor(Math.abs(150
          * Math.sin(n * Math.PI / 24 - Math.PI * 4 / 24) + Random.nextDouble()
          * 80));
      dataSM[n] = dataS[n] * 3 / 4;
    }
    
    // Create the Y axis
    YAxis ya = new YAxis();
    // Add the labels to the Y axis
    int maxY= getMax(dataN);
    ya.setRange(0, 1.1*maxY+1, 10);
    cm.setYAxis(ya);

    // Creation of the bar chart NORTH DATA
    FilledBarChart bchartN = new FilledBarChart();
    // Layout preferences
    bchartN.setTooltip("#val#");
    bchartN.setText("North");
    bchartN.setColour("#00aa00");
    bchartN.setAnimateOnShow(true);
    bchartN.addValues(dataN);

    LineChart lchartN = new LineChart();
    // Layout preferences
    lchartN.setTooltip("#val#");
    lchartN.setText("North Mean");
    lchartN.setColour("#006600");
    lchartN.setAnimateOnShow(true);
    lchartN.addValues(dataNM);

    if ((direction.equals("N") || direction.equals("B")) && route != null) {
      cm.addChartConfig(bchartN);
      cm.addChartConfig(lchartN);
    }

    // Creation of the bar chart SOUTH DATA
    FilledBarChart bchartS = new FilledBarChart();
    // Layout preferences
    bchartS.setTooltip("#val#");
    bchartS.setText("South");
    bchartS.setColour("#ff0000");
    bchartS.setAnimateOnShow(true);
    bchartS.addValues(dataS);

    LineChart lchartS = new LineChart();
    // Layout preferences
    lchartS.setTooltip("#val#");
    lchartS.setText("South Mean");
    lchartS.setColour("#cc0000");
    lchartS.setAnimateOnShow(true);
    lchartS.addValues(dataSM);

    if ((direction.equals("S") || direction.equals("B")) && route != null) {
      cm.addChartConfig(bchartS);
      cm.addChartConfig(lchartS);
    }

    // Creates the line for max Suggested load
    LineChart lchartD = new LineChart();
    lchartD.setColour("#0099FF");
    lchartD.setText("Max 30ft bus");
    for (double n = startT; n <= stopT; n = n + .5) {
      lchartD.addValues(40);
    }
    if (dataType.equals("Load"))
      cm.addChartConfig(lchartD);

    LineChart lchartM = new LineChart();
    lchartM.setColour("#0066FF");
    lchartM.setText("Max 40ft bus");
    for (double n = startT; n <= stopT; n = n + .5) {
      lchartM.addValues(70);
    }
    if (dataType.equals("Load"))
      cm.addChartConfig(lchartM);

    LineChart lchartU = new LineChart();
    lchartU.setColour("#0033FF");
    lchartU.setText("Max 60ft bus");
    for (double n = startT; n <= stopT; n = n + .5) {
      lchartU.addValues(95);
    }
    if (dataType.equals("Load"))
      cm.addChartConfig(lchartU);

    // Returns the Chart Model
    return cm;

  }

  /*
   * Create All Elements Right Bottom Portlet (Grid)
   */
  // Stat. Information Grid portlet
  public Portlet getGrid() {
    // Portlet for the Information Grid
    final Portlet gridPortlet = new Portlet();
    gridPortlet.setHeadingHtml("Route: " + route + "\tSummary");

    configPanel(gridPortlet);
    gridPortlet.setHeight(370);
    gridPortlet.setLayout(new FitLayout());
    gridPortlet.add(createGrid());
    r = new Resizable(gridPortlet);
    r.setDynamic(true);

    return gridPortlet;
  }

  // Stat. Information Grid pannel
  private ContentPanel createGrid() {
    final Grid<DataStats> grid;
    ArrayList<ColumnConfig> configs = new ArrayList<ColumnConfig>();

    ColumnConfig stopCol = new ColumnConfig();
    stopCol.setId("stop");
    stopCol.setHeaderHtml("Stop");
    stopCol.setWidth(120);
    TextField<String> text = new TextField<String>();
    text.setAllowBlank(false);
    stopCol.setEditor(new CellEditor(text));
    configs.add(stopCol);

    ColumnConfig minCol = new ColumnConfig();
    minCol.setId("min");
    minCol.setHeaderHtml("Max Load");
    minCol.setWidth(120);
    text = new TextField<String>();
    text.setAllowBlank(false);
    minCol.setEditor(new CellEditor(text));
    configs.add(minCol);

    ColumnConfig meanCol = new ColumnConfig();
    meanCol.setId("mean");
    meanCol.setHeaderHtml("Current Headway");
    meanCol.setWidth(120);
    text = new TextField<String>();
    text.setAllowBlank(false);
    meanCol.setEditor(new CellEditor(text));
    configs.add(meanCol);

    ColumnConfig thCol = new ColumnConfig();
    thCol.setId("th");
    thCol.setHeaderHtml("Recom Headway");
    thCol.setWidth(120);
    text = new TextField<String>();
    text.setAllowBlank(false);
    thCol.setEditor(new CellEditor(text));
    configs.add(thCol);

    ColumnConfig maxCol = new ColumnConfig();
    maxCol.setId("max");
    maxCol.setHeaderHtml("Running Time");
    maxCol.setWidth(120);
    text = new TextField<String>();
    text.setAllowBlank(false);
    maxCol.setEditor(new CellEditor(text));
    configs.add(maxCol);

    final ListStore<DataStats> store = new ListStore<DataStats>();
    store.add(GetData.getStats());

    ColumnModel cm = new ColumnModel(configs);

    panel = new ContentPanel();
    panel.setFrame(false);
    panel.setHeight(280);
    panel.setHeaderVisible(false);
    panel.setLayout(new FitLayout());

    GridSelectionModel<DataStats> selectModel = new GridSelectionModel<DataStats>();
    selectModel.select(stopTageoid, true);
    grid = new Grid<DataStats>(store, cm);
    grid.setBorders(true);
    grid.setSelectionModel(selectModel);

    panel.add(grid);

    return panel;
  }

  /*
   * Other functions
   */
  // Get the maximum value of an array
  public int getMax(Integer[] list) {
    int max = Integer.MIN_VALUE;
    for (int i = 0; i < list.length; i++) {
      if (list[i] > max) {
        max = list[i];
      }
    }
    return max;
  }

  // Panel configuration method for portlets
  private void configPanel(final ContentPanel panel) {
    // Layout configuration
    panel.setCollapsible(true);
    panel.setAnimCollapse(false);
    panel.setScrollMode(Scroll.AUTOY);
    // Close button
    panel.getHeader().addTool(
        new ToolButton("x-tool-close",
            new SelectionListener<IconButtonEvent>() {

              @Override
              public void componentSelected(IconButtonEvent ce) {
                panel.removeFromParent();
              }

            }));
  }
}