/**********************************************************************
 * Jhove - JSTOR/Harvard Object Validation Environment
 * Copyright 2003 by JSTOR and the President and Fellows of Harvard College
 *
 **********************************************************************/

package edu.harvard.hul.ois.jhove.module.jpeg;

import edu.harvard.hul.ois.jhove.Property;
import edu.harvard.hul.ois.jhove.PropertyArity;
import edu.harvard.hul.ois.jhove.PropertyType;

/**
 * Encapsulation of an SRS (selectively refined scan) entry for a JPEG image.
 * 
 * @author Gary McGath
 *
 */
public class SRS {

    private int _vertOffset;
    private int _horOffset;
    private int _vertSize;
    private int _horSize;
    
    
    /**
     *  Constructor.
     */
    public SRS(int vertOffset, int horOffset, int vertSize, int horSize)
    {
        _vertOffset = vertOffset;
        _horOffset = horOffset;
        _vertSize = vertSize;
        _horSize = horSize;
    }



    /**
     *  Returns a Property defining the SRS
     */
    public Property makeProperty ()
    {
        Property[] parray = new Property[4];
        parray[0] = new Property ("VerticalOffset",
                PropertyType.INTEGER,
                Integer.valueOf(_vertOffset));
        parray[1] = new Property ("HorizontalOffset",
                PropertyType.INTEGER,
                Integer.valueOf(_horOffset));
        parray[2] = new Property ("VerticalSize",
                PropertyType.INTEGER,
                Integer.valueOf(_vertSize));
        parray[3] = new Property ("HorizontalSize",
                PropertyType.INTEGER,
                Integer.valueOf(_horSize));
        return new Property ("SelectivelyRefinedScan",
                PropertyType.PROPERTY,
                PropertyArity.ARRAY,
                parray);
    }

}
