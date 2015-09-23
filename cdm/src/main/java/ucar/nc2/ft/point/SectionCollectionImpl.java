/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ft.point;

import ucar.nc2.ft.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.time.CalendarDateUnit;

import java.io.IOException;
import java.util.Iterator;

/**
 * Superclass for implementations of SectionFeatureCollection: series of profiles along a trajectory
 * Concrete subclass must implement getNestedPointFeatureCollectionIterator();
 *
 * @author caron
 * @since Oct 22, 2009
 */


public abstract class SectionCollectionImpl extends MultipleNestedPointCollectionImpl implements SectionFeatureCollection {

  protected SectionCollectionImpl(String name, CalendarDateUnit timeUnit, String altUnits) {
    super(name, timeUnit, altUnits, FeatureType.SECTION);
  }

  public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {
    throw new UnsupportedOperationException("SectionCollectionImpl does not implement getPointFeatureCollectionIterator()");
  }

  public SectionFeatureCollection subset(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException {
    return null;
  }

       /////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Iterator<SectionFeature> iterator() {
    return new SectionFeatureIterator();
  }

  private class SectionFeatureIterator implements Iterator<SectionFeature> {
    NestedPointFeatureCollectionIterator pfIterator;

    public SectionFeatureIterator() {
      try {
        this.pfIterator = getNestedPointFeatureCollectionIterator(-1);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public boolean hasNext() {
      try {
        return pfIterator.hasNext();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public SectionFeature next() {
      try {
        return (SectionFeature) pfIterator.next();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }


  /////////////////////////////////////////////////////////////////////////////////////
  // deprecated
  protected NestedPointFeatureCollectionIterator localIterator;

  public boolean hasNext() throws IOException {
     if (localIterator == null) resetIteration();
     return localIterator.hasNext();
   }

   public SectionFeature next() throws IOException {
     return (SectionFeature) localIterator.next();
   }

   public void resetIteration() throws IOException {
     localIterator = getNestedPointFeatureCollectionIterator(-1);
   }



}
