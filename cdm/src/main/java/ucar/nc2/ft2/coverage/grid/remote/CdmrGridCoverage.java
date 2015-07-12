/* Copyright */
package ucar.nc2.ft2.coverage.grid.remote;

import org.apache.http.Header;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.ft2.coverage.ArrayWithCoordinates;
import ucar.nc2.ft2.coverage.Coverage;
import ucar.nc2.ft2.coverage.CoverageSubset;
import ucar.nc2.stream.NcStream;
import ucar.nc2.stream.NcStreamProto;
import ucar.nc2.util.IO;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.zip.InflaterInputStream;

/**
 * Client side cdmrFeature interface to GridCoverage
 *
 * @author caron
 * @since 5/5/2015
 */
public class CdmrGridCoverage extends Coverage {

  String endpoint;
  HTTPSession httpClient;
  boolean debug = false;
  boolean showRequest = true;

  CdmrGridCoverage(String endpoint, String name, DataType dataType, List<Attribute> atts, String coordSysName, String units, String description) {
    super(name, dataType, atts, coordSysName, units, description);
    this.endpoint = endpoint;
  }

  @Override
  public ArrayWithCoordinates readData(CoverageSubset subset) throws IOException {
    if (httpClient == null)
      httpClient = HTTPFactory.newSession(endpoint);

    Formatter f = new Formatter();
    f.format("%s?req=data&var=%s", endpoint, getName());  // LOOK full vs short name

    for (Map.Entry<String,Object> entry : subset.getEntries()) {
      f.format("&%s=%s", entry.getKey(), entry.getValue());
    }

    if (showRequest)
      System.out.printf(" CdmrFeature data request for gridCoverage: %s%n url=%s%n", getName(), f);

    try (HTTPMethod method = HTTPFactory.Get(httpClient, f.toString())) {
      int statusCode = method.execute();

      if (statusCode == 404)
        throw new FileNotFoundException(getErrorMessage(method));

      if (statusCode >= 300)
        throw new IOException(getErrorMessage(method));

      Header h = method.getResponseHeader("Content-Length");
      if (h != null) {
        String s = h.getValue();
        int readLen = Integer.parseInt(s);
      }

      InputStream is = method.getResponseAsStream();
      if (!NcStream.readAndTest(is, NcStream.MAGIC_DATA))
        throw new IOException("Data transfer corrupted");

      // read Data message
      int psize = NcStream.readVInt(is);
      if (debug) System.out.println("  readData data message len= " + psize);
      byte[] dp = new byte[psize];
      NcStream.readFully(is, dp);
      NcStreamProto.Data dproto = NcStreamProto.Data.parseFrom(dp);
      if (debug) System.out.println(" readData proto = " + dproto);
      DataType dataType = NcStream.convertDataType(dproto.getDataType());
      Section section = NcStream.decodeSection(dproto.getSection());

      // read data message
      int dsize = NcStream.readVInt(is);
      if (debug) System.out.println("  readData data len= " + dsize);
      byte[] datab = new byte[dsize];
      NcStream.readFully(is, datab);
      Array data;

      // is it compressed ?
      NcStreamProto.Compress compress = dproto.getCompress();
      int uncompressedSize = dproto.getUncompressedSize();
      if (compress == NcStreamProto.Compress.DEFLATE) {
        ByteArrayInputStream bin = new ByteArrayInputStream(datab);
        InflaterInputStream in = new InflaterInputStream(bin);
        ByteArrayOutputStream bout = new ByteArrayOutputStream(uncompressedSize);
        IO.copy(in, bout);  // decompress
        byte[] resultb = bout.toByteArray();  // another fucking copy - overrride ByteArrayOutputStream(byte[] myown);
        data = Array.factory(dataType, section.getShape(), ByteBuffer.wrap(resultb)); // another copy, not sure can do anything

      } else {
        data = Array.factory(dataType, section.getShape(), ByteBuffer.wrap(datab));
      }

      if (debug) System.out.println("  readData data len= " + dsize);
      return new ArrayWithCoordinates(data, getCoordSys());
    }

  }

  public Array readSubset(List<Range> subset) throws IOException, InvalidRangeException {
    return null;
  }


  private static String getErrorMessage(HTTPMethod method) {
    String path = method.getURL();
    String status = method.getStatusLine();
    String content = method.getResponseAsString();
    return (content == null) ? path+" "+status : path+" "+status +"\n "+content;
  }
}
