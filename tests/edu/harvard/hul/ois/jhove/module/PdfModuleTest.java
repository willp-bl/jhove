package edu.harvard.hul.ois.jhove.module;
/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: May 16, 2008
 * Time: 11:50:58 AM
 * To change this template use File | Settings | File Templates.
 */

import junit.framework.*;
import edu.harvard.hul.ois.jhove.module.PdfModule;
import edu.harvard.hul.ois.jhove.RepInfo;

import java.io.IOException;
import java.io.RandomAccessFile;

public class PdfModuleTest extends TestCase {

    PdfModule pdfModule;

    RandomAccessFile raf;
    RepInfo info;



    /**
     * Sets up the fixture, for example, open a network connection. This method
     * is called before a test is executed.
     */
    protected void setUp() throws Exception {
        super.setUp();    //To change body of overridden methods use File | Settings | File Templates.
    }

    /**
     * Tears down the fixture, for example, close a network connection. This
     * method is called after a test is executed.
     */
    protected void tearDown() throws Exception {
        super.tearDown();    //To change body of overridden methods use File | Settings | File Templates.
    }


    public void testParse1() throws Exception {
        raf = new RandomAccessFile("examples/pdf/AA_Banner-single.pdf","r");
        info = new RepInfo("teststtesdss");
        pdfModule = new PdfModule();
        pdfModule.parse(raf, info);

        if (info.getWellFormed() != RepInfo.TRUE){
            fail("PDF no longer checks as wellformed");
        }
        if (info.getValid() != RepInfo.TRUE){
            fail("PDF no longer checks as valid");
        }

    }

    public void testParse2() throws Exception {

        raf = new RandomAccessFile("examples/pdf/AA_Banner.pdf","r");
        info = new RepInfo("teststtesdss");
        pdfModule = new PdfModule();
        pdfModule.parse(raf, info);

        if (info.getWellFormed() != RepInfo.TRUE){
            fail("PDF no longer checks as wellformed");
        }
        if (info.getValid() != RepInfo.FALSE){
            fail("PDF now  checks as valid");
        }



    }

    public void testParse3() throws Exception {



        raf = new RandomAccessFile("examples/pdf/bedfordcompressed.pdf","r");
        info = new RepInfo("teststtesdss");
        pdfModule = new PdfModule();
        pdfModule.parse(raf, info);

        if (info.getWellFormed() != RepInfo.TRUE){
            fail("PDF no longer checks as wellformed");
        }
        if (info.getValid() != RepInfo.TRUE){
            fail("PDF no longer checks as valid");
        }



    }

    public void testParse4() throws Exception {

        raf = new RandomAccessFile("examples/pdf/fallforum03.pdf","r");
        info = new RepInfo("teststtesdss");
        pdfModule = new PdfModule();
        pdfModule.parse(raf, info);

        if (info.getWellFormed() != RepInfo.TRUE){
            fail("PDF no longer checks as wellformed");
        }
        if (info.getValid() != RepInfo.TRUE){
            fail("PDF no longer checks as valid");
        }


    }


    public void testParse5() throws Exception {
        raf = new RandomAccessFile("examples/pdf/imd.pdf","r");
        info = new RepInfo("teststtesdss");
        pdfModule = new PdfModule();
        pdfModule.parse(raf, info);

        if (info.getWellFormed() != RepInfo.TRUE){
            fail("PDF no longer checks as wellformed");
        }
        if (info.getValid() != RepInfo.TRUE){
            fail("PDF no longer checks as valid");
        }



    }





}
