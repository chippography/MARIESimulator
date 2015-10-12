// File:        TextFileViewer.java
// Author:      Julie Lobur
// SDK Version: 1.4.0
// Date:        December 9, 2002
//              Modified April 22, 2006
// Notice:      This program augments the MARIE machine simulator, but can be used for many
//              other purposes.  This code may be freely used for noncommercial purposes.
package MarieSimulator;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import javax.swing.*;
import java.io.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.event.*;
import javax.print.*; 
import javax.print.attribute.*; 
import javax.print.attribute.standard.*; 

public class TextFileViewer extends JFrame {
/******************************************************************************************
*   This program provides utility functions for reading and printing text files using     *
*   the Java component JEditorPane.  Its functionality can be invoked in any of three     *
*   ways:  1. Through a console command:                                                  *
*                      java TextFileViewer <Window title> <FileToDisplay>                 *
*                                                                                         *
*          2. Through a call to the showFile method giving parameters:                    *
*                                                                                         *
*                      String frameTitle, String fileName, boolean exitMode               *
*                                                                                         *
*             where exitMode indicates whether the program should terminate               *
*             (System.exit(0)) or just return to its caller.                              *
*                                                                                         *
*          3. Through a call to the constructor giving parameters:                        *
*                                                                                         *
*                      String frameTitle, String fileName, boolean exitMode               *
*                                                                                         *
*             as explained above.                                                         *
*                                                                                         *
*   If the third method is used, the object created (a subclass of JFrame) created        *
*   can be manipulated by the calling program to change its sizing and position.          *
*                                                                                         *
*   If the TextFileViewer constructor is called using a filetype of HTML (".htm"),        *
*   the file is loaded directly into a JEditorPane with no scrollbar at the bottom,       *
*   allowing the HTML to wrap much as it would in a browser.                              *
*                                                                                         *
*   If the TextFileViewer constructor is called using any filetype except HTML, the       *
*   file contents are assumed to be plaintext.  This plaintext is wrapped in HTML tags    *
*   and rewritten to an intermediate scratchfile which is then used to call the HTML      *
*   JEditorPane file loader.                                                              *
*                                                                                         *
******************************************************************************************/
/* --                                                                                 -- */
/* --    Constants (class field attributes).                                          -- */
/* --                                                                                 -- */
  final static Color    BACKGROUND = new Color(195, 215, 220); // blue-ish
  final static Color    FOREGROUND = Color.black;
  final static Insets      MARGINS = new Insets(5, 5, 10, 5);  // top, left, bottom, right
  final static String HTML_CONTENT = "text/html";
  final static String   ICON_IMAGE = "M.gif";
  final static String HTMLFILE_EXT = ".htm";
  final static String      newLine = System.getProperty("line.separator");
  static String      fileName;

/* --                                                                                 -- */
/* --    Instance variables.                                                          -- */
/* --                                                                                 -- */
  JPanel              outputPane;                       // Container for main screen 
                                                        //       within the JFrame.
  JScrollPane  scrollPane;                              // Container to allow scrolling...
  JEditorPane      displayArea = new JEditorPane();     //   ... of screen text contents.
  static JEditorPane printArea = new JEditorPane();     //   ... a static image for printing
  JPanel       printPanel = new JPanel();               // Container for....
  JButton     printButton = new JButton("Print");       //   ... the print button and
  JButton     closeButton = new JButton("Close");       //   ... the close button.
  boolean     exitOnClose = true;                       // Do we just return to caller 
                                                        //       when done?

  public TextFileViewer(String frameTitle, String aTextFile, boolean exitMode) {
/******************************************************************************************
*   Main GUI constructor takes arguments as explained above.                              *
******************************************************************************************/
    fileName = aTextFile;
    exitOnClose = exitMode;                             // Set closing/program termination 
    addWindowListener(new WindowAdapter() {             // mode.
       public void windowClosing(WindowEvent e) {       // Either we quit or return to the
          if (exitOnClose)                              // calling entity.
             System.exit(0);
          else {
             dispose();
             return;
          }
        }
    });
    setIconImage(Toolkit.getDefaultToolkit().           // Put our icon in the frame.
                               createImage(TextFileViewer.class.getResource(ICON_IMAGE)));
    outputPane = (JPanel) getContentPane();
    outputPane.setLayout(new BorderLayout());           // Set display attributes.
    setTitle(frameTitle);
    displayArea.setEditable(false);
    displayArea.setBackground(BACKGROUND);
    displayArea.setForeground(FOREGROUND);
    displayArea.setMargin(MARGINS);
    displayArea.setContentType(HTML_CONTENT);
   
    if (fileName.endsWith(HTMLFILE_EXT)) {
      getHTMLContent(fileName);
      scrollPane = new JScrollPane(displayArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      setSize(new Dimension(350, 310));
      scrollPane.getViewport().add(displayArea);
      scrollPane.getViewport().setPreferredSize(new Dimension(300, 225));
    }
    else  {
      getTextContent(fileName, this);
      scrollPane = new JScrollPane(displayArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      setSize(new Dimension(350, 360));
      scrollPane.getViewport().add(displayArea);
      scrollPane.getViewport().setPreferredSize(new Dimension(300, 265));
    }
    printPanel.setLayout(new BorderLayout());
    
    printPanel.setPreferredSize(new Dimension(300, 40));
    printButton.setPreferredSize(new Dimension(70, 30));
    printButton.setMinimumSize(new Dimension(70, 30));
    printButton.setMaximumSize(new Dimension(70, 30));
    printButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          printContent();
        }
    });
    printPanel.add(printButton, BorderLayout.WEST);
    
    closeButton.setPreferredSize(new Dimension(70, 30));
    closeButton.setMinimumSize(new Dimension(70, 30));
    closeButton.setMaximumSize(new Dimension(70, 30));
    closeButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
         if (exitOnClose)                              // Do the same thing as
             System.exit(0);                           // window closing event.
          else {
             dispose();
             return;
          }
        }
    });
    printPanel.add(closeButton, BorderLayout.EAST);

    outputPane.add(scrollPane, BorderLayout.CENTER);
    outputPane.add(printPanel, BorderLayout.SOUTH);
  } // TextFileViewer()

  void getHTMLContent(String aFile) {
/******************************************************************************************
*   This method loads the JEditorPane with HTML content found in the filename passed      *
*   as an argument.  When plain text (non-HTML) content needs to be displayed, the        *
*   getTextContent() method should be called first.                                       *
******************************************************************************************/
    try {         
      InputStream fileIn = new FileInputStream(aFile);
      displayArea.read(fileIn, null);
    }
    catch (IOException e) {
      displayArea.setText(newLine+e); 
      return;
    }
  } // getHTMLContent()

  void getTextContent(String aTextFile, Object app) {
/******************************************************************************************
*  This method translates a plain text file to a preformatted HTML file by prepending     *
*  and appending the appropriate tags.  Once these tags have been added, the modified     *
*  text file is used for input by the getHTMLContent() method so that the plain text can  *
*  be displayed properly in a JEditorPane.  When JEditorPane reads a plain text file,     *
*  it exhibits the same "scroll-to-the-bottom" behavior as JTextPane.  HTML content       *
*  prevents this scrolling (!).  The temporary file that is used as input for the HTML    *
*  input is deleted before this method terminates.                                        *
*                                                                                         *
*  April 2006 Update:  The MARIE package has been modified to allow the simulator to      *
*                      run from an executable JAR file. In order to be able to read help  *
*                      files, etc., from the archive, the path qualification has to be    *
*                      removed. However, we still need to be able to read a qualified     *
*                      file name for MARIE assembly listings, etc. This is handled in a   *
*                      try..catch sequence that first tries to open straightforwardly     *
*                      whatever file name is passed to this routine. If that file cannot  *
*                      be found, then we attempt to open it as a "reource," which is how  *
*                      JAR file contents must be accessed. If neither one can be opened,  *
*                      the "file not found" message is displayed.                         *
******************************************************************************************/
        
    BufferedReader textFile = null;
    BufferedWriter tempFile = null;  
    InputStream in;

    boolean done = false;
    in = TextFileViewer.class.getResourceAsStream(aTextFile);
    
    try {                                             // Try to open the input.
      textFile = new BufferedReader( new FileReader(aTextFile) );
         } // try
    catch (FileNotFoundException e) {  
      try {                               // Try to open input as a JAR resource.
        textFile = new BufferedReader(new InputStreamReader(in));      
      } // try
      catch (Exception e1) {        
        displayArea.setText("<HTML>File " + e.getMessage() + " not found.</HTML>");
        return;
      } // catch
    } // catch
    try {                                             // Try to open the output.
      tempFile = new BufferedWriter( new FileWriter("TextFileViewer.out") );
      tempFile.write("<HTML><PRE>");                  // Set initial HTML tags.
    } // try
    catch (FileNotFoundException e) {
      System.err.println(newLine+"Error!  Cannot create file display.");
      return;
    } // catch
    catch (IOException e) {
      System.err.println(newLine+"Error!  Cannot create file display.");
      return;
    } // catch
    while (!done) {                                   // Loop through text file input.
      try {                                           // until end of file found.
          String inputLine = textFile.readLine(); 
          if (inputLine != null) {
             tempFile.write(inputLine+newLine);
          }
          else {
            done = true;
          }
      } // try
      catch (EOFException e) {
        done = true;
      } // catch
      catch (IOException e) {
        done = true;
      } // catch
    } // while
    try {                                             // Close source file.
       textFile.close();                              // Append HTML tags on the output.
       tempFile.write("</PRE></HTML>");
       tempFile.flush();
       tempFile.close();
    } // try
    catch (IOException e) {
       ;
    } // catch
    getHTMLContent("TextFileViewer.out");             // Load the reformatted content
    File aFile = new File("TextFileViewer.out");      // into the display pane and
    aFile.delete();                                   // delete the output file.
  } // getTextContent()

void printContent() { 
/******************************************************************************************
*  This method calls upon SDK 1.4 printing facilities to print the file that was passed   *
*  to the TextFileViewer.  Note:  If the filetype is not supported by the system printer, *
*  some odd-looking output may result.                                                    *
******************************************************************************************/

     DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE; 
     PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet(); 
     PrintService printService [] = PrintServiceLookup.lookupPrintServices(flavor,pras); 
     PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService(); 
     PrintService service =
          ServiceUI.printDialog(null,200,200,printService,defaultService,flavor,pras); 
     if (service != null) { 
        try {
          DocPrintJob job = service.createPrintJob(); 
          FileInputStream fis = new FileInputStream(fileName); 
          DocAttributeSet das = new HashDocAttributeSet(); 
          Doc doc = new SimpleDoc(fis, flavor, das); 
          job.print(doc, pras);
        } // try
        catch (Exception e) {
          ;
       } // catch
     } 
} 

  public static void showFile(String frameTitle, String aTextFile, boolean exitMode) {
/******************************************************************************************
*  This method provides access to the services of this program without the caller         *
*  needing to supply a TextFileViewer object instance.  See above for more detail.        *
******************************************************************************************/
    TextFileViewer tfv = new TextFileViewer(frameTitle, aTextFile, exitMode);
    tfv.show();
  } // showFile()

  public static void main(String args[]) {
/******************************************************************************************
*  This method provides access to the services of this program from the system command    *
*  line. See above for more detail.                                                       *
******************************************************************************************/
    if ( args.length != 2) {                                       // Make sure we have an
      System.err.println                                           // input file and title.
           (newLine+"Usage: java TextFileViewer <frame title> <filename>.");  
      System.exit(-1);
      } // endif
    TextFileViewer tfv = new TextFileViewer(args[0], args[1], true);
    tfv.validate();
    tfv.show();
  } // main()
} // TextFileViewer