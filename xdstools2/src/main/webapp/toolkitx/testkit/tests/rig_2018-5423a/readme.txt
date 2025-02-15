Multiple Imaging Document Source Actors (E, F)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <meta http-equiv="content-type" content="text/html;
      charset=windows-1252">
    <title>Responding Imaging Gateway: Multiple Imaging Document Source
      Actors (E, F)</title>
  </head>
  <body>
    <h2>Multiple Imaging Document Source Actors (E, F)</h2>
    <p>Tests the ability of the Responding Imaging Gateway actor (SUT)
      to respond correctly to a Cross Gateway Retrieve Imaging Document
      Set (RAD-75) transaction from a Initiating Imaging Gateway actor
      (Simulator) for DICOM image files from two Imaging Document Source
      actor (Simulators). </p>
    <p>One study is located in Imaging Document Source E, and the second
      study is located in Imaging Document Source F. The Responding
      Imaging Gateway is expected to submit retrieve requests to both
      Imaging Document Source actors and provide a consolidated result.
    </p>
    <h3>Retrieve Parameters</h3>
    <table border="1">
      <tbody>
        <tr>
          <td>RIG Home Community ID</td>
          <td>urn:oid:1.3.6.1.4.1.21367.13.70.201</td>
        </tr>
        <tr>
          <td>IDS Repository Unique ID (E)</td>
          <td>1.3.6.1.4.1.21367.13.71.201.1</td>
        </tr>
        <tr>
          <td>IDS Repository Unique ID (F)</td>
          <td>1.3.6.1.4.1.21367.13.71.201.2</td>
        </tr>
        <tr>
          <td>Transfer Syntax UID</td>
          <td>1.2.840.10008.1.2.1</td>
        </tr>
      </tbody>
    </table>
    <h3>Test Execution</h3>
    <p>The test consists of four steps: </p>
    <ol>
      <li>Test software sends RAD-75 request to System Under test and
        records response. The request contains requests for images in
        two separate DICOM studies found on separate Imaging Document
        Source systems..<br>
      </li>
      <ul>
        <li>System Under Test sends a RAD-69 request to each Imaging
          Document Source which stores the request.</li>
        <li>Imaging Document Source actors provide RAD-69 response to
          System Under Test.</li>
        <li>System Under Test provides RAD-75 response to test software.<br>
        </li>
      </ul>
      <li>Test software validates the <b>RAD-69 requests</b> that are
        sent by the System Under Test.</li>
      <li>Test software validates the RAD-75 response sent by the System
        Under Test.</li>
      <li>Test software validates the image returned in the RAD-75
        response to make sure the System Under Test did not alter the
        image data.<br>
      </li>
    </ol>
  </body>
</html>
