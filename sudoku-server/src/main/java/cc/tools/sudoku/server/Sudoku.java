package cc.tools.sudoku.server;

import java.io.*;
import java.time.Instant;
import java.util.*;
import java.net.*;
import java.net.http.HttpRequest;
import java.util.logging.*;
import java.util.regex.Pattern;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.*;
import javax.xml.parsers.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * This class implements a dynamic {@link javax.servlet.Servlet} service for
 * accessing Sudoku-Lib.
 * 
 * @author cc
 * @version %I%, %G%
 * @since 0.1
 */
public class Sudoku extends HttpServlet {

  /**
   * Auto-generated serialization ID.
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * Constructor for {@link Sudoku}. 
   * 
   * This method creates the JNI objects used within the class.
   */
  public Sudoku() {
    super();

    try {
      
      System.loadLibrary("sudoku");
      
      _JNIClass = Class.forName("sudoku_jlib");
      
      Class<?> JNIBUfferClass = Class.forName("SWIGTYPE_p_unsigned_char");
      
      Method method = _JNIClass.getMethod("getL_CONST_RESULT_BUFFER_SIZE");
          
      _JNIBufferSize = (int) method.invoke(null);
      
      logInfoMessageToServerLog("JNI buffer size: " + _JNIBufferSize);

      _JNINewMethod = 
          _JNIClass.getMethod("new_uint8Array", 
              new Class<?>[] { int.class } );
      
      _JNIDeleteMethod = 
          _JNIClass.getMethod("delete_uint8Array",
              new Class<?>[] { JNIBUfferClass });
      
      _JNIGetItemMethod = 
          _JNIClass.getMethod("uint8Array_getitem", 
              new Class<?>[] { JNIBUfferClass, int.class });

      _JNIGetMovesMethod = 
          _JNIClass.getMethod("get_sudoku_possible_moves_STUB", 
              new Class<?>[] { String.class , JNIBUfferClass, int[].class });

      _JNIGetSolutionMethod = 
          _JNIClass.getMethod("get_sudoku_solution_STUB", 
              new Class<?>[] { String.class , JNIBUfferClass, int[].class });

      if (_JNIBufferSize > 0 && 
          _JNINewMethod     != null &&
          _JNIDeleteMethod  != null &&
          _JNIGetItemMethod != null &&
          _JNIGetMovesMethod != null && 
          _JNIGetSolutionMethod != null) {
        
        _isValid = true;
      
        logInfoMessageToServerLog("servlet loaded and set to valid");
        
        return;
      } 

      logSevereMessageToServerLog("init failed ");
      
    } catch (Exception e) {
      
      System.err.println("exception: " + e.toString());      
      
      logExceptionToServerLog(e);
    }
  }
  
  /**
   * Method implements this {@link javax.servlet.http.HttpServlet} handler for Get
   * requests. This method overrides {@link javax.servlet.http.HttpServlet} method
   * {@link javax.servlet.http.HttpServlet#doGet(HttpServletRequest, HttpServletResponse)}.
   * 
   * @param request  client http call
   *                 {@link javax.servlet.http.HttpServletRequest} object.
   * @param response client http call
   *                 {@link javax.servlet.http.HttpServletResponse} object.
   * @throws IOException      .
   * @throws ServletException .
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    
    StringBuilder buffer = new StringBuilder();

    String clientIp = request.getRemoteAddr();
    
    String requestURI = request.getRequestURI();

    if (isEndpointSolution(requestURI)) {
      
      if (doEndpointSolution(request, response)) {
      
        logInfoMessageToServerLog("HttpGet Solution OK client " + 
          clientIp);
      
      } else {
        
        logSevereMessageToServerLog("HttpGet Solution NOT ok client " + 
          clientIp);
      }
      
      return;
    }

    if (isEndpointMoves(requestURI)) {
      
      if (doEndpointMoves(request, response)) {
        
        logInfoMessageToServerLog("HttpGet Moves OK client " + 
          clientIp);
        
      } else {
        
        logSevereMessageToServerLog("HttpGet Moves NOT ok client " + 
          clientIp);
      
      }
      
      return;
    }
  
    buffer.append("bad endpoint not in { " + 
      CONSTANT_URI_ENDPOINT_MOVES + 
      "," + 
      CONSTANT_URI_ENDPOINT_SOLUTION + 
      " }");
    
    logSevereMessageToServerLog(buffer.toString() + 
      " to remote ip " + 
      clientIp);
  }
  
  /**
   * Override of 'service' {@link javax.servlet.http.HttpServlet} life cycle
   * method
   * {@link javax.servlet.http.HttpServlet#service(HttpServletRequest, HttpServletResponse)}.
   * This method returns code {@value HttpURLConnection#HTTP_INTERNAL_ERROR} to
   * clients if {@link Sudoku#_isValid} is false. Otherwise it calls the
   * overridden superclass method.
   * 
   * @param request  client {@link javax.servlet.http.HttpServletRequest} object.
   * @param response client {@link javax.servlet.http.HttpServletResponse} object.
   * @throws IOException      .
   * @throws ServletException .
   */
  protected void service(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {

    if (!_isValid) {
      
      response.setStatus(HttpURLConnection.HTTP_INTERNAL_ERROR);
      
      response.getWriter().append(logSevereMessageToServerLog("servlet invalid."));
      
      return;
    }
    
    super.service(request, response);
  }
  
  /**
   * Method calls JNI get solution function and returns result.
   * 
   * @param position a string contaning a sudoku position.
   * @param movesData output buffer to hold possible moves returned by JNI call.
   * @param diagnosticsData output buffer to hold diagnostic message data if error occurs.
   * @param runtimeData output buffer to hold runtime data.
   * @return boolean true, indicating success or false otherwise.
   */
  private boolean doCallJNIMethodMoves(String position, StringBuilder movesData, 
      StringBuilder diagnosticsData, int[] runtimeData) {
    
    Object buffer = doCreateJNIBuffer();    
    
    int result = -1;

    try {
      
      result = (int) _JNIGetMovesMethod.invoke(null, 
          position, buffer, runtimeData);
    
    } catch (Exception e) {
      logExceptionToServerLog(e);
    }
    
    if (result != 0) {
      
      doDeleteJNIBuffer(buffer);

      if (!isWithinIntegerRange(result)) {
      
        diagnosticsData.append("JNI call bad return code.");
        
        return false;      
      }

      diagnosticsData.append(getResponseJNIResultText((int) result));

      return false;      
    }
    
    movesData.append(getStringFromJNIBuffer(buffer));

    doDeleteJNIBuffer(buffer);
    
    return true;
  }
  
  /**
   * Method calls JNI get solution function and returns result.
   * 
   * @param position a string contaning a sudoku position.
   * @param solutionData output buffer to hold solution moves returned by JNI call.
   * @param diagnosticsData output buffer to hold diagnostic message data if error occurs.
   * @param runtimeData output buffer to hold runtime data.
   * @return boolean true, indicating success or false otherwise.
   */
  private boolean doCallJNIMethodSolution(String position, StringBuilder solutionData, 
      StringBuilder diagnosticsData, int[] runtimeData) {
    
    Object buffer = doCreateJNIBuffer();
    
    int result = -1;

    try {
      
      result = (int) _JNIGetSolutionMethod.invoke(null, 
          position, buffer, runtimeData);
    
    } catch (Exception e) {
      logExceptionToServerLog(e);
    }
    
    if (result != 0) {
      
      doDeleteJNIBuffer(buffer);

      if (!isWithinIntegerRange(result)) {
      
        diagnosticsData.append("JNI call bad return code.");
        
        return false;      
      }

      diagnosticsData.append(getResponseJNIResultText((int) result));

      return false;      
    }
    
    solutionData.append(getStringFromJNIBuffer(buffer));

    doDeleteJNIBuffer(buffer);

    return true;
  }
  
  /**
   * Method to create a JNI buffer object for use as JNI output buffer.
   * @return Object output buffer.
   */
  private Object doCreateJNIBuffer() {
    Object jniBuffer = null;
    
    try {
      
      jniBuffer = _JNINewMethod.invoke(null, _JNIBufferSize);
    
    } catch (Exception e) {
      logExceptionToServerLog(e);
    }

    return jniBuffer;
  }
  
  /**
   * Method calls JNI API to delete resources held in JNIBuffer parameter object.
   * 
   * @param JNIBuffer Object to be deleted.
   */
  private void doDeleteJNIBuffer(Object JNIBuffer) {
  
    if (JNIBuffer == null) {
      return;
    }
    
    try {
      _JNIDeleteMethod.invoke(null, JNIBuffer);   
    } catch (Exception e) {
      logExceptionToServerLog(e);
    }
  }

  /**
   * Method implements processing for Get Moves endpoint. 
   * 
   * @param request  client {@link javax.servlet.http.HttpServletRequest} object.
   * @param response client {@link javax.servlet.http.HttpServletResponse} object.
   * @return boolean true indicating success, false otherwise.
   * @throws IOException      .
   * @throws ServletException .
   */
  private boolean doEndpointMoves(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    
    StringBuilder diagnosticsData = new StringBuilder();    
    StringBuilder position        = new StringBuilder();
    StringBuilder movesData       = new StringBuilder();
    
    int[] runtimeData = { -1 };

    if (!getStringFromRequest(request, CONSTANT_HTTP_FIELD_POSITION, true, "", position) ||
        !isCleanPositionString(position.toString())) {
      response.getWriter().append(
          logInfoMessageToServerLog("'position' parameter invalid")); 
      
      return false;
    }
    
    boolean isHtml = !isXml(request);
   
    if (!doCallJNIMethodMoves(position.toString(), movesData, diagnosticsData, runtimeData)) {
      
      if (isHtml) {      
        response.getWriter().append(getResponseJNICallFailHtml(position.toString(), 
            diagnosticsData.toString(), 
            runtimeData[0]));
        
      } else {
        
        if (isPretty(request)) {
          response.getWriter().append(getResponseJNICallFailXmlPretty(position.toString(), 
              new String[] { movesData.toString() }, 
              diagnosticsData.toString(), 
              runtimeData[0]));
          
          response.setContentType("application/xml");
          
        } else {
          response.getWriter().append(getResponseJNICallFailXml(position.toString(), 
              new String[] { movesData.toString() }, 
              diagnosticsData.toString(), 
              runtimeData[0]));
        }
      }
           
      return false;
    }
    
    if (isHtml) {
      response.getWriter().append(
          getResponseJNICallSuccessHtml(position.toString(), 
              getFormattedTextFromAPIXml(movesData.toString(), 1, "m", new int[] { 0, 1 }, "."), 
              runtimeData[0]));      
    } else {
              
      if (isPretty(request)) {  
        response.getWriter().append(
            getResponseJNICallSuccessXmlPretty(position.toString(), 
                new String[] { movesData.toString() }, diagnosticsData.toString(), runtimeData[0]));        
        
        response.setContentType("application/xml");

      } else {
        response.getWriter().append(
            getResponseJNICallSuccessXml(position.toString(), 
                new String[] { movesData.toString() }, diagnosticsData.toString(), runtimeData[0]));
      }
    }
    
    response.setStatus(HttpURLConnection.HTTP_OK);
    
    return true;
  }
    
  /**
   * Method implements processing for Get Solution endpoint. 
   * 
   * @param request  client {@link javax.servlet.http.HttpServletRequest} object.
   * @param response client {@link javax.servlet.http.HttpServletResponse} object.
   * @return boolean true indicating success, false otherwise.
   * @throws IOException      .
   * @throws ServletException .
   */
  private boolean doEndpointSolution(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {

    StringBuilder diagnosticsData = new StringBuilder();    
    StringBuilder position        = new StringBuilder();    
    StringBuilder solutionData    = new StringBuilder();

    int[] runtimeData = { -1 };

    if (!getStringFromRequest(request, CONSTANT_HTTP_FIELD_POSITION, true, "", position) ||
        !isCleanPositionString(position.toString())) {
      response.getWriter().append(
          logInfoMessageToServerLog("'position' not in request")); 
      
      return false;
    }

    boolean isHtml = !isXml(request);
   
    if (!doCallJNIMethodSolution(position.toString(), solutionData, 
        diagnosticsData, runtimeData)) {
      
      if (isHtml) {
        
        response.getWriter().append(getResponseJNICallFailHtml(position.toString(), 
            diagnosticsData.toString(), 
            runtimeData[0]));
      
      } else {
        
        if (isPretty(request)) {
          response.getWriter().append(getResponseJNICallFailXmlPretty(position.toString(), 
              new String[] { solutionData.toString() }, 
              diagnosticsData.toString(), 
              runtimeData[0]));
         
          response.setContentType("application/xml");

        } else {
          response.getWriter().append(getResponseJNICallFailXml(position.toString(), 
              new String[] { solutionData.toString() }, 
              diagnosticsData.toString(), 
              runtimeData[0]));
        }        
      }
      
      return false;
    }

    if (isHtml) {
      response.getWriter().append(
          getResponseJNICallSuccessHtml(position.toString(), 
              getFormattedTextFromAPIXml(solutionData.toString(), 9, "m", new int[] { 1 }, ""), 
              runtimeData[0]));        
    } else {
      
      if (isPretty(request)) {        
        response.getWriter().append(
            getResponseJNICallSuccessXmlPretty(position.toString(), 
                new String[] { solutionData.toString() }, 
                  diagnosticsData.toString(), runtimeData[0]));        
        
        response.setContentType("application/xml");

      } else {
        response.getWriter().append(
            getResponseJNICallSuccessXml(position.toString(), 
                new String[] { solutionData.toString() }, 
                  diagnosticsData.toString(), runtimeData[0]));
      }
    }
    
    response.setStatus(HttpURLConnection.HTTP_OK);
    
    return true;
  }
  
  /**
   * Method returns the boolean in field 'name' from Http request object.
   *
   * @param request client {@link javax.servlet.http.HttpServletRequest} object.
   * @param name field name for which to extract string value.
   * @param isMandatory indicates whether the field value is mandatory. 
   * @param fallback default fallback value to be used when non-mandatory field is empty.
   * @param result buffer in which translated boolean value, 'y' == true, 'n' == false, is returned.
   * @return boolean true, indicating success and false indicating fail.
   */
  private boolean getBooleanFromRequest(HttpServletRequest request, 
      String name, boolean isMandatory, boolean fallback, StringBuilder result) {
    
    String value = request.getParameter(name);

    if (isMandatory &&
        (value == null ||
            value.isEmpty())) {
      return false;
    }
    
    if (value != null &&
        !value.isEmpty()) {
      
      value = value.toLowerCase();

      if (value.equals("yes")  ||
          value.equals("y")    ||
          value.equals("true") ||
          value.equals("t")) {
        
        result.append('y');
        
        return true;
      } 

      if (value.equals("no")    ||
          value.equals("n")     ||
          value.equals("false") ||
          value.equals("f")) {
        
        result.append('n');
        
        return true;
      } 
    }
    
    if (isMandatory) {
      return false;
    }
    
    result.append(fallback ? 'y' : 'n');
    
    return true;
  }
  
  /**
   * Method to extract formatted Board from API space delimited board paramater.
   * 
   * @param boardPositionsText text contaning board.
   * @param countPerRow the number of elements for each row.
   * @return String containing formatted Board.
   */
  private String getFormattedTextFromBoardPosition(String boardPositionsText, int countPerRow) {
    
    StringBuilder buffer = new StringBuilder();
    
    StringTokenizer tokens = new StringTokenizer(boardPositionsText, " "); 
    
    int i = 0;

    while (tokens.hasMoreTokens()) {
      
      buffer.append(tokens.nextToken());

      if ((i + 1) % countPerRow == 0) {
        buffer.append("<br>"); 
      } else {
        buffer.append(" ");
      }

      ++i;        
    }
    
    return buffer.toString();
  }

  /**
   * Method extracts tabular plain text from Xml.
   * 
   * @param responseXmlText response text xml returned by sudoku-lib.
   * @param countPerRow the number of elements for each row.
   * @param recordNodeName main record node.
   * @param childIndexes children positions of main record node to be captured.
   * @param delimiter text used to delimit child indexes output where there is more than one.
   * @return String containing extracted text.
   */
  private String getFormattedTextFromAPIXml(String responseXmlText, int countPerRow, String recordNodeName, 
      int[] childIndexes, String delimiter) {

    try {
      StringBuilder buffer = new StringBuilder();

      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

      Document doc = builder.parse(new InputSource(new StringReader(responseXmlText)));

      doc.getDocumentElement().normalize();

      NodeList nodes = doc.getElementsByTagName(recordNodeName);

      for (int i = 0; i < nodes.getLength(); ++i) {

        Node node = nodes.item(i);

        for (int j = 0; j < childIndexes.length; ++j) {
          if (j > 0) {
            buffer.append(delimiter); 
          }
          
          int childIndex = childIndexes[j];
          
          buffer.append(node.getChildNodes().item(childIndex).
              getTextContent());
        }
        
        if ((i + 1) % countPerRow == 0) {
          buffer.append("<br>"); 
        } else {
          buffer.append(" ");
        }
      }

      return buffer.toString();
      
    } catch (IOException e) {
      logExceptionToServerLog(e);
      
    } catch (SAXException e) {
      logExceptionToServerLog(e);
      
    } catch (ParserConfigurationException e) {
      logExceptionToServerLog(e);
      
    }
    
    return "";
  }

  /**
   * Method returns the Html footer content.
   * 
   * @return Html footer content.
   */
  private String getHtmlTagsFooter() {
    return "</pre>";  
  }

  /**
   * Method returns the Html header content.
   * 
   * @return Html header content.
   */
  private String getHtmlTagsHeader() {
    return "<!DOCTYPE html><pre>";  
  }

  /**
   * Method returns the Html newline content.
   * 
   * @return Html newline content.
   */
  private String getHtmlTagsNewline() {
    return "<br>";  
  }

  /**
   * Method returns formatted request Xml
   * 
   * @param request data for Xml output.
   * @return request Xml as a String
   */
  private String getRequestXml(String request) {
    
    StringBuilder buffer = new StringBuilder();

    buffer.append("<request>");
    
    buffer.append(request);
    
    buffer.append("</request>");

    return buffer.toString(); 
  }
  
  /**
   * Method pretty prints the concated results of all strings in parameter os. 
   * 
   * @param xml xml String to be pretty-printed.
   * @param result results buffer for pretty printed output.
   * @return boolean true if success, false otherwise.
   */
  private boolean getPrettyPrintXml(String xml, StringBuilder result) {
        
    try {
      
      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

      Document doc = builder.parse(new InputSource(new StringReader(xml)));

      doc.getDocumentElement().normalize();
      
      Transformer tf = TransformerFactory.newInstance().newTransformer();
      
      tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      
      tf.setOutputProperty(OutputKeys.INDENT, "yes");
      
      Writer out = new StringWriter();
      
      tf.transform(new DOMSource(doc), new StreamResult(out));
      
      result.append(out.toString());
      
    } catch (Exception e) {
      logExceptionToServerLog(e);
      
      return false;
    }
    
    return true;
  }
  
  /**
   * Method generates Html fail response string.
   * 
   * @param position game position.
   * @param diagnosticsData diagnostic information.
   * @param runtimeData run-time of JBI call.
   * @return String containing formatted output.
   */
  private String getResponseJNICallFailHtml(String position, String diagnosticsData, int runtimeData) {
    StringBuilder buffer = new StringBuilder();
  
    buffer.append(getResponseJNIPrefixHtml(position));
    buffer.append(logSevereMessageToServerLog(diagnosticsData));
    buffer.append(getHtmlTagsNewline());
    buffer.append(getHtmlTagsNewline());
    buffer.append(getRuntimeDataHtml(runtimeData));
    buffer.append(getHtmlTagsFooter());
  
    return buffer.toString();    
  }

  /**
   * Method generates Xml fail response string.
   * 
   * @param request game position.
   * @param jniData jniData returned by JNI API.
   * @param diagnosticsData diagnostic information.
   * @param runtimeData run-time of JBI call.
   * @return String containing formatted output.
   */
  private String getResponseJNICallFailXml(String request, String[] jniData, String diagnosticsData, 
      int runtimeData) {
    
    StringBuilder buffer = new StringBuilder();
    
    buffer.append(getXmlTagsHeader());
    buffer.append(getRequestXml(request));
    
    for (String data : jniData) {
      buffer.append(data);      
    }
  
    buffer.append(getDiagnosticsDataXml(diagnosticsData.toString()));
    buffer.append(getXmlTagsFooter());
    
    return buffer.toString();
  }

  /**
   * Method generates Xml pretty-printed fail response string.
   * 
   * @param position game position.
   * @param jniData jniData returned by JNI API.
   * @param diagnosticsData diagnostic information.
   * @param runtimeData run-time of JBI call.
   * @return String containing formatted output or error message if xml is invalid.
   */
  private String getResponseJNICallFailXmlPretty(String position, String[] jniData, String diagnosticsData, 
      int runtimeData) {
    
    StringBuilder buffer = new StringBuilder();
    
    if (!getPrettyPrintXml(
        getResponseJNICallFailXml(position, jniData, diagnosticsData, runtimeData), 
        buffer)) {
      return "internal error: bad xml.";
    }      
  
    return buffer.toString();
  }

  /**
   * Method generates Html success response string.
   * 
   * @param position game position.
   * @param formattedTextFromAPIXml JNI API call results data in xml format.
   * @param runtimeData run-time of JBI call.
   * @return String containing formatted output.
   */
  private String getResponseJNICallSuccessHtml(String position, String formattedTextFromAPIXml, 
      int runtimeData) {
    
    StringBuilder buffer = new StringBuilder();
  
    buffer.append(getResponseJNIPrefixHtml(position));
    buffer.append(formattedTextFromAPIXml);    
    buffer.append(getHtmlTagsNewline());
    buffer.append(getRuntimeDataHtml(runtimeData));
    buffer.append(getHtmlTagsFooter());
  
    return buffer.toString();    
  }

  /**
   * Method generates Xml success response string.
   * 
   * @param request game position.
   * @param jniData jniData returned by JNI API.
   * @param diagnosticsData diagnostic information.
   * @param runtimeData run-time of JBI call.
   * @return String containing formatted output.
   */
  private String getResponseJNICallSuccessXml(String request, String[] jniData, String diagnosticsData, 
      int runtimeData) {
    
    StringBuilder buffer = new StringBuilder();
  
    buffer.append(getXmlTagsHeader());
    buffer.append(getRequestXml(request));
    
    for (String data : jniData) {
      buffer.append(data);      
    }
    
    buffer.append(getDiagnosticsDataXml(diagnosticsData));
    buffer.append(getXmlTagsFooter());
    
    return buffer.toString();
  }

  /**
   * Method generates Xml pretty-printed success response string.
   * 
   * @param position game position.
   * @param jniData jniData returned by JNI API.
   * @param diagnosticsData diagnostic information.
   * @param runtimeData run-time of JBI call.
   * @return String contaning formatted output or error message if xml is invalid.
   */
  private String getResponseJNICallSuccessXmlPretty(String position, String[] jniData, String diagnosticsData, 
      int runtimeData) {
    
    StringBuilder buffer = new StringBuilder();
    
    if (!getPrettyPrintXml(
        getResponseJNICallSuccessXml(position, jniData, diagnosticsData, runtimeData), 
        buffer)) {
      return "internal error: bad xml.";
    }      
  
    return buffer.toString();
  }

  /**
   * Method generates Html response prefix string.
   *
   * @param position String to be checked.
   * @return string contaning generated string.
   */
  private String getResponseJNIPrefixHtml(String position) {
    StringBuilder buffer = new StringBuilder();
    
    buffer.append(getHtmlTagsHeader());
    buffer.append(getFormattedTextFromBoardPosition(position.toString(), 9));
    buffer.append(getHtmlTagsNewline());
    
    return buffer.toString();
  }

  /**
   * Method returns user response message text for the parameter jni code.
   * 
   * @param jniCode JNI return code. Used to decide which message to log.
   * @return response text.
   */
  private String getResponseJNIResultText(int jniCode) {
  
    if (jniCode < 0) {
      return "JNI call failed."; 
    }
    
    if (jniCode == 0) {
      return "ok.";
    }
  
    switch (jniCode) {
  
    case 1: {
      return "bad parameter. check query position size and elements."; 
    }
  
    case 2: {
      return "setup failed. check query position."; 
    }
  
    case 3: {
      return "solved early. solved before end of setup."; 
    }
  
    case 4: {
      return "no solution.";
    }
  
    case 5: {
      return "timeout. took too long."; 
    }
  
    case 6: {
      return "internal error. something went wrong."; 
    }
  
    case 7: {
      return "shutdown exit. server recievd shutdown command.";
    }
  
    case 8: {
      return "server busy.";
    }
  
    default: {
      return "unknown error. unclassified error number returned.";
    }
  
    }
  }

  /**
   * Method returns formatted String for the runtimeMS parameter
   * 
   * @param runtimeMS runtime to be formatted.
   * @return String containing Html output 
   */
  private String getRuntimeDataHtml(int runtimeMS) {
    
    StringBuilder buffer = new StringBuilder();

    buffer.append("runtime: ");
    
    if (runtimeMS == -1) {
    
      buffer.append("n/a");
    
    } else {
      
      buffer.append(
          Integer.toString(runtimeMS));
    }
    
    buffer.append("ms");
    
    return buffer.toString();
  }
 
  /**
   * Method returns Xml String for the runtimeMS parameter
   * 
   * @param runtimeMS runtime to be formatted.
   * @return String containing runtimeMS Xml output 
   */
  private String getRuntimeDataXml(int runtimeMS) {
    
    StringBuilder buffer = new StringBuilder();

    buffer.append("<runtime>");
    
    if (runtimeMS != -1) {
      buffer.append(
          Integer.toString(runtimeMS));
    }
    
    buffer.append("</runtime>");

    return buffer.toString();
  }
  
  /**
   * Method returns Xml String for the diagnostic parameter
   * 
   * @param diagnostic diagnostic message to be formatted.
   * @return String containing diagnostic Xml output 
   */
  private String getDiagnosticsDataXml(String diagnostic) {
    
    StringBuilder buffer = new StringBuilder();

    buffer.append("<diagnostic>");
    
    buffer.append(diagnostic);
    
    buffer.append("</diagnostic>");

    return buffer.toString();    
  }
  
  /**
   * Method returns a String containing stack trace from Throwable parameter t.
   * 
   * @param throwable {@link java.lang.Throwable} object containing stack trace
   * @return String containing stack trace of throwable parameter
   */
  private String getStackTraceAsString(Throwable throwable) {
    StringWriter sw = new StringWriter();
    
    PrintWriter pw = new PrintWriter(sw, true);
    
    throwable.printStackTrace(pw);
    
    return sw.getBuffer().toString();
  }
  
  /**
   * Method extracts data from JNIBuffer object and returns it as a String.
   * 
   * @param JNIBuffer Object containg data returned from JNI call.
   * @return String containing extracted JNIBuffer data. String is null if and error occurs.
   */
  private String getStringFromJNIBuffer(Object JNIBuffer) {

    StringBuilder buffer = new StringBuilder();
    
    try {
      
      for (int i = 0; i < _JNIBufferSize; ++i) {
        
        short item = (short) _JNIGetItemMethod.invoke(null, JNIBuffer, i);
        
        if (item == 0) {
          break;
        }
        
        buffer.append((char) item);
      }
      
      return buffer.toString();
      
    } catch (Exception e) {
      logExceptionToServerLog(e);
    }
  
    return null;
  }
  
  /**
   * Method returns the string in field 'name' from Http request object.
   * 
   * @param request client {@link javax.servlet.http.HttpServletRequest} object.
   * @param name field name for which to extract String value.
   * @param isMandatory indicates whether the field value is mandatory. 
   * @param fallback default fallback value to be used when non-mandatory field is empty.
   * @param result the result is returned in this field.
   * @return boolean indicating success, true or false otherwise.
   */
  private boolean getStringFromRequest(HttpServletRequest request,
      String name, boolean isMandatory, String fallback, StringBuilder result) {

    String value = request.getParameter(name);

    if (value == null ||
            value.isEmpty()) {
      
      if (isMandatory) {
        return false;        
      }
      
      value = fallback;
    }

    result.append(value);
    
    return true;
  }
  
  /**
   * Method returns the Xml footer content.
   * 
   * @return Xml footer content.
   */
  private String getXmlTagsFooter() {
    return "</sudoku>";  
  }
  
  /**
   * Method returns the Xml header content.
   * 
   * @return Xml header content.
   */
  private String getXmlTagsHeader() {
    return "<sudoku>";  
  }
  
  /**
   * Method check whether position string only contains spaces and numbers 0 to 9.
   * 
   * @param position string to be checked.
   * @return boolean true, if yes, false otherwise.
   */
  private boolean isCleanPositionString(String position) {
    for (char c : position.toCharArray()) {
      if (c != ' ' &&
         !(c >= '0' && c <= '9')) {
        return false;
      }
    }
    
    return true;
  }
  
  /**
   * Method returns boolean indicating whether Uri is the solution endpoint.
   * 
   * @param uri contains Uri path to be tested.
   * @return boolean indicating whether Uri is the solution endpoint.
   **/
  private boolean isEndpointSolution(String uri) {
    return CONSTANT_URI_ENDPOINT_SOLUTION.compareToIgnoreCase(uri) == 0;
  }

  /**
   * Method returns boolean indicating whether Uri is the moves endpoint.
   * 
   * @param uri contains Uri path to be tested.
   * @return boolean indicating whether Uri is the moves endpoint.
   **/
  private boolean isEndpointMoves(String uri) {
    return CONSTANT_URI_ENDPOINT_MOVES.compareToIgnoreCase(uri) == 0;
  }
  
  /**
   * Method determines and returns value of flag in  
   * {@link javax.servlet.http.HttpServletRequest} as boolean
   * 
   * @param request client {@link javax.servlet.http.HttpServletRequest} object.
   * @return boolean indicating format is pretty xml or false for non-pretty xml.
   */
  private boolean isPretty(HttpServletRequest request) {
    StringBuilder buffer = new StringBuilder();
    
    if (!getBooleanFromRequest(request, 
        CONSTANT_HTTP_FIELD_XML_PRETTY, 
        false, 
        false, 
        buffer)) {
      
      logSevereMessageToServerLog("extract 'pretty' in request failed");

      return false;
    }
    
    return buffer.toString().equals("y") ? true : false;
  }
  
  /**
   * Method determines and returns value of flag in  
   * {@link javax.servlet.http.HttpServletRequest} as boolean
   * 
   * @param request client {@link javax.servlet.http.HttpServletRequest} object.
   * @return boolean indicating format is xml or false for html.
   */
  private boolean isXml(HttpServletRequest request) {
    StringBuilder buffer = new StringBuilder();
    
    if (!getBooleanFromRequest(request, 
        CONSTANT_HTTP_FIELD_XML_FORMAT, 
        false, 
        false, 
        buffer)) {
      
      logSevereMessageToServerLog("extract 'xml' in request failed");

      return false;
    }
    
    return buffer.toString().equals("y") ? true : false;
  }
  
  /**
   * Method checks to see if long parameter value is within valid integer range.
   * 
   * @param value value to be checked.
   * @return true if value is a valid integer, false otherwise.
   */
  private boolean isWithinIntegerRange(long value) {
    return value <= Integer.MAX_VALUE && 
        value >= Integer.MIN_VALUE;
  }
  
  /**
   * Method to log {@link Exception} to server log.
   * 
   * @param exception {@link Exception} object to be logged.
   */
  private void logExceptionToServerLog(Exception exception) {
    Logger.getLogger(Sudoku.class.getName()).log(Level.SEVERE,
        "exception: " + exception.getClass().getName() + " - " + exception.getMessage());
    
    Logger.getLogger(Sudoku.class.getName()).log(Level.SEVERE, getStackTraceAsString(exception));
  }

  /**
   * Method to report {@link java.util.logging.Level#INFO} message to server log
   * 
   * @param message {@link String} containing message to be logged.
   * @return message param value is returned for inline use.
   */
  private String logInfoMessageToServerLog(String message) {
    Logger.getLogger(Sudoku.class.getName()).log(Level.INFO, "info: " + message);
    return message;
  }

  /**
   * Method to report {@link java.util.logging.Level#SEVERE} message to server log.
   * 
   * @param message {@link String} containing message to be logged.
   * @return message param value is returned for inline use.
   */
  private String logSevereMessageToServerLog(String message) {
    Logger.getLogger(Sudoku.class.getName()).log(Level.SEVERE, "error: " + message);
    return message;
  }
  
  /**
   * JNI sudoku_jlib class object for JNI calls.
   */
  private Class<?> _JNIClass = null;

  /**
   * JNI delete method to delete JNI buffer.
   */
  private Method _JNIDeleteMethod = null;

  /**
   * JNI get item method to access JNI buffer contents.
   */
  private Method _JNIGetItemMethod = null;
      
  /**
   * JNI get moves method to call API get_moves_STUB.
   */
  private  Method _JNIGetMovesMethod = null;
      
  /**
   * JNI get solution method to call API get_solution_STUB.
   */
  private Method _JNIGetSolutionMethod = null;
      
  /**
   * JNI new method to create JNI buffer.
   */
  private  Method _JNINewMethod = null;

  /**
   * buffer size for JNI buffers.
   */
  private int _JNIBufferSize = 0;
  
  /**
   * boolean indicating whether this {@link Sudoku} object is in a valid state.
   */
  private boolean _isValid = false;
  
  /**
   * Parameter constant for the {@link javax.servlet.http.HttpServlet} query
   * endpoint Uri '{@value CONSTANT_URI_ENDPOINT_MOVES}'.
   */
  final public static String CONSTANT_URI_ENDPOINT_MOVES = "/sudoku/server/game/moves";

  /**
   * Parameter constant for the {@link javax.servlet.http.HttpServlet} get
   * endpoint Uri '{@value CONSTANT_URI_ENDPOINT_SOLUTION}'.
   */
  final public static String CONSTANT_URI_ENDPOINT_SOLUTION = "/sudoku/server/game/solution";

  /**
   * Parameter constant for the {@link javax.servlet.http.HttpServlet} query
   * field containing the input sudoku position.
   */
  final public static String CONSTANT_HTTP_FIELD_POSITION = "position";

  /**
   * Parameter constant for the {@link javax.servlet.http.HttpServlet} query
   * field containing the output format flag.
   * 
   * This can be set to either 'yes' (text: yes/y/true/t) or 'no' (text: no/n/fale/f).
   * 
   * The default output format is 'no' i.e. Html will be output.
   */
  final public static String CONSTANT_HTTP_FIELD_XML_FORMAT = "xml";  
  
  /**
   * Parameter constant for the {@link javax.servlet.http.HttpServlet} query
   * field containing the Xml output format flag.
   * 
   * This can be set to either 'yes' (text: yes/y/true/t) or 'no' (text: no/n/fale/f).
   * 
   * The default output format is 'no' i.e. non-pretty Xml will be output.
   */
  final public static String CONSTANT_HTTP_FIELD_XML_PRETTY = "pretty";  
}
