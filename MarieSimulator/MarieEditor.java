// File:        MarieEditor.java
// Author:      Julie Lobur
// SDK Version: 1.4.1
// Date:        November 10, 2002
// Notice:      This program augments the MARIE machine simulator, but can be used for many
//              other purposes.  This code may be freely used for noncommercial purposes.
package MarieSimulator;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.beans.*;

public class MarieEditor extends JFrame {
/******************************************************************************************
*   This program provides simple editing functions for programs that will be used in      *
*   the MARIE machine simulator environment.  The MARIE assembler can be invoked          *
*   through a menu pick on the main menu bar.  If the assembler finds errors, the user    *
*   is notified and a frame displays showing the assembler listing.  The listing may      *
*   also be summoned through a menu option.                                               *
*                                                                                         *
*    This program can be invoked in any of three ways:                                    *
*                                                                                         *
*          1. Through a console command:                                                  *
*                                                                                         *
*                      java MarieEditor                                                   *
*                                                                                         *
*          2. Through a call to the static method:                                        *
*                                                                                         *
*                      editNewFile (boolean exitMode)                                     *
*                                                                                         *
*             where exitMode indicates whether the program should terminate               *
*             (System.exit(0)) or just return to its caller.                              *
*                                                                                         *
*          3. Through a call to the static method:                                        *
*                                                                                         *
*                      editFile(String fileToEdit, boolean exitMode)                      *
*                                                                                         *
*             where exitMode has the same meaning as above.                               *
*                                                                                         *
*   For opening and saving files, we have set the default file filter to look for         *
*   MARIE assembly code files in the user's current directory.  Any text file in any      *
*   directory to which the user has authority may be edited using this editor.            *
*                                                                                         *
*   This program employs a JEditorPane instead of a JTextArea because of the objection-   *
*   able scroll pane behavior of the latter.  Because of this, we have to create our      *
*   own document listener as the Java DocumentListener is not applicable to JEditorPane.  *
*                                                                                         *
*   If the user selects the File | Exit menu option, a wondow-closing event is            *
*   dispatched so that the caller can monitor the event and destroy the editor's          *
*   instance if desired.                                                                  *
*                                                                                         *
******************************************************************************************/
  public static final String fileSeparator = System.getProperty("file.separator");
  public static final String      lineFeed = System.getProperty("line.separator");

  public static final String EDIT_HELP = "m1edit1.txt";
  public static final String INSTR_HELP = "m1isa1.txt";

  JPanel editorPane;                        // Container for all editor components
  JMenuBar menuBar = new JMenuBar();        // Container for the menu as follows:

  JMenu             fileMenu = new JMenu();       // "File" menu
  JMenuItem      newFileItem = new JMenuItem();   //       | new file
  JMenuItem     openFileItem = new JMenuItem();   //       | open file
  JMenuItem    closeFileItem = new JMenuItem();   //       | close
  JMenuItem     saveFileItem = new JMenuItem();   //       | save
  JMenuItem   saveAsFileItem = new JMenuItem();   //       | save as
  JMenuItem         exitItem = new JMenuItem();   //       | exit

  JMenu              editMenu = new JMenu();       // "Edit" menu

  JMenuItem       editCutItem = new JMenuItem();   //       | cut
  JMenuItem     editPasteItem = new JMenuItem();   //       | paste
  JMenuItem editSelectAllItem = new JMenuItem();   //       | select all
  JMenuItem      editUndoItem = new JMenuItem();   //       | undo
  JMenuItem      editRedoItem = new JMenuItem();   //       | redo

  JMenu        assemblerMenu = new JMenu();        // "Assembler" menu
  JMenuItem assembleFileItem = new JMenuItem();    //       | assemble (current) file
  JMenuItem     showListItem = new JMenuItem();    //       | show assembler listing
  TextFileViewer listingFileViewer;                //       |   (listing viewer)

  JMenu             HelpMenu = new JMenu();        // "Help" menu
  JMenuItem   editorHelpItem = new JMenuItem();    //       | Editor help
  TextFileViewer helpViewer01;                     //       |   (editor viewer)
  JMenuItem instrSetHelpItem = new JMenuItem();    //       | Instruction set display
  TextFileViewer helpViewer02;                     //       |   (instruction set viewer)

  JScrollPane scrollPane     = new JScrollPane(); // Container for text that allows scrolling. 
  JEditorPane sourceCodeArea = new JEditorPane(); // Component for displaying & editing text.
  PlainDocument sourceCode;                       // Text contents.

  JTextField messageField = new JTextField();     // Status message display area.

  public static final JFileChooser  sourceFileChooser = 
                    new JFileChooser(System.getProperty("user.dir"));  // Filters for loading

  public static final String ASSEMBLER_FILE_TYPE = ".mas";             // source and listing
  public static final String   LISTING_FILE_TYPE = ".lst";             // files.

  String   currFileName = null;        // File currently loaded.
  String currFilePrefix = null;        // Same as above, but without ".mas" extension.
  String   currFilePath = null;        // The current path, i.e., work directory.
  boolean   fileUpdated = false;       // Toggle for save action.
  boolean defaultFilterOn;             // Toggle for state of chooser file filter.
  boolean   errorsFound = false;       // Indicator of clean/erroneous assembly.
  boolean   exitOnClose = true;        // Says whether we'll exit() or dispose() when done.


  class MarieSourceFileFilter extends javax.swing.filechooser.FileFilter {
/******************************************************************************************
*  Inner class makes sure we can load only file types relevant to editor processing,      *
*  assembler files in particular.  This filter can be overridden by user-selection of     *
*  the wildcard option from the file chooser box.                                         *
******************************************************************************************/
    String myFileType = ASSEMBLER_FILE_TYPE;
    public boolean accept (File file) { 
       if (file.isDirectory())
         return true;
       if (file.getName().endsWith(myFileType))
         return true; 
       else
         return false;
    }
    public String getDescription() { return "*"+myFileType; }
  } // MarieSourceFileFilter

/* --                                                                                 -- */
/* --  Two constructors...                                                                                -- */
/* --                                                                                 -- */
  public MarieEditor(boolean exitMode) {
/******************************************************************************************
*  This constructor sets the exit mode (whether to just close the program or do a         *
*  System.exit) and calls the method to build the GUI interface.                          *
******************************************************************************************/
    exitOnClose = exitMode;
    initializeScreen();
  } // MarieEditor()

  public MarieEditor(String fileToEdit, boolean exitMode) {
/******************************************************************************************
*  This constructor sets the exit mode (whether to just close the program or do a System. *
*  exit) saves a default filename and loads the file after building the GUI screen.       *
******************************************************************************************/
    exitOnClose = exitMode;
    initializeScreen();
    currFileName = fileToEdit;
    getFile();
  } // MarieEditor()


/* --                                                                                 -- */
/* --                   Create the screen...                                          -- */
/* --                                                                                 -- */
  private void initializeScreen() {
/******************************************************************************************
*  Sets the appearance an behavior of the JFrame, and the menu.  Adds listeners to each   *
*  button.  The order in which the components are created is the order in which they      *
*  appear on the screen starting from left to right.                                      *
******************************************************************************************/
    setIconImage(Toolkit.getDefaultToolkit()
                    .createImage(MarieEditor.class      // Set JFrame appearance
                     .getResource("M.gif")));           // and behavior.
    editorPane = (JPanel) this.getContentPane();
    editorPane.setLayout(new BorderLayout());
    this.setSize(new Dimension(700, 400));
    this.setTitle("MARIE Assembler Code Editor");
    
    fileMenu.setText("File");                          // Populate the menu.
    fileMenu.setMnemonic('F');
    newFileItem.setText("New");
    newFileItem.setMnemonic('N');
    newFileItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           newFile();
         }
    });
    openFileItem.setText("Open");
    openFileItem.setMnemonic('O');
    openFileItem.addActionListener(new ActionListener() { 
        public void actionPerformed(ActionEvent e) {
          openFile();
        }
    });
    closeFileItem.setText("Close");
    closeFileItem.setMnemonic('C');
    closeFileItem.addActionListener (new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           newFile();
         }
    });
    saveFileItem.setText("Save");
    saveFileItem.setMnemonic('S');
    saveFileItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           saveFile();
         }
    });
    saveAsFileItem.setText("Save As");
    saveAsFileItem.setMnemonic('A');
    saveAsFileItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          saveAsFile();
        }
    });
    exitItem.setText("Exit");
    exitItem.setMnemonic('x');
    exitItem.addActionListener(new ActionListener() {
       public void actionPerformed(ActionEvent e) {
          exitProgram();
       }
    });
    fileMenu.add(newFileItem);
    fileMenu.add(openFileItem);
    fileMenu.add(closeFileItem);
    fileMenu.add(saveFileItem);
    fileMenu.add(saveAsFileItem);
    fileMenu.addSeparator();
    fileMenu.add(exitItem);

    editMenu.setText("Edit");
    editMenu.setMnemonic('E');

    editCutItem.setText("Cut");
    editCutItem.setMnemonic('C');
    editCutItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
          sourceCodeArea.cut();
          editPasteItem.setEnabled(true);
         }
    });
    editPasteItem.setText("Paste");
    editPasteItem.setMnemonic('P');
    editPasteItem.addActionListener(new ActionListener() { 
        public void actionPerformed(ActionEvent e) {
          sourceCodeArea.paste();
        }
    });
    editSelectAllItem.setText("Select All");
    editSelectAllItem.setMnemonic('A');
    editSelectAllItem.addActionListener (new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           sourceCodeArea.selectAll();
           editPasteItem.setEnabled(true);
         }
    });
    
    editMenu.add(editCutItem);
    editMenu.add(editPasteItem);
    editMenu.add(editSelectAllItem);
    
    assembleFileItem.setText("Assemble current file");
    assembleFileItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           assembleFile();
         }
    });
    assemblerMenu.setText("Assemble");
    assemblerMenu.add(assembleFileItem);

    showListItem.setText("Show assembly listing");
    showListItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           showListing();
         }
    });
    assemblerMenu.add(showListItem);
    HelpMenu.setToolTipText("Instruction set and editor help.");
    HelpMenu.setText("Help");
    editorHelpItem.setText("Editor Instructions");
    editorHelpItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           editHelp();
         }
    });
    HelpMenu.add(editorHelpItem);
    instrSetHelpItem.setText("Instruction Set Cheat Sheet");
    instrSetHelpItem.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           instructionSetHelp();
         }
    });
    HelpMenu.add(instrSetHelpItem);

    menuBar.add(fileMenu);
    menuBar.add(editMenu);
    menuBar.add(assemblerMenu);
    menuBar.add(HelpMenu);
    setJMenuBar(menuBar);

    setButtonsForNewFile();                        // Set buttons on/off appropriately.
/* --                                                                                 -- */
/* --  Set characteristics for the textArea.  Since we are using a JEditorPane        -- */
/* --  instead of a JTextArea we don't have the nifty document listeners at our       -- */
/* --  disposal in this JDK, so we roll our own using key listers.  When a            -- */
/* --  "substantive" change is made to the document, we flag it as modified.          -- */
/* --                                                                                 -- */
    sourceCodeArea.setFont(new Font("Monospaced", 0, 12));
    sourceCodeArea.addKeyListener(new KeyListener() {
      public void keyTyped(KeyEvent e) {
        int c = (int) e.getKeyChar();
        if (c == 27)
         ;        // Do nothing with an escape character
        else {
           fileUpdated = true;
           showMessage(0, createFileMsg());
           setButtonsForModifiedFile(); 
        }
      }
      public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if ((keyCode == 8) || (keyCode == 127)) { // If [BS] or [DEL] pressed
          fileUpdated = true;                     // also flag the update.
          showMessage(0, createFileMsg());
          setButtonsForModifiedFile();
         }
      }
      public void keyReleased(KeyEvent e) { }
    });

    scrollPane.getViewport().add(sourceCodeArea, null);  // Add the text area to
    editorPane.add(scrollPane, BorderLayout.CENTER);     // the screen via the viewport.

    messageField.setFont(new Font("Dialog", 0, 12));     // Set up the message
    messageField.setBorder(BorderFactory                 // feedback field.
                             .createLineBorder(Color.black));
    showMessage(0, " ");
    editorPane.add(messageField, BorderLayout.SOUTH);

    sourceFileChooser.addChoosableFileFilter(new MarieSourceFileFilter());
    sourceFileChooser.addPropertyChangeListener(new PropertyChangeListener() {
       public void propertyChange(PropertyChangeEvent e) {       // If user has selected
          if (sourceFileChooser.getFileFilter() ==               // the "all files"
                   sourceFileChooser.getAcceptAllFileFilter())   // filter, we need to
            defaultFilterOn = true;                              // know about it for
          else                                                   // file saves.
            defaultFilterOn = false;
       }
    });
    this.validate();
    Dimension screenSize = Toolkit                              // Position the screen.
                              .getDefaultToolkit().getScreenSize();
    Dimension myFrameSize = getSize();
    if (myFrameSize.height > screenSize.height) {
      myFrameSize.height = screenSize.height;
    }
    if (myFrameSize.width > screenSize.width) {
      myFrameSize.width = screenSize.width;
    }
    setLocation((screenSize.width - myFrameSize.width) / 2, 
                    (screenSize.height - myFrameSize.height) / 2);
    setVisible(true);

  } // initializeScreen()

  void setButtonsForNewFile() {
/******************************************************************************************
*  Set menu buttons so that they make sense in the context of a blank document.           *
******************************************************************************************/
/* Sets button colors properly for a blank file/screen */
          newFileItem.setEnabled(false);
        closeFileItem.setEnabled(false);
         saveFileItem.setEnabled(false);
       saveAsFileItem.setEnabled(false);

             editMenu.setEnabled(true);
          editCutItem.setEnabled(true);
   if (Toolkit.getDefaultToolkit().getSystemClipboard() == null) 
          editPasteItem.setEnabled(false);
   else
          editPasteItem.setEnabled(true);
    editSelectAllItem.setEnabled(true);

        assemblerMenu.setEnabled(false);
     assembleFileItem.setEnabled(false);
         showListItem.setEnabled(false);
  } // setButtonsForNewFile()


  void setButtonsForLoadedFile() {
/******************************************************************************************
*  Set menu buttons so that they make sense when a file is first loaded .                 *                                                                                    *
******************************************************************************************/
          newFileItem.setEnabled(true);
        closeFileItem.setEnabled(true);
       saveAsFileItem.setEnabled(true);
             editMenu.setEnabled(true);
          editCutItem.setEnabled(true);
    if (Toolkit.getDefaultToolkit().getSystemClipboard() == null)
          editPasteItem.setEnabled(false);
    else
          editPasteItem.setEnabled(true);
    editSelectAllItem.setEnabled(true);
    if ((currFileName!= null) && (currFileName.endsWith(ASSEMBLER_FILE_TYPE))) {
        assemblerMenu.setEnabled(true);
     assembleFileItem.setEnabled(true);
    }
  } // setButtonsForLoadedFile()


  void setButtonsForModifiedFile() {
/******************************************************************************************
*  Set menu buttons so that they make sense when we have sensed a substantive change      * 
*  to the document area.                                                                  *
******************************************************************************************/
          newFileItem.setEnabled(true);
        closeFileItem.setEnabled(true);
         saveFileItem.setEnabled(true);
       saveAsFileItem.setEnabled(true);
             editMenu.setEnabled(true);
          editCutItem.setEnabled(true);
    if (Toolkit.getDefaultToolkit().getSystemClipboard() == null)
          editPasteItem.setEnabled(false);
    else
          editPasteItem.setEnabled(true);
    editSelectAllItem.setEnabled(true);
    if ((currFileName!= null) && (currFileName.endsWith(ASSEMBLER_FILE_TYPE))) {
        assemblerMenu.setEnabled(true);
       assembleFileItem.setEnabled(true);
    }
  } // setButtonsForModifiedFile()


  void openFile() {
/******************************************************************************************
*  Identifies a file to load from disk, but first checks whether a currently-visible      *
*  document has been modified and whether those modifications can be abandoned.  If       *
*  the user decides not to abort the document, this method exits.                         *
*                                                                                         *
*  Next, a JFileChooser appears to allow selection of a file from disk.  This chooser     *
*  has been set so that the current directory is interrogated and only assembler code     *
*  source files appear in the list.  The user can override this and load any other type   *
*  of plain text file.                                                                    *
*                                                                                         *
*  If the JFileChooser.APPROVE_OPTION isn't true, nothing happens.  (The user has         *
*  canceled.)  If it is true, we store the name of the file in the variable currFileName  *
*  and attempt to load the selected file from disk.                                       *
******************************************************************************************/
   if (!abandonOkay()) {
     return;
   }
   if (JFileChooser.APPROVE_OPTION == sourceFileChooser.showOpenDialog(this)) {
     currFileName = sourceFileChooser.getSelectedFile().getPath();
     getFile();
    } // if (opening file)
  } // openFile()


  void getFile() {
/******************************************************************************************
*  Loads a file named currFileName from disk into the document area of the editor.        *
*  If the selected file is assembler source code, we also look for an assembler listing.  * 
******************************************************************************************/
     if (currFileName == null)
       return;
     showMessage(0, currFileName);
     setButtonsForLoadedFile();
     try {         
         sourceCodeArea.setContentType("text/plain");            // Get the source...
         InputStream fileIn = new FileInputStream(currFileName);
         sourceCodeArea.read(fileIn, null);
         }
     catch (IOException e) {
         setButtonsForNewFile();
         sourceCodeArea.setText("");
         currFileName = null;
         sourceCodeArea.setText(lineFeed+e); 
         showMessage(1, "File input error.");
         return;
         }
    int dirEndPos = 0;                              // Strip the path from the fileName
    int extensionStart = 0;                         // to get the filePrefix.
    dirEndPos = currFileName.lastIndexOf(fileSeparator); 
    if (dirEndPos < 0)                              // Also parse the filename to get
      dirEndPos = -1;                               // the file path.
    extensionStart = currFileName.lastIndexOf(ASSEMBLER_FILE_TYPE);
    if (extensionStart < 0) {
      setButtonsForNewFile();
      sourceCodeArea.setText("");
      currFileName = null;
      sourceCodeArea.setText(lineFeed);
      showMessage(1, "Invalid file name.");
      return;
    }
    currFilePrefix = currFileName.substring(dirEndPos+1, extensionStart);
    currFilePath = currFileName.substring(0, dirEndPos) + fileSeparator; 
    showListItem.setEnabled(false);                  // Turn off listing button.  If this
    if (currFileName.endsWith(ASSEMBLER_FILE_TYPE))  // is an assembler file, set button on
      listingAvailable();                            // a listing file exists for this source.
  } // getFile()

  boolean listingAvailable() {
/******************************************************************************************
*  Determines whether an assembly listing exists for the assembler file named by          *
*  currFileName.  If we find it, we simply enable the menu button that displays it.       *
*  Note: currFileName has an ASSEMBER_FILE_TYPE extension, so we have to parse it to      *
*  retrieve the root file name to which a LISTING_FILE_TYPE extension is added before     *
*  we look for the file.                                                                  *
******************************************************************************************/
    try { 
          listingFileViewer.dispose();              // We must kill any windows that may
          listingFileViewer = null;                 // have contained a previous listing.
        }                                           // Otherwise, it will disply the old 
    catch (Exception e) {                           // listing and not the new one.
        }
    if (currFileName == null)
      return false;
    if (currFilePrefix == null)
      return false;
    try {               
          InputStream fileIn = new FileInputStream(currFilePath+currFilePrefix
                                                         +LISTING_FILE_TYPE);
        }
    catch (IOException e) {                       // No listing file available!
          return false;
        }
    showListItem.setEnabled(true);
    return true;
  } // listingAvailable()

  void newFile() {
/******************************************************************************************
*  Clears the input area and resets the filename.  If modified text is in the document    *
*  area, the user is first asked to provide confirmation and given the opportunity to     *
*  save the modified text.                                                                *
******************************************************************************************/
   if (abandonOkay()) {
     sourceCodeArea.setText("");
     setButtonsForNewFile();
     newFileItem.setEnabled(true);
     fileUpdated = false;
     currFileName = null;
     currFilePrefix = null;
     showMessage(0, createFileMsg());
    }
  } // newFile()


  boolean saveFile(){
/******************************************************************************************
*  Saves the current file if its name isn't null.  If the file has the extension of an    *
*  assembly-language file, we activate buttons to permit assembly of the file.  If the    *
*  filename is null, we invoke the "save-as" method.  Whether the save is successful or   *
*  unsuccessful, the user is given feedback in the message area.                          *
******************************************************************************************/
    if (currFileName == null){
      return saveAsFile();
    }
    if (currFileName.endsWith(ASSEMBLER_FILE_TYPE)) {
        assemblerMenu.setEnabled(true);
        assembleFileItem.setEnabled(true);
    }
    try
    {
        File file = new File (currFileName);
        FileWriter fileOut = new FileWriter(file);
        String text = sourceCodeArea.getText();
        fileOut.write(text);
        fileOut.close();
        fileUpdated = false;
        showMessage(0, createFileMsg());
        return true;
      }
      catch (IOException e){
        showMessage(1, "Error saving "+currFileName);
      }
      return false;
    } // newFile() 


    boolean abandonOkay(){
/******************************************************************************************
*  If the text currently in the pane has been modified, display an option pane asking     *
*  the user to confirm that the updates can be abandoned.                                 *
******************************************************************************************/
      if (!fileUpdated) {
        return true;
      }
      int value =JOptionPane.showConfirmDialog(this,"Save changes?","Text Edit",
                     JOptionPane.YES_NO_CANCEL_OPTION);
      switch (value){
        case JOptionPane.YES_OPTION: return saveFile();
        case JOptionPane.NO_OPTION:  return true;
        case JOptionPane.CANCEL_OPTION:
        default:                     return false;
      }
    } // abandonOkay()


  boolean saveAsFile(){
/******************************************************************************************
*  Allows user selection or key entry of a file name to use for saving current edits.     *
*  If the file exists, the overlay confirmation method is invoked.                        *
******************************************************************************************/
    if (JFileChooser.APPROVE_OPTION ==sourceFileChooser.showSaveDialog(this)){
      currFileName =sourceFileChooser.getSelectedFile().getPath();
      if ((!defaultFilterOn) && (!currFileName.endsWith(ASSEMBLER_FILE_TYPE))) 
          currFileName = currFileName + ASSEMBLER_FILE_TYPE;
      listingAvailable();                                          // See if there's a 
      try {                                                        // listing.
          InputStream fileIn = new FileInputStream(currFileName);  // See if the file exists.
      }                                                            // If not, do the save.
      catch (IOException e) {       // Didn't find file on disk.
            return saveFile();
      } // catch
      if (overlayOkay())            // Found it, but overwrite okay.
        return saveFile();
    } // if
    this.repaint();                 // User canceled.
    return false;
  } //  saveAsFile()


  boolean overlayOkay() {
/******************************************************************************************
*  Returns true or false to confirm overlay of a file, based on value returned from       *
*  a JOptionPane.                                                                         *
******************************************************************************************/
    JFrame closingFrame = new JFrame("Confirm Overwrite");
    closingFrame.setIconImage(Toolkit.getDefaultToolkit()
                      .createImage(MarieSim.class.getResource("M.gif")));
    int option = JOptionPane
                    .showOptionDialog(closingFrame, "Rewrite "+currFileName+"?",
                                      "Rewrite Confirmation", JOptionPane.YES_NO_OPTION,
                                      JOptionPane.QUESTION_MESSAGE, null,
                                       new Object[] {"Yes", "No"}, "No");
    if (option == JOptionPane.NO_OPTION)
      return false;
    else
      return true;
  } // overlayOkay()


  String createFileMsg(){
/******************************************************************************************
*  Creates a string containing the state of the currently-loaded file.  This message is   *
*  displayed through the showMessage() method below.                                      *
******************************************************************************************/
    String message;
    if (currFileName == null){
       message ="Untitled";
    }
    else {
       message = currFileName;
    }
    if (fileUpdated){
      message = message+" (modified)";
    }
    return message;
  } // createFileMsg()


  void showMessage(int severity, String message) {
/******************************************************************************************
*  Places the parameter string in the message (text) field at the bottom of the editor    *
*  in a color appropriate to the severity code.                                           *
******************************************************************************************/
    switch (severity) {
      case 1: messageField.setBackground(Color.orange);
              break;
      case 2: messageField.setBackground(Color.yellow);
              break;
     default: messageField.setBackground(Color.lightGray);
              break;
    } // switch
      messageField.setText(" "+message);
      messageField.postActionEvent();
  } // showMessage()


  void assembleFile() {
/******************************************************************************************
*  Saves the current edits (if needed) and invokes the Assembler's static assembleFile    *
*  method, which returns the total number of errors found during assembly.  This method   *
*  then uses this error count to display a status message.  If errors were detected       *
*  during assembly, the showListing() method is invoked.                                  *
******************************************************************************************/
    boolean okayToBuild = true;
    if (fileUpdated)
      okayToBuild = saveFile();
    if (!okayToBuild) {
      showMessage(1, "Fatal error saving file.  Use [Save As].");
      return;
    }
    if (currFilePrefix == null) {                   // If this is a new file, we need to 
      getFile();                                    // parse the filename to get the file
      if (currFilePrefix == null) {                 // prefix. If we still have an error, 
        showMessage(2, " No MAS file loaded.");     // then the file type is wrong or no
        return;                                     // file is loaded.
      }
    }
    int errors = Assembler.assembleFile(currFileName);
    if (errors < 0) { 
      showMessage(1, createFileMsg()+" Fatal error in assembler.");
      return;
    }
    if (errors > 0) {
      if (errors == 1) 
        showMessage(2, createFileMsg()+" "+errors+" error found.");
      else
        showMessage(2, createFileMsg()+" "+errors+" errors found.");
      showListing();
    }
    else
      showMessage(0, createFileMsg()+" Assembly successful.");
    showListItem.setEnabled(true);
  } // assembleFile()


  void showListing() {
/******************************************************************************************
*  Uses the root filename of the current file concatenated with a listing suffix to       *
*  create an instance of the TextFileViewer.  The TextFileViewer constructor is passed    *
*  a string to place in the title of its frame, the filename to display and a boolean     *
*  that indicates whether TextFileViewer should call a System.exit(0) when it terminates. *
******************************************************************************************/
    if (!listingAvailable()) {                     // Should never need this
      showListItem.setEnabled(false);              // error trap.  If we do,
      return;                                      // this saves an ugly display.
    }
    try { 
          listingFileViewer.show();
          listingFileViewer.requestFocus();
    }
    catch (Exception e) {
          listingFileViewer  = new TextFileViewer(
                               "Assembly Listing for "+currFilePrefix+ASSEMBLER_FILE_TYPE, 
                                currFilePath + currFilePrefix+LISTING_FILE_TYPE, 
                                false);
                                             
          Dimension myFrameSize = this.getSize();
          Dimension tfvSize = listingFileViewer.getPreferredSize();
          Point location = getLocation();
          listingFileViewer.setLocation(((myFrameSize.width+(location.x/3)) / 2), 
                                         (location.y / 3));
          listingFileViewer.show();
    }
  } // showListing()


  void editHelp() {
/******************************************************************************************
*  Create an instance of the TextFileViewer to display the help file.  We pass a string   *
*  to be used for the frame title, the filename to display and a boolean that indicates   *
*  whether TextFileViewer should call a System.exit(0) when it terminates.                *
******************************************************************************************/
   try { 
          helpViewer01.show();
          helpViewer01.requestFocus();
    }
    catch (Exception e) {
      helpViewer01 = new TextFileViewer("MARIE Editor Help", EDIT_HELP, false);
      Dimension myFrameSize = this.getSize();
      Dimension tfvSize = helpViewer01.getPreferredSize();
      Point location = getLocation();
      helpViewer01.setLocation(((myFrameSize.width+(location.x/2)) / 2), (location.y / 2));
      helpViewer01.show();
    }
  } // editHelp()


  void instructionSetHelp() {
/******************************************************************************************
*  Same function as editHelp(), but use the instruction set help file instead.            *
******************************************************************************************/
    try { 
          helpViewer02.show();
          helpViewer02.requestFocus();
    }
    catch (Exception e) {
          helpViewer02 = new TextFileViewer("MARIE Instruction Set", INSTR_HELP, false);
          Dimension myFrameSize = this.getSize();
          Dimension tfvSize = helpViewer02.getPreferredSize();
          Point location = getLocation();
          helpViewer02.setLocation(((myFrameSize.width+(location.x/3)) / 2), (location.y / 3));
          helpViewer02.show();
    } // catch
  } // instructionSetHelp() 


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
    JFrame closingFrame = new JFrame("Confirm Editor Quit");
    closingFrame.setIconImage(Toolkit.getDefaultToolkit()
                      .createImage(MarieSim.class.getResource("M.gif")));
    int option = JOptionPane
                    .showOptionDialog(closingFrame, "Really quit editor?",
                                      "Quit Editor Confirmation", JOptionPane.YES_NO_OPTION,
                                      JOptionPane.QUESTION_MESSAGE, null,
                                       new Object[] {"Yes", "No"}, "Yes");
    if (option == JOptionPane.YES_OPTION) {
       try { 
             listingFileViewer.dispose();         // Before we go, kill listing window
       }                                          // if it exists.
       catch (Exception e) {
       }
       if (exitOnClose)          
          System.exit(0);                         // We terminate or return
       else {                                     // depending on exitMode     
          dispose();                              // used by the constructors.
          return;
       }
    }  
 } // exitProgram()
  
  
  public static void editNewFile(boolean exitMode) { 
/******************************************************************************************
*  Static method is callable from other objects.  Creates an instance of the editor with  *
*  no file loaded.  (A blank text pane.)                                                  *
******************************************************************************************/
    new MarieEditor(exitMode);
  } // editFile()


  public static void editFile(String fileToEdit, boolean exitMode) { // This method is invoked when called from outside.
/******************************************************************************************
*  Same as editNewFile() except it invokes a constructor that loads the file given in     *
*  the parameter list (If it can be found.)                                               *
******************************************************************************************/
    new MarieEditor(fileToEdit, exitMode);
  } // editFile()


  /**Main method*/
/******************************************************************************************
*  The presence of this main() method permits this program to be used in "console mode."  *
******************************************************************************************/
  public static void main(String[] args) {
    new MarieEditor(true);
  } // main()
} // MarieEditor