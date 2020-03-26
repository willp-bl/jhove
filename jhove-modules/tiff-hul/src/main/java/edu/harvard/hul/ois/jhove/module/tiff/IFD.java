/**********************************************************************
 * Jhove - JSTOR/Harvard Object Validation Environment
 * Copyright 2004 by JSTOR and the President and Fellows of Harvard College
 **********************************************************************/

package edu.harvard.hul.ois.jhove.module.tiff;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import edu.harvard.hul.ois.jhove.ErrorMessage;
import edu.harvard.hul.ois.jhove.InfoMessage;
import edu.harvard.hul.ois.jhove.ModuleBase;
import edu.harvard.hul.ois.jhove.Property;
import edu.harvard.hul.ois.jhove.PropertyArity;
import edu.harvard.hul.ois.jhove.PropertyType;
import edu.harvard.hul.ois.jhove.Rational;
import edu.harvard.hul.ois.jhove.RepInfo;
import edu.harvard.hul.ois.jhove.messages.JhoveMessage;
import edu.harvard.hul.ois.jhove.messages.JhoveMessages;

/**
 * Encapsulation of a TIFF image file directory (IFD).
 */
public abstract class IFD
{

    /******************************************************************
     * DEBUGGING FIELDS.
     * All debugging fields should be set to false for release code.
     ******************************************************************/

    /* Set to true to allow out-of-sequence tags. */
    private static final boolean DEBUG_ALLOW_OUT_OF_SEQUENCE = false;

    /******************************************************************
     * PUBLIC CLASS FIELDS.
     ******************************************************************/

    /** Standard TIFF IFD. */
    public static final int TIFF = 0;
    /** Exif IFD. */
    public static final int EXIF = 1;
    /** Exif Interoperability IFD. */
    public static final int INTEROPERABILITY = 2;
    /** GPSInfo IFD. */
    public static final int GPSINFO = 3;
    /** Global parameters IFD. */
    public static final int GLOBALPARAMETERS = 4;


    /** Undefined value for integer tags. */
    public static final int NULL = -1;

    /******************************************************************
     * PRIVATE CLASS FIELDS.
     ******************************************************************/

    /* TIFF data types. */

    /** TIFF BYTE (unsigned 8-bit) type. */
    public static final int BYTE = 1;
    /** TIFF ASCII type. */
    public static final int ASCII = 2;
    /** TIFF SHORT (unsigned 16-bit) type. */
    public static final int SHORT = 3;
    /** TIFF LONG (unsigned 32-bit) type. */
    public static final int LONG = 4;
    /** TIFF RATIONAL (two LONGs) type. */
    public static final int RATIONAL = 5;
    /** TIFF SBYTE (signed 8-bit) type. */
    public static final int SBYTE = 6;
    /** TIFF UNDEFINED (unsigned 8-bit) type. */
    public static final int UNDEFINED = 7;
    /** TIFF SSHORT (signed 16-bit) type. */
    public static final int SSHORT = 8;
    /** TIFF SLONG (signed 32-bit) type. */
    public static final int SLONG = 9;
    /** TIFF SRATIONAL (two SLONGs) type. */
    public static final int SRATIONAL = 10;
    /** TIFF FLOAT (32-bit IEEE floating point) type. */
    public static final int FLOAT = 11;
    /** TIFF DOUBLE (64-bit IEEE floating point) type. */
    public static final int DOUBLE = 12;
    /** TIFF IFD (LONG) type. */
    public static final int IFD = 13;

    /** TIFF type labels. */
    public static final String TYPE [] = {
        "", "BYTE", "ASCII", "SHORT", "LONG", "RATIONAL", "SBYTE", "UNDEFINED",
        "SSHORT", "SLONG", "SRATIONAL", "FLOAT", "DOUBLE", "IFD"
    };

    /******************************************************************
     * PRIVATE INSTANCE FIELDS.
     ******************************************************************/

    /** True if big-endian file. */
    protected boolean _bigEndian;

    /** List of errors. */
    private List<String> _errors;

    /** True if this is the first IFD. */
    private boolean _first;

    /** True if the is the "thumbnail" IFD. */
    private boolean _thumbnail;

    /** Format for converting float to string. */
    private NumberFormat _format;

    /** Representation information. */
    protected RepInfo _info;

    /** Offset of next IFD. */
    protected long _next;

    /** IFD offset. */
    protected long _offset;

    /** Open random access TIFF file. */
    private RandomAccessFile _raf;

    /** TIFF version. */
    protected int _version;

    /******************************************************************
     * CLASS CONSTRUCTOR.
     ******************************************************************/

    /** Instantiate an <code>IFD</code> object.
     * @param offset IFD offset
     * @param info Representation information
     * @param raf TIFF file
     * @param bigEndian True if big-endian file
     */
    public IFD(long offset, RepInfo info, RandomAccessFile raf,
                boolean bigEndian)
    {
        _offset    = offset;
        _info      = info;
        _raf       = raf;
        _bigEndian = bigEndian;

        _first   = false;
        _thumbnail = false;
        _next    = 0L;
        _version = 4;

        _errors = new LinkedList<>();

        _format = NumberFormat.getInstance();
        _format.setGroupingUsed(false);
        _format.setMinimumFractionDigits(0);
    }

    /******************************************************************
     * PUBLIC INSTANCE METHODS.
     ******************************************************************/

    /** Get any errors discovered during parsing.
     * @return list of strings with errors
     */
    public List<String> getErrors()
    {
        return _errors;
    }

    /** Get the offset of the next IFD.
     * @return next
     */
    public long getNext()
    {
        return _next;
    }

    /** Get the IFD offset.
     *  @return IFD offset
    */
    public long getOffset()
    {
        return _offset;
    }

    /** Get the IFD properties.
     *  @param rawOutput: boolean
     *  @return Property
     *  @throws edu.harvard.hul.ois.jhove.module.tiff.TiffException
    */
    public abstract Property getProperty(boolean rawOutput)
                        throws TiffException;

    /** Get the TIFF version.
     *  @return TIFF version
     */
    public int getVersion()
    {
        return _version;
    }

    /** Return true if this is the first IFD.
     * @return if it's first?
    */
    public boolean isFirst()
    {
        return _first;
    }

    /** Return true if this is the thumbnail IFD.
     * @return if it's a thumbnail
     */
    public boolean isThumbnail()
    {
        return _thumbnail;
    }


    /** Lookup IFD tag.
     *  @param tag
     *  @param type
     *  @param count
     *  @param value
     *  @throws edu.harvard.hul.ois.jhove.module.tiff.TiffException
     */
    public abstract void lookupTag(int tag, int type, long count, long value)
        throws TiffException;

    /** Parse the IFD.Errors are not suppressed, and odd byte offsets for
     * tags not allowed.
     *
     * @return The offset of the next IFD
     * @throws edu.harvard.hul.ois.jhove.module.tiff.TiffException
     */
    public long parse()
        throws TiffException
    {
	return parse(false, false);
    }

    /** Parse the IFD.
     * @param byteOffsetIsValid   If true, allow offsets on odd byte boundaries
     * @param suppressErrors      If true, return IFD even with errors
     * @return The offset of the next IFD
     * @throws edu.harvard.hul.ois.jhove.module.tiff.TiffException
     */
    public long parse(boolean byteOffsetIsValid, boolean suppressErrors)
        throws TiffException
    {
        try {
            return parse(byteOffsetIsValid);
        }
        catch (TiffException e) {
            // If we got a TiffException and we're suppressing errors,
            // cover over the exception and issue an info message;
            // but we can't follow the IFD chain further.
            if (suppressErrors) {
                _info.setMessage
                    (new InfoMessage(e.getMessage(), e.getOffset()));
                return 0;
            }
            throw e;
        }
    }


    /** Parse the IFD.Errors are not suppressed.
     *
     * @param byteOffsetIsValid   If true, allow offsets on odd byte boundaries
     * @return The offset of the next IFD
     * @throws edu.harvard.hul.ois.jhove.module.tiff.TiffException
     */
    public long parse(boolean byteOffsetIsValid)
        throws TiffException
    {
        /* Start at the IFD offset, read the number of entries, then
         * read the entire IFD.
         */
        long offset = _offset;
        _next = 0L;
        byte [] buffer;
        int nFields = 0;
        try {
            _raf.seek(offset);
            nFields = ModuleBase.readUnsignedShort(_raf, _bigEndian);
            offset += 2;

            int len = 12*nFields;
            buffer = new byte[len];
            _raf.read(buffer, 0, len);

            /* Read the offset of the next IFD (or 0 if none). */
            offset += len;
            _next = ModuleBase.readUnsignedInt(_raf, _bigEndian);
        }
        catch (Exception e) {
            throw new TiffException(MessageConstants.TIFF_HUL_1, offset);
        }

        DataInputStream ifdStream =
            new DataInputStream(new ByteArrayInputStream(buffer));

        try {
            int prevTag = 0;
            for (int i=0; i<nFields; i++) {
                int tag = ModuleBase.readUnsignedShort(ifdStream, _bigEndian,
                                                        null);
                /* Tags must be in ascending numerical order. */
                if (!DEBUG_ALLOW_OUT_OF_SEQUENCE && tag < prevTag) {
                    String mess = MessageFormat.format(MessageConstants.TIFF_HUL_2.getMessage(), tag);
                    JhoveMessage message = JhoveMessages.getMessageInstance(MessageConstants.TIFF_HUL_2.getId(), mess);
                    _info.setMessage(new ErrorMessage(message, _offset + 2 + 12*i));
                    _info.setWellFormed(false);
                }
                prevTag = tag;

                int type = ModuleBase.readUnsignedShort(ifdStream,
                                                        _bigEndian, null);
                /* Skip over tags with unknown type. */
                if (type < BYTE || type > IFD) {
                    String subMess = MessageFormat.format(MessageConstants.TIFF_HUL_3_SUB.getMessage(), type, Integer.valueOf(tag));
                    _info.setMessage(new ErrorMessage(MessageConstants.TIFF_HUL_3, subMess,
                    		                          _offset + 4 + 12*i));
                }
                else {
                    /* Type gives indication of the TIFF version. */
                    if (SBYTE <= type && type <= IFD) {
                        _version = 6;
                    }

                    long count = ModuleBase.readUnsignedInt(ifdStream,
                                                            _bigEndian, null);
                    long value = ModuleBase.readUnsignedInt(ifdStream,
                                                            _bigEndian, null);
                    if (calcValueSize(type, count) > 4) {
                        /* Value is the word-aligned offset of the actual
                         * value. */
			if ((value & 1) != 0) {
				final String mess = MessageFormat.format(MessageConstants.TIFF_HUL_4.getMessage(), value);
				JhoveMessage message = JhoveMessages.getMessageInstance(MessageConstants.TIFF_HUL_4.getId(), mess);
			    if (byteOffsetIsValid) {
				_info.setMessage(new InfoMessage(message, _offset + 10 + 12*i));
			    }
			    else {
				throw new TiffException(message,_offset + 10 + 12*i);
			    }
                        }
                    }
                    else {
                        /* Value is the actual value; pass the offset of
                         * the value. */
                        value = _offset + 10 + 12*i;
                    }
                    lookupTag(tag, type, count, value);
                }
            }
        }
        catch (IOException e) {
            throw new TiffException(MessageConstants.TIFF_HUL_5, _offset + 2);
        }
        postParseInitialization();

        return _next;
    }

    /** Sets flag indicating whether this is the first IFD.
     * @param first: true if it's the first IFD
     */
    public void setFirst(boolean first)
    {
        _first = first;
    }

    /** Sets flag indicating whether this is the "thumbnail" IFD.
     *  The second IFD in the top-level chain is assumed to be
     *  the Thumbnail IFD.
     * @param thumbnail: flag true if this is the thumbnail IFD
     */
    public void setThumbnail(boolean thumbnail)
    {
        _thumbnail = thumbnail;
    }

    /**
     *  Returns a Property representing a bitmask.
     *  If <code>rawOutput</code> is true, returns a LIST
     *  property whose elements are STRING properties.  The
     *  string values of these STRING properties are the
     *  elements of <code>labels</code> whose indices
     *  correspond to 1 bits in the bitmask, counting
     *  the low-order bit as bit 0.
     *  if <code>rawOutput</code> is false, returns a LONG
     *  property whose numeric value is <code>value</code>.
     *
     * @param name
     * @param value
     * @param labels
     * @param rawOutput: true if string output is wanted, false returns
     * a long property
     * @return property representing a bitmask
     *
     */
    protected Property addBitmaskProperty(String name, long value,
                                          String [] labels, boolean rawOutput)
    {
        Property prop = null;
        if (!rawOutput) {
            List<String> list = new LinkedList<>();
            try {
                for (int i=0; i<labels.length; i++) {
                    if ((value & (1 << i)) != 0) {
                        list.add(labels[i]);
                    }
                }
            }
            catch (Exception e) {
                _errors.add(MessageFormat.format(MessageConstants.TIFF_HUL_66.getMessage(), name, Long.valueOf(value)));
            }
            prop = new Property(name, PropertyType.STRING,
                                PropertyArity.LIST, list);
        }
        if (prop == null) {
            prop = new Property(name, PropertyType.LONG, value);
        }

        return prop;
    }

    /**
     *  Returns an Property representing an integer value.
     *  If <code>rawOutput</code> is true, returns
     *  an INTEGER property, and <code>labels</code> and
     *  <code>index</code> are unused.  Otherwise,
     *  returns a STRING property, with the
     *  string being the element of <code>labels</code>
     *  whose index is <code>value</code>.
     *
     * @param name
     * @param value
     * @param labels
     * @param rawOutput: true if integer output is wanted, false returns
     * a string property
     * @return property representing an integer value
     */
    protected Property addIntegerProperty(String name, int value,
                                           String [] labels, boolean rawOutput)
    {
        Property prop = null;
        if (!rawOutput) {
            try {
                prop = new Property(name, PropertyType.STRING, labels[value]);
            }
            catch (ArrayIndexOutOfBoundsException aioobe) {
                _errors.add(MessageFormat.format(MessageConstants.TIFF_HUL_66.getMessage(), name, Long.valueOf(value)));
            }
        }
        if (prop == null) {
            prop = new Property(name, PropertyType.INTEGER, value);
        }

        return prop;
    }

    /**
     *  Returns an Property representing an integer value.
     *  If <code>rawOutput</code> is true, returns
     *  an INTEGER property, and <code>labels</code> and
     *  <code>index</code> are unused.  Otherwise,
     *  returns a STRING property, with the
     *  string being the element of <code>labels</code>
     *  whose index is the index of
     *  <code>value</code> in <code>index</code>.
     *
     * @param name
     * @param value
     * @param labels
     * @param index
     * @param rawOutput: true if int output is wanted, false returns
     * a string property
     * @return property representing an integer value
     */
    protected Property addIntegerProperty(String name, int value,
                                          String [] labels, int [] index,
                                          boolean rawOutput)
    {
        Property prop = null;
        if (!rawOutput) {
            int n = -1;
            for (int i = 0; i < index.length; i++) {
                if (value == index[i]) {
                    n = i;
                    break;
                }
            }
            if (n > -1) {
                prop = new Property(name, PropertyType.STRING, labels[n]);
            }
            else {
                _errors.add(MessageFormat.format(MessageConstants.TIFF_HUL_66.getMessage(), name, Long.valueOf(value)));
            }
        }
        if (prop == null) {
            prop = new Property(name, PropertyType.INTEGER,
                                 value);
        }

        return prop;
    }

    /**
     *  Returns an ARRAY Property representing an integer array.
     *  If <code>rawOutput</code> is true, the elements of the property array
     *  are INTEGER properties, and <code>labels</code> is unused.  Otherwise,
     *  the elements of the array are STRING properties, with the
     *  elements of <code>value</code> used as indices into
     *  <code>labels</code>.
     *
     * @param name
     * @param value
     * @param labels
     * @param rawOutput: true if int output is wanted, false returns
     * a string property
     * @return property representing an integer array
     */
    protected Property addIntegerArrayProperty(String name, int [] value,
                                                String [] labels,
                                                boolean rawOutput)
    {
        Property prop = null;
        if (!rawOutput) {
            String [] s = new String[value.length];
            for (int i=0; i<value.length; i++) {
                try {
                    s[i] = labels[value[i]];
                }
                catch (ArrayIndexOutOfBoundsException aioobe) {
                    _errors.add(MessageFormat.format(MessageConstants.TIFF_HUL_66.getMessage(), name, Long.valueOf(value[i])));
                }
            }
            prop = new Property(name, PropertyType.STRING,
                                PropertyArity.ARRAY, s);
        }
        if (prop == null) {
            prop = new Property(name, PropertyType.INTEGER,
                                PropertyArity.ARRAY, value);
        }

        return prop;
    }

    /**
     * Returns a property for a tag with a RATIONAL value.
     * If rawOutput is true, returns a property with type
     * RATIONAL.  Otherwise, returns a property with type
     * STRING, and the text representation of the Rational
     * value as a floating-point ratio.
     *
     * @param name
     * @param r: a rational value
     * @param rawOutput: true if int output is wanted, false returns
     * a string property
     * @return property for a tag with a RATIONAL value
     */
    protected Property addRationalProperty(String name, Rational r,
                                            boolean rawOutput)
    {
        Property prop = null;
        if (!rawOutput) {
            prop = new Property(name, PropertyType.STRING,
                                 _format.format(r.toDouble()));
        }
        if (prop == null) {
            prop = new Property(name, PropertyType.RATIONAL, r);
        }

        return prop;
    }

    protected Property addRationalArrayProperty(String name, Rational [] r,
                                                 boolean rawOutput)
    {
        Property prop = null;
        if (!rawOutput) {
            String [] s = new String[r.length];
            for (int i=0; i<r.length; i++) {
                s[i] =  _format.format(r[i].toDouble());
            }
            prop = new Property(name, PropertyType.STRING,
                                PropertyArity.ARRAY, s);
        }
        if (prop == null) {
            prop = new Property(name, PropertyType.RATIONAL,
                                PropertyArity.ARRAY, r);
        }

        return prop;
    }

    /** Perform initializations that have to wait until after the
     * IFD has been parsed.
     */
    protected void postParseInitialization()
    {
    }

    /** Standard IFD property header.
     *
     * @param type
     * @param entries
     * @return IDF property header
     */
    protected Property propertyHeader(String type, List entries)
    {
        Property [] array = new Property [3];
        array[0] = new Property("Offset", PropertyType.LONG,_offset);
        array[1] = new Property("Type", PropertyType.STRING, type);
        array[2] = new Property("Entries", PropertyType.PROPERTY,
                                 PropertyArity.LIST, entries);

        return new Property("IFD", PropertyType.PROPERTY, PropertyArity.ARRAY,
                             array);
    }

    /**
     * Reads a string value from the TIFF file.
     * If there are non-ASCII characters, they're escaped as %XX
     * @param count Length of string
     * @param value Offset of string
     * @return ASCII string
     * @throws IOException
     */
    protected String readASCII(long count, long value)
        throws IOException
    {
        _raf.seek(value);

        byte [] buffer = new byte [(int) count];
        _raf.read(buffer);

        StringBuffer sb = new StringBuffer();
        for (int i=0; i<count; i++) {
            byte c = buffer[i];
            if (c == 0) {
                break;
            }
            // Escape characters that aren't ASCII. There really shouldn't
            // be any, and if there are, we don't know how they're encoded.
            if (c < 32 || c > 127) {
                sb.append(byteToHex(c));
            }
            else {
                sb.append((char) c);
            }
        }
        return sb.toString();
    }

    /** Reads an array of strings from the TIFF file.
     *
     *  @param  count  Number of strings to read
     *  @param  value  Offset from which to read
     *
     *  @return ASCII string array
     *  @throws IOException
     */
    protected String [] readASCIIArray(long count, long value)
        throws IOException
    {
        _raf.seek(value);

        int nstrs = 0;
        List<String> list = new LinkedList<>();
        byte[] buf = new byte[(int) count];
        _raf.read(buf);
        StringBuffer strbuf = new StringBuffer();
        for (int i=0; i<count; i++) {
            int b = buf[i];
            if (b == 0) {
                list.add(strbuf.toString());
                strbuf.setLength(0);
            }
            else {
                // Escape characters that aren't ASCII. There really shouldn't
                // be any, and if there are, we don't know how they're encoded.
                if (b < 32 || b > 127) {
                    strbuf.append(byteToHex((byte) b));
                }
                else {
                    strbuf.append((char) b);
                }
            }
        }
        /* We can't use ArrayList.toArray because that returns an
           Object[], not a String[] ... sigh. */
        String [] strs = new String[nstrs];
        ListIterator<String> iter = list.listIterator();
        for (int i=0; i<nstrs; i++) {
            strs[i] =  iter.next();
        }
        return strs;
    }

    /** Reads and returns a single unsigned 8-bit integer value.
     *
     *  @param  type   TIFF type to read; must be an 8-bit type
     *  @param  count  Unused
     *  @param  value  Offset from which to read
     *
     *  @return unsigned 8-bit integer value
     *  @throws java.io.IOException
     */
    protected int readByte(int type, long count, long value)
        throws IOException
    {
        _raf.seek(value);

        return (int) readUnsigned(type);
    }

    /** Reads an array of bytes and returns it as an int array.
     *
     *  @param  type   TIFF type to read; must be an 8-bit type
     *  @param  count  Number of bytes to read
     *  @param  value  Offset from which to read
     *
     *  @return int array of bytes
     *  @throws IOException
     */
    protected int [] readByteArray(int type, long count, long value)
        throws IOException
    {
        _raf.seek(value);

        int [] array = new int [(int) count];
        for (int i=0; i<count; i++) {
            array[i] = (int) readUnsigned(type);
        }

        return array;
    }


    /** Reads an array of bytes and returns it as a byte array.
     *
     *  @param  type   Unused
     *  @param  count  Number of bytes to read
     *  @param  value  Offset from which to read
     *
     *  @return byte array
     *  @throws java.io.IOException
     */
    protected byte [] readTrueByteArray(int type, long count, long value)
        throws IOException
    {
        _raf.seek(value);
        byte [] array = new byte [(int) count];
        _raf.read(array);
        return array;
    }

    /**
     *  Reads a TIFF array of DOUBLE 64-bit values and returns
     *  it as a double array.
     *
     *  @param  count  Number of values to read
     *  @param  value  Offset from which to read
     *
     *  @return double array
     *  @throws IOException
     */
    protected double [] readDoubleArray(long count, long value)
        throws IOException
    {
        _raf.seek(value);

        double [] darray = new double [(int) count];
        for (int i=0; i<count; i++) {
            darray[i] = ModuleBase.readDouble(_raf, _bigEndian);
        }

        return darray;
    }

    /** Reads and returns a single unsigned 32-bit integer value.
     *
     *  @param  type   TIFF type to read; must be a 32-bit type
     *  @param  count  Unused
     *  @param  value  Offset from which to read
     *
     *  @return unsigned 32-bit integer value
     *  @throws  IOException
     */
    protected long readLong(int type, long count, long value)
        throws IOException
    {
        _raf.seek(value);

        return readUnsigned(type);
    }



    /**
     *  Reads a TIFF array of signed 32-bit integer values and returns
     *  it as a long array.
     *
     *  @param  type   TIFF type to read; must be a 32-bit type
     *  @param  count  Number of values to read
     *  @param  value  Offset from which to read
     *
     *  @return long array
     *  @throws IOException
     */
    protected long [] readLongArray(int type, long count, long value)
        throws IOException
    {
        _raf.seek(value);

        long [] array = new long [(int) count];
        for (int i=0; i<count; i++) {
            array[i] = readUnsigned(type);
        }
        return array;
    }

    /** Reads an unsigned number of any type.
     *  @param  type   TIFF type to read
     *
     *  @return unsigned number
     *  @throws IOException
     */
    public long readUnsigned(int type)
        throws IOException
    {
        long u = 0L;

        switch (type) {
        case BYTE:
        case UNDEFINED:
            u = ModuleBase.readUnsignedByte(_raf);
            break;
        case SHORT:
            u = ModuleBase.readUnsignedShort(_raf, _bigEndian);
            break;
        case LONG:
        case IFD:
            u = ModuleBase.readUnsignedInt(_raf, _bigEndian);
            break;
        default:
            break;
        }

        return u;
    }


    /** Reads a RATIONAL value and returns it as a Rational.
     *
     * @param count
     * @param value
     *
     * @return rational
     * @throws IOException
     */
    protected Rational readRational(long count, long value)
        throws IOException
    {
        _raf.seek(value);

        long numer = ModuleBase.readUnsignedInt(_raf, _bigEndian);
        long denom = ModuleBase.readUnsignedInt(_raf, _bigEndian);
        return new Rational(numer, denom);
    }

    /** Reads an array of RATIONAL values and returns it as an
     *  array of Rational.
     *
     * @param count
     * @param value
     *
     * @return array of rational values
     * @throws IOException
     */
    protected Rational [] readRationalArray(long count, long value)
        throws IOException
    {
        _raf.seek(value);

        byte [] buffer = new byte [(int) calcValueSize(RATIONAL, count)];
        _raf.read(buffer);
        DataInputStream stream =
            new DataInputStream(new ByteArrayInputStream(buffer));

        Rational [] rarray = new Rational [(int) count];
        for (int i=0; i<count; i++) {
           long numer = ModuleBase.readUnsignedInt
                           (stream, _bigEndian, null);
           long denom = ModuleBase.readUnsignedInt
                           (stream, _bigEndian, null);
           rarray[i] = new Rational(numer, denom);
        }

        return rarray;
    }

    /** Reads an SRATIONAL value and returns it as a Rational.
     *
     * @param count
     * @param value
     *
     * @return rational
     * @throws IOException
     */
    protected Rational readSignedRational(long count, long value)
        throws IOException
    {
        _raf.seek(value);

        long numer = ModuleBase.readSignedInt(_raf, _bigEndian);
        long denom = ModuleBase.readSignedInt(_raf, _bigEndian);
        return new Rational(numer, denom);
    }

    /** Reads an array of SRATIONAL values and returns it as an
     *  array of Rational.
     *
     * @param count
     * @param value
     *
     * @return array of rational values
     * @throws IOException
     */
    protected Rational [] readSignedRationalArray(long count, long value)
        throws IOException
    {
        _raf.seek(value);

        byte [] buffer = new byte [(int) calcValueSize(SRATIONAL, count)];
        _raf.read(buffer);
        DataInputStream stream =
            new DataInputStream(new ByteArrayInputStream(buffer));

        Rational [] rarray = new Rational [(int) count];
        for (int i=0; i<count; i++) {
           long numer = ModuleBase.readSignedInt
                           (stream, _bigEndian, null);
           long denom = ModuleBase.readSignedInt
                           (stream, _bigEndian, null);
           rarray[i] = new Rational(numer, denom);
        }

        return rarray;
    }


    /** Reads and returns a single unsigned 16-bit value.
     *
     * @param type
     * @param count
     * @param value
     *
     * @return unsigned 16-bit value
     * @throws IOException
     */
    protected int readShort(int type, long count, long value)
        throws IOException
    {
        _raf.seek(value);

        return (int) readUnsigned(type);
    }

    /**
     *  Reads a TIFF array of unsigned 16-bit values and returns
     *  it as an int array.
     *
     * @param type
     * @param count
     * @param value
     *
     * @return int array
     * @throws IOException
     */
    protected int [] readShortArray(int type, long count, long value)
        throws IOException
    {
        _raf.seek(value);

        int [] iarray = new int [(int) count];
        for (int i=0; i<count; i++) {
            iarray[i] = (int) readUnsigned(type);
        }
        return iarray;
    }

    /**
     *  Reads a TIFF array of signed 16-bit values and returns
     *  it as an int array.
     *
     * @param type
     * @param count
     * @param value
     *
     * @return int array
     * @throws IOException
     */
    protected int [] readSShortArray(int type, long count, long value)
        throws IOException
    {
        _raf.seek(value);

        int [] iarray = new int [(int) count];
        for (int i=0; i<count; i++) {
            iarray[i] = ModuleBase.readSignedShort(_raf, _bigEndian);
        }
        return iarray;
    }

    /**
     * Calculate how many bytes a given number of fields of a given
     * type will require.
     * @param type Field type
     * @param count Field count
     *
     * @return number of bytes
     */
    public static long calcValueSize(int type, long count)
    {
        int fieldSize = 0;
        switch (type) {
        case BYTE:
        case ASCII:
        case SBYTE:
        case UNDEFINED:
            fieldSize = 1;
            break;
        case SHORT:
        case SSHORT:
        case FLOAT:
            fieldSize = 2;
            break;
        case LONG:
        case SLONG:
	case IFD:
            fieldSize = 4;
            break;
        case RATIONAL:
        case SRATIONAL:
        case DOUBLE:
            fieldSize = 8;
            break;
        default:
            break;
        }
        return  count*fieldSize;
    }

    /**
     *  Returns <code>true</code> if file is big-endian,
     *  <code>false</code> if little-endian.
     */
    public boolean isBigEndian()
    {
        return _bigEndian;
    }



    /******************************************************************
     * PRIVATE CLASS METHODS.
     ******************************************************************/

    /**
     * Check the tag entry count.
     * @param tag Tag entry value
     * @param count Tag entry count
     * @param minCount Tag count
     * @throws edu.harvard.hul.ois.jhove.module.tiff.TiffException
     */
    protected static void checkCount(int tag, long count, int minCount)
        throws TiffException
    {
        if (count < minCount) {
            String mess = MessageFormat.format(MessageConstants.TIFF_HUL_6.getMessage(),
                    tag, Integer.valueOf(minCount), Long.valueOf(count));
            JhoveMessage message = JhoveMessages.getMessageInstance(MessageConstants.TIFF_HUL_6.getId(), mess);
            throw new TiffException(message);
        }
    }

    /**
     * Check that the count is compatible with array instanciation.
     * @param tag Tag entry value
     * @param count Tag entry count
     */
    protected static void checkCountArray(int tag, long count)
        throws TiffException
    {
        if (count > Integer.MAX_VALUE) {
            String mess = MessageFormat.format(MessageConstants.TIFF_HUL_6.getMessage(), Integer.valueOf(tag), Integer.valueOf(Integer.MAX_VALUE), Long.valueOf(count));
            JhoveMessage message = JhoveMessages.getMessageInstance(MessageConstants.TIFF_HUL_6.getId(), mess);
            throw new TiffException(message);
        }
    }

    /**
     * Check the tag entry type.
     * @param tag Tag entry value
     * @param type Tag entry type
     * @param expected Tag
     * @throws edu.harvard.hul.ois.jhove.module.tiff.TiffException
     */
    protected static void checkType(int tag, int type, int expected)
        throws TiffException
    {
        /* Readers are supposed to accept BYTE, SHORT or LONG for any
         * unsigned integer field. */
        if ((type == BYTE || type == SHORT || type == LONG || type == IFD) &&
                (expected == BYTE || expected == SHORT || expected == LONG ||
		expected == IFD)) {
            return;    // it's OK
        }
        if (type != expected) {
            String mess = MessageFormat.format(MessageConstants.TIFF_HUL_7.getMessage(), tag, Integer.valueOf(expected), Integer.valueOf(type));
            JhoveMessage message = JhoveMessages.getMessageInstance(MessageConstants.TIFF_HUL_7.getId(), mess);
            throw new TiffException(message);
        }
    }

    /**
     * Check the tag entry type.
     * @param tag Tag entry value
     * @param type Tag entry type
     * @param type1 Tag type
     * @param type2 Alternate tag type
     * @throws edu.harvard.hul.ois.jhove.module.tiff.TiffException
     */
    protected static void checkType(int tag, int type, int type1, int type2)
        throws TiffException
    {
        if (type != type1 && type != type2) {
            String mess = MessageFormat.format(MessageConstants.TIFF_HUL_8.getMessage(),
                    tag, Integer.valueOf(type1), Integer.valueOf(type2), Integer.valueOf(type));
            JhoveMessage message = JhoveMessages.getMessageInstance(MessageConstants.TIFF_HUL_8.getId(), mess);
            throw new TiffException(message);
        }
    }

    protected static Rational average(Rational r1, Rational r2)
    {
        long d1 = r1.getDenominator();
        long d2 = r2.getDenominator();

        Rational f1 = new Rational(r1.getNumerator()*d2,
                                   r1.getDenominator()*d2);
        Rational f2 = new Rational(r2.getNumerator()*d1,
                                   r2.getDenominator()*d1);

        return new Rational((f1.getNumerator() + f2.getNumerator())/2,
                             f1.getDenominator());
    }


    /******************************************************************
     * PRIVATE INSTANCE METHODS.
     ******************************************************************/

    /** Represent a byte value as %XX */
    private String byteToHex(byte c) {
        int[] nibbles = new int[2];
        nibbles[0] = ((int) c & 0XF0) >> 4;
        nibbles[1] = (int) c & 0X0F;
        StringBuffer retval = new StringBuffer("%");
        for (int i = 0; i <= 1; i++) {
            int b = nibbles[i];
            if (b >= 10) {
                b += (int) 'A' - 10;
            }
            else {
                b += (int) '0';
            }
            retval.append((char) b);
        }
        return retval.toString();
    }
}
