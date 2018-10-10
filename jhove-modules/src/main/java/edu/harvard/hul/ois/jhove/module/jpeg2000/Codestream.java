/**********************************************************************
 * Jhove - JSTOR/Harvard Object Validation Environment
 * Copyright 2004 by JSTOR and the President and Fellows of Harvard College
 *
 **********************************************************************/

package edu.harvard.hul.ois.jhove.module.jpeg2000;

import edu.harvard.hul.ois.jhove.*;
import java.util.*;

/**
 * The information on a codestream, extracted from a Contiguous Codestream
 * or Fragment Table, and the corresponding Codestream Header if any.
 * @author Gary McGath
 *
 */
public class Codestream extends MainOrTile {

    private NisoImageMetadata _niso;
    
    /* List of Tile objects associated with the codestream */
    private List _tiles;
    
    /* List of lengths (Long objects) found in PPM code segments */
    private List _ppmLengthList;
    
    /* Label property */
    private Property _labelProperty;
    
    /* Component mapping property */
    private Property _compMapProperty;

    /* Property generated by the SIZ marker segment */
    private Property _sizProperty;

    /* Property generated by the CRG marker segment */
    private Property _crgProperty;
    
    /* Property generated by the PaletteBox of a CodestreamHeader */
    private Property _paletteProperty;

    /* Property generated by the ROIBox of a CodestreamHeader */
    private Property _roiProperty;
    
    /* List of tile length properties */
    private List tileLengthList;
    
    public Codestream ()
    {
        _precSize = null;

        _niso = new NisoImageMetadata ();
    }


    /** Builds a
     *  Property out of everything we've collected. */
    public Property makeProperty ()
    {
        List propList = new ArrayList (10);
        
        if (_labelProperty != null) {
            propList.add (_labelProperty);
        }
        if (_compMapProperty != null) {
            propList.add (_compMapProperty);
        }
        if (_paletteProperty != null) {
            propList.add (_paletteProperty);
        }
        if (_roiProperty != null) {
            propList.add (_roiProperty);
        }
        if (_sizProperty != null) {
            propList.add (_sizProperty);
        }
        if (_codProperty != null) {
            propList.add (_codProperty);
        }
        if (_qcdProperty != null) {
            propList.add (_qcdProperty);
        }
        if (_pocProperty != null) {
            propList.add (_pocProperty);
        }
        if (tileLengthList != null && !tileLengthList.isEmpty ()) {
            propList.add (new Property ("TileLengths",
                        PropertyType.PROPERTY,
                        PropertyArity.LIST,
                        tileLengthList));
        }
        if (_packetLengthList != null && !_packetLengthList.isEmpty ()) {
            propList.add (new Property ("PacketLengths",
                        PropertyType.LONG,
                        PropertyArity.LIST,
                        _packetLengthList));
        }
        if (_ppmLengthList != null && !_ppmLengthList.isEmpty ()) {
            propList.add (new Property ("PackedPacketHeaderLengths",
                        PropertyType.LONG,
                        PropertyArity.LIST,
                        _ppmLengthList));
        }
        if (_crgProperty != null) {
            propList.add (_crgProperty );
        }
        
        propList.add (new Property ("NisoImageMetadata",
                   PropertyType.NISOIMAGEMETADATA, _niso));
        if (_tiles != null && !_tiles.isEmpty ()) {
            List tpList = new ArrayList (_tiles.size ());
            ListIterator iter = _tiles.listIterator ();
            while (iter.hasNext ()) {
                Tile t = (Tile) iter.next ();
                tpList.add (t.makeProperty ());
            }
            propList.add (new Property ("Tiles",
                    PropertyType.PROPERTY,
                    PropertyArity.LIST,
                    tpList));
        }
        if (!_comments.isEmpty ()) {
            propList.add (new Property ("Comments",
                    PropertyType.PROPERTY,
                    PropertyArity.LIST,
                    _comments));
        }
        if (_components != null) {
            // The component array may be only partially populated, or
            // not at all, so we reduce it to a List.
            List clist = new ArrayList (_components.length);
            for (int i = 0; i < _components.length; i++ ) {
                Property c = _components[i];
                if (c != null) {
                    clist.add (c);
                }
            }
            if (!clist.isEmpty ()) {
                propList.add (new Property ("Components",
                        PropertyType.PROPERTY,
                        PropertyArity.LIST,
                        clist));
            }
        }
        return new Property ("Codestream",
                PropertyType.PROPERTY,
                PropertyArity.LIST,
                propList);
    }


    /** Set the initial Niso values from a default Niso object.
     *  This doesn't attempt to be complete, but sets
     *  the values which we know could have been set from the
     *  JP2 header. */
    public void setDefaultNiso (NisoImageMetadata dNiso)
    {
        _niso.setByteOrder(dNiso.getByteOrder ());
        _niso.setMimeType (dNiso.getMimeType ());
        _niso.setBitsPerSample (dNiso.getBitsPerSample ());
        _niso.setImageLength (dNiso.getImageLength ());
        _niso.setImageWidth (dNiso.getImageWidth ());
        _niso.setSamplesPerPixel (dNiso.getSamplesPerPixel ());
        _niso.setCompressionScheme (dNiso.getCompressionScheme ());
        _niso.setYSamplingFrequency (dNiso.getYSamplingFrequency ());
        _niso.setXSamplingFrequency (dNiso.getXSamplingFrequency ());
        _niso.setSamplingFrequencyUnit (dNiso.getSamplingFrequencyUnit ());
    }
    
    /** Returns the images NisoImageMetadata. */
    public NisoImageMetadata getNiso ()
    {
        return _niso;
    }

    /** Assign a List of Tile objects to the <code>tiles</code> field */
    public void setTiles (List tiles)
    {
        _tiles = tiles;
    }

    
    /** Add a tile length property to the list of tile lengths. */
    public void addTileLength (Property p)
    {
        if (tileLengthList == null) {
            tileLengthList = new LinkedList ();
        }
        tileLengthList.add (p);
    }
    
    /** Add a PPM tilepart header length to the list of lengths */
    public void addPPMLength (long len)
    {
        _ppmLengthList.add (Long.valueOf(len));
    }

    
    /** Sets the label property. */
    protected void setLabelProperty (Property p) 
    {
        _labelProperty = p;
    }

    /** Sets the component mapping property. */
    protected void setCompMapProperty (Property p) 
    {
        _compMapProperty = p;
    }

    /** Sets the palette property. */
    protected void setPaletteProperty (Property p) 
    {
        _paletteProperty = p;
    }

    /** Sets the ROI property. */
    protected void setROIProperty (Property p) 
    {
        _roiProperty = p;
    }

    /** Sets the SIZ property. */
    protected void setSIZProperty (Property p) 
    {
        _sizProperty = p;
    }

    /*  Sets the CRG property. */
    protected void setCRGProperty (Property p)
    {
        _crgProperty = p;
    }
    


    /** Set a property indexed by component.
     *  If a property for that component doesn't already
     *  exist, it is created.  <code>prop</code> is then
     *  added to the property list of that property. */
    public void setCompProperty (int idx, Property prop)
    {
        if (_components != null && _components.length > idx) {
            if (_components[idx] == null) {
                // Have to create the component property
                _components[idx] = new Property ("Component",
                        PropertyType.PROPERTY,
                        PropertyArity.LIST,
                        new LinkedList ());
            }
            List pList = (List) _components[idx].getValue ();
            pList.add (prop);
        }
    }



}
