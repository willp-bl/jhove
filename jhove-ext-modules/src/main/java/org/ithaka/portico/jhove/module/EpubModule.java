package org.ithaka.portico.jhove.module;

import static org.ithaka.portico.jhove.module.epub.ReportPropertyNames.PROPNAME_CHARCOUNT;
import static org.ithaka.portico.jhove.module.epub.ReportPropertyNames.PROPNAME_CONTRIBUTOR;
import static org.ithaka.portico.jhove.module.epub.ReportPropertyNames.PROPNAME_CREATOR;
import static org.ithaka.portico.jhove.module.epub.ReportPropertyNames.PROPNAME_DATE;
import static org.ithaka.portico.jhove.module.epub.ReportPropertyNames.PROPNAME_EPUB_METADATA;
import static org.ithaka.portico.jhove.module.epub.ReportPropertyNames.PROPNAME_FONT;
import static org.ithaka.portico.jhove.module.epub.ReportPropertyNames.PROPNAME_FONTFILE;
import static org.ithaka.portico.jhove.module.epub.ReportPropertyNames.PROPNAME_FONTNAME;
import static org.ithaka.portico.jhove.module.epub.ReportPropertyNames.PROPNAME_FONTS;
import static org.ithaka.portico.jhove.module.epub.ReportPropertyNames.PROPNAME_IDENTIFIER;
import static org.ithaka.portico.jhove.module.epub.ReportPropertyNames.PROPNAME_INFO;
import static org.ithaka.portico.jhove.module.epub.ReportPropertyNames.PROPNAME_LANGUAGE;
import static org.ithaka.portico.jhove.module.epub.ReportPropertyNames.PROPNAME_MEDIATYPES;
import static org.ithaka.portico.jhove.module.epub.ReportPropertyNames.PROPNAME_PAGECOUNT;
import static org.ithaka.portico.jhove.module.epub.ReportPropertyNames.PROPNAME_PUBLISHER;
import static org.ithaka.portico.jhove.module.epub.ReportPropertyNames.PROPNAME_REFERENCES;
import static org.ithaka.portico.jhove.module.epub.ReportPropertyNames.PROPNAME_RESOURCES;
import static org.ithaka.portico.jhove.module.epub.ReportPropertyNames.PROPNAME_RIGHTS;
import static org.ithaka.portico.jhove.module.epub.ReportPropertyNames.PROPNAME_SUBJECTS;
import static org.ithaka.portico.jhove.module.epub.ReportPropertyNames.PROPNAME_TITLE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.ithaka.portico.jhove.module.epub.JhoveRepInfoReport;
import org.ithaka.portico.jhove.module.epub.MessageConstants;
import org.jwat.common.RandomAccessFileInputStream;

import com.adobe.epubcheck.api.EPUBLocation;
import com.adobe.epubcheck.api.EpubCheck;
import com.adobe.epubcheck.messages.Severity;
import com.adobe.epubcheck.reporting.CheckMessage;
import com.adobe.epubcheck.util.PathUtil;

import edu.harvard.hul.ois.jhove.Agent;
import edu.harvard.hul.ois.jhove.Agent.Builder;
import edu.harvard.hul.ois.jhove.AgentType;
import edu.harvard.hul.ois.jhove.Document;
import edu.harvard.hul.ois.jhove.DocumentType;
import edu.harvard.hul.ois.jhove.ErrorMessage;
import edu.harvard.hul.ois.jhove.ExternalSignature;
import edu.harvard.hul.ois.jhove.Identifier;
import edu.harvard.hul.ois.jhove.IdentifierType;
import edu.harvard.hul.ois.jhove.InfoMessage;
import edu.harvard.hul.ois.jhove.InternalSignature;
import edu.harvard.hul.ois.jhove.Message;
import edu.harvard.hul.ois.jhove.ModuleBase;
import edu.harvard.hul.ois.jhove.Property;
import edu.harvard.hul.ois.jhove.PropertyArity;
import edu.harvard.hul.ois.jhove.PropertyType;
import edu.harvard.hul.ois.jhove.RepInfo;
import edu.harvard.hul.ois.jhove.Signature;
import edu.harvard.hul.ois.jhove.SignatureType;
import edu.harvard.hul.ois.jhove.SignatureUseType;
import edu.harvard.hul.ois.jhove.messages.JhoveMessage;
import edu.harvard.hul.ois.jhove.messages.JhoveMessages;

/**
 * Module for validation and metadata extraction on EPUB files. Validation is
 * performed by EPUBCheck https://github.com/w3c/epubcheck/ This module uses the
 * metadata produced by EPUBCheck to produce a JHOVE report.
 *
 * @author Karen Hanson
 *
 */
public class EpubModule extends ModuleBase {

    private static final String EPUB_MEDIATYPE = "application/epub+zip";
    private static final String FORMATNAME = "EPUB";

    private static final String NAME = "EPUB-ptc";
    private static final String RELEASE = "1.0";
    private static final int[] DATE = {2019, 5, 15};
    private static final String RIGHTS_YEAR = "2019";
    private static final String[] FORMAT = { FORMATNAME };
    private static final String COVERAGE = FORMATNAME;
    private static final String[] MIMETYPE = { EPUB_MEDIATYPE };
    private static final String WELLFORMED = "";
    private static final String VALIDITY = "";
    private static final String REPINFO = "";
    private static final String NOTE = "This module uses EPUBCheck for testing of EPUB files.";

    //EPUB agent information
    private static final String EPUB_AGENTNAME = "International Digital Publishing Forum";
    private static final AgentType EPUB_AGENTTYPE = AgentType.STANDARD;
    private static final String EPUB_AGENTADDRESS = "International Digital Publishing Forum (IDPF), "
            + "113 Cherry Street, Suite 70-719, Seattle, WA 98104";
    private static final String EPUB_AGENTWEBSITE = "http://idpf.org";
    private static final String EPUB_AGENTEMAIL = "membership@idpf.org";
    private static final String EPUB_AGENTPHONE = "+1-206-451-7250";

    //EPUB format doc information
    private static final String EPUB_FORMATDOCTITLE = FORMATNAME;
    private static final String EPUB_FORMATDOCDATE = "2019-05-15";
    private static final String EPUB_FORMATDOCURL = "http://www.idpf.org/epub/dir/";

    //Signatures
    private static final String EPUB_EXTENSION = ".epub";
    private static final int FIRST_SIG_POSITION = 0;
    private static final String FIRST_SIG_VALUE = "PK";
    private static final int SECOND_SIG_POSITION = 30;
    private static final String SECOND_SIG_VALUE = "mimetype";
    private static final int THIRD_SIG_POSITION = 38;
    private static final String THIRD_SIG_VALUE = EPUB_MEDIATYPE;

    /*
     * FATAL errors automatically set the status to not-well-formed. Non-fatal
     * "ERROR"s are more of a mixed bag. Some relate to e.g. XHTML or CSS validity
     * but others are issues with e.g. package structure. Here you can configure
     * error codes or error code prefixes that will indicate that a file is
     * not-well-formed. For example, any package-related ERROR will indicate the
     * EPUB is not well formed.
     */
    public static final String[] NOTWELLFORMED_ERRCODES = new String[] { "PKG-" };

    /* Top-level property list */
    protected List<Property> _propList;

    /* Top-level metadata property */
    protected Property _metadata;

    /**
     * ****************************************************************
     * CLASS CONSTRUCTOR.
     * ****************************************************************
     */
    /**
     * Instantiate a <tt>EpubModule</tt> object using default properties
     */
    public EpubModule() {
        super(NAME, RELEASE, DATE, FORMAT, COVERAGE, MIMETYPE, WELLFORMED,
                VALIDITY, REPINFO, NOTE, PorticoConstants.porticoRightsStmt(RIGHTS_YEAR), false);

        initializeInstance(PorticoConstants.porticoAgent());
    }

    /**
     * Instantiate a <tt>EpubModule</tt> object using constructor arguments
     *
     * @param name
     * @param release
     * @param date
     * @param format
     * @param coverage
     * @param mimetype
     * @param wellformedNote
     * @param validityNote
     * @param repinfoNote
     * @param note
     * @param rights
     * @param isRandomAccess
     * @param moduleAgent
     */
    public EpubModule(String name, String release, int[] date,
            String[] format, String coverage,
            String[] mimetype, String wellformedNote,
            String validityNote, String repinfoNote, String note,
            String rights, boolean isRandomAccess, Agent moduleAgent) {
        super(name, release, date, format, coverage, mimetype, wellformedNote, validityNote, repinfoNote,
                note, rights, isRandomAccess);
        initializeInstance(moduleAgent);
    }

    /**
     * Initialize core properties - module {@link Agent}, format specification
     * {@link Document}, and format {@link Signature}
     *
     * @param agentName
     * @param agentType
     * @param agentAddress
     * @param agentTelephone
     * @param agentEmail
     */
    protected void initializeInstance(Agent moduleAgent) {

        _vendor = moduleAgent;

        Agent formatDocAgent = new Builder(EPUB_AGENTNAME, EPUB_AGENTTYPE)
                .address(EPUB_AGENTADDRESS)
                .telephone(EPUB_AGENTPHONE)
                .web(EPUB_AGENTWEBSITE)
                .email(EPUB_AGENTEMAIL)
                .build();

        Document doc = new Document(EPUB_FORMATDOCTITLE, DocumentType.STANDARD);
        doc.setPublisher(formatDocAgent);
        doc.setDate(EPUB_FORMATDOCDATE);
        doc.setIdentifier(new Identifier(EPUB_FORMATDOCURL, IdentifierType.URL));
        _specification.add(doc);

        Signature sig = new ExternalSignature(EPUB_EXTENSION, SignatureType.EXTENSION,
                SignatureUseType.OPTIONAL);
        _signature.add(sig);

        // Signature matching based on:
        // https://www.loc.gov/preservation/digital/formats/fdd/fdd000308.shtml#sign
        // and https://www.loc.gov/preservation/digital/formats/fdd/fdd000278.shtml#sign

        // this first one will also match other kinds of zip files
        sig = new InternalSignature(FIRST_SIG_VALUE, SignatureType.MAGIC, SignatureUseType.MANDATORY, FIRST_SIG_POSITION, "");
        _signature.add(sig);

        // "mimetype" at 30
        sig = new InternalSignature(SECOND_SIG_VALUE, SignatureType.MAGIC, SignatureUseType.MANDATORY, SECOND_SIG_POSITION, "");
        _signature.add(sig);

        // this will also match other kinds of zip files
        sig = new InternalSignature(THIRD_SIG_VALUE, SignatureType.MAGIC, SignatureUseType.MANDATORY, THIRD_SIG_POSITION, "");
        _signature.add(sig);

    }

    // overriding to handle when _je is null, which is not handled in ModuleBase
    @Override
    protected void setupDataStream(final InputStream stream, final RepInfo info) {
        if (_je != null) {
            super.setupDataStream(stream, info);
        } else {
            _dstream = getBufferedDataStream(stream, 0);
        }
    }

    @Override
    public void checkSignatures(File file, InputStream stream, RepInfo info) throws IOException {
        super.checkSignatures(file, stream, info);
        // validity is not determined in this signature check, set to undetermined.
        info.setValid(RepInfo.UNDETERMINED);
    }

    /**
     * Parse the content of a purported EPUB file and store the results in RepInfo.
     *
     * @param stream     An InputStream, positioned at its beginning, which is
     *                   generated from the object to be parsed. If multiple calls
     *                   to <code>parse</code> are made on the basis of a nonzero
     *                   value being returned, a new InputStream must be provided
     *                   each time.
     *
     * @param info       A fresh (on the first call) RepInfo object which will be
     *                   modified to reflect the results of the parsing If multiple
     *                   calls to <code>parse</code> are made on the basis of a
     *                   nonzero value being returned, the same RepInfo object
     *                   should be passed with each call.
     *
     * @param parseIndex Must be 0 in first call to <code>parse</code>. If
     *                   <code>parse</code> returns a nonzero value, it must be
     *                   called again with <code>parseIndex</code> equal to that
     *                   return value.
     * @return
     * @throws java.io.IOException
     */
    @Override
    public int parse(InputStream stream, RepInfo info, int parseIndex) throws IOException {

        initParse();

        info.setModule(this);
        info.setFormat(_format[0]);
        info.setWellFormed(false);
        info.setValid(false);

        _propList = new LinkedList<>();
        _metadata = new Property(PROPNAME_EPUB_METADATA, PropertyType.PROPERTY, PropertyArity.LIST, _propList);

        // loads stream to _dstream so it can be checksummed when flag enabled
        _ckSummer = null;
        setupDataStream(stream, info);

        //Call tool and calculate stats
        try {
            JhoveRepInfoReport report = new JhoveRepInfoReport(info.getUri());
            EpubCheck epubCheck = new EpubCheck(_dstream, report, info.getUri());
            epubCheck.doValidate();

            info.setCreated(report.getCreationDate());
            info.setLastModified(report.getLastModifiedDate());
            info.setVersion(report.getVersion());

            List<CheckMessage> epubMessages = report.getAllMessages();

            // check if any of the messages are on the customized not-well-formed
            // list of errors
            int notWellFormedErrors = epubMessages.stream().filter(c -> triggerNotWellFormed(c))
                    .collect(Collectors.toSet()).size();

            info.setWellFormed(report.getFatalErrorCount() == 0 && notWellFormedErrors == 0);
            info.setValid(info.getWellFormed() == RepInfo.TRUE && report.getErrorCount() == 0);

            Set<Message> msgs = new TreeSet<Message>(new MessageComparator());
            for (CheckMessage msg : report.getAllMessages()) {
                msgs.addAll(toJhoveMessages(msg));
            }
            msgs.forEach(jhoveMsg -> info.setMessage(jhoveMsg));

            info.setMimeType(report.getFormat());

            generateProperties(report).forEach(prop -> _propList.add(prop));

        } catch (Exception f) {
            f.printStackTrace();
            if (f.getMessage() != null) {
                info.setMessage(new ErrorMessage(f.getMessage()));
            } else {
                info.setMessage(new ErrorMessage(MessageConstants.ERR_UNKNOWN));
            }
            info.setWellFormed(false); // may not be the file's fault
            return 0;
        }

        // Check if user has aborted
        if (_je != null && _je.getAbort()) {
            return 0;
        }

        // We parsed it.  Now assemble the properties.
        info.setProperty(_metadata);

        setChecksums(_ckSummer, info);

        return 0;
    }


    @Override
    public void parse(RandomAccessFile file, RepInfo info) throws IOException {
        try (InputStream stream = new RandomAccessFileInputStream(file)) {
            parse(stream, info, 0);
        }
    }

    /**
     * Generates a set of properties collected using EPUBCheck to be added to the JHOVE report
     * @param report
     * @return
     */
    private List<Property> generateProperties(JhoveRepInfoReport report) {
        List<Property> properties = new ArrayList<Property>();

        properties.add(generateProperty(PROPNAME_PAGECOUNT, report.getPageCount(), true));
        properties.add(generateProperty(PROPNAME_CHARCOUNT, report.getCharacterCount(), true));
        properties.add(generateProperty(PROPNAME_LANGUAGE, report.getLanguage()));

        Set<Property> infoProperties = new TreeSet<Property>(new PropertyComparator());
        addProperty(infoProperties, generateProperty(PROPNAME_IDENTIFIER, report.getIdentifier()));
        addProperty(infoProperties, generateProperty(PROPNAME_TITLE, report.getTitles()));
        addProperty(infoProperties, generateProperty(PROPNAME_CREATOR, report.getCreators()));
        addProperty(infoProperties, generateProperty(PROPNAME_CONTRIBUTOR, report.getContributors()));
        addProperty(infoProperties, generateProperty(PROPNAME_DATE, report.getDate()));
        addProperty(infoProperties, generateProperty(PROPNAME_PUBLISHER, report.getPublisher()));
        addProperty(infoProperties, generateProperty(PROPNAME_SUBJECTS, report.getSubjects()));
        addProperty(infoProperties, generateProperty(PROPNAME_RIGHTS, report.getRights()));

        properties.add(generateProperty(PROPNAME_INFO, infoProperties));

        Set<Property> fontList = generateFontProps(report.getEmbeddedFonts(), true);
        fontList.addAll(generateFontProps(report.getRefFonts(), false));
        properties.add(generateProperty(PROPNAME_FONTS, fontList));

        properties.add(generateProperty(PROPNAME_REFERENCES, report.getReferences()));
        properties.add(generateProperty(PROPNAME_RESOURCES, report.getResources()));
        properties.add(generateProperty(PROPNAME_MEDIATYPES, report.getMediaTypes()));

        for (String feature : report.getFeatures()) {
            properties.add(generateProperty(feature, true));
        }

        properties.removeAll(Collections.singletonList(null));

        return properties;
    }

    private static void addProperty(Set<Property> props, Property prop) {
      if (prop == null) {
		    return;
	    }
      props.add(prop);
    }

    /**
     * Returns true if a message is either (a) a fatal error or (b) non-fatal error
     * whose error id is listed in NOTWELLFORMED_ERRCODES
     *
     * @param id
     * @return
     */
    private boolean triggerNotWellFormed(CheckMessage msg) {
        if (msg.getSeverity().equals(Severity.FATAL)) {
            return true;
        }
        if (msg.getSeverity().equals(Severity.ERROR)) {
            for (String s : NOTWELLFORMED_ERRCODES) {
                if (msg.getID().startsWith(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Generates the a set of font properties to be included in the JHOVE report
     *
     * @param fonts    List of fonts found in the EPUB
     * @param fontFile true if the font file is embedded in the EPUB
     * @return
     */
    private Set<Property> generateFontProps(Set<String> fonts, boolean fontFile) {

        Set<Property> fontPropertiesList = new TreeSet<Property>(new PropertyComparator());

        if (fonts != null && fonts.size() > 0) {
            for (String font : fonts) {
                Set<Property> fontProps = new TreeSet<Property>(new PropertyComparator());
                addProperty(fontProps, generateProperty(PROPNAME_FONTNAME, font));
                addProperty(fontProps, generateProperty(PROPNAME_FONTFILE, fontFile));

                fontPropertiesList.add(generateProperty(PROPNAME_FONT, fontProps));
            }
        }
        return fontPropertiesList;

    }



    /**
     * Generate a JHOVE String {@link Property}
     * @param name
     * @param value
     * @return
     */
    private Property generateProperty(String name, String value) {
        if (value != null && value.trim().length() > 0) {
            return new Property(name, PropertyType.STRING, toUtf8(value));
        }
        return null;
    }

    /**
     * Generate a JHOVE Long {@link Property}
     * @param name
     * @param value
     * @param hideIfZero when true the method will return null if the value is zero
     * @return
     */
    private Property generateProperty(String name, Long value, boolean hideIfZero) {
        if (value != null && (!value.equals(0L) || !hideIfZero)) {
            return new Property(name, PropertyType.LONG, value);
        }
        return null;
    }

    /**
     * Generate JHOVE boolean {@link Property}
     * @param name
     * @param value
     * @return
     */
    private Property generateProperty(String name, boolean value) {
        return new Property(name, PropertyType.BOOLEAN, value);
    }

    /**
     * Generate a JHOVE String Array {@link Property}. If the array length=1, the
     * property will be a SCALAR rather than an ARRAY.
     * @param name
     * @param value
     * @return
     */
    private Property generateProperty(String name, String[] value) {
        if (value != null && value.length > 0) {
            for (int i = 0; i < value.length; i++) {
                value[i] = toUtf8(value[i]);
            }
            if (value.length == 1) {
                return new Property(name, PropertyType.STRING, PropertyArity.SCALAR, value[0]);
            } else {
                return new Property(name, PropertyType.STRING, PropertyArity.ARRAY, value);

            }
        }
        return null;
    }

    /**
     * Generate a JHOVE Set {@link Property}
     * @param name
     * @param value
     * @return
     */
    private Property generateProperty(String name, Set<Property> value) {
        if (value != null && value.size() > 0) {
            return new Property(name, PropertyType.PROPERTY, PropertyArity.SET, value);
        }
        return null;
    }

    /**
     * Convert the {@link CheckMessage} format received from the EPUBCheck module to
     * a set of JHOVE {@link Message}s. ERROR, FATAL, or WARNING messages will be
     * converted to {@link ErrorMessage}s while all other messages will be converted
     * to {@link InfoMessage}s
     *
     * @param msg The message from EPUBCheck
     * @return A JHOVE Message
     */
    private Set<Message> toJhoveMessages(CheckMessage msg) {
        Set<Message> msgs = new TreeSet<Message>(new MessageComparator());
        if (msg == null) {
            return msgs;
        }
        Severity severity = msg.getSeverity();
        String msgText = msg.getMessage();
        String msgId = msg.getID();

        if (msg.getLocations().size() > 0) {
            for (EPUBLocation location : msg.getLocations()) {
                addJhoveMessage(msgs, toJhoveMessage(msgId, msgText, severity, location));
            }
        } else {
          addJhoveMessage(msgs, toJhoveMessage(msgId, msgText, severity, null));
        }
        return msgs;
    }

    private static void addJhoveMessage(Set<Message> msgs, Message msg) {
        if (msg == null) {
          return;
        }
        msgs.add(msg);
    }

    /**
     * Convert the properties of a {@link CheckMessage} from the EPUBCheck module to
     * a JHOVE {@link Message}. ERROR, FATAL, or WARNING messages will be converted
     * to {@link ErrorMessage}s while all other messages will be converted to
     * {@link InfoMessage}s
     *
     * @param msgId       Message ID string from EPUBCheck
     * @param messageText Message text from EPUBCheck
     * @param severity    Severity level from EPUBCheck
     * @param location    EPUBLocation from EPUBCheck
     * @return A JHOVE {@link Message}
     */
    private Message toJhoveMessage(String msgId, String messageText, Severity severity, EPUBLocation location) {
        final String divider = ", ";

        String severityText = severity.toString();
        if (severity.equals(Severity.WARNING)) {
            severityText = "WARN";
        }
        String msgText = msgId + divider + severityText + divider + "[" + messageText + "]";

        // append location if there is one
        if (location != null) {
            String loc = "";
            if (location.getLine() > 0 || location.getColumn() > 0) {
                loc = " (" + location.getLine() + "-" + location.getColumn() + ")";
            }
            msgText = msgText + divider + PathUtil.removeWorkingDirectory(location.getPath()) + loc;
        }

        JhoveMessage msg = JhoveMessages.getMessageInstance(msgId, msgText);

        if (severity == Severity.ERROR || severity == Severity.FATAL || severity == Severity.WARNING) {
            return new ErrorMessage(msg);
        } else {
            return new InfoMessage(msg);
        }
    }

    /**
     * Make sure the string contains valid UTF-8 characters
     * @param inputString
     * @return escaped String
     */
    public static String toUtf8(String inputString) {
        byte[] b = inputString.getBytes(StandardCharsets.UTF_8);
        return new String(b, StandardCharsets.UTF_8);
    }

    @Override
    protected void initParse() {
        super.initParse();
    }

    static class PropertyComparator implements Comparator<Property> {
        public int compare(final Property firstProp, final Property secondProp) {
            int compVal = firstProp.getName().compareTo(secondProp.getName());
            return (compVal == 0) ? firstProp.getValue().toString().compareTo(secondProp.getValue().toString()) : compVal;
        }
    }

    static class MessageComparator implements Comparator<Message> {
      public int compare(final Message firstMess, final Message secondMess) {
        int compVal = firstMess.getId().compareTo(secondMess.getId()) ;
        return (compVal == 0) ? firstMess.getMessage().compareTo(secondMess.getMessage()) : compVal;
      }
    }
}
