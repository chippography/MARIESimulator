// File:        Assembler.java
// Author:      Julie Lobur
// SDK Version: 1.4.1
// Date:        November 9, 2002
// Notice:      Copyright 2003
//              This code may be freely used for noncommercial purposes.
package MarieSimulator;
import java.io.*;
import java.util.*;

public class Assembler implements Serializable {
/****************************************************************************************************
*   This program assembles code written for the MARIE (Machine Architecture that is Really          *
*   Intuitive and Easy) computer as described in *The Essentials of Computer Organization and       *
*   Architecture* by Null & Lobur.  As with most assemblers, this assembler works in two passes     *
*   over the source code:   The first pass performs simple translation of mnemonics to their        *
*   hex equivalent, e.g., LOAD = 1, and adding whatever symbols that it finds to a symbol table     *
*   maintaned by this program.  The first pass of the program creates an intermediate workfile      *
*   of serialized AssembledCodeLine objects, still containing symbolic names instead of memory      *
*   addresses.                                                                                      *
*                                                                                                   *
*   The second pass of the assembler, reads the intermediate workfile and supplies addresses        *
*   for the symbols using addresses found in the symbol table.  (Or producing an error if the       *
*   symbol can't be found.)  The output of this phase is written to another workfile.               *
*                                                                                                   *
*   The final output of the assembler, the "MARIE EXecutable" file is produced from the second-     *
*   pass workfile if there are no errors.  A map of symbol addresses from the symbol table is       *
*   also produced so that it can be used as an online reference when running the MARIE simulator.   *
*   A listing file is produced whether or not the assembly was successful. The output format of     *
*   both the assemby listing and symbol table is HTML to facilitate access within the environment   *
*   of the MARIE simulator.                                                                         *
*                                                                                                   *
*                                                                                                   *
*   Implementation Notes: 1. Unlike "real" assemblers, this assembler does not produce binary       *
*                            machine code (though it would be easy to make it do so).  The          *
*                            output is instead an object stream that is used by both the listing-   *
*                            formatting method (below) and the MARIE simulator program.  The MARIE  *
*                            simulator is the only place where output code from this program will   *
*                            run, so we format our output accordingly.                              *
*                         2. The reader may be curious as to why the author chose to use Java       *
*                            integers instead of Java short integers, which are exactly the         *
*                            same size as a MARIE word.  Java integers were used because they       *
*                            offered the path of least resistance, eliminating the need for         *
*                            type casts throughout programs related to the MARIE system.            *
*                            This decision meant that some fancy footwork had to be done when       *
*                            converting between radices, but overall fewer difficulties were        *
*                            presented by using this approach.                                      *
****************************************************************************************************/
/*
  Notes:   Labels are case sensitive, otherwise codeline is case insensitive.
           Hex literals must begin with a digit, e.g., BABE must be 0BABE.
           Address literals (instruction operands) must be in hex and in MARIE-addressable range.
           Numeric literals must be in the range of -32768 to 32767.  MARIE uses 16 bits only
             so 0 -> 7FFF =      0 to 32767 and 
                8000 -> 0 = -32768 to 0.
*/
/* --                                                                                 -- */
/* --   File definitions.                                                             -- */
/* --                                                                                 -- */
  public BufferedReader      sourceFile = null;   // Buffered reader for source code.
  public File                objectFile = null;   // Object input/output file.
  public ObjectOutputStream  objFileOut = null;   // Object writer for intermediate code.
  public BufferedWriter      lstFile    = null;   // Buffered writer for text output.
  public ObjectInputStream   objFileIn  = null;   // Object reader for intermediate code.
  public BufferedWriter      mapFile    = null;   // Output symbol table reference file.
  public String       sourceFileName    = null;   // Name of sourcefile to process.
  public static final String sourceType = "mas";  // File exentions: MAS = MARIE Source
  public static final String   listType = "lst";  //       LST = Assembler listing (text)
  public static final String    mapType = "map";  //       MAP = Symbol table (text)
  public static final String    exeType = "mex";  //       MEX = Executable for simulator

/* --                                                                                 -- */
/* --  Constants.                                                                     -- */
/* --                                                                                 -- */
  public static final int MAX_MARIE_INT   =  32767;
  public static final int MIN_MARIE_INT   = -32768;
  public static final int MAX_MARIE_ADDR  =   4095;
  public static final int DEC             =     -1;
  public static final int OCT             =     -2;
  public static final int HEX             =     -3;
  public static final int ORG             =     -4;
  public static final int END             =     -5;
  public static final int MAX_SYMBOL_PRINT_LEN  =   24; // Maximum size of symbol when printing
                                                        // symbol table.  All chars significant,
                                                        // only this many shown. 

  public static final int LABEL_DELIM   = (int) ',';   // Punctuation for a label statement.
  public static final int COMMENT_DELIM = (int) '/';   // Punctuation for inline comment.
  public static final String fileSeparator = System.getProperty("file.separator");
  public static final String lineFeed      = System.getProperty("line.separator");
  public static final String formFeed      = "\014";

  public static final String[] errorMsgs = {
           "ORiGination directive must be first noncomment line of program ", //  0
           "A label cannot have 0..9 as its beginning character.",            //  1
           "Statement label must be unique.",                                 //  2
           "Instruction not recognized.",                                     //  3
           "Missing instruction.",                                            //  4
           "Missing operand.",                                                //  5
           "Hex address literal out of range 0 to 0FFF allowable.",           //  6
           "Invalid decimal value: -32768 to 32767 allowable.",               //  7
           "Invalid octal value: 00000 to 177777 allowable.",                 //  8
           "Invalid hexadecimal value: 0 to FFFF allowable.",                 //  9
           "Operand undefined.",                                              // 10
           "Maximum source lines exceeded.  Assembly halted.",                // 11
           "Maximum line number exceeded.  Assembly halted."                  // 12
           };
/* --                                                                                 -- */
/* --   Instance variables                                                            -- */
/* --                                                                                 -- */
                                     // Hashtables are used for the instruction set and the 
                                     // symbol table so that we can easily search them and
                                     // retrieve values.
  public Hashtable symbolTable                    // Initial capacity 18, load factor 0.75.
                           = new Hashtable(18, (float) 0.75);
  public final Hashtable instructionSet = new Hashtable(18);
 
  public int lineNumber;                          // Current instruction address.
  public int errorCount = 0;                      // Total number of errors in assembly.
  public boolean errorFound = false;              // "Temporary" error flag.
  public boolean done;                            // Terminating condition found (e.g.EOF)?
  public ArrayList errorList = new ArrayList();   // Holds text of any errors found.
  public boolean operandReqd = false;             // Does current instruction need an operand?
  public boolean hasLabel = false;                // Is current instruction labeled?
  public int maxSymbolLength = 0;                 // Longest symbol in code (for formatting).

  class SymbolEntry {            
  /******************************************************************************************
  *  Inner class SymbolEntry is the framework for the objects that are used to store and    *
  *  retrieve values that form the Assembler's symbol table.  As these objects are          * 
  *  instantiated, they are placed in a HashTable structure.  As references to the symbols  *
  *  are found, they are added to the SymbolEntry's vector of references.                   *
  ******************************************************************************************/
    String symbol = null;        // The symbol itself.
    String address = null;       // Where it is defined.
    Vector referencedAt;         // List of locations where referenced.

    SymbolEntry(String s, String a) {
      symbol = s;
      address = a;
      referencedAt = new Vector();
    } // SymbolEntry()
  } // SymbolEntry

  class Instruction implements Serializable {
  /******************************************************************************************
  * Inner class Instruction stores the important components of a Marie machine instruction. *
  ******************************************************************************************/
    String mnemonic;             // Instruction mnemonic
    byte hexCode;                // Hex code of instruction
    boolean addrReqd;            // Flag to indicate whether an operand is required.

    Instruction(String mnemonic, byte hexCode, boolean addrReqd) {
      this.mnemonic = mnemonic;
      this.hexCode = hexCode;
      this.addrReqd = addrReqd;
    } // Instruction()
  } // Instruction

/******************************************************************************************
*  Create searchable instruction set hashtable from a stream of literal values.  The      *
*  Instruction objects end up in a Hashtable. The format for this hashtable is given      *
*  in the class definition for Instruction (above).                                       *
******************************************************************************************/
void loadInstructionSet() {
 instructionSet.put("JNS",        new Instruction("JNS",        (byte) 0,   true));
 instructionSet.put("LOAD",       new Instruction("LOAD",       (byte) 1,   true));
 instructionSet.put("STORE",      new Instruction("STORE",      (byte) 2,   true));
 instructionSet.put("ADD",        new Instruction("ADD",        (byte) 3,   true));
 instructionSet.put("SUBT",       new Instruction("SUBT",       (byte) 4,   true));
 instructionSet.put("INPUT",      new Instruction("INPUT",      (byte) 5,   false));
 instructionSet.put("OUTPUT",     new Instruction("OUTPUT",     (byte) 6,   false));
 instructionSet.put("HALT",       new Instruction("HALT",       (byte) 7,   false));
 instructionSet.put("SKIPCOND",   new Instruction("SKIPCOND",   (byte) 8,   true));
 instructionSet.put("JUMP",       new Instruction("JUMP",       (byte) 9,   true));
 instructionSet.put("CLEAR",      new Instruction("CLEAR",      (byte) 10,  false));
 instructionSet.put("ADDI",       new Instruction("ADDI",       (byte) 11,  true));
 instructionSet.put("JUMPI",      new Instruction("JUMPI",      (byte) 12,  true));
 instructionSet.put("DEC",        new Instruction("DEC",        (byte) DEC, true));
 instructionSet.put("OCT",        new Instruction("OCT",        (byte) OCT, true));
 instructionSet.put("HEX",        new Instruction("HEX",        (byte) HEX, true));
 instructionSet.put("ORG",        new Instruction("ORG",        (byte) ORG, true));
 instructionSet.put("END",        new Instruction("END",        (byte) END, false));
} // loadInstructionSet()


/* ------------------------------------------------------------------------------------- */
/* -- Input parsing and output creation                                               -- */
/* ------------------------------------------------------------------------------------- */

String statementLabel(String stmt) {
/******************************************************************************************
*  Looks for label punctuation in the parameter String.  Returns the label if found       *
*  and calls method to add the symbol to the instruction table.                           *
******************************************************************************************/
  String aSymbol = null;
  int i = stmt.indexOf(LABEL_DELIM);         // Find the delimiter.
  if (i < 0)                                 // If none found, we're outta here.
    return " ";
  hasLabel = true;                           // Set this for anything delimited. 
  if (i == 0)                                // Note: Index == 0 => label punct in first
    return " ";                              //         position => null label.

  aSymbol = stmt.substring(0, i);
  char ch = aSymbol.charAt(0);
  if (Character.isDigit(ch)) {
    setErrorMessage(errorMsgs[1]);
    return " ";
  }
  if (!addedToSymbolTable(aSymbol)) {
    return " ";
  }
  return aSymbol;
} // statementLabel()


boolean tokenIsLiteral(String token) {
/******************************************************************************************
* This method determines whether the token passed as a parameter is a valid hex literal.  *
* If a hex literal is used as an address literal (as opposed to a symbolic reference to   *
* an address) the address must begin with a zero, even if it means the literal will be    *
* longer than 3 characters.  (This is the only way we can tell the address A from the     *
* symbol A.)  Note, the check of the token is case insensitive.                           *
******************************************************************************************/
  char[] tokenChars = token.toCharArray();
  if (!Character.isDigit(tokenChars[0]))    // First character of a numeric
   return false;                            // literal must be 0..9.
  for (int i = 0; i < token.length(); i++)  // Now check that the rest of the literal is 
    if (!Character.isDigit(tokenChars[i]))  // a valid hex number.
      switch (tokenChars[i]) {
        case 'A':
        case 'a':
        case 'B':
        case 'b':
        case 'C':
        case 'c':
        case 'D':
        case 'd':
        case 'E':
        case 'e':
        case 'F': 
        case 'f':break;
        default: return false;
      } // switch
    return true;
} // tokenIsLiteral()


int validMarieValue(int number) {
/******************************************************************************************
* Used by the literalToInt() method to check the value of the parameter integer with      *
* respect to the 16-bit word size of Marie.  Specifically, Java values in the 32768 to    *
* 65535 (absolute value) translate to -32768 -> 0 in MARIE memory.  Otherwise, anything   *
* out of the -32768 to 32767 range returns Integer.MAX_VALUE.                             *
******************************************************************************************/
   if ((number >= MIN_MARIE_INT) && (number <= MAX_MARIE_INT))
      return number;

   int absNumber = Math.abs(number);

   if ((absNumber >= MAX_MARIE_INT) && (absNumber <= (2*MAX_MARIE_INT)+1)) 
      return (absNumber - 2*(MAX_MARIE_INT+1));

   return Integer.MAX_VALUE;
} // isValidMarieValue()


int literalToInt(int literalType, String literal, boolean directive) {
/******************************************************************************************
* Converts a String literal to integer.                                                   *
* Parameters:                                                                             *
*     int literalType = DEC, OCT, HEX, ORG, and END (final static int constants),         *
*     the String literal to be converted to an integer, and                               *
*     a boolean to indicate whether the String literal was found in a directive           *
*        statement, such as OCT or HEX, or whether it was found as an address literal     *
*        in an imperative MARIE assembler statement.                                      *
* This method will return Integer.MAX_VALUE to flag any exceptions thrown.  (We can get   *
* away with this because Marie's word size is smaller than Java's.)                       *
******************************************************************************************/
  int result = Integer.MAX_VALUE;

  switch (literalType) {
    case (DEC): {      // DECimal literal.
                 try {
                   result = validMarieValue(Integer.parseInt(literal, 10));
                   if (result == Integer.MAX_VALUE)
                     throw new NumberFormatException();
                 }
                 catch (NumberFormatException e) {
                   setErrorMessage(errorMsgs[7]);
                   result = Integer.MAX_VALUE;
                 }
                 break;
               }
    case (OCT): {      // OCTal literal.
                 try {
                  result = validMarieValue(Integer.parseInt(literal, 8));
                  if (result == Integer.MAX_VALUE)
                     throw new NumberFormatException();
                 }
                 catch (NumberFormatException e) {
                   setErrorMessage(errorMsgs[8]);
                   result = Integer.MAX_VALUE;
                 }
                 break;
               }
    case (HEX):
    case (ORG): {    // HEXadecimal literal or ORiGination directive.
                 try {
                   result = validMarieValue(Integer.parseInt(literal, 16));
                   if (result == Integer.MAX_VALUE)
                     throw new NumberFormatException();
                 }
                 catch (NumberFormatException e) {
                   if (literalType == ORG)
                     setErrorMessage(errorMsgs[6]);
                   else
                     setErrorMessage(errorMsgs[9]);
                   result = Integer.MAX_VALUE;
                 }
                 break;
               }
    case (END):
  } // switch()

  if ( result == Integer.MAX_VALUE )             // If we found an error, we're done.
    return 0; 

  if (!directive) {                              // If the String argument is part of
       if ((result < 0) || (result > MAX_MARIE_ADDR)) {   
         setErrorMessage(errorMsgs[6]);          // an address literal, make sure
         result = 0;                             // the address is within addressible
       }                                         // Marie memory.
  }
  return result;
} // literalToInt()


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


boolean addedToSymbolTable(String symbol) {
/******************************************************************************************
* Returns true if argument symbol (along with the line number where it is defined) is     *
* successfully added to the symbol table.  If the symbol is already in the symbol table,  *
* this method will return false.                                                          *
******************************************************************************************/
  SymbolEntry se;
  if (symbolTable.containsKey(symbol)) {
    setErrorMessage(errorMsgs[2]);
    return false;
  }
  se = new SymbolEntry(symbol, to3CharHexStr(lineNumber));
  symbolTable.put(symbol, se);
  if (symbol.length() > maxSymbolLength)      // Get this size for output formatting
     maxSymbolLength = symbol.length();
  return true;
} // addToSymbolTable()


String getSymbolAddress(String symbol, String referenceLine) {
/******************************************************************************************
* Retrieves the address where the symbol is defined from the symbol table.                *
******************************************************************************************/
  SymbolEntry se;
  String address = null;
  if (symbolTable.containsKey(symbol)) {
     se =(SymbolEntry) symbolTable.get(symbol);
     address = se.address;
     se.referencedAt.add(referenceLine);
     symbolTable.put(symbol, se);
    }
  else 
    setErrorMessage(errorMsgs[10]);
  return address;
} // getSymbolAddress()


String padStr(String s, int size) {
/******************************************************************************************
* Adds trailing blanks to pad the string s to the length (size) specified in the          *
* argument list, if it is longer than "size."  Truncates if shorter.                      *
******************************************************************************************/
  int strLen = s.length();
  if (strLen > size)
    strLen = size;
  StringBuffer sb = new StringBuffer(s.substring(0, strLen));
  for (int i = strLen; i < size; i++)
    sb.append(" ");
  return sb.toString();
} // padStr()


int getOpcode(String stmt) {
/******************************************************************************************
* Tries to find the argument stmt in the Hashmap instructionSet.  If not found, returns   *
* Java Integer.MIN_VALUE (a number we'd never see in a MARIE instruction set).            *
* Also makes a "special case" check that an ORiGination statement must be the first       *
* non-comment line of a MARIE program.                                                    *
******************************************************************************************/
  Instruction instruction;
  int value = 0;

  if (instructionSet.containsKey(stmt)) {                     // Try to find stmt value in
     instruction = (Instruction) instructionSet.get(stmt);    // instruction set.
     operandReqd = instruction.addrReqd;
     value = instruction.hexCode;
     if (instruction.hexCode == ORG) {                        // If found and is an
       if (lineNumber > 0) {                                  // ORiGination, return error
         setErrorMessage(errorMsgs[0]);                       // if not the first non-
         value = Integer.MIN_VALUE;                           // comment line.
       }
     }
  }
  else { 
         setErrorMessage(errorMsgs[3]);                       // Instruction not found.
         value = Integer.MIN_VALUE;
       }
  return value;
} // getOpcode()


void setErrorMessage(String msg) {
/******************************************************************************************
* Increments the error count, sets the error flag and adds the message string of the      *
* argument to the list (Vector) of errors for the current code line being parsed.         *
******************************************************************************************/
  errorCount++;
  errorFound = true;
  errorList.add(msg);
} // setErrorMessage()

/* --                                                                                 -- */
/* --   The "meat" of this program ....                                               -- */
/* --                                                                                 -- */

AssembledCodeLine parseCodeLine(String inputLine) {
/******************************************************************************************
* This method controls extraction of symbols from a single line of source code passed as  *
* a String argument.  The return value is an object composed of the extracted tokens      *
* along with a Vector containing any error messages found, and the input line itself.     *
* If the inputLine is a blank line or a comment, the token-related fields in the returned *
* object are all spaces, and the object is populated only with the source code line       *
* itself.                                                                                 *
*                                                                                         *
* Side effects:                                                                           *
*     As noncomment code lines are processed, the (global) line counter (or address       *
*     value) is incremented.  Also, if any errors are found (such as a missing token),    *
*     the error count  and global error flag are updated through calls to the             *
*     error handler.                                                                      *
******************************************************************************************/

 AssembledCodeLine aCodeLine = new AssembledCodeLine(); // Create the output object
 StringBuffer codeLine = new StringBuffer(" "),  // String buffer for parsing source code.
      instructionLabel = new StringBuffer(" "),  // String buffer for statement label.
               operand = new StringBuffer(" ");  // String buffer for the operand.
  int  instructionCode = 0,
            anIntValue = 0;
  errorList.clear();                             // Reset all short-term error control 
  errorFound = false;                            // fields.
  codeLine.delete(0, codeLine.length());
                                                 // Consider the line only up to any comments.
  int codeLength =  inputLine.indexOf(COMMENT_DELIM); 
  
  if (codeLength < 0)                            // Implies no comment present.
    codeLength = inputLine.length();
  if (codeLength > 0)  {                         // Copy noncomment code to working buffer.
    codeLine.append(inputLine.substring(0, codeLength));
    int lineLength = inputLine.length();         // and save the comment.
    if ((lineLength > 1) && ((lineLength - codeLength) > 0) )
      aCodeLine.comment = inputLine.substring(codeLength, lineLength);
  }

  aCodeLine.sourceLine = inputLine;              // Copy the source to output object.

  instructionLabel.delete(0, instructionLabel.length());
  StringTokenizer st = new StringTokenizer(codeLine.substring(0, codeLine.length()));
  if (st.countTokens()== 0)    {                 // If there are no tokens, we have a blank
    aCodeLine.comment = inputLine;               // line (or comment).  No need to parse it.
    return aCodeLine;
   }
  lineNumber++;                                  // Make sure we haven't exceeded the
  if (lineNumber > MAX_MARIE_ADDR) {             // storage capacity for source code.
    setErrorMessage(errorMsgs[12]);              // If so, halt assembly.
    int size = errorList.size();
    for (int i = 0; i < size; i++) 
       aCodeLine.errors.add((String) errorList.get(i));
    errorFound = true;
    done = true;
    return aCodeLine;
  }
/* --                                                                                 -- */
/* -- Get the label, if any.                                                          -- */
/* --   We assume Assume there is no label, the statementLabel() method sets the      -- */
/* --   hasLabel flag to true if it finds one.                                        -- */
/* --                                                                                 -- */
  String aToken = st.nextToken();
  hasLabel = false;             
  instructionLabel.append(statementLabel(aToken.trim()));
  aCodeLine.stmtLabel = instructionLabel.toString();
/* --                                                                                 -- */
/* -- Get the opcode.  (There better be one at this point.)                           -- */
/* -- Once we have the opcode, we also know whether an operand is needed.             -- */
/* --                                                                                 -- */
  operandReqd = true;                                     // Assume operand is needed.
  if (hasLabel) {                                         // If no label at all,
    if (st.hasMoreTokens()) {                             // get the next token.
       aToken = st.nextToken().toUpperCase();
       instructionCode = getOpcode(aToken); 
       aCodeLine.mnemonic = aToken;
    }                                                     // Otherwise, process the current
    else {                                                // token as an opCode.
       setErrorMessage(errorMsgs[4]);
       operandReqd = false;                               // If no operator, we need
       instructionCode = Integer.MIN_VALUE;               // no operand.
    }
  }
  else {
    instructionCode = getOpcode(aToken.toUpperCase());
    aCodeLine.mnemonic = aToken.toUpperCase();
  }
/* --                                                                                 -- */
/* -- Get the operand.  If no operand is needed, we would have found out when we got  -- */
/* -- the opcode.  In the process of finding the opcode, the operandReqd flag is set  -- */
/* -- based upon the characteristics of the Instruction.                              -- */
/* --                                                                                 -- */
  operand.delete(0, operand.length());
  if (operandReqd)  {
    if (st.hasMoreTokens()) {
      aToken = st.nextToken();
      if ((instructionCode >= ORG) && (instructionCode < 0)) {  // Do we have a "constant" directive?
        anIntValue = literalToInt(instructionCode, aToken.toUpperCase(), true);
        if ((instructionCode > ORG) || (errorFound)) {
          operand.append(to4CharHexStr(anIntValue));
          instructionCode = literalToInt(HEX, operand.substring(0, 1), false);
          operand.deleteCharAt(0);                        // Put first char of literal
          aCodeLine.operandToken = aToken.toUpperCase();  // in instructionCode field          
        }
        else {                                                  // Handle ORiGination
          lineNumber = anIntValue - 1;                          // directive. (No code
          aCodeLine.operandToken = aToken.toUpperCase();        // generated.)
          return aCodeLine;
        }
      }                                                         // If so, get value.
      else {                                                    // Otherwise, we must have an address
        if (tokenIsLiteral(aToken))  {                          // literal or label.
          anIntValue = literalToInt(HEX, aToken, false);
          aToken = to3CharHexStr(anIntValue).toUpperCase();     // Convert literal to uppercase 
          operand.append(aToken);
          aCodeLine.operandToken = aToken;
        }
        else {
          operand.append("_");                                  // Flag token for later lookup.
          operand.append(aToken);
          aCodeLine.operandToken = aToken;
        }
      }
    }
    else {
      setErrorMessage(errorMsgs[5]);
      operand.append("???");
    }
  } // operandReqd
  else operand.append("000");

/* --                                                                                 -- */
/* --  Finish populating the intermediate code object.                                -- */
/* --                                                                                 -- */
  aCodeLine.lineNo = to3CharHexStr(lineNumber);
  if (instructionCode >= 0)
    aCodeLine.hexCode = Integer.toHexString(instructionCode).toUpperCase();
  else if (instructionCode < -15)     // Invalid instruction found.
          aCodeLine.hexCode = "?";
  else if (instructionCode == END) {  // "END" directive has been found.
          aCodeLine.lineNo = "   ";
          aCodeLine.hexCode = " ";    // Clear code line except for directive.
          operand.delete(0, operand.length());
          operand.append("   ");
          done = true;               
       }
  aCodeLine.operand = operand.toString(); 
  if (errorFound) {               // Add any errors found to the output object.
    int last = errorList.size();
    for (int i = 0; i < last; i++) {
       aCodeLine.errors.add((String) errorList.get(i));
    }
  }
  return aCodeLine;
} //  parseCodeLine()


AssembledCodeLine symbolsToAddresses(AssembledCodeLine codeLine) {
/******************************************************************************************
*  This is the second pass of the assembler.  All we need to do is find the addresses of  *
*  the symbols in the assembler program.  We either find them in the symbol table or we   *
*  don't.                                                                                 *
******************************************************************************************/
  String currAddress = codeLine.lineNo;
  errorFound = false;
  errorList.clear();

  if (codeLine.lineNo.charAt(0) == ' ')   // If not an executable statement,
    return codeLine;                      // we don't care about any symbols.

  if (codeLine.operand.indexOf((int) '_') == 0) {
    currAddress = getSymbolAddress
                    (codeLine.operand.substring(1, codeLine.operand.length()), 
                      currAddress);
    if (currAddress != null) {
       codeLine.operand = currAddress;
    }
    else codeLine.operand = "???";    // Error: Symbol not found.
  }
  if (errorFound) {                                   // We have only one possible kind 
     codeLine.errors.add((String) errorList.get(0));  // of error from this pass.
    }
  return codeLine;
} // symbolsToAddresses()


int performFirstPass() {
/******************************************************************************************
*  This method calls methods to open all of the first-pass files, read the source file,   *
*  parse the input, and write the output to the intermediate output file.  When complete, *
*  all related files are closed.                                                          *
*      Note:  A negative return value indicates a critical error in the file handling     *
*             only and has nothing to do with any errors found in the assembly program    *
*             code.                                                                       *
*                                                                                         *
*  When this pass is complete, the "binary" program output is ready for second-pass       *
*  processing.                                                                            *
******************************************************************************************/
  AssembledCodeLine aCodeLine = new AssembledCodeLine();
  done = false;

  errorFound = false;
  loadInstructionSet();
  openFirstPassFiles();
  if (errorFound) {         // Negative return value is fatal error
    return -1;
  }
  while (!done) {           // Loop through source file input.
    try { 
          String inputLine = sourceFile.readLine(); 
          if (inputLine != null) {
            aCodeLine = parseCodeLine(inputLine);
            objFileOut.writeObject(aCodeLine);
          }
          else {
            done = true;
          }
    } // try
    catch (EOFException e) {
      done = true;
    } // catch
    catch (IOException e) {
      System.err.println(e); 
      return -1;
    } // catch
  } // while
  closeFirstPassFiles();
  return 0;
} // performFirstPass()


int performSecondPass() {
/******************************************************************************************
*  This method reads the partially assembled code file produced by the first pass         *
*  and calls the method that supplies addresses for the symbols in that file. The         *
*  The output is <filename>.MEX that is the MARIE  executable used by the  MARIE          *
*  machine simulator.  A file, <filename>.map, containing the symbol table is also        *
*  produced for the user's reference while running the MARIE simulator.                   *
*      Note:  A negative return value indicates a critical error in the file handling     *
*             only and has nothing to do with any errors found in the assembly program    *
*             code.                                                                       *
******************************************************************************************/
  errorFound = false;
  openSecondPassFiles();
  AssembledCodeLine aCodeLine = new AssembledCodeLine();
  done = false;

  if (errorFound) {
    return -1;        // Negative return value is fatal error
  }
  while (!done) {
    try { 
          aCodeLine = (AssembledCodeLine) objFileIn.readObject();
          if (aCodeLine != null) {
            aCodeLine = symbolsToAddresses(aCodeLine);
            objFileOut.writeObject(aCodeLine);
          }
          else
            done = true;
    } // try
    catch (EOFException e) {
      done = true;
    } // catch
    catch (ClassNotFoundException e) {
         done = true;
         System.err.println(e); 
         return -1;
    } // catch
    catch (IOException e) {
      System.err.println(e); 
      return -1;
    } // catch
  } // while
  closeSecondPassFiles();
  return 0;
} // performSecondPass()


void produceFinalOutput() {
/******************************************************************************************
*  This method produces the final outputs from the MARIE assembler, using the ".MO2"      *
*  file as input.  An assembly listing, <filename>.LST is always produced by this step.   *
*  If assembly was error-free, a ".MEX" (MARIE EXecutable) file is produced along with    *
*  a <filename>.MAP file containing the symbol table which can be used for later          *
*  reference when running the simulator.                                                  *
******************************************************************************************/
  AssembledCodeLine aCodeLine = new AssembledCodeLine();
  done = false;
  openFinalFiles();

  int dirEndPos = 0;                              // Strip the path from the fileName
  dirEndPos = sourceFileName.lastIndexOf(fileSeparator); 
  String currFilePrefix = sourceFileName.substring(dirEndPos+1, sourceFileName.length());
  
    try { 
           for (int i = 0; i < 5; i++)                             // Write title heading.
              lstFile.write(" ");
           lstFile.write("Assembly listing for: "+currFilePrefix+"."+sourceType+lineFeed);
           for (int i = 0; i < 5; i++) 
              lstFile.write(" ");
           lstFile.write("           Assembled: "+new Date()+lineFeed);
           lstFile.write(lineFeed);
        }
    catch (IOException e) {
         done = true;
         System.err.println(e);  
    } // catch

  if (maxSymbolLength < 6)                         // Format the symbol printing so that the
    maxSymbolLength = 6;                           // table won't wrap off of the right 
  else if (maxSymbolLength > MAX_SYMBOL_PRINT_LEN) // margin (assuming reasonable font size).
         maxSymbolLength = MAX_SYMBOL_PRINT_LEN;

  while (!done) {
    try {             
         aCodeLine = (AssembledCodeLine) objFileIn.readObject();     // Print a formatted
         if (aCodeLine == null)                                      // line on the listing.
            done = true;
         else {
           lstFile.write(aCodeLine.lineNo+" ");
           lstFile.write(aCodeLine.hexCode);
           lstFile.write(aCodeLine.operand+" | "); 
           lstFile.write(" "+padStr(aCodeLine.stmtLabel, maxSymbolLength));
           lstFile.write(" "+aCodeLine.mnemonic);
           lstFile.write(" "+padStr(aCodeLine.operandToken,   //Put spaces after the operand...
                                 maxSymbolLength+(9-aCodeLine.mnemonic.length())));
           lstFile.write(" "+aCodeLine.comment+lineFeed);     //...so comments will line up.
           for (int i = 0; i < aCodeLine.errors.size(); i++)               // Error list prints
             lstFile.write("   **** " + aCodeLine.errors.get(i)+lineFeed); // for each line.
         }
    } // try
    catch (ClassNotFoundException e) {
         done = true;
         System.err.println(e); 
    } // catch
    catch (EOFException e) {
         done = true;
    } // catch
    catch (IOException e) {
         System.err.println(e); 
         done = true;
    } // catch
    if (done)
       break;
  } // while
  try {             
        lstFile.write(lineFeed);
        if (errorCount > 0) {
          lstFile.write(errorCount + " error");
          if (errorCount > 1) 
            lstFile.write("s");
          lstFile.write(" found.  Assembly unsuccessful."+lineFeed); 
        }
        else
          lstFile.write("Assembly successful."+lineFeed);
        dumpSymbolTable();
   } // try
   catch (IOException e) {
         System.err.println(e); 
         done = true;
   } // catch
  closeFinalFiles();
} // produceFinalOutput()


void dumpSymbolTable() throws IOException {
/******************************************************************************************
* Place the contents of the symbol table, including line number where defined and line    *
* numbers where referenced, on the assembly listing.  We attempt to make a nicely-        *
* formatted table, padding out all symbol names to match the length of the longest name.  *
* The "symbol name" column will be a minumum of 6 characters and a maximum of             *
* MAX_SYMBOL_PRINT_LEN.  Symbols longer than MAX_SYMBOL_PRINT_LEN will be truncated for   *
* printing purposes, but the entire symbol name is significant (up to the limits imposed  *
* by the Java language).                                                                  *
* If assembly was successful, we also write symbol table entries to a plain text          *
* "mapfile" for later reference (by the progammer) while the MARIE program is executing.  *
******************************************************************************************/
  SymbolEntry se;
  int referenceCount  = 0;
  String       indent = "         ";
  ArrayList keyList = new ArrayList();
  Enumeration v, e = symbolTable.elements();
  while (e.hasMoreElements()) {
    se = (SymbolEntry) e.nextElement();
    keyList.add(se.symbol);
  }
  Collections.sort(keyList);                       // Sort the names of the symbols.
  for (int i = 1; i < 2; i++)
    lstFile.write(lineFeed);

  lstFile.write(indent);                           // First heading line.
  lstFile.write("SYMBOL TABLE"+lineFeed);

  lstFile.write(indent+"-------");                 // Second heading line.

  for (int i = 0; i < (maxSymbolLength-5); i++) {
    lstFile.write("-");
  }
  lstFile.write("------------------------------------------"+lineFeed);

  lstFile.write(indent + " Symbol");               // Third heading line. 
  for (int i = 0; i < (maxSymbolLength-5); i++)
    lstFile.write(" ");
  lstFile.write("| Defined | References "+lineFeed);

  lstFile.write(indent+"-------");                // Fourth heading line.
  for (int i = 0; i < (maxSymbolLength-5); i++)
    lstFile.write("-");
  lstFile.write("+---------+-------------------------------");
  
  if (mapFile != null) {                          // Write headings to symbol map file
    mapFile.write(" -----");                      // if assembly was successful.
    for (int i = 0; i < (maxSymbolLength-4); i++)
      mapFile.write("-");
    mapFile.write("----------"+lineFeed);
    mapFile.write(" Symbol");
    for (int i = 0; i < (maxSymbolLength-5); i++)
      mapFile.write(" ");
    mapFile.write("| Location"+lineFeed);
    mapFile.write(" ");  
    mapFile.write("-----");       
    for (int i = 0; i < (maxSymbolLength-4); i++)
       mapFile.write("-");
    mapFile.write("+---------");  
  }
  for (int i = 0; i < keyList.size(); i++) {      // Print table body.
    se = (SymbolEntry) symbolTable.get(keyList.get(i));
    lstFile.write(lineFeed);
    lstFile.write(indent+" "+padStr(se.symbol, maxSymbolLength) + " |   " + se.address+"   | ");
    if (mapFile != null) {
      mapFile.write(lineFeed);
      mapFile.write(       " "+padStr(se.symbol, maxSymbolLength) + " |   " + se.address);
    }
    v = se.referencedAt.elements();
    referenceCount = 0;
    boolean first = true;
    while (v.hasMoreElements()) {
      if (first) {
        lstFile.write(v.nextElement().toString());
        referenceCount++;
        first = false;
      }
      else {
        lstFile.write(", ");
        if ((referenceCount % 6) == 0) {
          lstFile.write(lineFeed);
          lstFile.write(indent+padStr(" ", maxSymbolLength)+"  |"+indent+"| ");
        }
        lstFile.write(v.nextElement().toString());
        referenceCount++;
      }
    }
  }
  lstFile.write(lineFeed);                          // Table bottom.
  lstFile.write(indent+"-------");
  for (int i = 0; i < (maxSymbolLength-5); i++)
    lstFile.write("-");
  lstFile.write("------------------------------------------"+lineFeed);
  lstFile.write(lineFeed);
} // dumpSymbolTable()


/* ------------------------------------------------------------------------------------- */
/* -- Mainline processing.                                                            -- */
/* ------------------------------------------------------------------------------------- */

public static int assembleFile(String fileName) {
/******************************************************************************************
*  This method is the mainline for the MARIE assembler.  It expects to be passed the      *
*  name of a MARIE assembly code file, <filename>, that will be opened as <filename>.MAS. *
*  Ultimately, a <filename>.LST, <filename>.MAP and <filename>.MEX files will be created  *
*  from the <filename>.MAS file.                                                          *
******************************************************************************************/
  Assembler   assembler = new Assembler();
  int         irrecoverableError = 0;
  
  if ( fileName == null) {                                   // Make sure we have an
    System.err.println("\nNull input file to assembler.");   // input file specified.
    return -1;
  }

  int i = fileName.lastIndexOf('.');                      // If the user supplied an
  if (i > 0)                                              // extension to the filename,
    fileName = fileName.substring(0, i);                  // ignore it.

  assembler.sourceFileName = fileName;
  assembler.lineNumber = -1;

  irrecoverableError = assembler.performFirstPass();       // Call functional methods.
  if (irrecoverableError == 0)                             // irrecoverableError(s) occur
    irrecoverableError = assembler.performSecondPass();    // as a result of unexpected
  else {                                                   // file IO problems.
    System.err.println("Irrecoverable IO error occurred during first assembly pass.");
  }
  if (irrecoverableError == 0)
    assembler.produceFinalOutput();
  else {
    System.err.println("Irrecoverable IO error occurred during second assembly pass.");
    return -1;
  }
  return assembler.errorCount;
} // assembleFile()


/* ------------------------------------------------------------------------------------- */
/* -- File handling                                                                   -- */
/* ------------------------------------------------------------------------------------- */

void openFirstPassFiles() {
/******************************************************************************************
* Opens files required by first-pass processing.                                          *
* The filename, sourceFileName (class field), has ".MAS" appended to it prior to any      *
* sourcefile open attempts.  An intermediate file <sourceFileName>.MO1 is created to      *
* hold assembled code "objects" used for input by the second pass of the assembler.       *
******************************************************************************************/
  try {                                             // Try to open the input.
    sourceFile = new BufferedReader( new FileReader(sourceFileName+"."+sourceType) );
  } // try
  catch (FileNotFoundException e) {
    System.err.println(lineFeed+"File " + sourceFileName + "."+sourceType+" not found.");
    errorFound = true;
    return;
  } // catch
  catch (IOException e) {
    System.err.println(lineFeed+e); 
    errorFound = true;
    return;
  } // catch
  try {                                             // Create the intermediate output code.
    objectFile = new File(sourceFileName+".MO1");   // MO1 = Marie Object, first pass
    objFileOut = new ObjectOutputStream( new FileOutputStream(objectFile) );
  } // try
  catch (IOException e) {
    System.err.println(lineFeed+e);
    errorFound = true;
  } // catch
} // openFirstPassFiles()

void closeFirstPassFiles() {
/******************************************************************************************
* Closes files opened in the first pass.                                                  *
******************************************************************************************/
     try {                                             // Close source file.
       sourceFile.close();
     } // try
     catch (IOException e) {
          System.err.println(e); 
     } // catch
     try {                                             // Close intermediate file.
       objFileOut.close();
     } // try
     catch (IOException e) {
          System.err.println(e); 
     } // catch
} // closeFirstPassFiles()


void openSecondPassFiles() {
/******************************************************************************************
*  Opens files required by second-pass processing.                                        *
*  We expect to see an ".M01" file that was created by the first pass.  We will write the *
*  output of this assembler phase to an ".MEX" file, which consists of assembled MARIE    *
*  code lines.                                                                            *
******************************************************************************************/
  try {                                             // Try to open the input.
    objectFile = new File(sourceFileName+".MO1");
    objFileIn = new ObjectInputStream( new FileInputStream(objectFile) );
  } // try
  catch (FileNotFoundException e) {
    System.err.println(lineFeed+"File " + sourceFileName + ".MO1 not found.");
    errorFound = true;
    return;
  } // catch
  catch (IOException e) {
    System.err.println(lineFeed+e); 
    errorFound = true;
    return;
  } // catch
  try {
    objFileOut = new ObjectOutputStream( new FileOutputStream(sourceFileName+"."+exeType) );
  } // try
  catch (IOException e) {
    System.err.println(lineFeed+e);
    errorFound = true;
  } // catch
} // openSecondPassFiles()


void closeSecondPassFiles() {
/******************************************************************************************
* Closes files opened in the second pass, deleting the workfile (<fileName>.MO1) created  *
* by the first pass.                                                                      *
******************************************************************************************/
     try {                                           // Close intermediate file.
       objFileIn.close();                            // and delete it.
       objectFile.delete();
       objectFile = null;
     } // try
     catch (IOException e) {
          System.err.println(e); 
     } // catch
     try {                                           // Close assembled file.
       objFileOut.close();
     } // try
     catch (IOException e) {
          System.err.println(e); 
     } // catch
} // closeSecondPassFiles()


void openFinalFiles() {
/******************************************************************************************
* The "final" assembler phase writes the output to a text listing file and creating the   *
* "binary" output.  Note the objectFile workfile from the second pass is our input.       *
******************************************************************************************/
  try {                                                 // Create the output listing.
    lstFile = new BufferedWriter( new FileWriter(sourceFileName+"."+listType) );
  } // try
  catch (IOException e) {
    System.err.println(lineFeed+e); 
    errorFound = true;
    return;
  } // catch
  try {                                                 // Open output and 
    objectFile = new File(sourceFileName+"."+exeType);  // try to open the input.
    objFileIn = new ObjectInputStream( new FileInputStream(objectFile) );
  } // try
  catch (FileNotFoundException e) {
    System.err.println(lineFeed+"File " + sourceFileName + "."+exeType+" not found.");
    errorFound = true;
    return;
  } // catch
  catch (IOException e) {
    System.err.println(lineFeed+e); 
    errorFound = true;
    return;
  } // catch
  if (errorCount == 0) {                     // If no errors, create a
     try {                                   // symbol table reference file.
       mapFile = new BufferedWriter( new FileWriter(sourceFileName+"."+mapType) );
     } // try
     catch (IOException e) {
       System.err.println(lineFeed+e); 
       errorFound = true;
       return;
     } // catch
  } // if
} // openFinalFiles()

void closeFinalFiles() {
/******************************************************************************************
* Closes files opened in the final pass, deleting the "binary" objectFile if assembly     *
* was unsuccessful.                                                                       *
******************************************************************************************/
     try {                                             // Close listing file.
       lstFile.write(formFeed);                        // We supply a formfeed to please
       lstFile.flush();                                // certain printers that need one.
       lstFile.close();
     } // try
     catch (IOException e) {
          System.err.println(e); 
     } // catch
     try {                                             // Close assembled file.
       objFileIn.close();
     } // try
     catch (IOException e) {
          System.err.println(e);
     } // catch
  if (errorCount == 0) {
     try {
       mapFile.write(lineFeed);
       mapFile.write(formFeed);
       mapFile.flush();                     // Close symbol table reference
       mapFile.close();                     // file if we opened it.
     } // try
     catch (IOException e) {
          System.err.println(e); 
     } // catch
 }
 else {                                    // If the assembly was unsuccessful, delete
    objectFile.delete();                   // the "executable" object file.
 } // else
} // closeFinalFiles()


public static void main(String args[]) {
/******************************************************************************************
*  This main method runs the MARIE assembler in standalone console mode by providing a    *
*  hook to the mainline processing method assembleFile().  We do this so that the         *
*  assembler can be used easily as a class method from another program.                   *
******************************************************************************************/
    assembleFile(args[0]);
  } // main()
} // Assembler
