package uzholdem.bot.meerkat;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.swing.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.RefineryUtilities;

public class Console extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8221338636006962763L;
	PipedInputStream piOut;
	PipedInputStream piErr;
	PipedOutputStream poOut;
	PipedOutputStream poErr;
	JTextArea textArea;
	private boolean turnedOn = false;
	private JScrollPane scrollPane;
	private JPanel titlePanel;
	private JLabel handCount;
	public static Console out;

	public void turnOn() {
		this.turnedOn = true;
		setVisible(true);
	}

	public void turnOff() {
		this.turnedOn = false;
		setVisible(false);
	}

	boolean autoScroll = true;
	private JButton expandButton;
	private JFreeChart chart;
	private ChartPanel chartPanel;

	double lastSum=0.0;
	XYSeries s1 = new  XYSeries("Winnings");

	public Console() throws IOException {

		setTitle("UZHoldem Bot Command - Gismo Edition 3");
		
		// LOGO
		URL url = getClass().getResource("/data/logo.png");
		ImageIcon icon = null;
		if (url != null) {
			icon = new ImageIcon(url);
		} else {
			icon = new ImageIcon("data/logo.png");
		}
		JLabel logoLabel = new JLabel();
		logoLabel.setIcon(icon);
		
		// KILLSWITCH
		JButton killswitch = new JButton("Shutdown");
		killswitch.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent arg0) {
				System.exit(0);
			}
		});

		killswitch.setBackground(Color.WHITE);

		// DEBUG ON/OFF
		this.expandButton = new JButton("Show Debug Info");
		expandButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent arg0) {
				boolean on = !scrollPane.isVisible();
				if(on) {
					expandButton.setText("Hide Debug Info");
				} else {
					expandButton.setText("Show Debug Info");
				}
				scrollPane.setVisible(on);
				pack();

			}
		});
		expandButton.setBackground(Color.WHITE);

		// HANDCOUNT
		handCount = new JLabel("Hand Count");
		handCount.setBackground(Color.WHITE);
		
		// SCROLLLOCK
		JCheckBox autoscrollButton = new JCheckBox("Scroll Lock");
		autoscrollButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent arg0) {
				autoScroll = !autoScroll;

			}
		});
		autoscrollButton.setBackground(Color.WHITE);
		
		// TITLE PANEL
		this.titlePanel = new JPanel();
		titlePanel.setBackground(Color.WHITE);
		titlePanel.add(logoLabel);
		titlePanel.add(killswitch);
		titlePanel.add(expandButton);
		titlePanel.add(autoscrollButton);
		titlePanel.add(handCount);
		getContentPane().add(titlePanel, BorderLayout.NORTH);
		
		// WIN/LOSS CHART
		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(s1);
		this.chart = this.createChart(dataset);
		this.chartPanel = new ChartPanel(chart);
		this.chartPanel.setFillZoomRectangle(true);
		this.chartPanel.setMouseWheelEnabled(true);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
		chartPanel.setVisible(true);
		getContentPane().add(chartPanel, BorderLayout.CENTER);   
		   
		// DEBUG OUTPUT
		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setRows(20);
		textArea.setColumns(60);
		this.scrollPane = new JScrollPane(textArea);
		this.scrollPane.setVisible(false);
		getContentPane().add(scrollPane, BorderLayout.SOUTH);

		
		pack();

		Properties p = System.getProperties();
		Enumeration keys = p.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			String value = (String) p.get(key);
			this.println(key + ": " + value);
		}

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}


	 public void hookSysOut() {
		// Set up System.out

		try {
			piOut = new PipedInputStream();
			poOut = new PipedOutputStream(piOut);
			System.setOut(new PrintStream(poOut, true));

			// Set up System.err
			piErr = new PipedInputStream();
			poErr = new PipedOutputStream(piErr);
			System.setErr(new PrintStream(poErr, true));

			// Create reader threads
			new ReaderThread(new PipedInputStream[] { piOut, piErr }).start();
		} catch (IOException e) {
			Util.printException(e);
			e.printStackTrace();
		}

	}

	public void println(String str) {

		textArea.append(str + "\n");
		System.out.println(str);
		if (autoScroll) {
			try {

				scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
			} catch (Exception e) {
			}
		}

	}

	public void print(String str) {

		textArea.append(str);
		System.out.print(str);
		if (autoScroll) {
			try {

				scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
			} catch (Exception e) {
			}
		}

	}

	public static void initConsole() {
		try {
			Console.out = new Console();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	class ReaderThread extends Thread {
		PipedInputStream[] pis;

		ReaderThread(PipedInputStream[] pi) {
			this.pis = pi;
		}

		public void run() {
			final byte[] buf = new byte[1024];
			try {
				while (true) {
					for (PipedInputStream pi : pis) {
						final int len = pi.read(buf);
						if (len == -1) {
							break;
						}
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								textArea.append(new String(buf, 0, len));

								// Make sure the last line is always visible
								textArea.setCaretPosition(textArea.getDocument().getLength());

								// Keep the text area down to a certain
								// character size
								/*
								 * int idealSize = 1000; int maxExcess = 999999;
								 * int excess =
								 * textArea.getDocument().getLength() -
								 * idealSize; if (excess >= maxExcess) {
								 * textArea.replaceRange("", 0, excess); }
								 */
							}
						});
					}
				}
			} catch (IOException e) {
			}
		}
	}

	public void setHandCount(int handCount) {
		this.handCount.setText("Hand: " + handCount);

	}

	 public void updateChart(int handCount, double amount) {
		    try {
		System.out.println(amount);
		lastSum+=amount;
		System.out.println(lastSum);
		s1.add(handCount,lastSum);
		    } catch (Exception e) {
				// TODO Auto-generated catch block

				DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				java.util.Date date = new java.util.Date();
				System.err.println("Current Date Time : " + dateFormat.format(date));
				e.printStackTrace();
			}
	}
	

	public void saveChart(String file) {

	       try {
	    	   BufferedImage test = chart.createBufferedImage(800, 600);
	    	   ImageIO.write(test,"png", new File(file));
		} catch (Exception e) {
			// TODO Auto-generated catch block

			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			java.util.Date date = new java.util.Date();
			System.err.println("Current Date Time : " + dateFormat.format(date));
			e.printStackTrace();
		}
	}

	/**
	     * Creates a chart.
	     *
	     * @param dataset  a dataset.
	     *
	     * @return A chart.
	     */
	    private JFreeChart createChart(XYDataset dataset) {
	    	try {
	    		File file = new File("data/bots/uzholdem.jar");
	    		URLClassLoader loader = URLClassLoader.newInstance(new URL[]{file.toURL()},Console.class.getClassLoader());
				org.jfree.chart.util.ResourceBundleWrapper.removeCodeBase((new File(".")).toURL(), loader);
			
	    	} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ResourceBundle tst = org.jfree.chart.util.ResourceBundleWrapper.getBundle( "org.jfree.chart.plot.LocalizationBundle");
	    	String strClassPath = System.getProperty("java.class.path");
	        JFreeChart chart = ChartFactory.createXYLineChart(
	            "Results",  // title
	            "Hand",             // x-axis label
	            "Cummulated Wins or Losses",   // y-axis label
	            dataset,            // data	
	            PlotOrientation.VERTICAL, 
	            true,               // create legend?
	            true,               // generate tooltips?
	            false               // generate URLs?
	        );
	
	        chart.setBackgroundPaint(Color.white);
	
	        XYPlot plot = (XYPlot) chart.getPlot();
	        plot.setBackgroundPaint(Color.lightGray);
	        plot.setDomainGridlinePaint(Color.white);
	        plot.setRangeGridlinePaint(Color.white);
	        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
	        plot.setDomainCrosshairVisible(true);
	        plot.setRangeCrosshairVisible(true);
	
	        XYItemRenderer r = plot.getRenderer();
	        if (r instanceof XYLineAndShapeRenderer) {
	            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
	            renderer.setBaseShapesVisible(false);
	            renderer.setBaseShapesFilled(false);
	            renderer.setDrawSeriesLineAsPath(true);
	        }
	
	        /*DateAxis axis = (DateAxis) plot.getDomainAxis();
	        axis.setDateFormatOverride(new SimpleDateFormat("MMM-yyyy"));
	*/
	        return chart;
	
	    }

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
	
		Console.initConsole();
		Console.out.turnOn();
		
		Console demo = Console.out;
	
	    int i =(int) (demo.s1.getMaxX()+1);
		demo.updateChart(i++ ,Math.random()-0.5);
		demo.updateChart(i++ ,Math.random()-0.5);
		demo.updateChart(i++ ,Math.random()-0.5);
		demo.updateChart(i++ ,Math.random()-0.5);
		demo.updateChart(i++ ,Math.random()-0.5);
		demo.updateChart(i++ ,Math.random()-0.5);
		demo.updateChart(i++ ,Math.random()-0.5);
		demo.updateChart(i++ ,Math.random()-0.5);
		demo.updateChart(i++ ,Math.random()-0.5);
		demo.updateChart(i++ ,Math.random()-0.5);
		demo.updateChart(i++ ,Math.random()-0.5);
		demo.updateChart(i++ ,Math.random()-0.5);
		demo.updateChart(i++ ,Math.random()-0.5);
		demo.updateChart(i++ ,Math.random()-0.5);
		demo.updateChart(i++ ,Math.random()-0.5);
		demo.updateChart(i++ ,Math.random()-0.5);
		demo.updateChart(i++ ,Math.random()-0.5);
		demo.updateChart(i++ ,Math.random()-0.5);
		demo.updateChart(i++ ,Math.random()-0.5);
		demo.updateChart(i++ ,Math.random()-0.5);
	
	
	}

}
