// File:        AssembledCodeLine.java
// Author:      Julie Lobur
// JDK Version: 1.3.1
// Date:        November 7, 2001
// Notice:      This code may be freely used for noncommercial purposes.
package MarieSimulator;
import java.io.*;
import java.util.*;

public class AssembledCodeLine implements Serializable {
/******************************************************************************************
* Class AssembledCode is used as a working object and the final output of the MARIE       *
* Assembler.  During the second pass, these objects are read and updated using the        *
* values stored in the symbol table, then they are written to another output file.  In    *
* the final assembly step, this output file is read and used to produce the listing and   *
* Marie "machine code" file .                                                             *
******************************************************************************************/
  public String lineNo = "     ",  // All of the principal fields are initialized to blanks
          hexCode      = " ",      // to make report formatting easier later.
          operand      = " ",
          sourceLine   = " ",
          stmtLabel    = " ",
          mnemonic     = " ",
          operandToken = " ",
          comment      = " "; 
  public ArrayList  errors = new ArrayList();  // Note:  The number or error messages is unlimited
} // AssembledCodeLine                         //        though we could have less than 10 max.
