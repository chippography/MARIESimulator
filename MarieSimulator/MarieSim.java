// File:        MarieSim.java
// Author:      Julie Lobur
// JDK Version: 1.3.1
// Date:        November 7, 2001
// Notice:      (c) 2003 Julia M. Lobur 
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

public class MarieSim extends JFrame {
/******************************************************************************************
*  This program simulates the operations that take place within a computer that uses a    *
*  "von Neumann" architecture.  This simulator is an implementation of the machine        *
*  described in *The Essentials of Computer Organization and Architecture* by Null and    *
*  Lobur.                                                                                 *
*                                                                                         *
*  Every reasonable effort has been made to give the user total control over the          *
*  operation and behavior of the simulator.  Programs can be written using the callable   *
*  editor, which also provides access to the MARIE assembler.  Once assembled, the user   *
*  can step through instructions one at a time, run a series of instructions to           *
*  breakpoints that he or she has set, or the program may be run to its programmed        *
*  termination.                                                                           *
*                                                                                         *
*  Overall control of the simulator is determined by the state of the machine, kept in    *
*  the instance variable machineState, the values of which are given below.  The state    *
*  of the machine determines what operations are allowable and which step can be taken    *
*  next.                                                                                  *
******************************************************************************************/
/* --                                                                                 -- */
/* --  Class fields and attributes.                                                   -- */
/* --                                                                                 -- */
  public static String         mexFile = null;     // Name of machine code file.
  public static String         mexPath = null;
  public static final String  MEX_TYPE = ".mex";  // File extension of executable code.
  public static final String  MAP_TYPE = ".map";  // File extension of symbol table.
  public static final String  SRC_TYPE = ".mas";  // File extension for source code.
  public static final String  DMP_TYPE = ".dmp";  // File extension for core dump.

  public static final String      linefeed = System.getProperty("line.separator");
  public static final String      formfeed = "\014";
  public static final String fileSeparator = System.getProperty("file.separator");

  public static final String HELP_FILE = "msimhlp1.txt";  // Help file name.
                                 
  public static final JFileChooser exeFileChooser = 
                                   new JFileChooser(System.getProperty("user.dir")); 

  public static final String[] errorMsgs = {
                                         "Program terminated normally.",       //  0
                                         "Illegal opcode",                     //  1
                                         "Illegal conditional operand",        //  2
                                         "Address out of range",               //  3
                                         "Invalid machine code format",        //  4
                                         "IO Exception on input file",         //  5
                                         "Invalid register",                   //  6
                                         "Illegal numeric value in register",  //  7
                                         "Maximum program statements reached"  //  8
                                         };
/* --                                                                                 -- */
/* --  boolean array operandReqd indicates whether an instruction with hexcode        -- */
/* --  corresponding to the position in the array requires an operand.                -- */
/* --  The fetch() operation needs to know this.                                      -- */
/* --                                                                                 -- */
  public static final boolean[] operandReqd = { true,   // JUMPNSTORE
                                                true,   // LOAD
                                                true,   // STORE
                                                true,   // ADD
                                                true,   // SUBT
                                                false,  // INPUT
                                                false,  // OUTPUT
                                                false,  // HALT
                                                false,  // SKIPCOND
                                                false,  // JUMP
                                                false,  // CLEAR
                                                true,   // ADDI
                                                true }; // JUMPI
/* --                                                                                 -- */
/* --  System constants.                                                              -- */
/* --                                                                                 -- */
  public static final int MAX_MARIE_INT   =  32767;
  public static final int MIN_MARIE_INT   = -32768;
  public static final int MAX_MARIE_ADDR  =   4095;
  public static final int HEX             =      0;      // Register display modes,
  public static final int DEC             =      1;      // i.e., "rendering modes."
  public static final int ASCII           =      2;

  public static final int MARIE_HALTED_NORMAL     =  0;  // Possible machine states.
  public static final int MARIE_RUNNING           =  1;
  public static final int MARIE_BLOCKED_ON_INPUT  =  2;
  public static final int MARIE_PAUSED            =  3;
  public static final int MARIE_HALTED_ABNORMAL   = -1;
  public static final int MARIE_HALTED_BY_USER    = -2;
  public static final int MARIE_NO_PROGRAM_LOADED = -3;
  public static final int MARIE_UNINITIALIZED     = 0xDEAD;

  public static final int AC     = 0;                    // Register numbers used for
  public static final int IR     = 1;                    // designations in inner class
  public static final int MAR    = 2;                    // Register for error-checking.
  public static final int MBR    = 3;
  public static final int PC     = 4;
  public static final int INPUT  = 5;
  public static final int OUTPUT = 6;

  static final int MEMORY_TABLE_ROW_HEIGHT = 15;  // Make sure the address and code table
                                                  // rows are the same height.
  static final int PROGRAM_TABLE_ROW_HEIGHT = 19; // Give us a bit larger row for 
                                                  // the program instructions.

  public static final int MINIMUM_DELAY = 10;
  public static final String[] base = {"Hex", "Dec", "ASCII"};
  public static final String[] outputControl = {"Control", "Use Linefeeds", "No Linefeeds", 
                                                "Clear output", "Print"};
  public static final Color simulatorBackground = new Color(175, 175, 200);
  public static final Color  registerForeground = new Color(105, 185, 225);
  public static final Color  registerBackground = new Color(105, 165, 200);
  public static final Color   registerTextColor = new Color(85, 55, 155);
  public static final Color   messageBackground = new Color(210, 210, 255);
  public static final Color    tableHeaderColor = new Color(65, 80, 150);

/* --                                                                                 -- */
/* --  Instance variables.                                                            -- */
/* --                                                                                 -- */
  int  instructionCode = 0;            // Machine code of instruction being run.
  int    codeLineCount = 0;            // Number of lines in the program
  boolean     stepping = false;        // Whether executing one instruction at a time.
  boolean breakpointOn = false;        // Whether executing to a breakpoint.
  int            delay = 10;           // Delay between instruction executions;
  boolean outputWithLinefeed = true;   // Determines whether characters output will have 
                                       // linefeeds supplied.  User can change this.
  static  String  statusMessage = null;
  static  Vector   outputStream = new Vector();  // Holds output so we can reformat.
  int              machineState = 0xDEAD;        // Machine state.

  boolean errorFound = false;   // Non-fatal error flag, e.g. invalid  user input.
  boolean fatalError = false;   // Fatal error flag, e.g., invalid branch address.
  int      errorCode = 0;
  JPanel simulatorPane; 

  JMenuBar       controlBar = new JMenuBar();  // Container for the menu as follows:
  JMenu            fileMenu = new JMenu();        // "File" menu
  JMenuItem    loadFileItem = new JMenuItem();    //       | load program
  JMenuItem    editFileItem = new JMenuItem();    //       | edit program
  MarieEditor   marieEditor;                      //       |  (program editor frame)
  JMenuItem  reloadFileItem = new JMenuItem();    //       | reload program
  JMenuItem    exitFileItem = new JMenuItem();    //       | quit

  JMenu             runMenu = new JMenu();        // "Run" menu
  JMenuItem      runRunItem = new JMenuItem();    //       | run loaded program
  JMenu         stepRunMenu = new JMenu();        //       | set stepping mode:
  JMenuItem       stepRunOn = new JMenuItem();    //          | on
  JMenuItem      stepRunOff = new JMenuItem();    //          | off
  JMenuItem        setDelay = new JMenuItem();    //       | set instruction delay
  DelayFrame     delayFrame;                      //       |  (frame to enter delay)
  JMenuItem     restartItem = new JMenuItem();    //       | restart from beginning
  JMenuItem       resetItem = new JMenuItem();    //       | reset the simulator
  JMenuItem         getDump = new JMenuItem();    //       | request core dump
  CoreDumpFrame   dumpFrame;                      //       |  (core dump param frame)
  TextFileViewer dumpViewer;                      //       |  (core dump frame)

  JButton           runStop = new JButton();      // "Stop" button
  JButton              step = new JButton();      // "Step" button

  JMenu       breakpointMenu = new JMenu();       // "Breakpont" menu 
  JMenuItem        runToItem = new JMenuItem();   //       | run to next breakpoint
  JMenuItem clearBPointsItem = new JMenuItem();   //       | remove all breakpoints

  JButton        showSymbols = new JButton();     // Symbol table display button
  TextFileViewer symbolTable;                     //       |  (symbol table frame)

  JMenu      helpMenu = new JMenu();              // Help menu
  JMenuItem   getHelp = new JMenuItem();          //       | general instructions
  TextFileViewer helpViewer;                      //       |   shown in this viewer
  JMenuItem helpAbout = new JMenuItem();          //       | "About" box
  HelpAboutFrame helpAboutFrame;                  //       |   shown in this frame

  JPanel      mainPanel = new JPanel();   // Container for program table, registers,
                                          // and output.
  Object[][] programArray;                // Holds instructions to be executed.
  JScrollPane  programPane = new JScrollPane();   // Scrollpane for program monitor
  JTable      programTable;                       // Program monitor table.
  ProgramTableModel    ptm = new ProgramTableModel();  // Program monitor table control.
  int      programFocusRow = 0;           // Current instruction pointer in monitor.
  Hashtable codeReference                 // codeReference provides correspondence
            = new Hashtable(16, (float) 0.75);   // between monitor table and 
                                          // instruction addresses.
                                          // Initial capacity 16, load factor 0.75.

  JPanel  registersPanel = new JPanel();      // Container for individual register panels.

  JPanel         acPanel = new JPanel();            // Small panels to hold the register,
  Register         regAC = new Register(AC);        // its label, and the combo box used to
  JLabel         acLabel = new JLabel();            // set the display mode rendering (base).
  JComboBox    acModeBox = new JComboBox(base);     // We make 6 of these small panels,
                                                    // one for each register except the
  JPanel         irPanel = new JPanel();            // output register.
  Register         regIR = new Register(IR);
  JLabel         irLabel = new JLabel();
  JComboBox    irModeBox = new JComboBox(base);

  JPanel        marPanel = new JPanel();
  Register        regMAR = new Register(MAR);
  JLabel        marLabel = new JLabel();
  JComboBox   marModeBox = new JComboBox(base);

  JPanel        mbrPanel = new JPanel();
  Register        regMBR = new Register(MBR);
  JLabel        mbrLabel = new JLabel();
  JComboBox   mbrModeBox = new JComboBox(base);

  JPanel         pcPanel = new JPanel();
  Register         regPC = new Register(PC);
  JLabel         pcLabel = new JLabel();
  JComboBox    pcModeBox = new JComboBox(base);

  JPanel      inputPanel = new JPanel();
  Register      regINPUT = new Register(INPUT);
  JLabel      inputLabel = new JLabel();
  JComboBox inputModeBox = new JComboBox(base);

  JPanel outputOuterPanel = new JPanel();                // Containers to hold output... we use
  JPanel outputInnerPanel = new JPanel();                // two for aesthetics.
  TitledBorder outputInnerBorder = new TitledBorder(BorderFactory.createRaisedBevelBorder(),
                                        "OUTPUT", TitledBorder.CENTER, TitledBorder.BELOW_TOP);
  Register           regOUTPUT = new Register(OUTPUT);
  JScrollPane outputScrollPane = new JScrollPane();            // Scrollpane for output display
  JTextArea         outputArea = new JTextArea();              // Text content of output
  JComboBox      outputModeBox = new JComboBox(base);          // Output display mode control
  JComboBox   outputControlBox = new JComboBox(outputControl); // Linefeed modes & print control.

  JScrollPane memoryPane = new JScrollPane();   // Scrollpane for memory display
  Object[][] memoryArray = new Object[256][17]; // Memory contents.
  JTable     memoryTable;                       // Table for memory contents display.
  int    memoryFocusCell = 0;                   // Current memory location in table.
     
  JTextField    msgField = new JTextField(); //   Status message field contents.
  JPanel     bottomPanel = new JPanel();     // Container for memory array table and
                                             //    message window. 
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
      JSlider milliseconds = new JSlider(JSlider.HORIZONTAL, 0, 3000, delay);
      milliseconds.setMajorTickSpacing(1000);               // Set the scale on the slider.
      milliseconds.setMinorTickSpacing(100);
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
            setStatusMessage(" Execution delay set at "      // closing event.
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


  class CoreDumpFrame extends JFrame {
/******************************************************************************************
*   This class presents a screen to the user to allow selection of a range of addresses   *
*   for which to provide a core dump.  (In some real machines, especially older ones with *
*   small memories, the *entire* contents of memory is dumped.)                           *
*   The selection is effected using a pair of JSliders, one for the starting address and  *
*   one for the ending address.  The scale on the sliders is given in decimal, but a      *
*   text field translates these addresses to hex.  The sliders are synchronized so that   *
*   the ending address is always equal to or greater than the starting address.  The      *
*   initial values on the sliders span the address range of the currently-loaded program. *
*                                                                                         *
*   Note: The sliders are synchronized so that the beginning address is always less       *
*         than the ending address.                                                        *
*                                                                                         *
*   If the user selects [Okay], a call is made to the produceCodeDump() method.           *
*   a [Cancel] choice bypasses the dump.  Both terminating events dispatch a window-      *
*   closing event, which is monitored by the invoking method and triggers the             *
*   nullification of the pointer to this frame.                                           *
******************************************************************************************/
    JPanel       buttons = new JPanel();
    JLabel   sliderLabel = new JLabel("Select memory range to dump.");
    JSlider     starting;
    JSlider       ending;
    JTextField startAddr = new JTextField();
    JTextField   endAddr = new JTextField();
    JLabel    blankLabel = new JLabel();      // Spacer
    JButton   okayButton = new JButton("Okay");
    JButton cancelButton = new JButton("Cancel");
    int      sliderStart;
    int        sliderEnd;

    CoreDumpFrame() {                        // Constructor
      super("Core Dump");                    // Set frame characteristics
      JPanel dumpPane = (JPanel) this.getContentPane();
      addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
               dispose();            
               return;
        } // windowClosing()
      }); // Listener
      setIconImage(Toolkit.getDefaultToolkit()
                             .createImage(MarieSim.class.getResource("M.gif")));
      setSize(new Dimension(500, 300));
      dumpPane.setPreferredSize(new Dimension(450, 150));
      dumpPane.setLayout(new FlowLayout()); 
                                            // Set default starting and ending 
                                            // addresses for dump
      sliderStart = stringToInt(HEX, ((String) programArray[0][1]).trim());
      sliderEnd   = sliderStart + codeLineCount - 1;

      sliderLabel.setPreferredSize(new Dimension(250, 30)); // Slider instructions.
      sliderLabel.setForeground(Color.black);
      starting = new JSlider(JSlider.HORIZONTAL, 0,         // Create starting
                             MAX_MARIE_ADDR, sliderStart);  // slider.
      starting.setMajorTickSpacing(1024);                   // Set the scale.
      starting.setMinorTickSpacing(128);
      starting.setPaintTicks(true);
      starting.setPaintLabels(true);
      starting.setValue(sliderStart);
      starting.setBorder(BorderFactory.createTitledBorder(
                         BorderFactory.createLineBorder(new Color(145, 145, 210), 2),
                         " Starting Memory Address "));
      starting.setPreferredSize(new Dimension(400, 80));  
      starting.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {           // Anonymous inner class
          JSlider source = (JSlider)e.getSource();          // for getting slider value.
          if (!source.getValueIsAdjusting()) {
             sliderStart = (int)source.getValue();
             startAddr.setText(" "+Integer.toHexString(sliderStart).toUpperCase());
             startAddr.postActionEvent();
             if (sliderStart > sliderEnd) {
               sliderEnd = sliderStart;
               endAddr.setText(" "+Integer.toHexString(sliderEnd).toUpperCase());
               endAddr.postActionEvent();
               ending.setValue(sliderStart);
             }
          }
        } // stateChanged()
      }); // Listener
      startAddr.setPreferredSize(new Dimension(57, 30)); 
      startAddr.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(145, 145, 210), 2),
                            BorderFactory.createLoweredBevelBorder()));
      startAddr.setFont(new Font("Monospaced", 0, 14));
      startAddr.setText(" "+to3CharHexStr(sliderStart));
      startAddr.setEditable(false);
      startAddr.setForeground(Color.black);
      ending = new JSlider(JSlider.HORIZONTAL, 0, MAX_MARIE_ADDR, sliderStart);
      ending.setValue(sliderEnd);
      ending.setMajorTickSpacing(1024);               // Set the scale on the slider.
      ending.setMinorTickSpacing(128);
      ending.setPaintTicks(true);
      ending.setPaintLabels(true);
      ending.setBorder(BorderFactory.createTitledBorder(
                         BorderFactory.createLineBorder(new Color(145, 145, 210), 2),
                         " Ending Memory Address "));
      ending.setPreferredSize(new Dimension(400, 80));  
      ending.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {           // Anonymous inner class
          JSlider source = (JSlider)e.getSource();          // for getting slider value.
          if (!source.getValueIsAdjusting()) {
             sliderEnd = (int)source.getValue();
             endAddr.setText(" "+Integer.toHexString(sliderEnd).toUpperCase());
             endAddr.postActionEvent();
             if (sliderEnd < sliderStart) {
               sliderStart = sliderEnd;
               startAddr.setText(" "+Integer.toHexString(sliderStart).toUpperCase());
               startAddr.postActionEvent();
               starting.setValue(sliderEnd);
             }
          }
        } // stateChanged()
      }); // Listener
      endAddr.setPreferredSize(new Dimension(57, 30)); 
      endAddr.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(145, 145, 210), 2),
                            BorderFactory.createLoweredBevelBorder()));
      endAddr.setFont(new Font("Monospaced", 0, 14));
      endAddr.setText(" "+to3CharHexStr(sliderEnd));
      endAddr.setEditable(false);
      blankLabel.setPreferredSize(new Dimension(30, 35));
      okayButton.setMaximumSize(new Dimension(80, 35));     // Populate the button pane.
      okayButton.setMinimumSize(new Dimension(80, 35));   
      okayButton.setPreferredSize(new Dimension(80, 35));
      okayButton.addActionListener(new ActionListener() {   // If user is happy, update
        public void actionPerformed(ActionEvent e) {                  // values and exit
            produceCoreDump(sliderStart, sliderEnd);                  // by dispatching 
            WindowEvent we = new WindowEvent(CoreDumpFrame.this,      // a window-
                                       WindowEvent.WINDOW_CLOSING);   // closing event.
            CoreDumpFrame.this.dispatchEvent(we);    
        }
      }); // Listener
      cancelButton.setMaximumSize(new Dimension(80, 35));   
      cancelButton.setMinimumSize(new Dimension(80, 35));   
      cancelButton.setPreferredSize(new Dimension(80, 35)); 
      cancelButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {                // Just quit if we're
            WindowEvent we = new WindowEvent(CoreDumpFrame.this,    // cancelled by 
                                       WindowEvent.WINDOW_CLOSING); // dispatching a 
            CoreDumpFrame.this.dispatchEvent(we);                   // closing event.
        }                                                   
      }); // Listener
      buttons.setPreferredSize(new Dimension(300, 75)); 
      buttons.add(okayButton);                              // Put the buttons in the
      buttons.add(cancelButton);                            // button panel.
      dumpPane.add(sliderLabel);                            // Add label, slider
      dumpPane.add(starting);                               // and buttons to the main     
      dumpPane.add(startAddr);                              // frame.
      dumpPane.add(ending);     
      dumpPane.add(endAddr);
      dumpPane.add(buttons);
      setLocation(200, 75);
      show();
    } // CoreDumpFrame()
  } // CoreDumpFrame

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
  JLabel  pgmTitle = new JLabel("MARIE Machine Simulator - Version 1.3.01");
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
      String headers[] =  { " ", " ", "label", "opcode", "operand", "hex"};
      public int getColumnCount() { return headers.length; }
      public int getRowCount() { return codeLineCount; }
      public String getColumnName(int col) {
        return headers[col]; }
      public Object getValueAt(int row, int col) {
        return programArray[row][col];
      }
  
      public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex == 0 )                // Determines whether the data
          return true;                        // value in a cell can be changed.
        else
          return false;
      }
      public void setValueAt(Object value, int row, int col) {
        programArray[row][col] = value;       // Only one column is editable,
        fireTableDataChanged();               // set by isCellEditable method.
      }
      public Class getColumnClass(int c) {    // This method is used to provide 
        return getValueAt(0, c).getClass();   // the default cell editor.  I.e.,
      }                                       // Booleans display as checkboxes.
    } // ProgramTableModel

  class Register extends JTextField {
/******************************************************************************************
*   MARIE registers have two principal characteristics:  their value and their rendering  *
*   mode, which is the radix system that is used to express their value.  For any         *
*   register instance, the user can change modes using the combobox on the register       *
*   display.                                                                              *
*                                                                                         *
*   Registers are designated by MARIE class constants, e.g., AC = 0, to allow us control  *
*   over which values are valid for the register.  Specifically, a memory address can     *
*   have a value of only FFF hex while the accumulator can use the entire machine word    *
*   size (up to FFFF).  We use bitwise operations to prevent unreasonable values from     *
*   causing problems.  This is mimics register overflow situations on real systems.       *
*                                                                                         *
*   Each register instance has a text rendering which is a function of both its value     *
*   and its mode.  So decimal value 65 would be "41" in hex mode and "A" in ASCII mode.   *
*   The rendering is also padded with blanks and zeroes to provide an orderly and         *
*   accurate display in the TextField.                                                    *
******************************************************************************************/
    int    designation; // Register number.
    short  value;       // value stored.
    int    mode;        // HEX, DEC, or ASCII.
    String rendering;   // String of value in mode: e.g., value = 12 = "C".

    public Register(int whichOne) {      // Constructor.
      if ((whichOne >= AC) && (whichOne <= OUTPUT))     // Make sure we have a valid 
        designation = whichOne;                         // designation.
      else {
        fatalError = true;
        errorCode = 6;
      }
      setValue(0);                                      // Initialize its value and
      mode = HEX;                                       // default the mode to hex.
    } // Register()

    public void setMode(int m) {
/******************************************************************************************
*   Set Register display mode, which defaults to hex.  The rendering (i.e., character     *
*   display mode) of the register is changed to reflect the mode of the register.  So     *
*   if the register contains decimal 15, its rendering in decimal is "15."  When the      *
*   mode is changed to hex, the rendering changes to "000F."                              *
******************************************************************************************/
      if ((m == DEC) || (m == ASCII)) 
        mode = m;
      else
        mode = HEX;
      setValue(value);
    } // setMode()

    public void setValue(int v) {
/******************************************************************************************
*   Sets the numeric value stored in the Register to the integer argument value.  The     *
*   argument is always a base 10 integer.  Hence, if the register is in hex mode and a    *
*   decimal 15 is passed in the argument, decimal 15 is stored in the value field and     *
*   "000F" is stored in the rendering field.                                              *
******************************************************************************************/
      value = (short) v;
      if ((this.designation == MAR)            // Wrap memory addresses that are
           ||(this.designation == PC))   {     // out of range.
         if ((value < 0) || (value > MAX_MARIE_ADDR))
           value = (short) (v & 0x00000FFF);
      }
      switch (mode) {
        case   HEX: if ((designation == MAR) 
                             ||(designation == PC))  
                      rendering = "  "+to3CharHexStr(value);
                    else
                      rendering = " "+to4CharHexStr(value);
                    break;
        case ASCII: if (v == 0)
                      rendering = null;
                    else
                      rendering = "    " + (char) (v % 128);
                    break;
           default: 
                    if ((designation != OUTPUT) && (value > 0))
                      rendering = " "+Integer.toString(value); 
                    else
                      rendering = Integer.toString(value); 
      } // switch
      setText(rendering);   // JTextFrame method
    } // setValue ()
  
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

    public String toString() { return rendering; }  // Accessor for value in string form.

    public int getValue() {                         // Accessor for value in integer form.
       if ((this.designation == MAR)                // If we have an address-type register,
             ||(this.designation == PC)) {          // always return a positive value.
         if (this.value < 0) {
           int v = value;
           value = (short) (v & 0x00000FFF);
         }
       }
    return value; 
    }
  } // Register


  public MarieSim() {
/******************************************************************************************
*  This is the constructor for the GUI simulator.  Components are defined and populated   *
*  in the order in which they appear on the screen:  left-to-right, top-to-bottom.        *
******************************************************************************************/
    setSize(new Dimension(780, 550));
    setIconImage(Toolkit.getDefaultToolkit()
                          .createImage(MarieSim.class.getResource("M.gif")));
    setTitle("MARIE Simulator");
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    simulatorPane = (JPanel) this.getContentPane();
    simulatorPane.setLayout(new FlowLayout());
    simulatorPane.setBackground(simulatorBackground);
    simulatorPane.setBorder(BorderFactory.createLineBorder(Color.black));
/* --                                                                                 -- */
/* --             Build the machine control panel (menu bar).                         -- */
/* --                                                                                 -- */
    fileMenu.setText("File");                 // "File" menu
    fileMenu.setMnemonic('F');
    fileMenu.setToolTipText("Start here!");
    exeFileChooser                           // Make sure we open only executable files.
             .addChoosableFileFilter(new MarieExecutableFileFilter());
    exeFileChooser
             .removeChoosableFileFilter(exeFileChooser.getAcceptAllFileFilter());
    loadFileItem.setText("Load");             // Load file
    loadFileItem.setMnemonic('L');
    loadFileItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            fileMenu.setToolTipText("Load, edit file or quit.");
            getProgram();
         }
    }); // Listener
    editFileItem.setText("Edit");            // Edit file (call the editor).
    editFileItem.setMnemonic('E');
    editFileItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           displayEditorFrame();
         }
    }); // Listener
    reloadFileItem.setText("Reload");              // Reload file
    reloadFileItem.setMnemonic('D');
    reloadFileItem.setEnabled(false);
    reloadFileItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            loadProgram();
         }
    }); // Listener
    exitFileItem.setText("Exit");                  // Quit the program
    exitFileItem.setMnemonic('X');
    exitFileItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           exitProgram();
         }
    }); // Listener
    fileMenu.add(loadFileItem);
    fileMenu.add(editFileItem);
    fileMenu.add(reloadFileItem);
    fileMenu.addSeparator();
    fileMenu.add(exitFileItem);

    runMenu.setText("Run");                        // "Run" menu
    runMenu.setMnemonic('R');
    runMenu.setToolTipText("Run, Stepping, Delay, Restart, Reset, and Core Dump.");
    runRunItem.setText("Run");                     // Run currently loaded file
    runRunItem.setMnemonic('R');
    runRunItem.setEnabled(false);
    runRunItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            restartItem.setEnabled(true);
            if (machineState != MARIE_BLOCKED_ON_INPUT) {
              stepping = false;
              step.setEnabled(false);
              stepRunOff.setEnabled(false);
              stepRunOn.setEnabled(true);
              repaint();
              runStop.setEnabled(true);
              if ((machineState == MARIE_HALTED_NORMAL) ||
                  (machineState == MARIE_PAUSED) ||
                  (machineState == MARIE_HALTED_ABNORMAL))  {
                restart();
              }
              runProgram();
            }
         }
    }); // Listener
    stepRunMenu.setText("Set Stepping mode");     // Set whether to step through program
    stepRunMenu.setMnemonic('S');                 // execution.
    stepRunOn.setText("On");                      // Set stepping on.
    stepRunOn.setMnemonic('N');
    stepRunOn.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           setStatusMessage(" Press [Step] to start.");
           stepping = true;
           stepRunOff.setEnabled(true);
           stepRunOn.setEnabled(false);
           step.setEnabled(true);
         }
    }); // Listener
    stepRunOff.setText("Off");                        // Button to stepping off/on.
    stepRunOff.setEnabled(false);
    stepRunOff.setMnemonic('F');
    stepRunOff.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           setStatusMessage(" ");
           stepping = false;
           stepRunOff.setEnabled(false);
           stepRunOn.setEnabled(true);
           step.setEnabled(false);
         }
    }); // Listener

    setDelay.setText("Set Delay");                    // Set delay between program
    setDelay.setEnabled(true);                        // instruction execution steps.
    setDelay.setMnemonic('D');
    setDelay.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           displayDelayFrame();
         }
    }); // Listener

    restartItem.setText("Restart");                  // Starts over without reloading.
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

    getDump.setText("Core Dump");                   // Requests a core dump.
    getDump.setEnabled(false);
    getDump.setMnemonic('D');
    getDump.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            displayCoreDump();
         }
    }); // Listener

    runMenu.add(runRunItem);                       // Build the "run" menu from
    stepRunMenu.add(stepRunOn);                    // components defined above.
    stepRunMenu.add(stepRunOff);
    runMenu.add(stepRunMenu);
    runMenu.add(setDelay);
    runMenu.addSeparator();
    runMenu.add(restartItem);
    runMenu.add(resetItem);
    runMenu.add(getDump);

    runStop.setText("Stop");                      // Add the "stop" button.
    runStop.setMnemonic('S');
    runStop.setEnabled(false);
    runStop.setMaximumSize(new Dimension(70, 27));   
    runStop.setMinimumSize(new Dimension(70, 27));   
    runStop.setPreferredSize(new Dimension(70, 27));   
    runStop.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
           runStop.setEnabled(false);
           machineState = MARIE_HALTED_BY_USER;
           setStatusMessage(" Halted at user request.");
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
            fetchNext();
            execute();
          }
      }
    }); // Listener

    breakpointMenu.setText("Breakpoints");       // Breakpoint control...
    breakpointMenu.setToolTipText("Run and reset.");
    breakpointMenu.setMnemonic('B');
    breakpointMenu.setEnabled(false);

    runToItem.setText("Run to Breakpoint");
    runToItem.setMnemonic('R');
    runToItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           restartItem.setEnabled(true);
           if ((machineState != MARIE_BLOCKED_ON_INPUT) && (!fatalError)) {
             runStop.setEnabled(true);
             controlBar.repaint();
             runToBreakpoint();
             runStop.setEnabled(false);
           }
         }
    }); // Listener
    
    clearBPointsItem.setText("Clear Breakpoints");
    clearBPointsItem.setMnemonic('C');
    clearBPointsItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           clearBreakPoints();
         }
    }); // Listener

    breakpointMenu.add(runToItem);               // Populate breakpoint menu.
    breakpointMenu.add(clearBPointsItem);

    showSymbols.setText("Symbol Map");           // Symbol table button
    showSymbols.setMnemonic('M');
    showSymbols.setMaximumSize(new Dimension(120, 27));   
    showSymbols.setMinimumSize(new Dimension(120, 27));   
    showSymbols.setPreferredSize(new Dimension(120, 27));   
    showSymbols.setFocusPainted(false);
    showSymbols.setEnabled(false);
    showSymbols.setToolTipText("Display symbol table.");
    showSymbols.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           displaySymbolTable();
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

    controlBar.add(fileMenu);                    // Add all submenus and buttons to the 
    controlBar.add(runMenu);                     // frame's main menu.
    controlBar.add(runStop);
    controlBar.add(step);
    controlBar.add(breakpointMenu);
    controlBar.add(showSymbols);
    controlBar.add(helpMenu);
    setJMenuBar(controlBar);

/* --                                                                                 -- */
/* --  Create a container for holding three other containers:  The first container    -- */
/* --  contains a table that we use for monitoring execution of program instructions, -- */
/* --  the second holds the registers, and the third contains the output.             -- */
/* --                                                                                 -- */
    mainPanel.setPreferredSize(new Dimension(760, 270));
    mainPanel.setLayout(new FlowLayout());
    mainPanel.setBorder(BorderFactory.createLineBorder(simulatorBackground, 4));
    mainPanel.setBackground(simulatorBackground);

    programPane = createProgramPanel();                     // Create program execution
    programPane.setPreferredSize(new Dimension(318, 248));  // monitor table.
    programPane.setBorder(BorderFactory.createEtchedBorder());

    mainPanel.add(programPane);  
/* --                                                                                 -- */
/* --                             Set up register panel.                              -- */
/* --                                                                                 -- */
/* --   registersPanel is the container object that holds all registers except        -- */
/* --   the output register.                                                          -- */
/* --                                                                                 -- */
    registersPanel.setBackground(registerBackground);
    registersPanel.setBorder(BorderFactory.createEtchedBorder());
    registersPanel.setPreferredSize(new Dimension(194, 250));
    registersPanel.setLayout(new FlowLayout());
/* --                                                                                 -- */
/* -- Accumulator                                                                     -- */
/* --                                                                                 -- */
    acPanel.setPreferredSize(new Dimension(172, 35));       // Set AC register's panel 
    acPanel.setBorder(BorderFactory                         // characteristics
                             .createRaisedBevelBorder());
    acPanel.setBackground(registerForeground);
    acPanel.setLayout(new FlowLayout());
    acLabel.setPreferredSize(new Dimension(30, 15));       // Set label characteristics
    acLabel.setFont(new Font("Dialog", 1, 12));
    acLabel.setText("AC");
    acLabel.setForeground(registerTextColor);
    acLabel.setToolTipText("Accumulator");
    regAC.setPreferredSize(new Dimension(52, 20));         // Set Register characteristics
    regAC.setBorder(BorderFactory.createLoweredBevelBorder());
    regAC.setFont(new Font("Monospaced", 0, 14));
    regAC.setValue(0);
    regAC.setEditable(false);
    acModeBox.setPreferredSize(new Dimension(57, 20));     // Set combo box characteristics
    acModeBox.setBackground(registerForeground);
    acModeBox.setFont(new Font("Dialog", 0, 11));
    acModeBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int i = acModeBox.getSelectedIndex();
        regAC.setMode(i);
      }
    }); // Listener
    acPanel.add(acLabel);                        // Add the above components to
    acPanel.add(regAC);                          // the main register panel.
    acPanel.add(acModeBox);
    registersPanel.add(acPanel);
/* --                                                                                 -- */
/* -- Instruction Register (IR)                                                       -- */
/* --                                                                                 -- */
    irPanel.setPreferredSize(new Dimension(172, 34));
    irPanel.setBorder(BorderFactory.createRaisedBevelBorder());
    irPanel.setBackground(registerForeground);
    irPanel.setLayout(new FlowLayout());
    irLabel.setPreferredSize(new Dimension(30, 15));  
    irLabel.setFont(new Font("Dialog", 1, 12));
    irLabel.setText("IR");
    irLabel.setForeground(registerTextColor);
    irLabel.setToolTipText("Instruction Register");
    regIR.setPreferredSize(new Dimension(52, 20));
    regIR.setBorder(BorderFactory.createLoweredBevelBorder());
    regIR.setFont(new java.awt.Font("Monospaced", 0, 14));
    regIR.setValue(0);
    regIR.setEditable(false);
    irModeBox.setPreferredSize(new Dimension(57, 20));  
    irModeBox.setBackground(registerForeground);
    irModeBox.setFont(new Font("Dialog", 0, 11));
    irModeBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int i = irModeBox.getSelectedIndex();
        regIR.setMode(i);
      }
    }); // Listener
    irPanel.add(irLabel);
    irPanel.add(regIR);
    irPanel.add(irModeBox);
    registersPanel.add(irPanel);
/* --                                                                                 -- */
/* -- Memory Address Register (MAR)                                                   -- */
/* --                                                                                 -- */
    marPanel.setPreferredSize(new Dimension(172, 34));  
    marPanel.setBorder(BorderFactory.createRaisedBevelBorder());
    marPanel.setBackground(registerForeground);
    marPanel.setLayout(new FlowLayout());
    marLabel.setPreferredSize(new Dimension(30, 15));  
    marLabel.setFont(new Font("Dialog", 1, 12));
    marLabel.setText("MAR");
    marLabel.setForeground(registerTextColor);
    marLabel.setToolTipText("Memory Address Register");
    regMAR.setPreferredSize(new Dimension(52, 20));  
    regMAR.setBorder(BorderFactory.createLoweredBevelBorder());  
    regMAR.setFont(new Font("Monospaced", 0, 14));
    regMAR.setValue(0);
    regMAR.setEditable(false);
    marModeBox.setPreferredSize(new Dimension(57, 20));  
    marModeBox.setBackground(registerForeground);
    marModeBox.setFont(new Font("Dialog", 0, 11));
    marModeBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int i = marModeBox.getSelectedIndex();
        regMAR.setMode(i);
      }
    }); // Listener
    marPanel.add(marLabel);
    marPanel.add(regMAR);
    marPanel.add(marModeBox);
    registersPanel.add(marPanel);
/* --                                                                                 -- */
/* -- Memory Buffer Register (MBR)                                                    -- */
/* --                                                                                 -- */
    mbrPanel.setPreferredSize(new Dimension(172, 34));
    mbrPanel.setBorder(BorderFactory.createRaisedBevelBorder());
    mbrPanel.setBackground(registerForeground);
    mbrPanel.setLayout(new FlowLayout());
    mbrLabel.setPreferredSize(new Dimension(30, 15));  
    mbrLabel.setFont(new Font("Dialog", 1, 12));
    mbrLabel.setText("MBR");
    mbrLabel.setForeground(registerTextColor);
    mbrLabel.setToolTipText("Memory Buffer Register");
    regMBR.setPreferredSize(new Dimension(52, 20));
    regMBR.setBorder(BorderFactory.createLoweredBevelBorder()); 
    regMBR.setFont(new Font("Monospaced", 0, 14));
    regMBR.setValue(0);
    regMBR.setEditable(false);
    mbrModeBox.setPreferredSize(new Dimension(57, 20)); 
    mbrModeBox.setBackground(registerForeground);
    mbrModeBox.setFont(new Font("Dialog", 0, 11));
    mbrModeBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int i = mbrModeBox.getSelectedIndex();
        regMBR.setMode(i);
      }
    }); // Listener
    mbrPanel.add(mbrLabel);
    mbrPanel.add(regMBR);
    mbrPanel.add(mbrModeBox);
    registersPanel.add(mbrPanel);
/* --                                                                                 -- */
/* -- Program Counter (PC)                                                            -- */
/* --                                                                                 -- */
    pcPanel.setPreferredSize(new Dimension(172, 35));
    pcPanel.setBorder(BorderFactory.createRaisedBevelBorder());
    pcPanel.setBackground(registerForeground);
    pcPanel.setLayout(new FlowLayout());
    pcLabel.setPreferredSize(new Dimension(30, 15));  
    pcLabel.setFont(new Font("Dialog", 1, 12));
    pcLabel.setText("PC");
    pcLabel.setForeground(registerTextColor);
    pcLabel.setToolTipText("Program counter");
    regPC.setPreferredSize(new Dimension(52, 20)); 
    regPC.setBorder(BorderFactory.createLoweredBevelBorder());  
    regPC.setFont(new Font("Monospaced", 0, 14));
    regPC.setValue(0);
    regPC.setEditable(false);
    pcModeBox.setPreferredSize(new Dimension(57, 20)); 
    pcModeBox.setBackground(registerForeground);
    pcModeBox.setFont(new Font("Dialog", 0, 11));
    pcModeBox.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int i = pcModeBox.getSelectedIndex();
        regPC.setMode(i);
      }
    }); // Listener
    pcPanel.add(pcLabel);
    pcPanel.add(regPC);
    pcPanel.add(pcModeBox);
    registersPanel.add(pcPanel);
/* --                                                                                 -- */
/* -- Input Register (INPUT)                                                          -- */
/* --                                                                                 -- */
    inputPanel.setPreferredSize(new Dimension(172, 35));  
    inputPanel.setLayout(new FlowLayout());
    inputPanel.setBorder(BorderFactory.createRaisedBevelBorder()); 
    inputPanel.setBackground(registerForeground);
    inputLabel.setPreferredSize(new Dimension(33, 15)); 
    inputLabel.setFont(new Font("Dialog", 1, 11)); 
    inputLabel.setText("INPUT");
    inputLabel.setForeground(registerTextColor);
    inputLabel.setToolTipText("Input Register");
    regINPUT.setPreferredSize(new Dimension(52, 20));  
    regINPUT.setBorder(BorderFactory.createLoweredBevelBorder());  
    regINPUT.setFont(new Font("Monospaced", 0, 14));    
    regINPUT.setValue(0);
    regINPUT.setMode(ASCII);
    regINPUT.setEditable(false);
    regINPUT.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        input();
      }
    }); // Listener
    inputModeBox.setPreferredSize(new Dimension(57, 20)); 
    inputModeBox.setBackground(registerForeground);
    inputModeBox.setFont(new Font("Dialog", 0, 11));
    inputModeBox.setSelectedIndex(2);                            // Default input to ASCII
    inputModeBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int i = inputModeBox.getSelectedIndex();
        regINPUT.setMode(i);
      }
    }); // Listener
    inputPanel.add(inputLabel);
    inputPanel.add(regINPUT);  
    inputPanel.add(inputModeBox);  
    registersPanel.add(inputPanel);    
    mainPanel.add(registersPanel);      // Put the registers container in the center panel.
/* --                                                                                 -- */
/* -- Panel for output register (OUTPUT)                                              -- */
/* --                                                                                 -- */
    outputScrollPane.getViewport()                         // Construct the output area
                      .setBackground(Color.white);
    outputScrollPane.setBorder(BorderFactory.createLoweredBevelBorder());
    outputScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    outputScrollPane.setPreferredSize(new Dimension(158, 166));
    outputArea.setFont(new Font("Monospaced", 0, 12));
    outputArea.setBorder(BorderFactory.createLineBorder(Color.white, 4));
    outputScrollPane.getViewport().add(outputArea, null);
    outputModeBox.setPreferredSize(new Dimension(60, 22));
    outputModeBox.setBackground(registerForeground);
    outputModeBox.setFont(new Font("Dialog", 0, 11));
    outputModeBox.setToolTipText("Output can be displayed in hex, decimal or ASCII.");
    outputModeBox.setSelectedIndex(2);
    regOUTPUT.setMode(ASCII);
    outputModeBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int i = outputModeBox.getSelectedIndex();
        if (regOUTPUT.mode != i) {
          regOUTPUT.setMode(i);
          reformatOutput();
        }
      }
    }); // Listener
    outputControlBox.setFont(new Font("Dialog", 0, 11));
    outputControlBox.setBackground(registerForeground);
    outputControlBox.setPreferredSize(new Dimension(98, 22));
    outputControlBox.setToolTipText("Linefeed control and printing.");
    outputControlBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int i = outputControlBox.getSelectedIndex();
        switch (i) {
          case 1: outputWithLinefeed = true;
                  break;
          case 2: outputWithLinefeed = false;
                  break;
          case 3: outputArea.setText("");                    // Clear the output display as
                  outputStream = new Vector();               // well as the Vector that
                  outputControlBox.setSelectedIndex(0);      // holds the output contents.
                  break;
          case 4: printOutput();
         default: break;
        } // switch
      }
    }); // Listener
    outputOuterPanel.setPreferredSize(new Dimension(208, 250));      // Outermost OUTPUT container
    outputOuterPanel.setBackground(registerBackground);
    outputOuterPanel.setBorder(BorderFactory.createEtchedBorder());

    outputInnerPanel.setBackground(registerForeground);              // Next level OUTPUT container.
    outputInnerBorder.setTitleColor(registerTextColor);              // (Contained within above.)
    outputInnerPanel.setBorder(outputInnerBorder);
    outputInnerPanel.setPreferredSize(new Dimension(186, 232));
    outputInnerPanel.setLayout(new FlowLayout());
    outputInnerPanel.add(outputScrollPane);                          // Add the scrollpane and
    outputInnerPanel.add(outputModeBox);                             // display controls.
    outputInnerPanel.add(outputControlBox);

    outputOuterPanel.add(outputInnerPanel);
    mainPanel.add(outputOuterPanel);                   
    simulatorPane.add(mainPanel);         //  Add the center panel to the main screen.

/* --                                                                                 -- */
/* --  Create and populate the container that will hold the memory table display and  -- */
/* --  the status message text field.                                                 -- */
/* --                                                                                 -- */
    bottomPanel.setPreferredSize(new Dimension(760, 210));   // Container for memory array
    bottomPanel.setLayout(new FlowLayout());                 // and message line.
    bottomPanel.setBackground(simulatorBackground);
    memoryPane = createMemoryPanel();
    memoryPane.setPreferredSize(new Dimension(692, 155));
    memoryPane.setBorder(BorderFactory.createEtchedBorder());

    msgField.setBackground(messageBackground);            // Machine message
    msgField.setFont(new Font("SansSerif", 0, 11));       // text field
    msgField.setPreferredSize(new Dimension(740, 40));
    msgField.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(simulatorBackground, 10),
                            BorderFactory.createLoweredBevelBorder()));
    msgField.setEditable(false);
    setStatusMessage(" Ready to load program instructions.");

    bottomPanel.add(memoryPane);         // Add memory array to bottom container.
    bottomPanel.add(msgField);           // Add message field to bottom container.
    simulatorPane.add(bottomPanel);      // Add bottom container to screen.
    validate();
  } // MarieSim()

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
         setBackground(Color.green);     // Highlight the currently-executing                             
       }                                 // instruction in green.
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
       if (column > 0) {
         setText((String) value);
         setBackground(Color.lightGray); 
       }
       setToolTipText("Check box to set breakpoint.");
       setFont(new Font("sanserif", 0, 11));
       setForeground(new Color(0, 50, 165));
       return this; 
    } // getTableCellRendererComponent()
  } // RowHeaderTableCellRenderer

    TableColumnModel cm = new DefaultTableColumnModel() { 
      int colno = 0;                              // This is the column
      public void addColumn(TableColumn tc) {     // model for the main
        switch (colno) {                          // part of the program
        case 0:                                   // table.
        case 1: colno++;
                return;   // Drop first two columns.
        case 3: tc.setMinWidth(62);
                tc.setMaxWidth(62);
                tc.setPreferredWidth(62);
                break;
        case 5: tc.setMinWidth(40);
                tc.setMaxWidth(40);
                tc.setPreferredWidth(40);
                break;
        default:
          tc.setMinWidth(68);
          tc.setMaxWidth(68);
        } // switch
        super.addColumn(tc);
        colno++;
        if (colno > 5)
          colno = 0;
      }
    };  // TableColumnModel

   TableColumnModel rowHeaderModel = new DefaultTableColumnModel() { 
      int twocols = 0;                           // This is the column 
      public void addColumn(TableColumn tc) {    // model for first two
        if (twocols < 2) {                       // columns only.
           if (twocols == 0) 
             tc.setMaxWidth(20);
           else 
             tc.setMaxWidth(40);
           twocols++;
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
    headerColumn.setMaximumSize(new Dimension(60, 10000));
    headerColumn.setBackground(new Color(224, 224, 224));   // Gray background for checkbox.
    headerColumn.setShowVerticalLines(false);
    headerColumn.setSelectionBackground(Color.lightGray);   // Makes header selection
                                                            // invisible.
    headerColumn.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
    headerColumn.setColumnSelectionAllowed(false);
    headerColumn.setCellSelectionEnabled(false);
    headerColumn.setOpaque(false);
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
    codeLineCount = 12;
    programArray  = new Object[12][6];        // Load program instruction monitor table
    for (int i = 0; i < 12; i++) {            // with blanks.
      programArray[i][0] = new Boolean(false);
      programArray[i][1] = "  ";
      programArray[i][2] = "  ";
      programArray[i][3] = "  ";
      programArray[i][4] = "  ";
      programArray[i][5] = "  ";
    }
    jsp.setRowHeader(jv);
    return jsp;
 } // createProgramPanel()


 JScrollPane createMemoryPanel() {
/******************************************************************************************
*  Much the same as the program table, this method sets up a JTable that provides the     *
*  means to monitor memory activity.  It is placed in a JScrollPane which is returned     *
*  to the calling method.                                                                 *
*                                                                                         *
*  As with the program monitor, we use two tables to render the memory monitor:  The      *
*  first is a table containing memory addresses in oncrements of 16, and the second       *
*  holds the memory contents in hexadecimal strings.                                      *
*                                                                                         *
*  As memory is accessed, the memory calls are highlighted using a special table cell     *
*  cell renderer.                                                                         *
*                                                                                         *
*  Much of the code in this method was derived from Eckstein, Loy and Wood's fine book,   *
*  *Java Swing* O'Reilly 1998.                                                            *
******************************************************************************************/
    class MemoryTableCellRenderer extends JLabel 
                                    implements TableCellRenderer { 
     public Component getTableCellRendererComponent( 
                                   JTable table, Object value, boolean isSelected, 
                                   boolean hasFocus, int row, int column)          { 
       setText((String) value); 
       setFont(new Font("Monospaced", 0, 11));
       setOpaque(true); 
       setBackground(Color.white); 
       setForeground(Color.black); 
       setBorder(new EmptyBorder(0, 0, 0, 0)); 
       if (((row * 16) + column) == memoryFocusCell) {     // Set the green cell
         setBackground(Color.green);                       // highlight if the
       }                                                   // application is using 
       else {                                              // the cell.
        setBackground(Color.white);                                  
       }
       return this; 
    } // getTableCellRendererComponent()
  } // MemoryTableCellRenderer

    class RowHeaderTableCellRenderer extends JLabel 
                                    implements TableCellRenderer { 
     public Component getTableCellRendererComponent( 
                                   JTable table, Object value, boolean isSelected, 
                                   boolean hasFocus, int row, int column)          { 
       setText((String) value); 
       setFont(new Font("Dialog", 0, 11));
       setForeground(tableHeaderColor);
       return this; 
    } // getTableCellRendererComponent()
  } // RowHeaderTableCellRenderer

    TableModel tm = new AbstractTableModel () {
      String headers[] =  { " ", "+0", "+1", "+2", "+3", "+4", "+5", "+6", "+7", 
                                 "+8", "+9", "+A", "+B", "+C", "+D", "+E", "+F" };
      public int getColumnCount()                { return headers.length; }
      public int getRowCount()                   { return 256;  }
      public String getColumnName(int col)       { return headers[col];   }
      public Object getValueAt(int row, int col) { return memoryArray[row][col];   }
      public boolean isCellEditable(int rowIndex, int columnIndex) { return false; }

      public void setValueAt(Object value, int row, int col) {
        memoryArray[row][col] = value;        // Only one column is editable,
        fireTableDataChanged();               // set by isCellEditable method.
      }
      public Class getColumnClass(int c) {    // This method is used to provide 
        return getValueAt(0, c).getClass();   // the default cell editor.  I.e.,
      }                                       // Booleans display as checkboxes.

    }; // TableModel

    TableColumnModel cm = new DefaultTableColumnModel() {
      boolean first = true;                               // This is the column model
      public void addColumn(TableColumn tc) {             // for the main memory
        if (first) {                                      // contents.
          first = false;
          return; 
        }
        tc.setMinWidth(40);
        tc.setMaxWidth(40);
        tc.setPreferredWidth(40);
        super.addColumn(tc);
      } // addColumn()
    };  // TableColumnModel

   TableColumnModel rowHeaderModel = new DefaultTableColumnModel() {
      boolean first = true;                               // This is the column model
      public void addColumn(TableColumn tc) {             // for the first column which
        if (first) {                                      // contains the memory
           tc.setMaxWidth(30);                            // addresses in increments 
           super.addColumn(tc);                           // of 16.
           first = false;
        }
      }
    };  // TableColumnModel

    memoryTable = new JTable(tm, cm);
    memoryTable.setRowSelectionAllowed(false);
    memoryTable.setCellSelectionEnabled(false);
    memoryTable.setSelectionBackground(Color.white); 
    memoryTable.setShowHorizontalLines(false);             // Turn off all gridlines.
    memoryTable.setShowVerticalLines(false);
    memoryTable.setRowHeight(MEMORY_TABLE_ROW_HEIGHT);

    MemoryTableCellRenderer renderer = new MemoryTableCellRenderer();
    memoryTable.setDefaultRenderer(Object.class, renderer);
    JTableHeader jth = memoryTable.getTableHeader();
    jth.setForeground(tableHeaderColor);
    jth.setFont(new Font("Dialog", 0, 11));
    jth.setReorderingAllowed(false);
    jth.setResizingAllowed(false);
    jth.setBorder(new EtchedBorder(EtchedBorder.LOWERED)); 
    memoryTable.createDefaultColumnsFromModel();

    JTable headerColumn = new JTable(tm, rowHeaderModel);
    headerColumn.createDefaultColumnsFromModel();
    headerColumn.setMaximumSize(new Dimension(30, 150));
    headerColumn.setBackground(Color.lightGray);
    headerColumn.setOpaque(false);
    headerColumn.setShowVerticalLines(false);
    headerColumn.setSelectionBackground(Color.lightGray);  // Make header selection 
                                                           // invisible.
    headerColumn.setBorder(new EtchedBorder(EtchedBorder.LOWERED)); 
    headerColumn.setColumnSelectionAllowed(false);
    headerColumn.setCellSelectionEnabled(false);
    RowHeaderTableCellRenderer headerRenderer = new RowHeaderTableCellRenderer();
    headerColumn.setDefaultRenderer(Object.class,headerRenderer);
    headerColumn.setRowHeight(MEMORY_TABLE_ROW_HEIGHT);

    memoryTable.setSelectionModel                        // Sharing the same model
                     (headerColumn.getSelectionModel()); // keeps tables in synch.

    JViewport jv = new JViewport();
    jv.setView(headerColumn);
    jv.setPreferredSize(headerColumn.getMaximumSize());
    headerColumn.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    JViewport jv1 = new JViewport();
    jv1.setView(memoryTable);
    jv1.setPreferredSize(memoryTable.getMaximumSize());
    memoryTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    for (int i = 0; i < 4095; i+= 16)  {                // Populate memory with zeros
      Arrays.fill(memoryArray[i / 16], " 0000");        // and provide address labels.
      memoryArray[i / 16][0] = "  "+to3CharHexStr(i);
    }

    JScrollPane jsp = new JScrollPane(memoryTable);
    jsp.setRowHeader(jv);
    return jsp;
  } // createMemoryPane() 

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


  void displayEditorFrame() {
/******************************************************************************************
*  If the editor has not yet been invoked, create an instance of it.  If it has been      *
*  invoked, try to move it to the front.  If this causes an exception, create another     *
*  instance of it.                                                                        *
*                                                                                         *
*  When creating a new editor instance, the source file associated with the currently     *
*  loaded file will automatically be loaded.                                              *
*                                                                                         *
*  We add a window listener to the editor so that we can completely destroy the object    *
*  after the editor is closed.   Note that when the user selects "exit" from the          *
*  editor's menu, the editor has to dispatch a window closing event so we can be          *
*  notified that the application is done.                                                 *
******************************************************************************************/
     try {
           marieEditor.show(); 
           marieEditor.requestFocus();   
     }
     catch (Exception e) {
           if (mexFile != null) {
             marieEditor = new MarieEditor(mexFile+SRC_TYPE, false);

             reloadFileItem.setEnabled(true);
           }
           else
             marieEditor = new MarieEditor(false);
           marieEditor.addWindowListener(new WindowAdapter() {
              public void windowClosing(WindowEvent e) {
                   marieEditor = null;
              } // windowClosing()            
           }); // Listener         
     }   
  } // displayEditorFrame()

  void displayDelayFrame() {
/******************************************************************************************
*  As with the editor frame, if we have previously created an instance of the frame       *
*  that accepts the delay parameter, we will make it visible.  If the frame has not       *
*  been instantiated yet, or if it was disposed of, an exception is thrown and we         *
*  create another instance.                                                               *
*                                                                                         *
*  As with the editor, we add a window listener so that we can completely destroy         *
*  the frame object after the JFrame is closed.                                           *
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

  void displayCoreDump() {
/******************************************************************************************
*  This method works the same way as displayDelayFrame().  See explanation above.         *
******************************************************************************************/
     try {
           dumpViewer.show();
           dumpViewer.requestFocus();
     }
     catch (Exception e1) {
            try {
                  dumpFrame.requestFocus();   
            }
            catch (Exception e2) {
                  dumpFrame = new CoreDumpFrame(); 
                  dumpFrame.addWindowListener(new WindowAdapter() {
                      public void windowClosing(WindowEvent e) {
                           dumpFrame = null;
                      } // windowClosing()
                  }); // Listener 
            }
     }                               
  } //displayCoreDump()


  void displaySymbolTable() {
/******************************************************************************************
*  This method works the same way as displayDelayFrame().  See explanation above.         *
******************************************************************************************/
     try {
           symbolTable.show();
           symbolTable.requestFocus();   
     }
     catch (Exception e) {         
              symbolTable = new TextFileViewer("Symbol Table",mexFile+MAP_TYPE, false);
              symbolTable.setSize(200, 280);
              symbolTable.setLocation(550, 40);
              symbolTable.addWindowListener(new WindowAdapter() {
                  public void windowClosing(WindowEvent e) {
                       symbolTable = null;
                  } // windowClosing()
              }); // Listener 
              symbolTable.show();
     }
  } // displaySymbolTable() 


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

  void printOutput() {
/******************************************************************************************
*  This method calls upon SDK 1.4 printing facilities to print the contents of the        *
*  output area.  We first write the contents of this area to a file because the           *
*  DocFlavor.STRING.TEXT_PLAIN is not supported by all systems.  Although the same type   *
*  of content is involved, the DocFlavor.INPUT_STREAM.AUTOSENSE, however, doesn't give    *
*  us this trouble. Go figure!                                                            *
******************************************************************************************/
   BufferedWriter tempFile = null;
   String formFeed      = "\014";
   try {                                                 // Create the temporary output.
     tempFile = new BufferedWriter( new FileWriter("msim.tmp") );
   } // try
   catch (IOException e) {
     System.err.println(e); 
   return;
   } // catch
   try {            
     tempFile.write(outputArea.getText());             // Capture the text in the
     tempFile.write(formFeed);                          // trace JTextArea.
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
        FileInputStream fis = new FileInputStream("msim.tmp");
        DocAttributeSet das = new HashDocAttributeSet(); 
        Doc doc = new SimpleDoc(fis, flavor, das); 
        job.print(doc, pras);
      } // try
      catch (Exception e) {
        ;
     } // catch
   } 
   try {              
     File tmp = new File("msim.tmp");                  // Delete the temp file.
     tmp.delete();
     }
   catch (Exception e) {
     ;
   } // catch
} // printOutput()

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
                      .createImage(MarieSim.class.getResource("M.gif")));
    int option = JOptionPane
                    .showOptionDialog(closingFrame, "Really quit?",
                                      "Quit Confirmation", JOptionPane.YES_NO_OPTION,
                                      JOptionPane.QUESTION_MESSAGE, null,
                                       new Object[] {"Yes", "No"}, "Yes");
    if (option == JOptionPane.YES_OPTION)
      System.exit(0);
 } // exitProgram()



/* --  Marie machine functional methods --------------------------------------------- -- */
/* --                                                                                 -- */
/* --  Manipulators and converters for hex, decimal and ASCII values.                 -- */
/* --                                                                                 -- */
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
* Same as above (to3CharHexStr()), only returns a string of exactly 4-characters that are *
* in the (decimal) range -32,768 to 32,767.                                               *
******************************************************************************************/
    if (number < 0) {                           // If number negative, convert to 16-bit 2's
      number = number << 16;                    // complement by shifting the low-order 16
    }                                           // bits to the high-order bits.  (We lose the
                                                // rightmost 16 bits below.)
    String    hexStr = Integer.toHexString(number).toUpperCase();
    switch (hexStr.length()) {
       case 1: hexStr =  "000" + hexStr;             // Pad strings shorter than 4 chars.
               break;
       case 2: hexStr =  "00"  + hexStr;
               break;
       case 3: hexStr =  "0"   + hexStr;
               break;
       case 4: break;
      default: return hexStr.substring(0, 4);   // Truncate strings longer than 4 chars.
    } // switch()
    return hexStr;
  } // to4CharHexStr()


  String rightJustifyIn4 (String s) {
/******************************************************************************************
*  Right-justifies the argument string into a four-character cell, padding out with       *
*  leading blanks.  If string is longer than 4 chars, only its rightmost 4 chars          *
*  are taken.                                                                             *
******************************************************************************************/
    StringBuffer justified = new StringBuffer("    ");
    int len = s.length(),
          j = 3;
    for (int i = len-1; ((i >=0) && (j >=0)) ; i--) {
       justified.setCharAt((j--),s.charAt((i)));
    }
    return justified.toString();
  } // rightJustifyIn4()


/* ------------------------------------------------------------------------------------- */
/* -- Machine state output methods.                                                   -- */
/* ------------------------------------------------------------------------------------- */
  void dumpRegs(BufferedWriter dumpFile) throws Exception {
/******************************************************************************************
*    Show all register contents (in their current rendering).                             *
******************************************************************************************/
    dumpFile.write(linefeed);
    dumpFile.write("    PC: "+regPC+"   MAR: "+regMAR+"      AC: " +regAC+linefeed);
    dumpFile.write("    IR: "+regIR+"   MBR: "+regMBR+"   INPUT:  ");
    if (regINPUT.toString() != null)
      dumpFile.write(regINPUT.toString().trim());
    else
      dumpFile.write("null");
    dumpFile.write("  OUTPUT: ");
    if (regOUTPUT.toString() != null)
      dumpFile.write(regOUTPUT.toString().trim()+linefeed);
    else
      dumpFile.write("null"+linefeed);
  } // dumpRegs()


  void dumpMemory(int start, int end, BufferedWriter dumpFile) throws Exception {
/******************************************************************************************
*    Dump memory between the starting and ending addresses (inclusive) supplied in the    *
*    arguments.  The output is given in columns of eight addresses at a time.             *
*    If either argument is negative, the entire contents of memory are shown.             *
******************************************************************************************/ 
    if ((start < 0) || (end < 0)) {
      start = 0;
      end = MAX_MARIE_ADDR;
    }
    int i = start, column = 0;
    dumpFile.write(linefeed);
    dumpFile.write("       ");
    dumpFile.write("Memory dump for addresses "+to3CharHexStr(start));
    dumpFile.write(" through "+to3CharHexStr(end)+linefeed+linefeed);
    for ( ; i <= MAX_MARIE_ADDR; i++) {                // Note:  If we get a value out of 
      if (column == 0)                                 //   range for the ending address, we 
        dumpFile.write(" "+to3CharHexStr(i)+":  ");    //   stop at the max address anyway.
     dumpFile.write((memoryArray[i / 16][i % 16 + 1]).toString()+"  "); 
      if (i == end) {
        dumpFile.write(linefeed);
        return;
      }
      column++;
      if (column == 8) {
        column = 0;
        dumpFile.write(linefeed);
      }
    }
    dumpFile.write(linefeed);
  } // dumpMemory()

  void produceCoreDump(int start, int end) {
/******************************************************************************************
*  This is the driver method for the core dump.  Output is directed to a DMP file so      *
*  that the user can read it in a display window, or print it through some other means.   *
*  Once the dump is created, the resulting file is displayed in a viewer.                 *
******************************************************************************************/ 
    BufferedWriter dumpFile = null; 
    boolean done = false;
    try {                                             // Try to open the output.
      dumpFile = new BufferedWriter( new FileWriter(mexFile+DMP_TYPE) );
    } // try
    catch (FileNotFoundException e) {
      setStatusMessage(" Error!  Cannot create core dump file.");
      return;
    } // catch
    catch (IOException e) {
      setStatusMessage(" "+e); 
      return;
    } // catch
    try {
       dumpFile.write("Machine dump for "+mexFile+MEX_TYPE+"           "
                        +new Date()+linefeed+linefeed);
       dumpRegs(dumpFile);                            // Dump the registers.
    } // try
    catch (Exception e) {
      setStatusMessage(" "+e); 
    } // catch
    try {
       dumpMemory(start, end, dumpFile);              // Dump the address range specified.
    } // try
    catch (Exception e) {
      setStatusMessage(" "+e); 
    } // catch
    try {
       dumpFile.write(formfeed);
       dumpFile.flush();                              // Flush and close output.
       dumpFile.close();
    } // try
    catch (IOException e) {
      setStatusMessage(" "+e); 
    } // catch
    dumpViewer = new TextFileViewer("Core Dump", mexFile+DMP_TYPE, false);
    dumpViewer.setSize(600, 400);
    dumpViewer.setLocation(150, 40);
    dumpViewer.show();
  } // produceCoreDump()

/* ------------------------------------------------------------------------------------- */
/* -- General output methods.                                                         -- */
/* ------------------------------------------------------------------------------------- */
  void setStatusMessage(String msg) {
/******************************************************************************************
*  Places the message, msg, in the text field at the bottom of the screen and then fires  *
*  an update event.  (The text field has a listener for this event.)                      *
******************************************************************************************/
   statusMessage = msg;
   msgField.setText(statusMessage);
   msgField.postActionEvent();
  } // setErrorMessage()


  void reformatOutput() {
/******************************************************************************************
*    Takes the contents of the Vector that holds output (in integer form) and translates  *
*    to the current rendering mode (HEX, DEC or ASCII) state of the OUTPUT register.      *
*    Note:  We could have fed the contents of the Vector back through the OUTPUT register *
*           but fewer method invocations are required using a more direct approach.       *
******************************************************************************************/
     Enumeration e = outputStream.elements();
     Integer value;
     outputArea.setText("");
     while (e.hasMoreElements()) {                             // Reformat the entire
       value = (Integer) e.nextElement();                      // contents of the output
       switch (regOUTPUT.mode) {                               // Vector.
          case HEX: outputArea.append(to4CharHexStr(value.intValue()));   // Use the current OUTPUT
                    break;                                     // register mode.
          case DEC: outputArea.append(value.toString());
                    break;
          default:  outputArea.append(""+ (char) (value.intValue() % 128));
       } // switch
       if (outputWithLinefeed)                                 // And check linefeed
         outputArea.append(linefeed);                          // preference as well.
       else
         if ((value.intValue() == 13) && (regOUTPUT.mode == ASCII))
            outputArea.append(linefeed);
     } // while
     outputArea.repaint();
  } // reformatOutput()


/* ------------------------------------------------------------------------------------- */
/* -- Machine loading and reset methods.                                              -- */
/* ------------------------------------------------------------------------------------- */
  void getProgram() {
/******************************************************************************************
*  This method accepts a filename and path from a JFileChooser dialog.  The path and      *
*  file extension are stripped off so the root filename can be used to locate other       *
*  files related to the executable, such as the symbol table.  The pathname is also       *
*  retained so that it can be passed to the editor if the user wishes to edit the         *
*  program source code.  When all of this parsing is completed, the loadProgram()         *
*  method is invoked.                                                                     *
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
    try {
       symbolTable.dispose();                          // Reset all subordinate frames
       symbolTable = null;                             // if they exist.  If they
    }                                                  // don't exist, we don't care.
    catch (Exception e) {
    }
    try {
       dumpViewer.dispose();
       dumpViewer = null;
    }
    catch (Exception e) {
    }
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
*  buttons appropriately.  If a symbol table for the loaded program can be found, the     *
*  menu option for displaying this table is also enabled.  If all loading was successful, *
*  the machineState will be HALTED_NORMAL.  If there was a fatal error encountered        *
*  during the load, the machineState will be NO_PROGRAM_LOADED after the marieReset()     *
*  call is performed.                                                                     *
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
    reloadFileItem.setEnabled(false);
    runRunItem.setEnabled(false);              // Set menu buttons and options
    getDump.setEnabled(false);                 // to assume errors.
    breakpointMenu.setEnabled(false);
    showSymbols.setEnabled(false);
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
        programArray[i][0] = new Boolean(false);
        programArray[i][1] = "  ";
        programArray[i][2] = "  ";
        programArray[i][3] = "  ";
        programArray[i][4] = "  ";
        programArray[i][5] = "  "; 
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
    programArray  = new Object[codeLineCount][6];    // Prepare program-specific data 
    codeReference.clear();                           // structures.
    Enumeration e = codeVector.elements();
    int lineCount = 0;

    while (e.hasMoreElements()) {                    // Load data structures.
      aCodeLine = (AssembledCodeLine) e.nextElement();
      programArray[lineCount][0] = new Boolean(false);      // Load the monitor table...
      programArray[lineCount][1] = "  "+aCodeLine.lineNo;
      programArray[lineCount][2] = " "+aCodeLine.stmtLabel;
      programArray[lineCount][3] = aCodeLine.mnemonic;
      programArray[lineCount][4] = aCodeLine.operandToken;
      programArray[lineCount][5] = " "+aCodeLine.hexCode+aCodeLine.operand; 
      codeReference.put(aCodeLine.lineNo, new Integer(lineCount));
      lineCount++;
      try {
        addr = Integer.parseInt(aCodeLine.lineNo, 16);      // ... and load memory.
      }
      catch (NumberFormatException exception) {
        continue;
      } // catch
        memoryArray[addr / 16][addr % 16 + 1] = " "+aCodeLine.hexCode+aCodeLine.operand;
    } // while();
    ptm.fireTableStructureChanged();
    String aString = (String) programArray[0][1];
    try {                                                  // Get memory cell of
          addr = Integer.parseInt(aString.trim(), 16);     // first instruction.
    }                                               
    catch (NumberFormatException exception) {
           ;
    } // catch
    int memoryRow = addr / 16;                             // Scroll it to top
    int memoryCol = addr % 16 + 1;                         // of scrollpane.
    Rectangle rect = memoryTable
                       .getCellRect((memoryRow), memoryCol, false);
    memoryTable.scrollRectToVisible(rect);                   
    regPC.setValue(addr);                                  // Set PC to first address
    regPC.postActionEvent();                               // of program.
    rect = programTable.getCellRect(0, 2, false);
    programFocusRow = 0;                                  // Highlight and make visible the
    programTable.scrollRectToVisible(rect);               // first program instruction.
    programPane.repaint();
    memoryPane.repaint();
    setStatusMessage(" "+mexPath+fileSeparator+mexFile+MEX_TYPE+" loaded.");
    reloadFileItem.setEnabled(true);
    runRunItem.setEnabled(true);                          // Set menu buttons and options
    getDump.setEnabled(true);                             // accordingly.
    breakpointMenu.setEnabled(true);
    breakpointOn = false;
    step.setEnabled(true);
    stepRunOff.setEnabled(true);
    stepRunOn.setEnabled(false);
    checkForMap();
    if (stepping)
       step.setEnabled(true);
    machineState = MARIE_HALTED_NORMAL;
  } // loadProgram()


  void checkForMap() {
/******************************************************************************************
*   Checks to see whether there is a symbol table on disk that goes with the program      *
*   that has been loaded.  If we find one, we activate the [Show Symbol Table] button.    *
******************************************************************************************/
    showSymbols.setEnabled(true);
    try { // Prove the previous statement true or false...                       
          InputStream fileIn = new FileInputStream(mexFile+MAP_TYPE);
        }
    catch (IOException e) {                       // No listing file available!
        showSymbols.setEnabled(false);
        }
  } // checkForMap()


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
     regINPUT.setEditable(false);
     regINPUT.repaint();   
     if (stepping) {
       setStatusMessage(" Press [Step] to start.");
       step.setEnabled(true);
     }
     else {
        setStatusMessage("  Press [Run] to start.");
     }   
     regPC.setValue(((String) programArray[0][1]).trim()); // Set PC to first address of program loaded.
     Rectangle rect = programTable.getCellRect(0, 2, false);
     programFocusRow = 0;
     programTable.scrollRectToVisible(rect);
     machineState = MARIE_RUNNING;
     programTable.repaint();
  } // restart()


  void marieReset() {
/******************************************************************************************
*  This method has the effect of pressing the reset button on a physical machine: It      *
*  clears everything.                                                                     *
******************************************************************************************/
    regAC.setValue(0);                          // Reset all registers to 0.
    regAC.postActionEvent(); 
    regIR.setValue(0);
    regIR.postActionEvent(); 
    regMAR.setValue(0);
    regMAR.postActionEvent(); 
    regMBR.setValue(0);
    regMBR.postActionEvent(); 
    regPC.setValue(0);
    regPC.postActionEvent(); 
    regINPUT.setValue(0);
    regINPUT.setEditable(false);
    regOUTPUT.setValue(0);
    outputArea.setText("");
    outputArea.setText("");                    // Clear the output display and the
    outputStream = new Vector();               // output Vector.
    for (int i = 0; i < 4095; i+= 16)  {       // Initialize memory.
      Arrays.fill(memoryArray[i / 16], " 0000");
      memoryArray[i / 16][0] = "  "+to3CharHexStr(i);
    }
    if (codeLineCount >=0)                     // If we already loaded a program, clear it.
      for (int i = 0; i < codeLineCount; i++) {
        programArray[i][0] = new Boolean(false);
        programArray[i][1] = "  ";
        programArray[i][2] = "  ";
        programArray[i][3] = "  ";
        programArray[i][4] = "  ";
        programArray[i][5] = "  "; 
      }
    programFocusRow = 0;
    memoryFocusCell = 0;
    machineState = MARIE_NO_PROGRAM_LOADED;
    runRunItem.setEnabled(false);            // Nothing to run...
    restartItem.setEnabled(false);           // Nothing to restart...
    reloadFileItem.setEnabled(false);        // Nothing to reload...
    showSymbols.setEnabled(false);           // No symbol table...
    setStatusMessage(" ");
    breakpointOn = false;
    repaint();
  } // marieReset

/* --                                                                                 -- */
/* --  Marie operational methods.   (MARIE Microcode.)                                -- */
/* --                                                                                 -- */
  void fetchNext() {
/******************************************************************************************
*   This method performs the "fetch" part of the "fetch-execute" cycle.                   *
*                                                                                         *
*      Side effects: IR contains instruction,                                             *
*                    MAR contains operand address (if any)                                *
*                    MBR contains operand (if any)                                        *
*                    fatalError set if invalid opcode in instruction                      *
*                                     or invalid operand address.                         *
*                    Machine state set to MARIE_RUNNING.                                  *
******************************************************************************************/
    String aString;                  // These local variables make the code
    int  memoryRow, memoryCol;       // cleaner and easier to read.
    if (fatalError)  {                           // Stop if there has been an error.
      halt();
      return;
    }
    regMAR.setValue(regPC.getValue());           // Set MAR to address of next instruction.
    regMAR.postActionEvent();                    // Note: This part happens too fast to be
    int addr = regMAR.getValue();                // seen on the screen, but we do it this
    memoryRow = addr / 16;                       // way because it's how the fetch-execute
    memoryCol = addr % 16 + 1;                   // process works.
    try {                                                             // Pull instruction
      regIR.setValue((" "+(String) memoryArray[memoryRow][memoryCol]).trim()); // from memory 
      regIR.postActionEvent();                                        // into IR.
    }
    catch (ArrayIndexOutOfBoundsException e) {
             errorCode = 3;
             fatalError = true;
             return;
    } // catch
    aString = regPC.toString().trim();           // Move the cursor.
    if (codeReference.containsKey(aString)) {
      programFocusRow =((Integer) codeReference.get(aString)).intValue();
      Rectangle rect = programTable.getCellRect(programFocusRow, 5, false);
      programTable.scrollRectToVisible(rect);
    }
    aString = regIR.toString().trim();
    try {
       instructionCode = Integer.parseInt(aString.substring(0,1), 16);
       if (instructionCode >= operandReqd.length)  // Make sure we have a valid hexcode.
         throw new NumberFormatException();        // This double-checks the operandReqd
    }                                              // array as well!
    catch (NumberFormatException e) {
      fatalError = true;
      errorCode = 1;
      return;
    }
    if (operandReqd[instructionCode]) {            // If instruction needs one,
      regMAR.setValue(regIR.toString().trim().substring(1,4));  // load the operand into MBR
      regMAR.postActionEvent();                    // using MAR value.
      addr = regMAR.getValue();
      memoryRow = addr / 16;
      memoryCol = addr % 16 + 1;
      memoryFocusCell = addr;
      try {
        Rectangle rect = memoryTable.getCellRect(memoryRow, memoryCol, false);
        memoryTable.scrollRectToVisible(rect);
        regMBR.setValue((" "+memoryArray[memoryRow][memoryCol]).trim()); 
        if (fatalError)
          return; 
        regMBR.postActionEvent();
      }
      catch (ArrayIndexOutOfBoundsException e) {
             errorCode = 3;
             fatalError = true;
             return;
      } // catch
    } // if operand
    regPC.setValue(regPC.getValue()+1);            // Increment PC.
    if (regPC.getValue() > MAX_MARIE_ADDR) {
       errorCode = 8;
       fatalError = true;
    }
    if (fatalError)
      return; 
    regPC.postActionEvent();
    errorFound = false;                            // Reset error flag.
    machineState = MARIE_RUNNING;
    programTable.repaint();
    memoryTable.repaint();
  } // fetchNext()

  void execute () {
/******************************************************************************************
*   This method is the mainline of the "execute" part of the "fetch-execute" cycle.       *
******************************************************************************************/
    switch (instructionCode) {
       case  0: jnS();
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


  void jnS() { 
/******************************************************************************************
*   Jump and Store: Store PC at address [MAR] and set PC (jump) to address [MAR]+1.       *
*   (This instruction can be used to create subroutines in MARIE assembly language.)      *
******************************************************************************************/
     int addr = regMAR.getValue();
     int memoryRow = addr / 16;
     int memoryCol = addr % 16 + 1;
     try {
       memoryArray[memoryRow][memoryCol] = " "+to4CharHexStr(regPC.getValue());
       regPC.setValue(regMAR.getValue()+1);
       regPC.postActionEvent();
     }
     catch (ArrayIndexOutOfBoundsException e) {
             errorCode = 3;
             fatalError = true;
     }
   } // jnS() 

 
  void load() { 
/******************************************************************************************
*   Copies the operand from the MBR to the AC.  (The MBR is loaded during                 *
*   instruction fetch.)                                                                   *
******************************************************************************************/
     regAC.setValue(regMBR.getValue());
     regAC.postActionEvent();
   } // load()


  void store() {
/******************************************************************************************
*   Store whatever is in the accumulator to the address specified in the MAR              *
*   by first moving it to the MBR.                                                        *
*   We have to make sure that the value is stored as a 4-char hex string, no              *
*   matter what the current rendering/mode of the MBR, so we don't just use the           *
*   current rendering, which could be in any radix mode.                                  *
******************************************************************************************/
     int memoryRow, memoryCol;
     regMBR.setValue(regAC.getValue());
     if (fatalError)
       return; 
     int addr = regMAR.getValue();
     memoryRow = addr / 16;
     memoryCol = addr % 16 + 1;
     try {
       memoryArray[memoryRow][memoryCol] = " "+to4CharHexStr(regMBR.getValue());
       Rectangle rect = memoryTable.getCellRect(memoryRow, memoryCol, false);
       memoryTable.scrollRectToVisible(rect);
       regMBR.postActionEvent();
     }
     catch (ArrayIndexOutOfBoundsException e) {
             errorCode = 3;
             fatalError = true;
     }
   } // store()


  void add() { 
/******************************************************************************************
*   Adds the value in the MBR to the AC.  (The MBR is loaded during instruction fetch.)   *
******************************************************************************************/
     regAC.setValue(regAC.getValue() + regMBR.getValue());
     regAC.postActionEvent();
   } // add() 


  void subt() { 
/******************************************************************************************
*   Subtracts the value in the MBR from the AC.                                           *
******************************************************************************************/
     regAC.setValue(regAC.getValue() - regMBR.getValue());
     regAC.postActionEvent();
   } // subt() 


  void input() { 
/******************************************************************************************
*   This method is called twice to effect one input.  The first time through, the         *
*   machine state is set to BLOCKED_ON_INPUT and the input register is enabled.  The      *
*   second time through, the input is moved to the accumulator and the register is        *
*   closed to additional input.  The second entry into this method is triggered by        *
*   an action event on the INPUT register.                                                *
*                                                                                         *
*   After the second pass, we need to resume processing after the blocking call.  If      *
*   the machineState is MARIE_RUNNING, we just call the runProgram() method again         *
*   because it terminated when the input() instruction was encountered.  If the           *
*   simulator is being run in "step" mode, we send a completion message and return        *
*   to the caller.                                                                        *
******************************************************************************************/
     if (machineState == MARIE_RUNNING) {       // First time through???
       setStatusMessage(" Waiting for input.");
       machineState = MARIE_BLOCKED_ON_INPUT;      // Block further execution.
       regINPUT.setText("");
       inputPanel.setBackground(Color.pink);
       inputModeBox.setBackground(Color.pink);
       regINPUT.setEditable(true);                 // Enable register input.
       regINPUT.repaint();
       regINPUT.requestFocus();      
     }
     else if (machineState == MARIE_BLOCKED_ON_INPUT) {  // Second time through???
       regINPUT.setValue(regINPUT.getText());
       regINPUT.setEditable(false);              // "Close" the register to input
       runStop.requestFocus();                   // until needed again.
       inputPanel.setBackground(registerForeground);
       inputModeBox.setBackground(registerForeground);
       regINPUT.repaint();
       if (fatalError) {
         halt();
         return;
       } 
       regAC.setValue(regINPUT.getValue());
       if (fatalError) {
         halt();
         return;
       } 
       regAC.repaint();
       machineState = MARIE_RUNNING;             // Reset the machine state.
       if (stepping)                             // Proceed with next instruction
         setStatusMessage(" Press [Step] to continue.");        // or step.
       else {
         setStatusMessage(" ");
         if (breakpointOn)
           runToBreakpoint();
         else  
           runProgram();
       }
     } // else
   } // input()


  void output() { 
/******************************************************************************************
*   Copies the value in the AC to the output register and concatenates the value to       *
*   the text output vector.  Note:  The output appearance is controlled by the radix      *
*   mode of the output register.                                                          *
******************************************************************************************/
     regOUTPUT.setValue(regAC.getValue());
     outputStream.addElement(new Integer(regOUTPUT.getValue()));
     if (regOUTPUT.toString() != null)
       outputArea.append(regOUTPUT.toString().trim());
     if (outputWithLinefeed)
       outputArea.append(linefeed);
     else
       if ((regOUTPUT.getValue() == 13) && (regOUTPUT.mode == ASCII))
          outputArea.append(linefeed);
     Document d = outputArea.getDocument();
     outputArea.select(d.getLength(), d.getLength());
   } // output() 


  void halt() { 
/******************************************************************************************
*   Changes the machine state from (probably) RUNNING to HALTED using the fatalError      *
*   boolean to determine which one is which.  The statusMessage is also loaded with       *
*   a string from the errorMessage array so that it can be displayed.                     *
******************************************************************************************/
    step.setEnabled(false);
    runStop.setEnabled(false);
    if (fatalError) {
       machineState = MARIE_HALTED_ABNORMAL;
       if (errorCode < errorMsgs.length)
         setStatusMessage(" Machine halted abnormally.  Error: "+errorMsgs[errorCode]);
       else
         setStatusMessage(" Machine halted abnormally.");
    }
    else {
       machineState = MARIE_HALTED_NORMAL;
       setStatusMessage(" Machine halted normally.");
    }
   } // halt()

  void skipCond() { 
/******************************************************************************************
*   This instruction will skip over the instruction at PC+1 based on the value in the     *
*   instruction's lower 12 bits.                                                          *
******************************************************************************************/
     int cond = regIR.getValue();
     cond = cond & 0x0C00;   // Strip the opcode from the instruction.
     cond = cond >> 10;      // Shift the conditional to the low-order bits.
     if (cond == 3) {        // Check valid values for condition 0, 1, & 2.
      fatalError = true;
      errorCode = 2;
      return;
     }
     int accumulator = regAC.getValue();
     if (((accumulator < 0) && (cond == 0))        // Skip if accumulator negative.
          ||((accumulator == 0) && (cond == 1))    // Skip if accumulator zero.
          ||((accumulator > 0) && (cond == 2))) {  // Skip if accumulator positive.
       regPC.setValue(regPC.getValue()+1);
       if (fatalError)
         return; 
       regPC.postActionEvent();
     }
   } // skipCond()


  void jump() { 
/******************************************************************************************
*   Move the value in the instruction's lower 12 bits to the PC (which always             *
*   contains the memory address of the next instruction to execute).                      *
******************************************************************************************/
     int addr = regIR.getValue();
     addr = addr & 0x0FFF;        // Strip the opcode from the instruction,
     regPC.setValue(addr);        // leaving the address.
     if (fatalError)
       return; 
     regPC.postActionEvent();
   } // jump()


  void clear() { 
/******************************************************************************************
*   Clears the accumulator.                                                               *
******************************************************************************************/
     regAC.setValue(0);
     regAC.postActionEvent();
   } // clear()


  void addI() { 
/******************************************************************************************
*   addI adds to the accumulator the value that is found in memory at the location        *
*   pointed to by the memory address in the MBR. So to get the augend, we need to         *
*   get the address of the augend and put it in the MAR, and then pull the                *
*   augend into the MBR so it can be added using the add() instruction.                   *
******************************************************************************************/
     regMAR.setValue(regMBR.getValue());
     if (fatalError) {
       return;
     }
     int addr = regMAR.getValue();
     int memoryRow = addr / 16;
     int memoryCol = addr % 16 + 1;
     try {
           regMBR.setValue(" "+memoryArray[memoryRow][memoryCol]); 
           regMAR.postActionEvent();
           regMBR.postActionEvent();
           add();
     }
     catch (ArrayIndexOutOfBoundsException e) {
             errorCode = 3;
             fatalError = true;
             return;
     }
   } // addI()


  void jumpI() { 
/******************************************************************************************
*   Similar to ADDI, the address of the address to jump to can be found at the            *
*   address to which the MBR is pointing.  After the fetch cycle, the MBR                 *
*   contains the address to jump to.  Notice that unlike the JUMP instruction,            *
*   this instruction takes a memory operand.                                              *
******************************************************************************************/
     regPC.setValue(regMBR.getValue());
     if (fatalError)
       return; 
     regPC.postActionEvent();
   } // jumpI()


/* --                                                                                 -- */
/* --  Marie execution control methods.                                               -- */
/* --                                                                                 -- */
  void runToBreakpoint() {
/******************************************************************************************
*   If we have a runnable program loaded, we will run instructions until we encounter a   *
*   breakpoint or the program terminates.  The tricky part is to capture the condition    *
*   where we have just resumed from a previous breakpoint condition.  This method sets    *
*   a boolean variable, "resumed," to toggle past the first instruction executed after    *
*   a previous breakpoint pause.                                                          *
******************************************************************************************/
     Runnable runIt = new Runnable() {         // Create a thread in which to run.
       int lastStatementRun;                   // Hold the value of the PC for the
       Boolean isBreakpoint;                   // instruction we will run.
       String aString;
       public void run() {
         machineState = MARIE_RUNNING;
         while ((machineState == MARIE_RUNNING) && (!fatalError)) {
           runStop.setEnabled(true);
           aString = regPC.toString().trim();           // Move the cursor.
           if (codeReference.containsKey(aString)) {
             lastStatementRun =((Integer) codeReference.get(aString)).intValue();
           }
           fetchNext();
           try {                              // Give the user a chance to abort and also
             Thread.sleep(delay);             // a chance to see what's happening.
           }
           catch (InterruptedException e) {
           }
           if (!fatalError) {
             execute();
           }
           isBreakpoint = (Boolean) programArray[lastStatementRun][0];
           if ((machineState == MARIE_RUNNING) 
               && (isBreakpoint.booleanValue()))  {    // Check for a breakpoint.
             machineState = MARIE_PAUSED;              // If we find one, pause.
             setStatusMessage(" Stopped for breakpoint."); 
           }
           repaint();   
         } // while
       } // run()
     }; // runIt
   if ((machineState == MARIE_UNINITIALIZED) ||
        (machineState == MARIE_NO_PROGRAM_LOADED))
     return;
   if ((machineState == MARIE_HALTED_NORMAL) ||
        (machineState == MARIE_HALTED_ABNORMAL))
     restart();
   fatalError = false;
   validate();                           // Reset fatal errors.
   breakpointOn = true;
   Thread runThread = new Thread(runIt);         // Run this in a thread.
   runThread.start();                            // Fire it off.
   if (fatalError)                               // Stop on errors.
     halt();
  } // runToBreakpoint()


  void clearBreakPoints() {
/******************************************************************************************
*   Unconditionally removes all breakpoints from the programArray.                        *
******************************************************************************************/
    for (int i = 0; i < codeLineCount; i++)
      programArray[i][0] = new Boolean(false);
    programTable.repaint();
    breakpointOn = false;
} // clearBreakPoints()


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
           fetchNext();
           try {                              // Give the user a chance to abort and also
             Thread.sleep(delay);             // a chance to see what's happening.
           }
           catch (InterruptedException e) {
           }
           if (!fatalError) {
             execute();
           }
         } // while
         if (fatalError) {
           halt();
         }
       } // run()
     }; // runIt

   try {                                  // Give the screen a chance to refresh itself
         Thread.sleep(1000);              // before we start running.
       }
   catch (InterruptedException e) {
       }
   setStatusMessage(" ");
   breakpointOn = false;
   Thread runThread = new Thread(runIt);
   runThread.start();
  } // runProgram()


  public static void main(String args[]) {
    MarieSim sim = new MarieSim();
    sim.show();
  }
} // MarieSim
