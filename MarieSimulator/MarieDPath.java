// File:        MarieDPath.java
// Author:      Julie Lobur
// JDK Version: 1.4.1
// Date:        April 30, 2003
// Notice:      Copyright 2003 Julia M. Lobur 
//              This code may be freely used for noncommercial purposes.
package MarieSimulator;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.print.*; 
import javax.print.attribute.*; 
import javax.print.attribute.standard.*; 

public class MarieDPath extends JFrame {
/******************************************************************************************
*  This program simulates the data movement activities that take place within a           *
*  "von Neumann" architecture computer.  This simulator is an implementation of the       *
*  machine described in *The Essentials of Computer Organization and Architecture* by     *
*  Null and Lobur.                                                                        *
*                                                                                         *
*  This data path simulator augments the MARIE machine simulator, to show the details     *
*  of instruction decoding, data movement and machine control.  This simulator runs ONLY  *
*  programs that have been assembled through the MARIE machine simulator.                 *
*                                                                                         *
*  The control unit sets control lines that indicate which component(s) will be active at *
*  a particular time.  If you are reading this program, it will be helpful to know that   *
*  "write" control lines.  The value set on these lines (in binary) determines which      *
*  register (or memory) will be written to (that is, which will be the destination of a   *
*  data transfer.  The registers are assigned numbers that agree with the numbering in    *
*  the text: IR = 7, OUT = 6, IN = 5, AC = 4, MBR = 3, PC = 2, MAR = 1, and               *
*  Main memory = 0.  Thus, if "write" lines 0 and 2 are active, the MBR will be the       *
*  destination for the data movement.  The "read" lines are control lines 3, 4, and 5.    *
*                                                                                         *
*  Control line 6 connects the AC and the MBR; control line 7 connects the MAR to main    *
*  memory; control line 8 connects the ALU to the AC; and control line 9 connects the     *
*  ALU to the MBR.                                                                        *
*                                                                                         *
*  Active components and control lines are shown in a different color than when they are  *
*  inactive. As execution proceeds, the assembled MARIE code is highlighted in a monitor  *
*  window, and changes in register values are written to a trace pane than can be printed.*
*                                                                                         *
*  A good bit of the code for this simulator was taken directly, or is slghtly modified   *
*  code from MarieSim.java.                                                               *
******************************************************************************************/

/* --                                                                                 -- */
/* --  MarieDPath class fields and attributes.                                        -- */
/* --                                                                                 -- */
  public static String         mexFile = null;     // Name of machine code file.
  public static String         mexPath = null;
  public static final String  MEX_TYPE = ".mex";  // File extension of executable code.

  public static final String      linefeed = System.getProperty("line.separator");
  public static final String      formfeed = "\014";
  public static final String fileSeparator = System.getProperty("file.separator");

  public static final String HELP_FILE = "mdphlp1.txt";  // Help file name.
  
  public static final JFileChooser exeFileChooser = 
                                   new JFileChooser(System.getProperty("user.dir")); 
  public static final int HEX             =      0;      // Register display modes,
  public static final int DEC             =      1;      // i.e., "rendering modes."
  public static final int ASCII           =      2;                                   
  public static final int MAX_MARIE_ADDR  =   4095;
  public static final int MARIE_HALTED_NORMAL     =  0;  // Possible machine states.
  public static final int MARIE_RUNNING           =  1;
  public static final int MARIE_BLOCKED_ON_INPUT  =  2;
  public static final int MARIE_PAUSED            =  3;
  public static final int MARIE_HALTED_ABNORMAL   = -1;
  public static final int MARIE_HALTED_BY_USER    = -2;
  public static final int MARIE_NO_PROGRAM_LOADED = -3;
  public static final int MARIE_UNINITIALIZED     = 0xDEAD;
  
  static final int PROGRAM_TABLE_ROW_HEIGHT = 19; // Row height for program instructions.

  public static final int MINIMUM_DELAY = 10;     // Min execution delay.
  public static final String[] base = {"Hex", "Dec", "ASCII"};  // Radix for input.

/* --                                                                                 -- */
/* --  Boundaries and parameters for graphics.  Register offsets are X-offsets.       -- */
/* --                                                                                 -- */  
  public static final int regTopBound  = 70;
  public static final int regWidth = 60;
  public static final int regHeight = 25;
  public static final int[] regOffset =  {675, 596,  522,  440, 300, 220, 140,  60};
  public static final String[] regName = {"MM","MAR","PC","MBR","AC","IN","OUT","IR"};
  public static final int[] aluXvals = {368,388,393,403,408,428,418,378,368};
  public static final int[] aluYvals = {105,105,115,115,105,105,132,132,105};
/* --                                                                                 -- */
/* -- Colors....                                                                      -- */
/* --                                                                                 -- */
  public static final Color panelBackground = new Color(233, 255, 255);
  public static final Color componentLabelColor = new Color(0, 63, 161); 
  public static final Color regActiveColor = new Color(255, 237, 41);     
  public static final Color regActiveFontColor = new Color(0, 63, 161); 
  public static final Color regInactiveColor = new Color(173, 184, 0); 
  public static final Color regInactiveFontColor = new Color(35, 0, 151);
  public static final Color rTLFontColor = new Color(0, 63, 161); 
  public static final Color busActiveColor = new Color(0, 225, 7);  
  public static final Color busInactiveColor = new Color(0, 153, 0); 
  public static final Color memoryActiveColor = new Color(255, 248, 61);
  public static final Color memoryInactiveColor = new Color(173, 184, 0);   
  public static final Color controllerActiveColor = new Color(255, 200, 200); 
  public static final Color componentOutlineColor = new Color(208, 0, 92); 
  public static final Color controlLineActiveColor = new Color(255, 0, 38);      
  public static final Color controllerInactiveColor = new Color(108, 48, 255); 
  public static final Color controlLineInactiveColor = Color.blue; 
  public static final Color monitorPanelColor = new Color(0,155,215);
  public static final Color controlPanelBackground = new Color(224, 224, 224);
  public static final Color tableHeaderColor   = new Color(65, 80, 150);
  public static final Color messageBackground  = new Color(100, 220, 255);
  public static final Color registerTextColor  = new Color(0, 50, 165);
  public static final Color buttonPanelColor   = new Color(200, 220, 255);
  public static final Color programCursorColor = new Color(120, 210, 255);
  
  public static final int MM  = 0;                    // Register numbers used for
  public static final int MAR = 1;                    // designations in inner class
  public static final int PC  = 2;                    // Register for error-checking.
  public static final int MBR = 3;
  public static final int AC  = 4;
  public static final int IN  = 5;
  public static final int OUT = 6;
  public static final int IR  = 7;
  
/* --                                                                                 -- */
/* --  Instance variables.                                                            -- */
/* --                                                                                 -- */
  int   codeLineCount = 0;            // Number of lines in the program
  boolean    stepping = false;        // Whether executing one instruction at a time. 
  boolean   fastFetch = false;        // Whether fast fetching is enabled.
  
  int   machineState = MARIE_UNINITIALIZED;
  int[]  memoryArray = new int[4096];
  boolean errorFound = false;   // Non-fatal error flag, e.g. invalid  user input.
  boolean fatalError = false;   // Fatal error flag, e.g., invalid branch address.
  int      errorCode = 0;
  static String statusMessage = null;
    
  JPanel dataPathSimPane;
  JMenuBar       controlBar = new JMenuBar();  // Container for the menu as follows:
  JMenu            fileMenu = new JMenu();        // "File" menu
  JMenuItem    loadFileItem = new JMenuItem();    //       | load program
  JMenuItem     restartItem = new JMenuItem();    //       | restart from beginning
  JMenuItem       resetItem = new JMenuItem();    //       | reset the simulator  
  JMenuItem    exitFileItem = new JMenuItem();    //       | quit

  JButton           runStop = new JButton();      // "Stop" button
  JButton              step = new JButton();      // "Step" button

  JButton           setSpeed = new JButton();     // Speed control button
  DelayFrame     delayFrame;                      //       |  (frame to enter delay)
  JButton       setFastFetch = new JButton();     // Turns fast fetch mode off or on.

  JMenu      helpMenu = new JMenu();              // Help menu
  JMenuItem   getHelp = new JMenuItem();          //       | general instructions
  TextFileViewer helpViewer;                      //       |   shown in this viewer
  JMenuItem helpAbout = new JMenuItem();          //       | "About" box
  HelpAboutFrame helpAboutFrame;                  //       |   shown in this frame
  
  DataPathPanel dataPathPanel;                 // Container for graphical components.
  BasicStroke outline     = new BasicStroke(0.5f);   // Component outline for contrast
  BasicStroke controlLine = new BasicStroke(2.5f);   // Connecting control line
  BasicStroke dataBus     = new BasicStroke(8.0f);   // A fat line for the data bus
  ControlUnit controlUnit = new ControlUnit();       // MARIE components ...
  ALU aLU            = new ALU();
  MainMemory memory  = new MainMemory();
  Register regIR     = new Register(7);
  Register regOUTPUT = new Register(6);
  Register regINPUT  = new Register(5);
  Register regAC     = new Register(4);
  Register regMBR    = new Register(3);
  Register regPC     = new Register(2);
  Register regMAR    = new Register(1);
  
  JPanel monitorPanel = new JPanel();   // Container for monitoring and input ...
  Object[][] programArray;                        // Holds program instructions
  JScrollPane  programPane = new JScrollPane();// Scrollpane for program monitor
  JTable      programTable;                            // Program monitor table.
  ProgramTableModel    ptm = new ProgramTableModel();  // Program monitor table control.
  int      programFocusRow = 0;                        // Current instruction
                                                       //    pointer in monitor.
  Hashtable codeReference                 // codeReference provides correspondence
            = new Hashtable(16, (float) 0.75);   // between monitor table and 
                                                 // instruction addresses.
                                          // Initial capacity 16, load factor 0.75.
                                          
  JScrollPane tracePane = new JScrollPane();   // Execution trace pane.
  TitledBorder tracePaneBorder = new TitledBorder(BorderFactory.createEtchedBorder(),
                                        "  IR    OUT    IN     AC     MBR   PC    MAR",
                                        TitledBorder.LEFT, TitledBorder.BELOW_TOP,
                                        new Font("monospaced", Font.PLAIN, 11));
                                        
  JPanel    controlPanel  = new JPanel();   // Container for buttons and execution trace.
  JPanel      buttonPanel = new JPanel();        // Container for input and print buttons.                             
  JPanel       printPanel = new JPanel();            // Trace print button container.       
  JLabel printButtonLabel = new JLabel();               // Print button label.
  JButton      printTrace = new JButton();              // Trace print button.
  
  JPanel      inputPanel  = new JPanel();            // Input items container.
  JLabel      inputLabel  = new JLabel("Input");        // Input label.
  JComboBox    inputBase  = new JComboBox(base);        // Radix of input.
  JTextField inputContent = new JTextField();           // Input contents.
  JTextArea traceTextArea = new JTextArea();    // Execution trace area.
  
  JTextField    msgField = new JTextField(); // Status message field contents.
  
  int delay      = 1000;                    // Execution delay in milliseconds.
  int briefDelay = 500;
  
  class DelayFrame extends JFrame {
/******************************************************************************************
*   This class displays a slider in a frame to allow the user to change the value of the  *
*   delay between consecutive instruction executions.                                     *
*      Side effect:  The value of the class variable "delay" may be changed, but not      *
*                    to a value smaller than MINIMUM_DELAY.                               *
*   Whether or not a change is made to the delay value, this method dispatches a window-  *
*   closing event, which is monitored by the invoking method and triggers the             *
*   nullification of the pointer to this frame.                                           *
******************************************************************************************/
    JPanel       buttons = new JPanel();
    JLabel   sliderLabel = 
                         new JLabel("Select instruction execution delay in milliseconds.");
    JTextField   msDelay = new JTextField();
    JLabel    blankLabel = new JLabel();      // Spacer
    JButton   okayButton = new JButton("Okay");
    JButton cancelButton = new JButton("Cancel");
    int      sliderDelay = delay;
    DelayFrame() {                                         // Frame constructor
      super("Set Delay");
      JPanel delayPane = (JPanel) this.getContentPane();   // Set frame characteristics.
      setSize(new Dimension(400, 200));
      addWindowListener(new WindowAdapter() {
          public void windowClosing(WindowEvent e) {
               dispose();            
               return;
          } // windowClosing()
      }); // Listener
      delayPane.setPreferredSize(new Dimension(350, 150));
      delayPane.setLayout(new FlowLayout());
      setIconImage(Toolkit.getDefaultToolkit()
                             .createImage(MarieSim.class.getResource("M.gif")));
      setStatusMessage(" Execution delay set at "
                             +delay+" milliseconds.");      // Message in parent frame.
      sliderLabel.setPreferredSize(new Dimension(300, 50)); // Slider instructions.
      sliderLabel.setForeground(Color.black);
      JSlider milliseconds = new JSlider(JSlider.HORIZONTAL, 0, 30000, delay);
      milliseconds.setMajorTickSpacing(10000);              // Set the scale on the slider.
      milliseconds.setMinorTickSpacing(2500);
      milliseconds.setPaintTicks(true);
      milliseconds.setPaintLabels(true);
      milliseconds.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
      milliseconds.setPreferredSize(new Dimension(325, 50));  
      milliseconds.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {           // Anonymous inner class
          JSlider source = (JSlider)e.getSource();          // for getting slider value.
          if (!source.getValueIsAdjusting()) {
             sliderDelay = (int)source.getValue();
             msDelay.setText(" "+sliderDelay);
             msDelay.postActionEvent();
          }
        } // stateChanged()
      }); // Listener
      msDelay.setPreferredSize(new Dimension(57, 30)); 
      msDelay.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(145, 145, 210), 2),
                            BorderFactory.createLoweredBevelBorder()));
      msDelay.setFont(new Font("Monospaced", 0, 14));
      msDelay.setText(" "+delay);
      msDelay.setEditable(false);
      blankLabel.setPreferredSize(new Dimension(30, 35));
      okayButton.setMaximumSize(new Dimension(80, 35));     // Populate the button pane.
      okayButton.setMinimumSize(new Dimension(80, 35));   
      okayButton.setPreferredSize(new Dimension(80, 35));  
      JRootPane root = getRootPane();
      root.setDefaultButton(okayButton);
      okayButton.addActionListener(new ActionListener() {    // On the okay button, 
        public void actionPerformed(ActionEvent e) {         // set the delay 
            if (sliderDelay < MINIMUM_DELAY)                 // according to the
              sliderDelay = MINIMUM_DELAY;                   // slider value and
            delay = sliderDelay;                             // dispatch a window-
            briefDelay = delay / 2;                          // closing event.
            setStatusMessage(" Execution delay set at " 
                                    +delay+" milliseconds.");    // Note: minimum delay
            WindowEvent we = new WindowEvent(DelayFrame.this,    // is a constant.
                                       WindowEvent.WINDOW_CLOSING);
            DelayFrame.this.dispatchEvent(we);
        }
      });  // Listener
      cancelButton.setMaximumSize(new Dimension(80, 35));   
      cancelButton.setMinimumSize(new Dimension(80, 35));   
      cancelButton.setPreferredSize(new Dimension(80, 35)); 
      cancelButton.addActionListener(new ActionListener() {    // If we're canceled, just
        public void actionPerformed(ActionEvent e) {           // dispatch a window-
             WindowEvent we = new WindowEvent(DelayFrame.this, // closing event.
                                       WindowEvent.WINDOW_CLOSING);
             DelayFrame.this.dispatchEvent(we);
        }
      }); // Listener
      buttons.setPreferredSize(new Dimension(300, 75)); 
      buttons.add(msDelay);
      buttons.add(blankLabel);
      buttons.add(okayButton);                              // Put the buttons in the
      buttons.add(cancelButton);                            // button panel.
      delayPane.add(sliderLabel);                           // Add label, slider
      delayPane.add(milliseconds);                          // and buttons to the main
      delayPane.add(buttons);                               // frame.
      setLocation(200, 75);
      show();
    } // DelayFrame()
  } // DelayFrame

  public class HelpAboutFrame extends JDialog implements ActionListener {
/******************************************************************************************
*   This class displays a "Help about" frame describing version and copyright information.*
******************************************************************************************/

  JPanel mainPanel = new JPanel();      // Top-level screen panel
  JPanel centerPanel = new JPanel();

  JPanel    logoPanel = new JPanel();       // Logo panel.
  JLabel    logoLabel = new JLabel();           // JLabel to hold our picture.
  ImageIcon      logo = new ImageIcon();        // The picture for the label.
  
  JPanel    infoPanel = new JPanel();       // Information panel.
  JLabel  pgmTitle = new JLabel("MARIE DataPath Simulator - Version 1.0");
  JLabel copyRight = new JLabel("Copyright (c) 2003, 2006");
  JLabel accompany = new JLabel("To accompany:");
  JLabel   theBook = new JLabel("The Essentials of Computer Organization and Architecture 2/e  ");
  JLabel   authors = new JLabel("By Linda M. Null & Julia M. Lobur");
  JLabel publisher = new JLabel("Jones & Bartlett Publishers");
  
  JPanel okBttonPanel = new JPanel();       // Button panel.
  JButton    okButton = new JButton("Ok");      // And its button.

  GridLayout gridLayout = new GridLayout();

  public HelpAboutFrame(Frame parent) {  // Frame constructor.

    super(parent);
    this.setTitle("About this Program");
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    mainPanel.setLayout(new BorderLayout());
    
    logo = new ImageIcon(MarieDPath.class.getResource("ECOA.jpg"));

    logoPanel.setLayout(new FlowLayout());            // Build the logo panel.
    logoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    logoLabel.setIcon(logo);
    logoPanel.add(logoLabel, null);
    centerPanel.setLayout(new BorderLayout());
    centerPanel.add(logoPanel, BorderLayout.WEST);
    
    copyRight.setFont(new Font("sanserif", 0, 12));   // Build the main content area.
    accompany.setFont(new Font("sanserif", 0, 12));
    theBook.setFont(new Font("sanserif", Font.BOLD + Font.ITALIC, 14));
    authors.setFont(new Font("sanserif", 0, 12));
    publisher.setFont(new Font("sanserif", 0, 12)); 
    infoPanel.setLayout(gridLayout);     
    gridLayout.setRows(6);
    gridLayout.setColumns(1);
    infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 60, 10, 10));
    
    okBttonPanel.setLayout(new FlowLayout());
    okButton.addActionListener(this);

    this.getContentPane().add(mainPanel, null);
    infoPanel.add(pgmTitle, null);
    infoPanel.add(copyRight, null);
    infoPanel.add(accompany, null);
    infoPanel.add(theBook, null);
    infoPanel.add(authors, null);
    infoPanel.add(publisher, null);
    centerPanel.add(infoPanel, BorderLayout.CENTER);
    
    mainPanel.add(centerPanel, BorderLayout.NORTH);
    okBttonPanel.add(okButton, null);
    mainPanel.add(okBttonPanel, BorderLayout.SOUTH);
    setResizable(true);
    setLocation(75, 75);
    setModal(true);
    pack();
    show();
  } // HelpAboutFrame()
  
  protected void processWindowEvent(WindowEvent e) { // Process window closing, anything
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {   // else will be handled by the 
      cancel();                                      // super class.
    }
    super.processWindowEvent(e);
  }
  
  void cancel() {                                    // Close the frame and free its memory.
    dispose();
    return;
  }

  public void actionPerformed(ActionEvent e) {       // Close the frame on a button event
    if (e.getSource() == okButton) {
      cancel();
    }
  }
} // HelpAboutFrame

 class MarieExecutableFileFilter extends javax.swing.filechooser.FileFilter {
/******************************************************************************************
*  This class provides a JFileChooser with a template so that only those files ending     *
*  with a valid MARIE executable file extension are displayed by the JFileChooser.        *
******************************************************************************************/
    String myFileType = MEX_TYPE;
    public boolean accept (File file) { 
       if (file.isDirectory())
         return true;
       if (file.getName().endsWith(myFileType))
         return true; 
       else
         return false;
    }
    public String getDescription() { return "*"+myFileType; }
  } // MarieExecutableFileFilter

  class ProgramTableModel extends AbstractTableModel {
/******************************************************************************************
*  This class provides the framework for the table used for the program statement         *
*  execution monitor window.  We have to make it global to the simulator because we       *
*  change the structure of the table (i.e., the number of rows) each time a different     *
*  program is loaded from disk.)                                                          *
******************************************************************************************/
      String headers[] =  {" ", "label", "opcode", "operand", "hex"};
      public int getColumnCount() { return headers.length; }
      public int getRowCount() { return codeLineCount; }
      public String getColumnName(int col) {
        return headers[col];
      }
      public Object getValueAt(int row, int col) {
        return programArray[row][col];
      }
      public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;                         // Determines whether the data
      }                                       // value in a cell can be changed.
    } // ProgramTableModel
  
  
  class ControlUnit {
/******************************************************************************************
*  The control unit for this simulator is simply a large rectangle that changes colors    *
*  depending on whether it is activated.  It is connected to 10 control lines that        *
*  enable the reading and writing of register and memory values.  The first three, 0-->2, *
*  are write enable lines to the registers, the next three, 3-->5, are read enable lines. *
*  separate data path lines are provided between the MAR and MM (line 6), the AC and MBR, *
*  (line 7), and between the AC and the ALU (line 8), and the ALU and MBR (line 9).       *
*  Technically, these lines should be "wide" data bus type lines, but the wider lines     *
*  don't look good in the simulator.                                                      *
*                                                                                         *
*  The conrol unit also places a text string at the center of the graphical simulator     *
*  area (when a non-null string is passed to the refresh() method).  This string should   *
*  contain the register transfer language for the microoperation currently executing.     *
******************************************************************************************/
  boolean active;      // whether participating in current operation.
  String microoperation;
  int[] controlVector = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};  // Control lines.

  public ControlUnit() {      // Constructor.
    this.active = false;
    this.microoperation = new String();
  } //ControlUnit()
   
  void setState(boolean a, int[] config, String rtl) {
    this.active = a;
    if (rtl.length() > 0)
      this.microoperation = rtl;
    for (int i = 0; i < 10; i++)
      this.controlVector[i] = config[i];
  } // setState()
  
   void refresh(Graphics g) {        // Redraw the control unit and its control lines.
     Graphics2D g2 = (Graphics2D)g;
     String contents;
     if (this.active) {
       g2.setColor(controllerActiveColor);
       g.fillRect(12, 35, 26, 142);
       g2.setColor(componentOutlineColor);
       g.drawRect(12, 35, 26, 142);
     }  
     else {
       g2.setColor(controllerInactiveColor);
       g.fillRect(12, 35, 26, 142);
     }
     g2.setColor(componentLabelColor);
     g2.setFont(new Font("sansserif", Font.BOLD, 10));
     g2.drawString("Write", 42, 42);    
     g2.drawString("Read", 42, 156);               
     g2.setFont(new Font("sansserif", Font.BOLD, 12));
     g2.drawString("Control", 12, 192);
     g2.drawString("Unit", 12, 207);
     g2.setStroke(controlLine); 
     for (int i = 0; i < 10; i++) {
      if (controlVector[i] == 0)
       g2.setColor(controlLineInactiveColor);
      else
       g2.setColor(controlLineActiveColor);
      switch(i) { 
       case 0:  g.drawLine(39, 46, 675, 46);
                for (int j = 1; j <=7; j++) {
                  g.drawLine(regOffset[j]+ 5, 46, regOffset[j]+ 5, regTopBound);
                }
                break;
       case 1:  g.drawLine(39, 50, 675, 50);
                for (int j = 1; j <=7; j++) {
                 g.drawLine(regOffset[j]+ 10, 50, regOffset[j]+ 10, regTopBound);
                }
                break;
       case 2:  g.drawLine(39, 54, 675, 54);
                for (int j = 1; j <=7; j++) {
                 g.drawLine(regOffset[j]+15, 54, regOffset[j]+15, regTopBound);
                }
                break;
       case 3:  g.drawLine(39, 160, 675, 160);
                for (int j = 1; j <=7; j++) {
                 g.drawLine(regOffset[j]+37, regTopBound+regHeight, regOffset[j]+37, 160);
                }
                break;
       case 4:  g.drawLine(39, 164, 675, 164);
                for (int j = 1; j <=7; j++) {
                 g.drawLine(regOffset[j]+42, regTopBound+regHeight, regOffset[j]+42, 164);
                }
                break;
       case 5:  g.drawLine(39, 168, 675, 168);
                for (int j = 1; j <=7; j++) {
                 g.drawLine(regOffset[j]+47, regTopBound+regHeight, regOffset[j]+47, 168);
                }
                break;
       case 6:  g.drawLine(regOffset[4]+regWidth, regTopBound+8,    // Between AC and MBR
                                regOffset[3], regTopBound+8);
                break;
       case 7:  g.drawLine(regOffset[1]+regWidth-7, regTopBound+12, // Between MAR and MM
                                regOffset[0], regTopBound+12);
                break;
       case 8:  g.drawLine(regOffset[4]+regWidth, regTopBound+18,   // Between AC and ALU
                                aluXvals[0]+10, regTopBound+18);
                g.drawLine(aluXvals[0]+10, regTopBound+18, 
                                aluXvals[0]+10, aluYvals[0]);
                break;
       case 9:  g.drawLine(aluXvals[4]+10, aluYvals[4],            // Between ALU and MBR
                                aluXvals[4]+10, regTopBound+18); 
                g.drawLine(aluXvals[4]+10, regTopBound+18, 
                                regOffset[3], regTopBound+18);
       default: break;
     } // switch
    } // for
    g2.setColor(rTLFontColor);               
    g2.setFont(new Font("monospaced", Font.BOLD, 16));
    if (microoperation.length() > 0)
       g2.drawString(microoperation, 340 -(microoperation.length()*4), 200);  
    else
       g2.drawString(" ", 310, 200);   
   } // refresh()
} // ControlUnit     

    class ALU extends Polygon {
/******************************************************************************************
*  This class is an extension of a Polygon.  We add its particular representation, its    *
*  state (active or inactive), its connecting data lines, and also a redrawing method     *
*  that is called by the paintComponent() method of our graphics pane.                    *
******************************************************************************************/
    boolean active;      // whether participating in current operation.

    public ALU() {      // Constructor.
      super(aluXvals,aluYvals,9);
      this.active = false;
    } //aLU()
   
  void setState(boolean a) {  // Ultimately controls what color to use in
    this.active = a;          // repainting the component.
  } // setState()
  
   void refresh(Graphics g) {       // This method is used by paintComponent() to render
     Graphics2D g2 = (Graphics2D)g; // to render the polygon and its data lines.
     String contents;  
     if (this.active) {                  // If the ALU is active, use a bright color
       g2.setColor(regActiveColor);      // and an outline.
       g.fillPolygon(this);  
       g2.setColor(componentOutlineColor);
       g2.setStroke(outline);  
       g.drawPolygon(this);
     }
     else {
       g2.setColor(regInactiveColor);    // Otherwise, use a subdued color.
       g.fillPolygon(this); 
     }  
     g2.setColor(componentLabelColor);
     g2.setFont(new Font("sansserif", Font.BOLD, 14));
     g2.drawString("ALU", 383, 147);
   } // refresh()
} // ALU     
  
  class Register  {
/******************************************************************************************
*  The Register class comprises MARIE registers to include their state (active or         *
*  inactive), their contents (value), the manner in which they will be displayed (mode),  *
*  as well as a "register number" that identifies which register we have.  This class     *
*  contains several mutator and accessor methods.  The most important of these are the    *
*  graphics rendering (drawing) method that is used by paintComponent, and the method to  *
*  write the contents of all registers when a value changes and the machine state is      * 
*  active.                                                                                *
******************************************************************************************/
    int     designation; // Register number.
    short   value;       // value stored.
    int     mode;        // Mode of expression: DEC, HEX, or ASCII   
    boolean active;      // whether participating in register operation.

    public Register(int whichOne) {      // Constructor.
    if ((whichOne > MM) && (whichOne <= IR))     // Make sure we have a valid 
        this.designation = whichOne;                  // designation.
      else {
        this.designation = 0;                         // Default to MM.
      }
      this.value  = (short) 0;
      this.mode   =  0;
      this.active = false;
    } // Register()
  
  void setValue(int v) {
    if ((this.designation == MAR)          // If we have an address-type register,
          ||(this.designation == PC)) {    // always store a positive 12-bit value.
       if (this.value < 0) 
         v = v * -1;
       v = v & 0x0FFF;
    }
    this.value = (short) v;
    displayValues();
  } // setValue()
  
      public void setValue(String v) {
/******************************************************************************************
*   Sets the value in the register to the numeric value passed in the String argument     *
*   v.  This value is interpreted according to the current mode of the register.  So      *
*   if the register is in decimal mode, the string will be parsed consistent with the     *
*   mode. For example, if the register is in decimal mode, the string "15" is             *
*   interpreted as decimal 15.  If the register is in hex mode, the string "15" is        *
*   interpreted as decimal 21.                                                            *
******************************************************************************************/
        value = (short) stringToInt(mode, v);
        if (!errorFound)  {                          // If the string converted ok,
          setValue(value);                           // put it in the register.
        }
        else {                                       // Did not convert okay.
          fatalError = true;                         // Fatal error.
          errorCode = 7;
        }
    } // setValue()
   
   public String toString() {
/******************************************************************************************
*   Returns the string form of a register's contents.  The radix used for the string      *
*   is determined by the mode of the register.                                            *
******************************************************************************************/
      String rendering = new String();
      switch (this.mode) {
        case   HEX: if ((this.designation == MAR) 
                             ||(this.designation == PC))  
                      rendering = "  "+to3CharHexStr(this.value);
                    else
                      rendering = " "+to4CharHexStr(this.value);
                    break;
        case ASCII: if (this.value == 0)
                      rendering = null;
                    else
                      rendering = "    " + (char) (this.value % 128);
                    break;
           default: 
                    if ((designation != OUT) && (value > 0))
                      rendering = " "+Integer.toString(value); 
                    else
                      rendering = Integer.toString(value); 
      } // switch 
      return rendering;
    } // toString ()
       
  void setState(boolean a) {
    this.active = a;
  } // setState()
  
  void setMode(int m) {
    if ((m >= HEX) && (m <= ASCII)) 
      this.mode = m;
  } // setState()
  
  int getMode() {
    return mode;
  } // setState()
  
  int getValue() {                         // Accessor for value in integer form.
    return value; 
  } // getValue()
  
  void displayValues() {
  /************************************************************************************
  *  This method dumps the values of all registers to the monitor TextArea.  It will  * 
  *  be called anytime a register value changes.  We check that the machine is        *
  *  running before we start just to prevent the initializations from displaying.     *
  ************************************************************************************/
    if (machineState == MARIE_RUNNING) {
      String stateString = " "  + to4CharHexStr(regIR.getValue())     +  
                           "  " + to4CharHexStr(regOUTPUT.getValue()) +
                           "  " + to4CharHexStr(regINPUT.getValue())  +
                           "  " + to4CharHexStr(regAC.getValue())     + 
                           "  " + to4CharHexStr(regMBR.getValue())    +
                           "  " + to3CharHexStr(regPC.getValue())     + 
                           "  " + to3CharHexStr(regMAR.getValue())    + linefeed;
      traceTextArea.append(stateString);
      Document d = traceTextArea.getDocument();
      traceTextArea.select(d.getLength(), d.getLength());  // Scroll textarea to 
    } // if                                                // last entry made.
  } // displayValues()
  
   void refresh(Graphics g) {
     Graphics2D g2 = (Graphics2D)g;
     int width = regWidth;
     String contents;
     if (this.designation <= PC) {
       contents = new String(to3CharHexStr(this.value));
       width = width - 8;
       }
     else
       contents = new String(to4CharHexStr(this.value));
     if (this.active) {               // Draw active register in bright color
       g2.setColor(regActiveColor);   // with an outline.
       g2.fillRect(regOffset[this.designation], regTopBound, width ,regHeight);
       g2.setColor(componentOutlineColor);
       g2.setStroke(outline);  
       g2.drawRect(regOffset[this.designation], regTopBound, width ,regHeight);     
       g2.setFont(new Font("monospaced", Font.BOLD, 14));
       g2.setColor(regActiveFontColor); 
     }  
     else {
       g2.setColor(regInactiveColor);
       g2.fillRect(regOffset[this.designation], regTopBound, width ,regHeight);
       g2.setFont(new Font("monospaced", Font.BOLD, 14));
       g2.setColor(regInactiveFontColor);
     }
     g2.drawString(contents, regOffset[this.designation]+14, regTopBound+(regHeight/2)+5);    
     g2.setColor(componentLabelColor);               
     g2.setFont(new Font("sansserif", Font.BOLD, 14));
     g2.drawString(regName[this.designation], regOffset[this.designation], regTopBound+regHeight+16);
   } // refresh()
} // Register     
  
  int stringToInt(int mode, String literal) {
/******************************************************************************************
* Converts a String to an integer value.                                                  *
* Parameters:                                                                             *
*     int mode = DEC, HEX or ASCII (final static int constants),                          *
*     the String literal to be converted to an integer.                                   *
* As with actual hardware registers, this method returns a value only up to the word size *
* of the system.  So to return a value that can be stored in a 16-bit register, we use    *
* an intermediate short integer.                                                          *
* This method will return Integer.MAX_VALUE to flag any exceptions thrown.  (We can get   *
* away with this because Marie's word size is smaller than Java's.) The errorFound flag   *
* is also set.  The invoking method can decide what to do about it.                       *
******************************************************************************************/
    errorFound = false;
    String numStr = literal.trim();
    short shortResult = 0;
    int result = Integer.MAX_VALUE;

    switch (mode) {
      case (DEC): {      // DECimal literal.
                 try {
                   shortResult = (short) Integer.parseInt(numStr, 10);
                 }
                 catch (NumberFormatException e) {
                   errorFound = true;
                   return result;
                 }
                 break;
               }

      case (HEX): {    // HEXadecimal literal.
                 try {
                   shortResult = (short) Integer.parseInt(numStr, 16);
                 }
                 catch (NumberFormatException e) {
                   errorFound = true;
                   return result;
                 }
                 break;
               }
      case (ASCII):   // Return value of first ASCII character of a string.
                  if (numStr.length() == 0)     // Return zero on a null string.
                    return 0;
                  int i = (int) numStr.charAt(0);
                  return i % 128;
      } // switch()
    result = shortResult;
    return result;
  } // stringToInt()
  
    String to3CharHexStr(int number) {
/******************************************************************************************
* Converts the argument number to a string containing exactly 3 characters by padding     *
* shorter strings and truncating longer strings.  So an argument larger than 8092 or      *
* smaller than 0 will be truncated to end up in the (unsigned) range 0 - 4095.            *
******************************************************************************************/
                                              // If number negative, convert to 16-bit 2's
    if (number < 0) {                         // complement by shifting the low-order 20
      number = number << 20;                  // bits to the high-order bits.  (We lose the
    }                                         // rightmost bits below.)

    String    hexStr = Integer.toHexString(number).toUpperCase();
    switch (hexStr.length()) {
       case 1: hexStr = "00"+hexStr;                 // Pad strings shorter than 3 chars.
               break;
       case 2: hexStr = "0" +hexStr;
               break;
       case 3: break;
      default: hexStr =  hexStr.substring(0, 3);     // Truncate strings longer than 3 chars
    } // switch()
    return hexStr;
  } // to3CharHexStr()
  
   String to4CharHexStr(int number) {
/******************************************************************************************
*    Returns a string of exactly 4-characters that are in the (decimal) range             *
*    -32,768 to 32,767.                                                                   *
******************************************************************************************/
    if (number < 0) {                           // If number negative, convert to 16-bit 2's
      number = number << 16;                    // complement by shifting the low-order 16
    }                                           // bits to the high-order bits.  (We lose the
                                                // rightmost 16 bits below.)
    String    hexStr = Integer.toHexString(number).toUpperCase();
    switch (hexStr.length()) {
       case 1: hexStr =  "000"+hexStr;             // Pad strings shorter than 4 chars.
               break;
       case 2: hexStr =  "00" +hexStr;
               break;
       case 3: hexStr =  "0" +hexStr;
               break;
       case 4: break;
      default: return hexStr.substring(0, 4);   // Truncate strings longer than 4 chars.
    } // switch()
    return hexStr;
  } // to4CharHexStr()
  
 class MainMemory {
/******************************************************************************************
*   This class defines a the MARIE main memory and the data bus as they appear on the     *
*   screen.  The actual memory contents are stored in a simple array.  The MainMemory     * 
*   representation can be active or inactive, and the memory bus can be independently     *
*   set to active if a register-to-register transfer is taking place.                     *
******************************************************************************************/
    boolean active;      // whether participating in current operation.
    boolean busActive;

    public MainMemory() {      // Constructor.
      this.active = false;
      this.busActive = false;
    } //MainMemory()
   
  void setState(boolean a, boolean d) {
    this.active = a;
    this.busActive = d;
  } // setState()
  
   void refresh(Graphics g) {
     Graphics2D g2 = (Graphics2D)g;
     if (this.busActive)                   // First, draw the bus.
       g2.setColor(busActiveColor);
     else
     g2.setColor(busInactiveColor);
     g2.setStroke(dataBus); 
     g.drawLine(regOffset[7]+ 27, regTopBound-40, regOffset[7]+ 27, regTopBound-4);
     g.drawLine(regOffset[7]+ 27, regTopBound-40, regOffset[0], regTopBound-40);
     g.drawLine(regOffset[6]+ 27, regTopBound-40, regOffset[6]+ 27, regTopBound-4);
     g.drawLine(regOffset[5]+ 27, regTopBound-40, regOffset[5]+ 27, regTopBound-4);
     g.drawLine(regOffset[4]+ 27, regTopBound-40, regOffset[4]+ 27, regTopBound-4);
     g.drawLine(regOffset[3]+ 27, regTopBound-40, regOffset[3]+ 27, regTopBound-4);
     g.drawLine(regOffset[2]+ 27, regTopBound-40, regOffset[2]+ 27, regTopBound-4);
     g.drawLine(regOffset[1]+ 27, regTopBound-40, regOffset[1]+ 27, regTopBound-4);
     if (this.active) {
       g2.setColor(memoryActiveColor);     // Then draw memory.  If its active, 
       g.fillRect(675, 15, 55, 170);        // draw in bright color with an outline.
       g2.setColor(componentLabelColor);
       g2.setColor(componentOutlineColor);
       g2.setStroke(outline);  
       g.drawRect(675, 15, 55, 170);
     }  
     else {
       g2.setColor(memoryInactiveColor);   // Otherwise, draw it in subdued color.
       g.fillRect(675, 15, 55, 170);
     }  
     g2.setColor(componentLabelColor);               
     g2.setFont(new Font("sansserif", Font.BOLD, 12));
     g2.drawString("Main", 675, 200);
     g2.drawString("Memory", 675, 215);
   } // refresh()
  } // MainMemory 
  
  class DataPathPanel extends JPanel {
/******************************************************************************************
*   The DataPathPanel class renders the graphics for data movement simulaton. Its prin-   * 
*   cipal function is to contain and render these graphics.                               *
******************************************************************************************/ 
    DataPathPanel() {
      super();
    }
    public void paintComponent(Graphics g)	{
      Graphics2D g2 = (Graphics2D)g;
      super.paintComponent(g2);
      controlUnit.refresh(g);
      regIR.refresh(g);
      regOUTPUT.refresh(g);
      regINPUT.refresh(g);
      regAC.refresh(g);
      regMBR.refresh(g);
      regPC.refresh(g);
      regMAR.refresh(g);
      aLU.refresh(g);
      memory.refresh(g);
    } // paintComponent()
  } // DataPathPanel

  public MarieDPath() {
/******************************************************************************************
*  This is the constructor for the simulator frame.  It populates the screen with a       *
*  menu bar and two frames: one for the graphics, and one for the execution monitor.      *
******************************************************************************************/  
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    
    this.setSize(new Dimension(760, 545));
    this.setIconImage(Toolkit.getDefaultToolkit()
                          .createImage(MarieDPath.class.getResource("M.gif")));
    this.setTitle("MARIE Data Path Simulator");
    this.setResizable(false);
    exeFileChooser                              // Make sure we open only executable files.
             .addChoosableFileFilter(new MarieExecutableFileFilter());
    exeFileChooser
             .removeChoosableFileFilter(exeFileChooser.getAcceptAllFileFilter());    
    dataPathSimPane = (JPanel) this.getContentPane();
    dataPathSimPane.setLayout(null);
    dataPathSimPane.setBackground(Color.lightGray);
/* --                                                                                 -- */
/* --             Build the machine control panel (menu bar).                         -- */
/* --                                                                                 -- */
    fileMenu.setText("File");                 // "File" menu
    fileMenu.setMnemonic('F');
    fileMenu.setToolTipText("Start here!");
    loadFileItem.setText("Load");                // Load file
    loadFileItem.setMnemonic('L');
    loadFileItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            getProgram();
         }
    }); // Listener
    
    restartItem.setText("Restart");             // Start over without reloading.
    restartItem.setEnabled(false);
    restartItem.setMnemonic('E');
    restartItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { 
            restart();
         }
    }); // Listener

    resetItem.setText("Reset Simulator");            // Simulator reset.
    resetItem.setMnemonic('S');
    resetItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           confirmReset();
         }
    }); // Listener

    exitFileItem.setText("Exit");                   // Quit the program
    exitFileItem.setMnemonic('X');
    exitFileItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           exitProgram();
         }
    }); // Listener
    fileMenu.add(loadFileItem);
    fileMenu.add(restartItem);
    fileMenu.add(resetItem);
    fileMenu.addSeparator();
    fileMenu.add(exitFileItem);
    
    runStop.setText("Run");                       // Add the "stop" button.
    runStop.setMnemonic('R');
    runStop.setEnabled(false);
    runStop.setMaximumSize(new Dimension(70, 27));   
    runStop.setMinimumSize(new Dimension(70, 27));   
    runStop.setPreferredSize(new Dimension(70, 27));   
    runStop.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {        // This button is a toggle.   
           if (machineState == MARIE_RUNNING) {           // If the machine is running,
             runStop.setText("Run");                      // stop it;  otherwise,
             step.setEnabled(true);                       // start it.
             machineState = MARIE_HALTED_BY_USER;
             setStatusMessage(" Halted at user request.");              
           }
           else {
             restartItem.setEnabled(true);
             step.setEnabled(false);
             runStop.setText("Stop"); 
             runStop.setMnemonic('T');
             runStop.setEnabled(true);
             if ((machineState == MARIE_HALTED_NORMAL) ||
                 (machineState == MARIE_HALTED_ABNORMAL))
               restart();  
             machineState = MARIE_RUNNING;  
             runProgram();
            }
      }
    }); // Listener
   
    step.setText("Step");                        // Set properties for the "next 
    step.setMnemonic('T');                       // step" button.
    step.setEnabled(false);
    step.setMaximumSize(new Dimension(70, 27));   
    step.setMinimumSize(new Dimension(70, 27));   
    step.setPreferredSize(new Dimension(70, 27));   
    step.setFocusPainted(false);
    step.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
          restartItem.setEnabled(true);         
          if ((machineState != MARIE_BLOCKED_ON_INPUT) && (!fatalError)) {
            setStatusMessage(" Press [Step] to continue.");
            stepping = true;
            runStop.setText("Stop");
            runStop.setMnemonic('T');              
             if ((machineState == MARIE_HALTED_NORMAL) ||
                 (machineState == MARIE_HALTED_ABNORMAL))
              restart();
            machineState = MARIE_RUNNING;
            runProgram();
          }
      }
    }); // Listener
   
    setSpeed.setText("Set Speed");           // Delay frame button
    setSpeed.setMnemonic('D');
    setSpeed.setMaximumSize(new Dimension(100, 27));   
    setSpeed.setMinimumSize(new Dimension(100, 27));   
    setSpeed.setPreferredSize(new Dimension(100, 27));   
    setSpeed.setFocusPainted(false);
    setSpeed.setEnabled(true);
    setSpeed.setToolTipText("Set delay between execution steps.");
    setSpeed.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           displayDelayFrame();
      }
    }); // Listener   

    setFastFetch.setText("Set Fast Fetch On");         // Set fast fetching mode is off.
    setFastFetch.setMnemonic('E');
    setFastFetch.setFont(new java.awt.Font("sanserif", Font.BOLD, 11));
    setFastFetch.setMaximumSize(new Dimension(130, 27));   
    setFastFetch.setMinimumSize(new Dimension(130, 27));   
    setFastFetch.setPreferredSize(new Dimension(130, 27));   
    setFastFetch.setFocusPainted(false);
    setFastFetch.setEnabled(true);
    setFastFetch.setToolTipText("Turn fast fetch mode on.");
    setFastFetch.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           if (fastFetch) {
             fastFetch = false;
             setFastFetch.setText("Set Fast Fetch On");  
             setFastFetch.setToolTipText("Turn fast fetch mode on.");
             setStatusMessage(" Fast fetch mode is off.");
           }
           else {
             fastFetch = true;
             setFastFetch.setText("Set Fast Fetch Off");  
             setFastFetch.setToolTipText("Turn fast fetch mode off.");
             setStatusMessage(" Fast fetch mode is on.");
           }  
      }
    }); // Listener   

    getHelp.setText("Help");
    getHelp.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        displayHelpFrame();
      }
    }); // Listener

    helpMenu.setText("Help");
    helpAbout.setText("About");
    helpAbout.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        displayHelpAboutFrame();
      }
    }); // Listener

    helpMenu.add(getHelp);
    helpMenu.add(helpAbout);

    controlBar.add(fileMenu);
    controlBar.add(runStop);
    controlBar.add(step);
    controlBar.add(setSpeed);
    controlBar.add(setFastFetch);
    controlBar.add(helpMenu);
    this.setJMenuBar(controlBar);
    
    dataPathPanel = new DataPathPanel();      // Construct graphics panel and add to frame.
    dataPathPanel.setBounds(new Rectangle(1, 1, 749, 240));
    dataPathPanel.setBackground(panelBackground);
    dataPathPanel.setEnabled(true);
    dataPathPanel.setFont(new java.awt.Font("sanserif", 0, 10));
    dataPathPanel.setLayout(null);
    dataPathSimPane.add(dataPathPanel, null);
        
    monitorPanel.setBounds(new Rectangle(1, 242, 749, 245));          // Construct panel 
    monitorPanel.setBorder(BorderFactory.createRaisedBevelBorder());  // containing monitoring
    monitorPanel.setBackground(monitorPanelColor);                    // elements.
    monitorPanel.setLayout(null);
    
    programPane = createProgramPanel();                         // Create program execution  
    programPane.setBorder(BorderFactory.createEtchedBorder());  // monitor table.
        
    programPane.setBounds(new Rectangle(5, 14, 300, 190));
    monitorPanel.add(programPane);
    
    controlPanel.setBorder(BorderFactory.createEtchedBorder()); // Create panel for controls
    controlPanel.setOpaque(true);                               // and execution trace pane.
    controlPanel.setBounds(new Rectangle(308, 14, 435, 190));
    controlPanel.setBackground(controlPanelBackground);
    controlPanel.setLayout(null);
    
    buttonPanel.setBounds(new Rectangle(2, 2, 80, 185));        // This panel holds the
    buttonPanel.setBorder(BorderFactory.createEtchedBorder());  // print button and its label.
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
    
    printButtonLabel.setText("Trace");
    printButtonLabel.setForeground(registerTextColor);
    printButtonLabel.setSize(60, 60);  
    printPanel.setMinimumSize(new Dimension(72, 80));  
    printPanel.setMaximumSize(new Dimension(72, 80));   
    printPanel.setPreferredSize(new Dimension(72, 80)); 
    printPanel.setBackground(buttonPanelColor);
    printTrace.setText("Print");
    printTrace.setBackground(buttonPanelColor);
    printTrace.setForeground(registerTextColor);
    printTrace.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           printTraceText();
      }
    }); // Listener   
    printPanel.add(printButtonLabel);
    printPanel.add(printTrace);
    buttonPanel.add(printPanel);
    
    inputPanel.setMinimumSize(new Dimension(80, 95)); 
    inputPanel.setMaximumSize(new Dimension(80, 95));
    inputPanel.setPreferredSize(new Dimension(80, 95));  
    inputPanel.setBackground(buttonPanelColor);
    inputPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(3, 2, 2, 3),
                                                      new EtchedBorder()));
    inputPanel.setLayout(new FlowLayout());
    inputLabel.setFont(new Font("sanserif", 0, 12));
    inputLabel.setForeground(registerTextColor);  
    inputLabel.setMinimumSize(new Dimension(40, 25)); 
    inputLabel.setToolTipText("Input Register");
    
    inputContent.setMaximumSize(new Dimension(60, 20));   
    inputContent.setMinimumSize(new Dimension(60, 20));   
    inputContent.setPreferredSize(new Dimension(60, 20));   
    inputContent.setBorder(BorderFactory.createCompoundBorder( 
                                         BorderFactory.createLoweredBevelBorder(),
                                                // Top, Left, Bottom, Right
                                         new EmptyBorder(0, 10, 0, 0)));
    inputContent.setText(" ");
    inputContent.setEditable(false);
    inputContent.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        input();
        inputContent.requestFocus();
      }
    }); // Listener
    inputBase.setSize(new Dimension(60, 20));   
    inputBase.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(3, 4, 2, 3),
                                                      new EtchedBorder()));
    inputPanel.add(inputLabel);
    inputPanel.add(inputContent);
    inputBase.setFont(new Font("sanserif", 0, 10));
    inputBase.setBackground(buttonPanelColor);
    inputBase.setForeground(registerTextColor);
    inputBase.setSelectedIndex(0);                            // Default input to hex
    regINPUT.setMode(0);
    inputPanel.add(inputBase);
    inputBase.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int i = inputBase.getSelectedIndex();
        regINPUT.setMode(i);
        inputContent.setText(regINPUT.toString());
      }
    }); // Listener

    buttonPanel.add(inputPanel);
    controlPanel.add(buttonPanel);
    
    tracePane.setBounds(new Rectangle(82, 2, 350, 185));
    tracePaneBorder.setTitleColor(registerTextColor); 
    tracePane.setBorder(tracePaneBorder);
    traceTextArea.setFont(new Font("monospaced", Font.PLAIN, 14));
    traceTextArea.setText("");

    tracePane.getViewport().add(traceTextArea, null);
    controlPanel.add(tracePane);
    
    monitorPanel.add(controlPanel);
    dataPathSimPane.add(monitorPanel);
    
    msgField.setBackground(messageBackground);            // Machine message
    msgField.setFont(new Font("SansSerif", 0, 11));       // text field
    msgField.setBounds(new Rectangle(6, 205, 730, 35));
    msgField.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(monitorPanelColor, 8),
                            BorderFactory.createLoweredBevelBorder()));
    msgField.setEditable(false);
    setStatusMessage(" Ready to load program instructions.");
    monitorPanel.add(msgField);
    dataPathPanel.repaint();
  } // DataPath()

 JScrollPane createProgramPanel() {
/******************************************************************************************
*  This method sets up a JTable that provides the means to monitor program execution.     *
*  it is placed in a JScrollPane which is returned to the calling method.                 *
*                                                                                         *
*  We use two tables to render the program monitor:  The first is a table containing      *
*  the program statement numbers and a checkbox that is used to set execution             *
*  breakpoints.  The second holds the program instructions themselves along with their    *
*  hexadecimal equivalents.                                                               *
*                                                                                         *
*  As program execution proceeds, the statements are highlighted using a special table    *
*  cell renderer, which is in the global table model above.  Before returning to the      *
*  caller, we load the table with blanks for aesthetic reasons.                           *
*  Much of the code in this method was derived from Eckstein, Loy and Wood's fine book,   *
*  *Java Swing* O'Reilly 1998.                                                            *
******************************************************************************************/
    class ProgramTableCellRenderer extends JLabel 
                                    implements TableCellRenderer { 
     public Component getTableCellRendererComponent( 
                                   JTable table, Object value, boolean isSelected, 
                                   boolean hasFocus, int row, int column)          { 
       setText((String) value); 
       setFont(new Font("Monospaced", 0, 12));
       setOpaque(true); 
       setBackground(Color.white); 
       setForeground(Color.black); 
       setBorder(new EmptyBorder(0, 0, 0, 0)); 
       if (row == programFocusRow) {                        
         setBackground(programCursorColor);                                  
       }                                                             
       else {                                                        
        setBackground(Color.white);                                  
       }                         
       return this; 
    } // getTableCellRendererComponent()
  } // ProgramTableCellRenderer

    class RowHeaderTableCellRenderer extends JLabel 
                                    implements TableCellRenderer { 
     public Component getTableCellRendererComponent( 
                                   JTable table, Object value, boolean isSelected, 
                                   boolean hasFocus, int row, int column)          { 
       setText((String) value); 
       setFont(new Font("sanserif", 0, 11));
       setForeground(new Color(0, 50, 165));
       return this; 
    } // getTableCellRendererComponent()
  } // RowHeaderTableCellRenderer

    TableColumnModel cm = new DefaultTableColumnModel() { 
      int colno = 0;                              // This is the column
      public void addColumn(TableColumn tc) {     // model for the main
        switch (colno) {                          // part of the program
        case 0: colno++;                          // table.
                return;   // Drop first column.
        case 1: tc.setMinWidth(62);
                tc.setMaxWidth(62);
                tc.setPreferredWidth(62);
                break;
        case 4: tc.setMinWidth(40);
                tc.setMaxWidth(40);
                tc.setPreferredWidth(40);
                break;
        default:
          tc.setMinWidth(68);
          tc.setMaxWidth(68);
        } // switch
        super.addColumn(tc);
        colno++;
        if (colno > 4)
          colno = 0;
      }
    };  // TableColumnModel

   TableColumnModel rowHeaderModel = new DefaultTableColumnModel() { 
      boolean firstCol = true;                   // This is the column 
      public void addColumn(TableColumn tc) {    // model for first
        if (firstCol) {                          // column only. 
          tc.setMaxWidth(40);
          firstCol = false;
          super.addColumn(tc);
        }
      }
    };  // TableColumnModel

    programTable = new JTable(ptm, cm);
    programTable.setRowSelectionAllowed(false);             // Prohibit mouse clicks
    programTable.setCellSelectionEnabled(false);            // from highlighting 
    programTable.setSelectionBackground(Color.white);       // cells.
    programTable.setShowHorizontalLines(false);             // Gridline control: we 
    programTable.setShowVerticalLines(false);               // turn everything off and  
    programTable.setIntercellSpacing(new Dimension(0, 1));  // zero out borders so the 
                                                            // highlight will look like 
    programTable.setRowHeight(PROGRAM_TABLE_ROW_HEIGHT);    // a solid bar.
    ProgramTableCellRenderer renderer = new ProgramTableCellRenderer();
    programTable.setDefaultRenderer(Object.class, renderer);

    JTableHeader tableHeader = programTable.getTableHeader();
    tableHeader.setForeground(tableHeaderColor);
    tableHeader.setFont(new Font("Dialog", 0, 11));
    tableHeader.setReorderingAllowed(false);
    tableHeader.setResizingAllowed(false);
    tableHeader.setBorder(new EtchedBorder(EtchedBorder.LOWERED)); 
    programTable.createDefaultColumnsFromModel();

    JTable headerColumn = new JTable(ptm, rowHeaderModel);
    headerColumn.createDefaultColumnsFromModel();
    headerColumn.setMaximumSize(new Dimension(40, 10000));
    headerColumn.setBackground(Color.lightGray);
    headerColumn.setShowVerticalLines(false);
    headerColumn.setSelectionBackground(Color.lightGray);   // Makes header selection
                                                            // invisible.
    headerColumn.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
    headerColumn.setColumnSelectionAllowed(false);
    headerColumn.setCellSelectionEnabled(false);
    RowHeaderTableCellRenderer headerRenderer = new RowHeaderTableCellRenderer();
    headerColumn.setDefaultRenderer(Object.class,headerRenderer);
    headerColumn.setRowHeight(PROGRAM_TABLE_ROW_HEIGHT);

    programTable.setSelectionModel                          // Keep tables in synch
                    (headerColumn.getSelectionModel());     // by using same model.
    JViewport jv = new JViewport();
    jv.setView(headerColumn);
    jv.setPreferredSize(headerColumn.getMaximumSize());
    headerColumn.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    JScrollPane jsp = new JScrollPane(programTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    codeLineCount = 9;
    programArray  = new Object[9][5];        // Load program instruction monitor table
    for (int i = 0; i < 9; i++) {            // with blanks.
      programArray[i][0] = "  ";
      programArray[i][1] = "  ";
      programArray[i][2] = "  ";
      programArray[i][3] = "  ";
      programArray[i][4] = "  ";
    }
    jsp.setRowHeader(jv);
    return jsp;
 } // createProgramPanel()

/* --                                                                                 -- */
/* --  Marie frame control methods.                                                   -- */
/* --                                                                                 -- */
  void confirmReset() {
/******************************************************************************************
*  When a reset is invoked by the user, we first ask for confirmation, since it's easier  *
*  to accidentally press this option with the mouse than to press a real reset button.)   *
******************************************************************************************/
    JFrame resetFrame = new JFrame("Confirm Reset");
    resetFrame.setIconImage(Toolkit.getDefaultToolkit()
                      .createImage(MarieSim.class.getResource("M.gif")));
    int option = JOptionPane
                    .showOptionDialog(resetFrame, "Are you sure?",
                                      "Reset Confirmation", JOptionPane.YES_NO_OPTION,
                                      JOptionPane.QUESTION_MESSAGE, null,
                                       new Object[] {"Yes", "No"}, "No");
    if (option == JOptionPane.NO_OPTION)
      return;
    marieReset();
  } // confirmReset()
  
  void displayDelayFrame() {
/******************************************************************************************
*  If we have previously created an instance of the frame that accepts the delay          *
*  parameter, we will make it visible.  If the frame has not  been instantiated yet,      *
*  or if it was disposed of, an exception is thrown and we  create another instance.      *
*                                                                                         *
*  We add a window listener so that we can completely destroy the frame object after the  *
*  JFrame is closed.                                                                      *
******************************************************************************************/
     try {
           delayFrame.show();
           delayFrame.requestFocus();   
     }
     catch (Exception e) {  
           delayFrame = new DelayFrame();
           delayFrame.addWindowListener(new WindowAdapter() {
              public void windowClosing(WindowEvent e) {
                   delayFrame = null;
              } // windowClosing()
           }); // Listener
     }                               
  } //displayDelayFrame()
  
  void displayHelpFrame() {
/******************************************************************************************
*  This method works the same way as displayDelayFrame().  See explanation above.         *
******************************************************************************************/
     try {
           helpViewer.show();
           helpViewer.requestFocus();
     }
     catch (Exception e) {         
              helpViewer = new TextFileViewer("Simulator Help", HELP_FILE, false);
              helpViewer.setSize(400, 280);
              helpViewer.setLocation(350, 50);
              helpViewer.addWindowListener(new WindowAdapter() {
                  public void windowClosing(WindowEvent e) {
                       helpViewer = null;
                  } // windowClosing()
              }); // Listener 
              helpViewer.show();
     }
  } // displayHelpFrame()

  void displayHelpAboutFrame() {
/******************************************************************************************
*  This method works the same way as displayDelayFrame().  See explanation above.         *
******************************************************************************************/
     try {
           helpAboutFrame.show();
           helpAboutFrame.requestFocus();   
     }
     catch (Exception e) {  
           helpAboutFrame = new HelpAboutFrame(this);
           helpAboutFrame.addWindowListener(new WindowAdapter() {
              public void windowClosing(WindowEvent e) {
                   helpAboutFrame = null;
              } // windowClosing()
           }); // Listener
     }                               
  } //displayHelpAboutFrame()  
  
    protected void processWindowEvent(WindowEvent e) {  // Overridden JFrame method.
/******************************************************************************************
*  This overridden JFrame method gives us control over the exit mode taken from the       *
*  program no matter how the user closes the window.  It simply calls the same exit       *
*  method that the user would pick from the File | Exit menu pick.                        *
******************************************************************************************/
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      exitProgram();
    } // if
  } // processWindowEvent()

  void exitProgram() {
/******************************************************************************************
*  Before closing the simulator, we ask the user to confirm the decision (to avoid        *
*  accidental selection).  The confirmation is done through a JOptionPane popup set       *
*  to default to "Yes."                                                                   *
******************************************************************************************/
    JFrame closingFrame = new JFrame("Confirm Quit");
    closingFrame.setIconImage(Toolkit.getDefaultToolkit()
                      .createImage(MarieDPath.class.getResource("M.gif")));
    int option = JOptionPane
                    .showOptionDialog(closingFrame, "Really quit?",
                                      "Quit Confirmation", JOptionPane.YES_NO_OPTION,
                                      JOptionPane.QUESTION_MESSAGE, null,
                                       new Object[] {"Yes", "No"}, "Yes");
    if (option == JOptionPane.YES_OPTION)
      System.exit(0);
 } // exitProgram()

  void setStatusMessage(String msg) {
/******************************************************************************************
*  Places the message, msg, in the text field at the bottom of the screen and then fires  *
*  an update event.  (The text field has a listener for this event.)                      *
******************************************************************************************/
   statusMessage = msg;
   msgField.setText(statusMessage);
   msgField.postActionEvent();
  } // setErrorMessage()

  void printTraceText() {
/******************************************************************************************
*  This method calls upon SDK 1.4 printing facilities to print the contents of the        *
*  register trace area.  We first write the contents of this area to a file because the   *
*  DocFlavor.STRING.TEXT_PLAIN is not supported by all systems.  Although the same type   *
*  of content is involved, the DocFlavor.INPUT_STREAM.AUTOSENSE, however, doesn't give    *
*  us this trouble. Go figure!                                                            *
******************************************************************************************/
   BufferedWriter tempFile = null;
   String formFeed      = "\014";
   try {                                                 // Create the temporary output.
     tempFile = new BufferedWriter( new FileWriter("datapath.tmp") );
   } // try
   catch (IOException e) {
     System.err.println(e); 
   return;
   } // catch
   try {            
     tempFile.write(traceTextArea.getText());             // Capture the text in the
     tempFile.write(formFeed);                            // trace JTextArea.
     tempFile.close();           
     }
   catch (IOException e) {
     System.err.println(e);
     return;  
   } // catch
                                                        // Print the file.
   DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE; 
   PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet(); 
   PrintService printService [] = PrintServiceLookup.lookupPrintServices(flavor,pras); 
   PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();      
   PrintService service =
        ServiceUI.printDialog(null,100,100,printService,defaultService,flavor,pras); 
   if (service != null) { 
      try {
        DocPrintJob job = service.createPrintJob(); 
        FileInputStream fis = new FileInputStream("datapath.tmp");
        DocAttributeSet das = new HashDocAttributeSet(); 
        Doc doc = new SimpleDoc(fis, flavor, das); 
        job.print(doc, pras);
      } // try
      catch (Exception e) {
        ;
     } // catch
   } 
   try {              
     File tmp = new File("datapath.tmp");                  // Delete the temp file.
     tmp.delete();
     }
   catch (Exception e) {
     ;
   } // catch
} // printTraceText()

/* ------------------------------------------------------------------------------------- */
/* -- Machine loading and reset methods.                                              -- */
/* ------------------------------------------------------------------------------------- */
  void getProgram() {
/******************************************************************************************
*  This method accepts a filename and path from a JFileChooser dialog.  The path and      *
*  file extension are stripped off so the root filename can be used to locate other       *
*  files related to the executable, such as the symbol table.                             *
******************************************************************************************/
    String aFileName = null;
    if (!(JFileChooser.APPROVE_OPTION == exeFileChooser.showOpenDialog(this))) {
      setStatusMessage(" File loading canceled.");
      return;
    }
    aFileName = exeFileChooser.getSelectedFile().getPath();
    int dirEndPos = 0;                                 // Strip the path to
    int extensionStart = 0;                            // get the filePrefix.
    dirEndPos = aFileName.lastIndexOf(fileSeparator);
    if (dirEndPos < 0)
     dirEndPos = -1;
    else
      mexPath = aFileName.substring(0, dirEndPos);     // Save the path.
    extensionStart = aFileName.lastIndexOf(MEX_TYPE);
    if (extensionStart > 0)                            // Save the root filename.
      mexFile = mexPath + fileSeparator + aFileName.substring(dirEndPos+1, extensionStart);
    loadProgram();                                     // Get the program.
  } // getProgram()


  void loadProgram() {
/******************************************************************************************
*  This method does the work of loading an ObjectStream of executable AssembledCodeLines  *
*  from disk.  This method should be called only by methods that have already established *
*  a valid filename.  We check to make sure that this filename isn't null before trying   *
*  to find the file.                                                                      *
*                                                                                         *
*  If we don't find the file, or if it's corrupted, the Exception caught is sent to the   *
*  message area of the simulator.                                                         *
*                                                                                         *
*  If we have found a valid file, the first thing we do is clear any remnants from a      *
*  previously-loaded program.  Then we load the codelines into a Vector from which        *
*  an enumeration will be used to load the program instruction array (programArray)       *
*  and the memoryArray.  We load into a Vector prior to loading the data structures       *
*  so that we can find out how big to make the programArray.  (This is created new        *
*  for each program loaded.)                                                              *
*                                                                                         *
*  Program line numbers are loaded into a HashTable that provides a correspondence        *
*  between the memory address of the program statement and the location of that           *
*  statement in the program monitor table.  We retrieve these addresses during the        *
*  fetch cycle.                                                                           *
*                                                                                         *
*  After all of the data structures are loaded, we set the menu options and menu          *
*  buttons appropriately.  If all loading was successful, the machineState will be        *
*  HALTED_NORMAL.  If there was a fatal error encountered during the load, the            *
*  machineState will be NO_PROGRAM_LOADED after the marieReset() call is performed.       *
******************************************************************************************/
    File               objectFile = null; 
    ObjectInputStream   objFileIn = null;
    AssembledCodeLine   aCodeLine = new AssembledCodeLine();
    Vector             codeVector = new Vector();
    errorFound = false;
    if (mexFile == null) {
      setStatusMessage(" No file to load.  Use File+Load menu picks.");
      return;
    }
    runStop.setEnabled(false);              // Assume program not runnable.
    step.setEnabled(false);
    restartItem.setEnabled(false);
    try {                                      // Try to open the input.
      objectFile = new File(mexFile+MEX_TYPE);
      objFileIn = new ObjectInputStream( new FileInputStream(objectFile) );
    } // try
    catch (FileNotFoundException e) {
      setStatusMessage(" File " + mexFile + MEX_TYPE + " not found.");
      errorFound = true;
    } // catch
    catch (IOException e) {
      setStatusMessage(" "+e); 
      errorFound = true;
    } // catch
    catch (Exception e) {
      setStatusMessage(" "+e); 
      errorFound = true;
    } // catch
    if (errorFound)                            // If we've found any problems,
      return;                                  // return to caller.
    marieReset();                              // Clear the simulator, including
    if (codeLineCount >=0)                     // any program loaded.         
      for (int i = 0; i < codeLineCount; i++) {
        programArray[i][0] = "  ";
        programArray[i][1] = "  ";
        programArray[i][2] = "  ";
        programArray[i][3] = "  ";
        programArray[i][4] = "  ";
      }
    codeLineCount = 0;
    boolean done = false;                      // Begin loading the program...
    while (!done) {
       if (codeLineCount >= MAX_MARIE_ADDR) {
         setStatusMessage(" Maximum program statements reached."); 
         errorFound = true;
         done = true;
       } // if
       try {             
          aCodeLine = (AssembledCodeLine) objFileIn.readObject();
          if (aCodeLine == null)
             done = true;
          else { 
            if (aCodeLine.lineNo.charAt(0) != ' ') {
              codeVector.add(aCodeLine);
              codeLineCount++;
            } // if
          } // else
       } // try
       catch (EOFException e) {                // At EOF, we're done.  
         done = true;                          // Other exceptions are "fatal."
       } // catch
       catch (IOException e) {
         setStatusMessage(" "+e);  
         errorFound = true;
         done = true;
       } // catch
      catch (Exception e) {
        setStatusMessage(" "+e); 
        errorFound = true;
        done = true;
      } // catch
      if (done)
        break;
    } // while
    try {                                      // Close the input.
       objFileIn.close(); 
       objectFile = null;
    } // try
    catch (IOException e) {
       setStatusMessage(" "+e); 
    } // catch
    if (errorFound)                            // If we found serious errors, return
      return;                                  // to caller.
    int addr = 0;
    programArray  = new Object[codeLineCount][5];    // Prepare program-specific data 
    codeReference.clear();                           // structures.
    Enumeration e = codeVector.elements();
    int lineCount = 0;

    while (e.hasMoreElements()) {                    // Load data structures.
      aCodeLine = (AssembledCodeLine) e.nextElement();
      programArray[lineCount][0] = "   "+aCodeLine.lineNo;  // Load the monitor table...
      programArray[lineCount][1] = aCodeLine.stmtLabel;
      programArray[lineCount][2] = aCodeLine.mnemonic;
      programArray[lineCount][3] = aCodeLine.operandToken;
      programArray[lineCount][4] = " "+aCodeLine.hexCode+aCodeLine.operand; 
      codeReference.put(aCodeLine.lineNo, new Integer(lineCount));
      lineCount++;
      try {
        addr = Integer.parseInt(aCodeLine.lineNo, 16);      // ... and load memory.
      }
      catch (NumberFormatException exception) {
        continue;
      } // catch
       memoryArray[addr] = Integer.parseInt(aCodeLine.hexCode+aCodeLine.operand, 16);
    } // while();
    ptm.fireTableStructureChanged();
    String aString = (String) programArray[0][0];
    try {                                                  // Get memory cell of
          addr = Integer.parseInt(aString.trim(), 16);     // first instruction.
    }                                               
    catch (NumberFormatException exception) {
           ;
    } // catch
                 
    regPC.setValue(addr);                                  // Set PC to first program address.
    Rectangle rect = programTable.getCellRect(0, 2, false);
    programFocusRow = 0;                                  // Highlight and make visible the
    programTable.scrollRectToVisible(rect);               // first program instruction.
    programPane.repaint();
    setStatusMessage(" "+mexPath+fileSeparator+mexFile+MEX_TYPE+" loaded.");
    runStop.setEnabled(true);                             // Set buttons accordingly.
    runStop.setText("Run");  
    runStop.setMnemonic('R');
    step.setEnabled(true);
    restartItem.setEnabled(true); 
    machineState = MARIE_HALTED_NORMAL;
  } // loadProgram()

  void restart() {
/******************************************************************************************
*   If the system is initialized and a program is loaded, this method resets the display  *
*   and program counter to their states after the program was initially loaded.  We       *
*   preserve user settings (e.g., "step mode") as they have been set by the user.         *
******************************************************************************************/
     if ((machineState == MARIE_UNINITIALIZED) ||
          (machineState == MARIE_NO_PROGRAM_LOADED))
        return;
     fatalError = false;
     errorCode = 0;
     if (stepping) {
       setStatusMessage(" Press [Step] to start.");
       step.setEnabled(true);
     }
     else setStatusMessage(" ");
     int startAddr = Integer.parseInt(((String) programArray[0][0]).trim(), 16);
     regPC.setValue(startAddr); // Set PC to first address of program loaded.
     Rectangle rect = programTable.getCellRect(0, 2, false);
     programFocusRow = 0;
     programTable.scrollRectToVisible(rect);
     traceTextArea.setText("  IR   OUT    IN    AC   MBR   PC   MAR" + linefeed);
     traceTextArea.repaint();
     programTable.repaint();
  } // restart()


  void marieReset() {
/******************************************************************************************
*  This method has the effect of pressing the reset button on a physical machine: It      *
*  clears everything.                                                                     *
******************************************************************************************/
    regAC.setValue(0);                          // Reset all registers to 0. 
    regIR.setValue(0);
    regMAR.setValue(0);
    regMBR.setValue(0);
    regPC.setValue(0);
    regINPUT.setValue(0);
    regOUTPUT.setValue(0);
    inputContent.setText("");
    traceTextArea.setText("");
    
    Arrays.fill(memoryArray, 0);               // Initialize memory.
    if (codeLineCount >=0)                     // If we already loaded a program, clear it.
      for (int i = 0; i < codeLineCount; i++) {
        programArray[i][0] = "  ";
        programArray[i][1] = "  ";
        programArray[i][2] = "  ";
        programArray[i][3] = "  ";
        programArray[i][4] = "  ";
      }
    programFocusRow = 0;
    machineState = MARIE_NO_PROGRAM_LOADED;
    runStop.setEnabled(false);               // Nothing to run...
    restartItem.setEnabled(false);           // Nothing to restart...
    step.setEnabled(false);                  // Nothing to step...
    setStatusMessage(" ");
    repaint();
  } // marieReset
  
/* --                                                                                 -- */
/* --  Marie machine functional methods --------------------------------------------- -- */
/* --                                                                                 -- */
    void fetch() {
  /****************************************************************************************
  *  This function performs two microoperations: MAR <-- PC; IR <-- M[MAR]; and           *
  *  PC <-- PC + 1 to load an from memory into the IR and prepare for the next            *
  *  instruction fetch by incrementing the PC.  At the end of this function, we leave     *
  *  the IR turned on, because it leads into the next action taken by the computer.       *
  *                                                                                       *
  *      Side effects: IR contains instruction,                                           *
  *                    MAR contains operand address (if any)                              *
  *                    MBR contains operand (if any)                                      *
  *                    fatalError set if invalid opcode in instruction                    *
  *                                     or invalid operand address.                       *
  ****************************************************************************************/
  
    int[] controlLines = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    int saveDelay = 0;
    int saveBriefDelay = 0;
    
    if (fatalError)  {                           // Stop if there has been an error.
      halt();
      return;
    }  
    
    setFastFetch.setEnabled(false);              // Prevent fast fetch from being changed.
    
    if (fastFetch) {                             // If the user has elected fast fetching
      saveDelay = delay;                         // mode, we will go through the fetch
      saveBriefDelay = briefDelay;               // cycle quickly by setting the delay 
      delay = 10;                                // between microoperations as short
      briefDelay = 10;                           // as possible.
      setSpeed.setEnabled(false);                // Prevent delay from being changed
    }                                            // after we have saved the delay.
    controlBar.repaint();                                          
    
    String aString = to3CharHexStr(regPC.getValue());           // Move the cursor.
    if (codeReference.containsKey(aString)) {
      programFocusRow =((Integer) codeReference.get(aString)).intValue();
      Rectangle rect = programTable.getCellRect(programFocusRow, 4, false);
      programTable.scrollRectToVisible(rect);
    }
    programTable.repaint();
                               // First microoperation: MAR <-- PC                   
    controlUnit.setState(false, controlLines, "(Fetch cycle) MAR <-- PC");   
    regPC.setState(true);
    controlLines[0] = 0;         // Set control lines to read PC and write to MAR
    controlLines[1] = 0; 
    controlLines[2] = 1;  
    regMAR.setState(true);
    controlLines[3] = 0;
    controlLines[4] = 1; 
    controlLines[5] = 0;     
    controlUnit.setState(true, controlLines, "");   
    dataPathPanel.repaint();
    waitABit(briefDelay);

    memory.setState(false, true);  // Turn on bus.
    regMAR.setValue(regPC.getValue());          // Place address in MAR.
    dataPathPanel.repaint();
    waitABit(briefDelay);
    
    regPC.setState(false);         // Turn off PC, control lines, and bus.
    memory.setState(false, false);
    controlLines[0] = 0;
    controlLines[1] = 0; 
    controlLines[2] = 0;  
    controlLines[3] = 0;
    controlLines[4] = 0; 
    controlLines[5] = 0; 
    controlLines[7] = 0;  
    controlUnit.setState(false, controlLines, ""); 
    dataPathPanel.repaint();
    waitABit(delay);                   // End of microop.
    
    memory.setState(true, false);  // Second microoperation: IR <-- M[MAR]
    regMAR.setState(true);
    controlLines[7] = 1;
    controlUnit.setState(false, controlLines, "(Fetch Cycle) IR <-- M[MAR]");        
    memory.setState(true, true);     // Turn on memory and connection to IR.
    controlLines[0] = 1; 
    controlLines[1] = 1;
    controlLines[2] = 1; 
    regIR.setState(true);
    controlUnit.setState(true, controlLines, "");  
    dataPathPanel.repaint();
    waitABit(briefDelay);    

    int instr = memoryArray[regMAR.getValue()];  // Retrieve instruction from
    regIR.setValue(instr);                       // memory and place into IR.
    dataPathPanel.repaint();
    waitABit(briefDelay);
    
    memory.setState(false, false);      // Turn off bus and control unit.
    regMAR.setState(false);
    controlLines[0] = 0;
    controlLines[1] = 0;
    controlLines[2] = 0;
    controlLines[7] = 0;
    regIR.setState(false); 
    controlUnit.setState(false, controlLines, "");
    dataPathPanel.repaint();
    waitABit(delay);                    // End of microop.
   
                                    //  Third microoperation: PC <-- PC + 1. 
    regPC.setState(true);
    controlUnit.setState(false, controlLines, "(Fetch Cycle) PC <-- PC + 1");    
    dataPathPanel.repaint(); 
    waitABit(briefDelay);
    
    regPC.setValue(regPC.getValue() + 1);  // Increment PC.
    dataPathPanel.repaint();
    waitABit(briefDelay);                // End of microop.
        
    regPC.setState(false);       // Turn off everything except IR.
    controlLines[0] = 0;
    controlLines[1] = 0;
    controlLines[2] = 0;
    controlUnit.setState(false, controlLines, " ");  
    monitorPanel.repaint(); 
    regIR.setState(true);
    errorFound = false;          // Reset error flag.
    if (fastFetch) {             // If fast fetch mode on, restore the delays.                
      delay = saveDelay;
      briefDelay = saveBriefDelay;
      setSpeed.setEnabled(true);
    }
    setFastFetch.setEnabled(true);              // Enable resetting fast fetch.
    controlBar.repaint();
  } // fetch()
  
    void jns() {
  /****************************************************************************************
  *  This function performs seven microoperations for the "jump and store" instruction:   *
  *     MBR <-- PC; MAR <-- X;  M[MAR]<-- MBR;                                            *
  *     MBR <-- X;  AC <-- 1; AC = AC + MBR; and PC <-- AC                                *
  *  to store a return address and branch to another program location.                    *
  *  Note: X is  <-- IR[11-0].                                                            *  
  ****************************************************************************************/
    int[] controlLines = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        
                                   // First microoperation: MBR <-- PC   
    controlUnit.setState(false, controlLines, "MBR <-- PC");                                   
    controlLines[0] = 0;                              // Set control lines to read PC 
    controlLines[1] = 1;                              // and write to MBR.
    controlLines[2] = 1;  
    regMBR.setState(true);  
    controlLines[3] = 0;
    controlLines[4] = 1; 
    controlLines[5] = 0;
    regPC.setState(true);  
    controlUnit.setState(true, controlLines, "");
    dataPathPanel.repaint();
    waitABit(briefDelay);      
    memory.setState(false, true);                    // Turn on the bus.
    regMBR.setValue(regPC.getValue()); // Activate MBR and move data from PC.
    dataPathPanel.repaint();
    waitABit(briefDelay);
    
    memory.setState(false, false);                       
    controlLines[0] = 0;                      
    controlLines[1] = 0;            
    controlLines[2] = 0;  
    regMBR.setState(false);  
    controlLines[3] = 0;
    controlLines[4] = 0; 
    controlLines[5] = 0;
    regPC.setState(false);   
    controlLines[6] = 0;               // Turn off connection between PC and MBR.
    regAC.setState(false);
    controlUnit.setState(false, controlLines, "(Decode IR[15-12])");  // End of microop.
    waitABit(delay);
    dataPathPanel.repaint();    
                                       // Second microoperation: MAR <-- IR[11-0]
    controlUnit.setState(false, controlLines, "MAR <-- IR[11-0]");  
    controlLines[0] = 0;                              // Set control lines to read IR 
    controlLines[1] = 0;                              // and write to MAR.
    controlLines[2] = 1;  
    controlLines[3] = 1;
    controlLines[4] = 1; 
    controlLines[5] = 1;
    regIR.setState(true);                             // Turn on IR     
    regMAR.setState(true);                            // Turn on MAR.
    memory.setState(false, true);                     // Turn on bus.
    controlUnit.setState(true, controlLines, "");     // Turn on control unit.
    dataPathPanel.repaint();
    waitABit(briefDelay);
    
    regMAR.setValue(regIR.getValue() & 0x0FFF);       // Get MAR value from IR.
    dataPathPanel.repaint();
    waitABit(briefDelay);
    
    regIR.setState(false);                            // Turn off IR, control lines
    memory.setState(false, false);                    // and bus.
    controlLines[0] = 0;
    controlLines[1] = 0; 
    controlLines[2] = 0;  
    controlLines[3] = 0;
    controlLines[4] = 0; 
    controlLines[5] = 0;   
    controlUnit.setState(false, controlLines, "");
    regMAR.setState(false);                           // Turn off MAR.
    waitABit(delay);
    dataPathPanel.repaint();             // Microop complete.
    
     
    controlLines[4] = 1;        // Third microop: M[MAR] <-- MBR.            
    controlLines[5] = 1;               // Set control lines to read MBR.
    regMBR.setState(true);
    controlLines[7] = 1;               // Activate control line between MAR and MM.
    regMAR.setState(true);
    controlUnit.setState(true, controlLines, "M[MAR] <-- MBR");  // Engage control unit.  
    memory.setState(false, true);      // Turn on the bus.
    dataPathPanel.repaint();
    waitABit(briefDelay);
    memory.setState(true, true);       // Turn on memory.
    memoryArray[regMAR.getValue()] = regMBR.getValue();    // Update memory.
    dataPathPanel.repaint();
    waitABit(delay);               // Microop complete.
    
    regMAR.setState(false);
    controlLines[7] = 0;
    memory.setState(false, false);
                              // Fourth microoperation: MBR <-- IR[11-0]
    controlLines[0] = 0;            // Set control lines to read from IR 
    controlLines[1] = 1;            // and write to MBR. 
    controlLines[2] = 1;
    controlLines[3] = 1;
    controlLines[4] = 1;
    controlLines[5] = 1;
    regIR.setState(true);
    controlUnit.setState(true, controlLines, "MBR <-- IR[11-0]"); 
    memory.setState(false, true);                     // Turn on the bus.
    dataPathPanel.repaint();
    waitABit(briefDelay);
    regMBR.setValue(regIR.getValue() & 0x0FFF);       // Get MBR value from IR.
    controlUnit.setState(false, controlLines, ""); 
    regIR.setState(false);
    dataPathPanel.repaint();
    waitABit(delay);            // End of microop.
    
                            // Fifth microop: AC <-- 1 
    controlLines[0] = 1;            // Set control lines to write to AC
    controlLines[1] = 0;
    controlLines[2] = 0;
    regAC.setState(true);
    controlLines[3] = 0;
    controlLines[4] = 0;
    controlLines[5] = 0;  
    regMBR.setState(false);  
    memory.setState(false, false);     // Turn off bus.
    controlUnit.setState(true, controlLines, "AC <-- 1");  
    dataPathPanel.repaint();
    waitABit(briefDelay);
    regAC.setValue(1);
    dataPathPanel.repaint();
    waitABit(delay);            // End of microop.
    
                            // Sixth microop: AC <-- AC + MBR 
    regMBR.setState(true);              // Enable MBR.
    controlLines[0] = 0;                // Reset control lines to AC.
    controlLines[1] = 0;
    controlLines[2] = 0;
    controlUnit.setState(false, controlLines, "AC <-- AC + MBR");
    controlLines[8] = 1;                // Set control lines between AC, MBR, and ALU
    controlLines[9] = 1;  
    aLU.setState(true);                 // Engage ALU.          
    controlUnit.setState(false, controlLines, ""); 
    dataPathPanel.repaint(); 
    waitABit(briefDelay);
    
    regMBR.setState(false);               // Turn off MBR.
    controlLines[9] = 0;  
                                          // Add value in MBR to value in AC.
    regAC.setValue(regAC.getValue() + regMBR.getValue());
    controlUnit.setState(false, controlLines, ""); 
    dataPathPanel.repaint(); 
    waitABit(briefDelay);
    
    aLU.setState(false);
    controlLines[8] = 0;
    controlUnit.setState(false, controlLines, ""); 
    dataPathPanel.repaint();
    waitABit(delay);              // End of microop.
        
                              // Seventh microop: PC <-- AC 
    controlLines[0] = 0;            // Set control lines to write to PC ... 
    controlLines[1] = 1;
    controlLines[2] = 0;
    regPC.setState(true);
    controlLines[3] = 1;            // ... and read from AC.
    controlLines[4] = 0;
    controlLines[5] = 0;    
    memory.setState(false, true);     // Turn on bus.
    controlUnit.setState(true, controlLines, "PC <-- AC");  
    dataPathPanel.repaint();
    waitABit(briefDelay);
    regPC.setValue(regAC.getValue());
    regAC.setState(false);
    memory.setState(false, false);     // Turn off bus.
    controlUnit.setState(false, controlLines, "");  
    dataPathPanel.repaint();
    waitABit(delay);            // End of microop.    
        
    regPC.setState(false);        // Turn off everything.
    controlLines[0] = 0; 
    controlLines[1] = 0;
    controlLines[2] = 0;
    controlLines[3] = 0;
    controlLines[4] = 0;
    controlLines[5] = 0;
    controlUnit.setState(false, controlLines, " ");  
    dataPathPanel.repaint(); 
  } // jns()
  
  void load() {
  /****************************************************************************************
  *  This function performs two microoperations: MAR <-- IR[11-0]; and                    *
  *  MBR <-- M[MAR], AC <-- MBR to load a value from memory into the accumulator.         *
  ****************************************************************************************/
    int[] controlLines = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                            
    regIR.setState(true);
                                 // First microoperation: MAR <-- IR[11-0]
    controlUnit.setState(false, controlLines, "MAR <-- IR[11-0]");   
    controlLines[0] = 0;         // Set control lines to read IR and write to MAR
    controlLines[1] = 0; 
    controlLines[2] = 1;  
    controlLines[3] = 1;
    controlLines[4] = 1; 
    controlLines[5] = 1;  
    regMAR.setState(true);   
    controlUnit.setState(true, controlLines, "");   
    memory.setState(false, true);  // Turn on bus.
    dataPathPanel.repaint();
    waitABit(briefDelay);
    
    regMAR.setValue(regIR.getValue() & 0x0FFF);       // Get MAR value from IR.    
    regIR.setState(false);         // Turn off IR, control lines, and bus.
    memory.setState(false, false);
    controlLines[0] = 0;
    controlLines[1] = 0; 
    controlLines[2] = 0;  
    controlLines[3] = 0;
    controlLines[4] = 0; 
    controlLines[5] = 0; 
    controlUnit.setState(false, controlLines, "(Decode IR[15-12])");   
    dataPathPanel.repaint();
    waitABit(delay);                   // End of microop.
    
    controlLines[7] = 1;  
    memory.setState(true, true);  // Second microoperation: MBR <-- M[MAR](first part)
    controlUnit.setState(false, controlLines, "MBR <-- M[MAR]");                        
    controlLines[2] = 1;
    controlLines[1] = 1; 
    regMBR.setState(true);  
    controlUnit.setState(true, controlLines, "");  
    dataPathPanel.repaint();
    waitABit(briefDelay);    
                                                    
    int operand = memoryArray[regMAR.getValue()];  // Retrieve operand from
    regMBR.setValue(operand);                      // memory and place into MBR.
    dataPathPanel.repaint();
    waitABit(briefDelay);
    memory.setState(false, false);      // Turn off bus and control unit.
    
    regMAR.setState(false);
    controlLines[1] = 0;
    controlLines[2] = 0;
    controlLines[7] = 0; 
    controlLines[6] = 1;            //  Second part of second microop: AC <-- MBR. 
    regAC.setState(true);
    controlUnit.setState(false, controlLines, "AC <-- MBR");    
    dataPathPanel.repaint(); 
    waitABit(briefDelay);
                  
    regAC.setValue(regMBR.getValue());   // Transfer to AC.
    dataPathPanel.repaint();
    waitABit(delay);                  // End of microop.
        
    regMBR.setState(false);       // Turn off everything.
    regAC.setState(false);
    controlLines[6] = 0;
    controlUnit.setState(false, controlLines, " ");  
    dataPathPanel.repaint(); 
  } // load()
  
  void store() {
  /****************************************************************************************
  *  This function performs two microoperations: MAR <-- IR[11-0]; and MBR <-- AC,        *
  *  M[MAR]<-- MBR to store a value to memory from the accumulator.                       *
  ****************************************************************************************/
    int[] controlLines = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

                              // First microoperation: MAR <-- IR[11-0]
    regIR.setState(true);                             // Turn on IR
    controlUnit.setState(false, controlLines, "MAR <-- IR[11-0]");  
     
    controlLines[0] = 0;                              // Set control lines to read IR 
    controlLines[1] = 0;                              // and write to MAR.
    controlLines[2] = 1;  
    controlLines[3] = 1;
    controlLines[4] = 1; 
    controlLines[5] = 1; 
    regMAR.setState(true);                            // Turn on MAR.    
    controlUnit.setState(true, controlLines, "");     // Turn on bus and control unit.
    memory.setState(false, true);
    dataPathPanel.repaint();
    waitABit(briefDelay);
    
    regMAR.setValue(regIR.getValue() & 0x0FFF);       // Get MAR value from IR.
    regIR.setState(false);                            // Turn off IR, control lines
    memory.setState(false, false);                    // and bus.
    controlLines[0] = 0;
    controlLines[1] = 0;
    controlLines[2] = 0;   
    controlLines[3] = 0;
    controlLines[4] = 0; 
    controlLines[5] = 0;   
    controlUnit.setState(false, controlLines, "(Decode IR[15-12])");
    regMAR.setState(false);                           // Turn off MAR.
    waitABit(delay);
    dataPathPanel.repaint();                // Microop complete.
    
    regAC.setState(true);          // Second microoperation (first part): MBR <-- AC   
    controlLines[6] = 1;                // Activate control line to load MBR from AC
    regMBR.setState(true);              // Activate MBR
    controlUnit.setState(false, controlLines, " MBR <-- AC, M[MAR] <-- MBR");
    waitABit(delay);
    dataPathPanel.repaint();   
           
    regMBR.setValue(regAC.getValue()); // Activate MBR and move data from AC.
    dataPathPanel.repaint();
    waitABit(briefDelay);
     
    controlLines[6] = 0;               // Turn off connection between AC and MBR.
    regAC.setState(false);
    controlUnit.setState(false, controlLines, "");  // End of first part.
    
    controlLines[3] = 0;        // Second part of second microop: M[MAR] <-- MBR.
    controlLines[4] = 1;               // Set control lines to read MBR.
    controlLines[5] = 1;
    controlLines[7] = 1;               // Activate control line between MAR and MM.
    regMAR.setState(true);
    controlUnit.setState(true, controlLines, "");  // Engage control unit.  
    memory.setState(true, true);       // Turn on memory and bus.
    memoryArray[regMAR.getValue()] = regMBR.getValue();    // Update memory.
    dataPathPanel.repaint();
    waitABit(delay);          // Microop complete.
        
    regMBR.setState(false);    // Turn off everything
    regMAR.setState(false);
    memory.setState(false, false);
    controlLines[4] = 0;
    controlLines[5] = 0;
    controlLines[7] = 0;
    controlUnit.setState(false, controlLines, "  ");
    dataPathPanel.repaint(); 
  } // store()
  
  void add() {
  /****************************************************************************************
  *  This function performs three microoperations:                                        *
  *                  MAR <-- IR[11-0] ; MBR <-- M[MAR]; and AC <-- AC + MBR               *
  *  to add a value from memory to the value in the accumulator.                          *
  ****************************************************************************************/
    int[] controlLines = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    
    regIR.setState(true);     // First microoperation: MAR <-- IR[11-0] 
    controlUnit.setState(false, controlLines, "MAR <-- IR[11-0]");   
    
    controlLines[0] = 0;         // Set control lines to read IR and write to MAR
    controlLines[1] = 0; 
    controlLines[2] = 1;  
    controlLines[3] = 1;
    controlLines[4] = 1; 
    controlLines[5] = 1;    
    regMAR.setState(true); 
    controlUnit.setState(true, controlLines, "");   
    memory.setState(false, true);  // Turn on bus
    dataPathPanel.repaint();
    waitABit(briefDelay);
    
    regMAR.setValue(regIR.getValue() & 0x0FFF);  // Place address in MAR         
    controlUnit.setState(true, controlLines, "(Decode IR[15-12])");
    dataPathPanel.repaint();
    waitABit(briefDelay);
    
    regIR.setState(false);         // Turn off IR and control lines
    memory.setState(false, false);
    controlLines[0] = 0;
    controlLines[1] = 0; 
    controlLines[2] = 0;  
    controlLines[3] = 0;
    controlLines[4] = 0; 
    controlLines[5] = 0; 
    controlLines[7] = 1;  
    controlUnit.setState(false, controlLines, "");   
    dataPathPanel.repaint();
    waitABit(delay);                   // End of microop.
    
    memory.setState(true, false);  // Second microoperation: MBR <-- M[MAR]
    controlUnit.setState(false, controlLines, "MBR <-- M[MAR]"); 
        
    memory.setState(true, true);     // Turn on memory and connection to MBR
    controlLines[1] = 1;
    controlLines[2] = 1; 
    regMBR.setState(true);  
    controlUnit.setState(true, controlLines, "");  
    dataPathPanel.repaint();
    waitABit(briefDelay);
                   
    regMBR.setValue(memoryArray[regMAR.getValue()]);  // Load MBR.
    dataPathPanel.repaint();
    waitABit(briefDelay);
    memory.setState(false, false);      // Turn off bus and control unit.
    regMAR.setState(false);
    controlLines[1] = 0;
    controlLines[2] = 0;
    controlLines[7] = 0;
    controlUnit.setState(false, controlLines, "");
    dataPathPanel.repaint();
    waitABit(delay);                    // End of microop.
   
    controlLines[8] = 1;            //  Third microop: AC <-- AC + MBR. 
    controlLines[9] = 1;  
    aLU.setState(true);                   // Engage ALU.
    regAC.setState(true);                 // Enable AC.          
    controlUnit.setState(false, controlLines, "AC <-- AC + MBR"); 
    
    regMBR.setState(false);               // Turn off MBR.
    controlLines[9] = 0;  
                                          // Add value in MBR to value in AC.
    regAC.setValue(regAC.getValue() + regMBR.getValue());
    controlUnit.setState(false, controlLines, " "); 
    dataPathPanel.repaint(); 
    waitABit(briefDelay);
    
    aLU.setState(false);
    controlLines[8] = 0;
    controlUnit.setState(false, controlLines, " "); 
    dataPathPanel.repaint();
    waitABit(delay);                  // End of microop.
        
    regAC.setState(false);        // Turn off everything.
    controlUnit.setState(false, controlLines, " ");  
    dataPathPanel.repaint(); 
  } // add()
    
  void subt() {
  /****************************************************************************************
  *  This function performs three microoperations:                                        *
  *                   MAR <-- IR[11-0]; MBR <-- M[MAR]; and AC <-- AC - MBR               *
  *  to add a value from memory to the value in the accumulator.                          *
  ****************************************************************************************/
    int[] controlLines = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    
    regIR.setState(true);     // First microoperation: MAR <-- IR[11-0] 
    controlUnit.setState(false, controlLines, "MAR <-- IR[11-0]");   
    
    controlLines[0] = 0;         // Set control lines to read IR and write to MAR
    controlLines[1] = 0; 
    controlLines[2] = 1;  
    controlLines[3] = 1;
    controlLines[4] = 1; 
    controlLines[5] = 1;
    regMAR.setState(true);     
    controlUnit.setState(true, controlLines, "");   
    memory.setState(false, true);  // Turn on bus
    dataPathPanel.repaint();
    waitABit(briefDelay);
    
    regMAR.setValue(memoryArray[regMAR.getValue()]);   // Place address in MAR
    controlUnit.setState(false, controlLines, "(Decode IR[15-12])");
    dataPathPanel.repaint();
    waitABit(briefDelay);
    
    regIR.setState(false);         // Turn off IR and control lines
    memory.setState(false, false);
    controlLines[0] = 0;
    controlLines[1] = 0; 
    controlLines[2] = 0;  
    controlLines[3] = 0;
    controlLines[4] = 0; 
    controlLines[5] = 0;   
    controlUnit.setState(false, controlLines, "");   
    dataPathPanel.repaint();
    waitABit(delay);                   // End of microop.
    
    memory.setState(true, false);  // Second microoperation: MBR <-- M[MAR]
    controlLines[7] = 1;
    controlUnit.setState(false, controlLines, "MBR <-- M[MAR]"); 
    dataPathPanel.repaint();
    waitABit(briefDelay);
        
    memory.setState(true, true);     // Turn on memory and connection to MBR
    controlLines[1] = 1;
    controlLines[2] = 1; 
    controlLines[7] = 0; 
    regMBR.setState(true);              // Load MBR.
    controlUnit.setState(true, controlLines, "");  
    dataPathPanel.repaint();
    waitABit(briefDelay);
        
    regMBR.setValue(memoryArray[regMAR.getValue()]);
    dataPathPanel.repaint();
    waitABit(briefDelay);
    memory.setState(false, false);      // Turn off bus and control unit.
    regMAR.setState(false);
    controlLines[1] = 0;
    controlLines[2] = 0;
    controlUnit.setState(false, controlLines, "");
    dataPathPanel.repaint();
    waitABit(delay);                    // End of microop.
   
    controlLines[8] = 1;            //  Third microop: AC <-- AC - MBR. 
    controlLines[9] = 1;  
    aLU.setState(true);                   // Engage ALU.
    regAC.setState(true);                 // Enable AC.          
    controlUnit.setState(false, controlLines, "AC <-- AC - MBR"); 
    dataPathPanel.repaint(); 
    waitABit(briefDelay);
                                          // ASubtract value in MBR from value in AC.
    regAC.setValue(regAC.getValue() - regMBR.getValue());
    controlUnit.setState(false, controlLines, " "); 
    dataPathPanel.repaint(); 
    waitABit(briefDelay);
    
    regMBR.setState(false);               // Turn off MBR.
    controlLines[9] = 0;  
    aLU.setState(false);
    controlLines[8] = 0;
    controlUnit.setState(false, controlLines, " "); 
    dataPathPanel.repaint();
    waitABit(delay);                  // End of microop.
        
    regAC.setState(false);        // Turn off everything.
    controlUnit.setState(false, controlLines, " ");  
    dataPathPanel.repaint(); 
  } // subt()
 
  void input() {
  /****************************************************************************************
  *  This function performs one microoperation: AC <--InREG a value from the outside      *
  *  world into the accumulator.                                                          *
  ****************************************************************************************/
    int[] controlLines = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    controlUnit.setState(false, controlLines, "AC <-- InREG");   
    dataPathPanel.repaint();
    waitABit(briefDelay);    
       
    if (machineState == MARIE_RUNNING) {       // First time through???
       setStatusMessage(" Waiting for input.");
       machineState = MARIE_BLOCKED_ON_INPUT;      // Block further execution.
       inputContent.setText("");
       inputPanel.setBackground(Color.pink);
       inputBase.setBackground(Color.pink);       
       inputContent.setEditable(true);                 // Enable register input.
       inputContent.requestFocus();
       inputContent.repaint();          
       controlLines[0] = 1;         // Set control lines to read InREG and write to AC
       controlLines[1] = 0; 
       controlLines[2] = 0;  
       controlLines[3] = 1;
       controlLines[4] = 0; 
       controlLines[5] = 1;     
       controlUnit.setState(true, controlLines, "(Decode IR[15-12])");
       dataPathPanel.repaint();
       waitABit(briefDelay);        
       controlUnit.setState(false, controlLines, "Waiting for input. . ."); 
       regINPUT.setState(true);       // Turn on InREG.
       regIR.setState(false);         // Turn off IR.
       return;
    } // if machineState = MARIE_RUNNING
    else if (machineState == MARIE_BLOCKED_ON_INPUT) {  // Second time through???
       regINPUT.setValue(stringToInt(regINPUT.getMode(), inputContent.getText()));
       inputContent.setEditable(false);              // "Close" the register to input
       runStop.requestFocus();                   // until needed again.
       inputPanel.setBackground(buttonPanelColor);
       inputBase.setBackground(buttonPanelColor);
       inputContent.setEditable(false);                 // Disable register input.
       inputContent.repaint();
       if (fatalError) {
         halt();
         return;
       } 
       controlLines[0] = 1;         // Set control lines to read InREG and write to AC
       controlLines[1] = 0; 
       controlLines[2] = 0;  
       controlLines[3] = 1;
       controlLines[4] = 0; 
       controlLines[5] = 1;     
       regINPUT.setState(true);       // Turn on InREG.
       memory.setState(false, true);  // Turn on bus.
       controlUnit.setState(false, controlLines, "AC <-- InREG");
       regAC.setState(true);          // Transfer to AC.   
       dataPathPanel.repaint();
       waitABit(briefDelay);
    
       regAC.setValue(regINPUT.getValue());
       dataPathPanel.repaint();
       waitABit(briefDelay);
    
       memory.setState(false, false); // Turn off bus, control lines, and InREG.
       regINPUT.setState(false);  // End of microop.        
        
       regAC.setState(false);        // Turn off everything that's on.
       controlLines[0] = 0; 
       controlLines[3] = 0;
       controlLines[5] = 0; 
       controlUnit.setState(false, controlLines, " ");  
       dataPathPanel.repaint();
       machineState = MARIE_RUNNING;             // Reset the machine state.
       if (stepping)  {                          // Proceed with next instruction
          setStatusMessage(" Press [Step] to continue.");        // or step.
          runStop.setText("Run");
       }   
       else
          setStatusMessage(" ");
      runProgram();
     } // else     
  } // input()
 
  void output() {
  /****************************************************************************************
  *  This function performs one microoperation: OutREG <-- AC a value from the AC is      *
  *  sent to the outside world.                                                           *
  ****************************************************************************************/
    int[] controlLines = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    
    controlLines[0] = 1;         // Set control lines to read AC and write to OutREG.
    controlLines[1] = 1; 
    controlLines[2] = 0;  
    controlLines[3] = 1;
    controlLines[4] = 0; 
    controlLines[5] = 1;     
    controlUnit.setState(true, controlLines, "OutREG <-- AC");
    dataPathPanel.repaint();
    waitABit(briefDelay);   
     
    controlUnit.setState(false, controlLines, "(Decode IR[15-12])"); 
    regAC.setState(true);          // Turn on AC.
    regOUTPUT.setState(true);      // Turn on OUTPUT.
    regIR.setState(false);         // Turn off IR.
    memory.setState(false, true);  // Turn on bus.
    dataPathPanel.repaint();
    waitABit(briefDelay); 
    
    controlUnit.setState(false, controlLines, "");   
    regOUTPUT.setValue(regAC.getValue());   // Transfer to OutREG.
    dataPathPanel.repaint();
    waitABit(briefDelay);
    
    memory.setState(false, false); // Turn off bus, control lines, and AC.
    regAC.setState(false);
    dataPathPanel.repaint();
    waitABit(delay);                  // End of microop.
        
    regOUTPUT.setState(false);    // Turn off everything that's on.
    controlLines[0] = 0; 
    controlLines[1] = 0;
    controlLines[3] = 0;
    controlLines[5] = 0; 
    controlUnit.setState(false, controlLines, " ");  
    dataPathPanel.repaint();
  } // output()
 
  void halt() { 
  /****************************************************************************************
  *   Changes the machine state from (probably) RUNNING to HALTED using the fatalError    *
  *   boolean to determine which one is which.  All registers and control lines are       *
  *   set to an inactive state.                                                           *
  ****************************************************************************************/
    int[] controlLines = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};  // Initialized control lines.
    
    controlUnit.setState(false, controlLines, "(Decode IR[15-12])");    
    runStop.setText("Run");
    regIR.setState(false);         // Turn off all components.
    regOUTPUT.setState(false);
    regINPUT.setState(false);
    regAC.setState(false);
    regMBR.setState(false);
    regPC.setState(false);
    regMAR.setState(false);
    aLU.setState(false);
    memory.setState(false, false);
    controlUnit.setState(false, controlLines, "");
    dataPathPanel.repaint();
    waitABit(briefDelay);
    
    controlUnit.setState(false, controlLines, "Halt");
    dataPathPanel.repaint();
    waitABit(briefDelay);
    
    if (fatalError) {
      machineState = MARIE_HALTED_ABNORMAL;
      controlUnit.setState(false, controlLines, "Machine halted abnormally.");
      setStatusMessage(" Machine halted abnormally.");
    }
    else {
       machineState = MARIE_HALTED_NORMAL;
       controlUnit.setState(false, controlLines, "Machine halted normally.");
       setStatusMessage(" Machine halted normally.");
    }
    step.setEnabled(true);
    dataPathPanel.repaint();
   } // halt()

  void skipCond() { 
  /****************************************************************************************
  *   This instruction will skip over the instruction at PC+1 based on the value in the   *
  *   instruction's lower 12 bits.  Possibly three microoperations are carried out:       *
  *   First, we look at the IR to see which condition to test (AC < 0, AC = 0, AC > 0)    *
  *   Then carry out the conditional evaluation.  If the condition is true, the PC is     *
  *   incremented.                                                                        *
  ****************************************************************************************/
    int[] controlLines = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
     
    regIR.setState(true);
    
    int cond = regIR.getValue();
    cond = cond & 0x0C00;   // Strip the opcode from the instruction.
    cond = cond >> 10;      // Shift the conditional to the low-order bits.
    if (cond == 3) {        // Check valid values for condition 0, 1, & 2.
     fatalError = true;
     halt();
     return;
    }
    
    controlUnit.setState(false, controlLines, "(Decode IR[15-12])");
    dataPathPanel.repaint();
    waitABit(briefDelay);
    
    controlLines[3] = 1; 
    controlLines[4] = 1;
    controlLines[5] = 1;
    int accumulator = regAC.getValue();
    
    switch (cond) {
      case 0:           // Accumulator negative?
        controlUnit.setState(true, controlLines, "IR[11-10] = 00");   
        dataPathPanel.repaint();    // Display interrogation of IR, tho' we've 
        waitABit(briefDelay);       // already checked it.
        controlLines[3] = 1;  
        controlLines[4] = 0;   
        controlLines[5] = 0;
        regIR.setState(false);  // We're done with IR, now look at AC.
        regAC.setState(true);
        controlLines[8] = 1;    // Engage ALU.
        aLU.setState(true);  
        controlUnit.setState(true, controlLines, "AC < 0?");
        dataPathPanel.repaint();
        waitABit(briefDelay); 
        if (accumulator < 0) {
          controlLines[0] = 0;  // Set contol lines to write to PC.
          controlLines[1] = 1;
          controlLines[2] = 0;
          regPC.setState(true);
          controlLines[3] = 0; // Turn off read from AC.
          controlLines[4] = 0;
          controlLines[5] = 0;
          controlLines[8] = 0;
          regAC.setState(false);
          aLU.setState(false);  
          controlUnit.setState(true, controlLines, "PC <-- PC + 1");
          dataPathPanel.repaint();
          waitABit(briefDelay);
          regPC.setValue(regPC.getValue()+1);   // Increment PC.
        } // if
        else {
          controlUnit.setState(true, controlLines, "AC < 0?  FALSE!");
        } // else  
        break;
      case 1:  // Accumulator zero?
        controlUnit.setState(true, controlLines, "IR[11-10] = 01");   
        dataPathPanel.repaint();    // Display interrogation of IR, tho' we've 
        waitABit(briefDelay);       // already checked it.
        controlLines[3] = 1;  
        controlLines[4] = 0;   
        controlLines[5] = 0;
        regIR.setState(false);  // We're done with IR, now look at AC.
        regAC.setState(true);
        controlLines[8] = 1;    // Engage ALU.
        aLU.setState(true);  
        controlUnit.setState(true, controlLines, "AC = 0?");
        dataPathPanel.repaint();
        waitABit(delay); 
        if (accumulator == 0) {
          controlLines[0] = 0;  // Set contol lines to write to PC.
          controlLines[1] = 1;
          controlLines[2] = 0;
          regPC.setState(true);
          controlLines[3] = 0;  // Turn off read from AC.
          controlLines[4] = 0;
          controlLines[5] = 0;
          controlLines[8] = 0;
          regAC.setState(false);
          aLU.setState(false);  
          controlUnit.setState(true, controlLines, "PC <-- PC + 1");
          dataPathPanel.repaint();
          waitABit(briefDelay);
          regPC.setValue(regPC.getValue()+1);   // Increment PC.
        } // if  
        else {
          controlUnit.setState(true, controlLines, "AC = 0?  FALSE!");
        } // else
        break;
      case 2:  // Accumulator positive?
        controlUnit.setState(true, controlLines, "IR[11-10] = 10");   
        dataPathPanel.repaint();         // Display interrogation of IR, tho' we've 
        waitABit(briefDelay);       // already checked it.
        controlLines[3] = 1;  
        controlLines[4] = 0;   
        controlLines[5] = 0;
        regIR.setState(false);  // We're done with IR, now look at AC.
        regAC.setState(true);
        controlLines[8] = 1;    // Engage ALU.
        aLU.setState(true);  
        controlUnit.setState(true, controlLines, "AC > 0?");
        dataPathPanel.repaint();
        waitABit(briefDelay); 
        if (accumulator > 0) {
          controlLines[0] = 0;  // Set contol lines to write to PC.
          controlLines[1] = 1;
          controlLines[2] = 0;
          regPC.setState(true);
          controlLines[3] = 0; // Turn off read from AC.
          controlLines[4] = 0;
          controlLines[5] = 0;
          controlLines[8] = 0;
          regAC.setState(false);
          aLU.setState(false);  
          controlUnit.setState(true, controlLines, "PC <-- PC + 1");
          dataPathPanel.repaint();
          waitABit(briefDelay);
          regPC.setValue(regPC.getValue()+1);   // Increment PC.
        } // if  
        else {
          controlUnit.setState(true, controlLines, "AC > 0?  FALSE!");
        } // else
     default:  // Should never get here.
        break;
    } // switch
    dataPathPanel.repaint();
    waitABit(delay);
    controlLines[0] = 0;  // Turn everything off that we might have turned on.
    controlLines[1] = 0;
    controlLines[2] = 0;
    controlLines[3] = 0;
    controlLines[4] = 0;
    controlLines[5] = 0;
    controlLines[8] = 0;
    regAC.setState(false);
    aLU.setState(false);
    regPC.setState(false);  
    controlUnit.setState(false, controlLines, " ");
    dataPathPanel.repaint();
  } // skipCond()

  void jump() { 
  /****************************************************************************************
  *   Move the value in the instruction's lower 12 bits to the PC (which always           *
  *   contains the memory address of the next instruction to execute).  This instruction  *
  *   has only one microoperation: Moving the low-order 12 bits of the IR to the PC.      *
  ****************************************************************************************/
     int[] controlLines = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
     
     controlUnit.setState(false, controlLines, "(Decode IR[15-12])");
     dataPathPanel.repaint();
     waitABit(briefDelay);
     
     controlLines[0] = 0;         // Set contol lines to write to the PC...
     controlLines[1] = 1;
     controlLines[2] = 0;
     regPC.setState(true);
     controlLines[3] = 1;         // ... and read from the IR.
     controlLines[4] = 1;
     controlLines[5] = 1;
     controlUnit.setState(true, controlLines, "PC <-- IR[11 - 0]");
     memory.setState(false, true);  // Turn on the bus.
     dataPathPanel.repaint();
     waitABit(briefDelay);

     int addr = regIR.getValue();
     addr = addr & 0x0FFF;        // Strip the opcode from the instruction,
     regPC.setValue(addr);        // leaving the address.
     controlUnit.setState(false, controlLines, "");   // End of microop.        
     
     controlLines[0] = 0;  // Turn everything off that we might have turned on.
     controlLines[1] = 0;
     controlLines[2] = 0;
     controlLines[3] = 0;
     controlLines[4] = 0;
     controlLines[5] = 0;
     regIR.setState(false);
     regPC.setState(false);  
     memory.setState(false, false);  // Turn on the bus.
     controlUnit.setState(false, controlLines, " ");
     dataPathPanel.repaint();    
   } // jump()
   
  void clear() {
  /****************************************************************************************
  *  This function performs one microoperation: AC <-- 0 to clear the accumulator.        *
  ****************************************************************************************/
    int[] controlLines = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    
    controlUnit.setState(false, controlLines, "(Decode IR[15-12])");
    dataPathPanel.repaint();
    waitABit(briefDelay);
    
    controlLines[0] = 1;         // Set control lines to write to AC
    controlLines[1] = 0; 
    controlLines[2] = 0;  
    controlUnit.setState(true, controlLines, "AC <--- 0000");   
    
    regAC.setState(true);    // Activate AC
    dataPathPanel.repaint();
    waitABit(briefDelay);   
     
    regAC.setValue(0);
    controlUnit.setState(false, controlLines, "");  
    dataPathPanel.repaint();
    waitABit(delay);                  // End of microop.
        
    regAC.setState(false);        // Turn off everything that's on.
    regIR.setState(false);  
    controlLines[0] = 0;
    controlUnit.setState(false, controlLines, " ");  
    dataPathPanel.repaint();
    waitABit(briefDelay);
  } // clear()
   
  void addI() {
  /****************************************************************************************
  *  This function performs five microoperations: MAR <-- IR[11-0]; MBR <-- M[MAR];       *
  *  MAR <-- MBR; MBR <-- M[MAR]; and AC <-- AC + MBR.                                    *
  ****************************************************************************************/
    int[] controlLines = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    
    regIR.setState(true);    
    controlUnit.setState(false, controlLines, "MAR <-- IR[11-0]");   
    dataPathPanel.repaint();
    waitABit(briefDelay);
    
    controlLines[0] = 0;         // Set control lines to read IR and write to MAR.
    controlLines[1] = 0; 
    controlLines[2] = 1;  
    controlLines[3] = 1;
    controlLines[4] = 1; 
    controlLines[5] = 1;     
    regMAR.setState(true);             // Place address in MAR. 
    controlUnit.setState(true, controlLines, "");   
    memory.setState(false, true);      // Turn on bus.
    dataPathPanel.repaint();
    waitABit(briefDelay);
           
    int addr = regIR.getValue() & 0x0FFF;  // Strip address from instruction.
    regMAR.setValue(addr);  
    controlUnit.setState(false, controlLines, "");
    dataPathPanel.repaint();
    waitABit(briefDelay);
    
    regIR.setState(false);             // Turn off IR, control lines, and bus.
    memory.setState(false, false);
    controlLines[0] = 0;
    controlLines[1] = 0; 
    controlLines[2] = 0;  
    controlLines[3] = 0;
    controlLines[4] = 0; 
    controlLines[5] = 0; 
    controlUnit.setState(false, controlLines, "(Decode IR[15-12])");   
    dataPathPanel.repaint();
    waitABit(delay);                   // End of microop.
    
                                   // Second microoperation: MBR <-- M[MAR]
    controlLines[7] = 1;                                    
    controlUnit.setState(false, controlLines, "MBR <-- M[MAR]");                
    memory.setState(true, true);     // Turn on memory and connection to MBR.
    controlLines[1] = 1;
    controlLines[2] = 1; 
    controlUnit.setState(true, controlLines, "");          
    regMBR.setState(true);           // Load MBR.
    dataPathPanel.repaint();
    waitABit(briefDelay); 
    addr = memoryArray[regMAR.getValue()];          // Retrieve operand's address from
    regMBR.setValue(addr);                          // memory and place into MBR.
    dataPathPanel.repaint();
    waitABit(delay);                   // End of microop.
    
                                    // Third microop: MAR <-- MBR.
    memory.setState(false, true);      // Turn off memory, but keep bus on. 
    controlLines[0] = 0;               // Set control lines to write to MAR...
    controlLines[1] = 0; 
    controlLines[2] = 1; 
    controlLines[3] = 0;               // ... and read from MBR.
    controlLines[4] = 1;
    controlLines[5] = 1;
    controlLines[7] = 0; 
    controlUnit.setState(true, controlLines, "MAR <-- MBR");
    dataPathPanel.repaint();
    waitABit(briefDelay); 
    
    regMAR.setValue(regMBR.getValue()); // Update the MAR.
    regMBR.setState(false);
    dataPathPanel.repaint(); 
    waitABit(delay);                 // End of microop.        
         
                                // Fourth microoperation: MBR <-- M[MAR]
    controlUnit.setState(false, controlLines, "MBR <-- M[MAR]");                
    memory.setState(true, true);     // Turn on memory and connection to MBR.
    controlLines[0] = 0;
    controlLines[1] = 1;
    controlLines[2] = 1;
    controlLines[3] = 0;
    controlLines[4] = 0;
    controlLines[5] = 0; 
    controlLines[7] = 1; 
    controlUnit.setState(true, controlLines, "");          
    regMBR.setState(true);           // Load MBR.
    dataPathPanel.repaint();
    waitABit(briefDelay); 
    int operand = memoryArray[regMAR.getValue()];          // Retrieve operand from
    regMBR.setValue(operand);                              // memory and place into MBR.
    dataPathPanel.repaint();
    waitABit(delay);                // End of microop.        
     
    controlLines[7] = 0;        //  Fifth microop: AC <-- AC + MBR. 
    controlLines[8] = 1; 
    controlLines[9] = 1; 
    aLU.setState(true);                   // Engage ALU.
    regAC.setState(true);                 // Enable AC.
    controlLines[8] = 1;                 
    controlUnit.setState(false, controlLines, "AC <-- AC + MBR"); 
    regMAR.setState(false);               // Turn off MAR, memory and bus.
    memory.setState(false, false); 
    dataPathPanel.repaint(); 
    waitABit(briefDelay);  
    regMBR.setState(false);               // Turn off MBR.
    controlLines[1] = 0;
    controlLines[2] = 0;
    controlLines[9] = 0;
                                          // Add value in MBR to value in AC.
    regAC.setValue(regAC.getValue() + regMBR.getValue());
    controlUnit.setState(false, controlLines, " "); 
    dataPathPanel.repaint(); 
    waitABit(briefDelay);
    
    aLU.setState(false);
    controlLines[8] = 0;
    controlUnit.setState(false, controlLines, " "); 
    dataPathPanel.repaint();
    waitABit(delay);                  // End of microop.
        
    regAC.setState(false);        // Turn off everything.
    memory.setState(false, false); 
    controlLines[0] = 0;
    controlLines[1] = 0; 
    controlLines[2] = 0; 
    controlLines[3] = 0;
    controlLines[4] = 0; 
    controlLines[5] = 0; 
    controlUnit.setState(false, controlLines, " ");  
    dataPathPanel.repaint(); 
  } // addI()
   

  void jumpI() {
  /****************************************************************************************
  *  This function performs three microoperations:                                        *
  *           MAR <-- IR[11-0]; MBR <-- M[MAR]; and PC <-- MBR;                           *
  *  to carry out an indirect jump.                                                       *
  ****************************************************************************************/
    int[] controlLines = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    
    regIR.setState(true);    
    controlUnit.setState(false, controlLines, "MAR <-- IR[11-0]");   
    controlLines[0] = 0;         // Set control lines to read IR and write to MAR.
    controlLines[1] = 0; 
    controlLines[2] = 1;  
    controlLines[3] = 1;
    controlLines[4] = 1; 
    controlLines[5] = 1;     
    regMAR.setState(true);             // Place address in MAR.
    controlUnit.setState(true, controlLines, "");   
    memory.setState(false, true);      // Turn on bus.
    dataPathPanel.repaint();
    waitABit(briefDelay);
    
    controlLines[7] = 1;          
    int addr = regIR.getValue() & 0x0FFF;  // Strip address from instruction.
    regMAR.setValue(addr);  
    controlUnit.setState(true, controlLines, "(Decode IR[15-12])");
    dataPathPanel.repaint();
    waitABit(briefDelay);
    
    regIR.setState(false);             // Turn off IR, control lines, and bus.
    memory.setState(false, false);
    controlLines[0] = 0;
    controlLines[1] = 0; 
    controlLines[2] = 0;  
    controlLines[3] = 0;
    controlLines[4] = 0; 
    controlLines[5] = 0; 
    controlUnit.setState(false, controlLines, "");   
    dataPathPanel.repaint();
    waitABit(delay);                   // End of microop.
    
                                   // Second microoperation: MBR <-- M[MAR]
    controlUnit.setState(false, controlLines, "MBR <-- M[MAR]");                
    memory.setState(true, true);     // Turn on memory and connection to MBR.
    controlLines[1] = 1;
    controlLines[2] = 1; 
    regMBR.setState(true);           // Load MBR.
    controlUnit.setState(true, controlLines, "");          
    dataPathPanel.repaint();
    waitABit(briefDelay); 
    
    addr = memoryArray[regMAR.getValue()];          // Retrieve target address from
    regMBR.setValue(addr);                          // memory and place into MBR.
    regMAR.setState(false);
    controlLines[7] = 0;
    dataPathPanel.repaint();
    waitABit(delay);                   // End of microop.
    
                                    // Third microop: PC <-- MBR.
    memory.setState(false, true);      // Turn off memory, but keep bus on. 
    controlLines[0] = 0;               // Set control lines to write to PC...
    controlLines[1] = 1; 
    controlLines[2] = 0;
    regPC.setState(true);  
    controlLines[3] = 0;               // ... and read from MBR.
    controlLines[4] = 1;
    controlLines[5] = 1;
    controlUnit.setState(true, controlLines, "PC <-- MBR");
    dataPathPanel.repaint();
    waitABit(briefDelay); 

    regPC.setValue(regMBR.getValue()); // Update the PC.
    regMBR.setState(false);
    memory.setState(false, false);  
    dataPathPanel.repaint(); 
    waitABit(delay);                 // End of microop.        
      
    regPC.setState(false);        // Turn off everything.
    controlLines[0] = 0;
    controlLines[1] = 0; 
    controlLines[2] = 0; 
    controlLines[3] = 0;
    controlLines[4] = 0; 
    controlLines[5] = 0; 
    controlUnit.setState(false, controlLines, " ");  
    dataPathPanel.repaint(); 
  } // jumpI()
   
   void execute () {
/******************************************************************************************
*   This method is the mainline of the "execute" part of the "fetch-execute" cycle.       *
******************************************************************************************/
    int instructionCode;
    String aString = to4CharHexStr(regIR.getValue()).trim();
    try {
       instructionCode = Integer.parseInt(aString.substring(0,1), 16);
       }
    catch (NumberFormatException e) {
      fatalError = true;
      errorCode = 1;
      return;
    }       
     
    switch (instructionCode) {
       case  0: jns();
                break;
       case  1: load();     
                break;
       case  2: store();  
                break;
       case  3: add();     
                break;
       case  4: subt();       
                break;
       case  5: input();       
                break;
       case  6: output();      
                break;
       case  7: halt();       
                break;
       case  8: skipCond();     
                break;
       case  9: jump();      
                break;
       case 10: clear();     
                break;
       case 11: addI();      
                break;
       case 12: jumpI();       
                break;
      default:
        fatalError = true;
        errorCode = 1;
    } // switch
  } // execute()

  void runProgram() {
/******************************************************************************************
*   This method creates a thread that repeatedly invokes the fetch-execute cycle of the   *
*   simulator until a the program stops or a fatal error is encountered.  Before this     *
*   method starts, the [Stop] button on the simulator is enabled so that the thread can   *
*   be interrupted.                                                                       *
******************************************************************************************/
     Runnable runIt = new Runnable() {
       public void run() {
         while ((machineState == MARIE_RUNNING) && (!fatalError)) {
           fetch();
           if (!fatalError) {
             execute();               
             if ((stepping) && (machineState == MARIE_RUNNING))  {
               machineState = MARIE_PAUSED;
               setStatusMessage(" Press [Step] to continue.");
               runStop.setText("Run");
               stepping = false;
             }                
           } // !fatalError
          try {                               // Give the user a chance to abort and also
             Thread.sleep(briefDelay);        // a chance to see what's happening.
           }
           catch (InterruptedException e) {
           }        
         } // while
         if (fatalError) {
           halt();
         }
       } // run()
     }; // runIt
   waitABit(100);  // Give the screen a chance to refresh before we start running.
   setStatusMessage(" ");  
   Thread runThread = new Thread(runIt);
   runThread.start();
  } // runProgram()
  
  void waitABit(int howLong) {
   if (machineState == MARIE_RUNNING)
      try { 
        Thread.sleep(howLong);
      }
        catch (InterruptedException e) {
      }
  } // waitABit()
    
  public static void main(String[] args) {
    boolean packFrame = false;
    MarieDPath dpFrame = new MarieDPath();
    if (packFrame) {
      dpFrame.pack();
    }
    else {
      dpFrame.validate();
    }
    dpFrame.setVisible(true);
    dpFrame.repaint();
  }
} // MarieDPath
